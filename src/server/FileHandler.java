package server;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import common.FileTransfer;

public class FileHandler implements Runnable {
    private final Socket socket;
    private final SoaptaServer server;
    // private final static

    public FileHandler(Socket socket, SoaptaServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        ChatHub hub = ChatHub.getInstance();
        String transferId = null;
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            StringBuilder sb = new StringBuilder();
            int b;
            while ((b = in.read()) != -1 && b != '\n') {
                sb.append((char) b);
            }

            // [ROLE] transferId clientId
            String res = sb.toString().trim();
            String[] parts = res.split("\\s+");
            String role = res.substring(parts[0].lastIndexOf("[") + 1, parts[0].lastIndexOf("]"));
            transferId = parts[1];
            String clientId = parts[2];

            FileTransfer transfer = hub.getFileTransfer(transferId);
            if (transfer == null) {
                socket.close();
                return;
            }
            if ("SENDER".equals(role)) {
                if (!clientId.equals(transfer.senderId())) {
                    socket.close();
                    return;
                }
            } else if ("RECEIVER".equals(role)) {
                if (!clientId.equals(transfer.receiverId())) {
                    socket.close();
                    return;
                }
            } else {
                socket.close();
                return;
            }
            CompletableFuture<Void> transDoneFuture = hub.getTransferDoneFuture(transferId);
            if ("RECEIVER".equals(role)) {
                boolean result = hub.getReceiverSocketFuture(transferId).complete(socket);
                transDoneFuture.get(60, TimeUnit.SECONDS);
            } else if ("SENDER".equals(role)) { // send file
                Socket socketReceiver = hub.getReceiverSocketFuture(transferId).get(60, TimeUnit.SECONDS);
                OutputStream out = socketReceiver.getOutputStream();
                long remaining = transfer.fileSize();
                byte[] data = new byte[16384];
                while (remaining > 0) {
                    int read = in.read(data, 0, (int) Math.min(data.length, remaining));
                    // no more data
                    if (read == -1) {
                        throw new EOFException("Sender disconnected early");
                    }
                    out.write(data, 0, read);
                    remaining -= read;
                }
                out.flush();
                transDoneFuture.complete(null);
            }

        } catch (ExecutionException | TimeoutException | InterruptedException | IOException e) {
            if (transferId != null) {
                hub.getTransferDoneFuture(transferId).completeExceptionally(e);
            }
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
            }
            if (transferId != null) {
                hub.cleanupFileFutures(transferId);
            }
            transferId = null;
            server.releaseConn();
        }
    }
}
