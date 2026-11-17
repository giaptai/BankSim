package client.console;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import common.constants.AppConstants;

/**
 * Nhap du lieu toi server
 * va hien thu du lieu tren console
 */
public class SoaptaClient {
    private static final Logger LOGGER = Logger.getLogger(SoaptaClient.class.getName());
    private final String host;
    private final int port;
    private Socket socket;
    private String clientId;
    private final ConcurrentMap<String, Path> pendingUploads = new ConcurrentHashMap<>();
    private final Queue<Path> sendQueue = new ConcurrentLinkedDeque<>();
    private final ConcurrentMap<String, String> pendingDownloads = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> pendingDownloadSizes = new ConcurrentHashMap<>();

    public SoaptaClient() {
        try (Scanner sc = new Scanner(System.in)) {
            String ip = "";
            String port = "";
            do {
                System.out.print("Enter host IP: ");
                ip = sc.nextLine();
                if (!ip.matches(AppConstants.VALIDATE_IP)) {
                    System.out.println("Invalid host ip!");
                    continue;
                }
                System.out.print("Enter host port: ");
                port = sc.nextLine();
                if (Integer.parseInt(port) < 0 || Integer.parseInt(port) > 65535) {
                    System.out.println("Invalid host port!");
                    continue;
                }
                this.host = ip;
                this.port = Integer.parseInt(port);
                break;
            } while (true);
        }
    }

    public void start() {
        try {
            System.out.printf("Connecting to SOAPTA Server (%s:%d)...\n", this.host, this.port);
            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(this.host, this.port), 300000);
            try (
                    Scanner sc = new Scanner(System.in);
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
                System.out.println("Connected!\n");

                System.out.print("Enter display name: ");
                String displayName = sc.nextLine();
                out.println(displayName);

                String welcome = in.readLine();
                String help = in.readLine();

                System.out.println(welcome);
                System.out.println(help);
                System.out.println();
                this.clientId = welcome.substring(welcome.lastIndexOf(" ") + 1);

                // receive message from server
                Thread.ofPlatform().daemon(true).start(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String msgFromServer;
                            while ((msgFromServer = in.readLine()) != null) {
                                System.out.print("\r\033[K");
                                System.out.println(msgFromServer);
                                if (msgFromServer.startsWith("[FILE]")) {
                                    if (msgFromServer.contains("File request sent to")) {
                                        String transferId = msgFromServer.substring(
                                                msgFromServer.lastIndexOf("[ID: ") + 5, msgFromServer.lastIndexOf("]"));
                                        Path path = sendQueue.poll();
                                        if (path != null) {
                                            pendingUploads.put(transferId, path);
                                        }
                                    }
                                    String role = null;
                                    if (msgFromServer.contains("You accepted the file transfer request")) {
                                        role = "RECEIVER";
                                    } else if (msgFromServer.contains("accepted your file transfer request")) {
                                        role = "SENDER";
                                    }
                                    if (role != null) {
                                        String transferId = msgFromServer.substring(
                                                msgFromServer.lastIndexOf("[ID: ") + 5, msgFromServer.lastIndexOf("]"));
                                        if ("RECEIVER".equals(role)) {
                                            String fName = msgFromServer.substring(
                                                    msgFromServer.indexOf("for '") + 5,
                                                    msgFromServer.lastIndexOf("' ("));
                                            long fileSize = Long.parseLong(
                                                    msgFromServer.substring(
                                                            msgFromServer.lastIndexOf("(") + 1,
                                                            msgFromServer.lastIndexOf("bytes)") - 1));
                                            pendingDownloads.put(transferId, fName);
                                            pendingDownloadSizes.put(transferId, fileSize);
                                        }
                                        String finalRole = role;
                                        // open connect to file server
                                        Thread.ofVirtual().start(() -> connFileServer(transferId, finalRole));
                                    }
                                }
                                if (msgFromServer.contains("Disconnected from server")) {
                                    break;
                                }
                                System.out.print("> ");
                            }
                        } catch (IOException e) {
                            System.out.println("SoapClient: " + e.getMessage());
                        }
                    }
                });

                // send message to server
                String input = "";
                do {
                    System.out.print("> ");
                    if (!sc.hasNextLine()) {
                        break;
                    }
                    input = sc.nextLine().trim();
                    if (input.isEmpty()) {
                        continue;
                    }
                    if (input.startsWith("/file send ")) {
                        sendFile(input, out);
                    } else {
                        out.println(input);
                    }
                } while (!"/exit".equalsIgnoreCase(input));
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
            }
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        }
    }

    private void sendFile(String input, PrintWriter out) {
        String subArgs = input.substring("/file send".length()).trim();
        String[] parts = subArgs.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println("[ERROR] Usage: /file send <clientId> <filePath>");
            return;
        }
        String receiverId = parts[0];
        String filePath = parts[1];
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("[ERROR] File does not exist.");
                return;
            } else if (file.isDirectory()) {
                System.out.println("[ERROR] Invalid file path.");
                return;
            }
            long fileSize = Files.size(file.toPath());
            if (fileSize > 150L * 1024 * 1024) {
                System.out.println("[ERROR] File size exceeds the 150 MB limit.");
                return;
            } else if (fileSize == 0) {
                System.out.println("[ERROR] Cannot send an empty file.");
                return;
            }
            String fileName = file.getName();
            sendQueue.offer(file.toPath());
            out.println(String.format("/file send %s %d %s", receiverId, fileSize, fileName));
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }
    }

    private void connFileServer(String transferId, String role) {
        Socket socket = null;
        try {
            socket = new Socket(this.host, 8081);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            out.println(String.format("[%s] %s %s", role, transferId, this.clientId));
            out.flush();

            // handle from server
            if ("RECEIVER".equals(role)) {
                String fileName = pendingDownloads.remove(transferId);
                Long fileSize = pendingDownloadSizes.remove(transferId);
                if (fileName == null || fileSize == null) {
                    throw new IOException("Missing file transfer information.");
                }
                Path savePath = Path.of(System.getProperty("user.home"), "Downloads", transferId + "_" + fileName);
                try (InputStream in = socket.getInputStream();
                        OutputStream fos = Files.newOutputStream(savePath)) {
                    byte[] buffer = new byte[16384];
                    long remaining = fileSize;
                    while (remaining > 0) {
                        int bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (bytesRead == -1) {
                            throw new EOFException("File transfer interrupted.");
                        }
                        fos.write(buffer, 0, bytesRead);
                        remaining -= bytesRead;
                    }
                    fos.flush();
                }
                System.out.print("\r\033[K");
                System.out.println("[SYSTEM] Download completed: " + savePath);
                System.out.print("> ");
            } else if ("SENDER".equals(role)) {
                Path localPath = pendingUploads.remove(transferId);
                if (localPath == null) {
                    System.out.println("[ERROR] Cannot find the file to upload.");
                    return;
                }
                try (InputStream inStream = Files.newInputStream(localPath);
                        OutputStream outStream = socket.getOutputStream()) {
                    byte[] buffer = new byte[16384];
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                    outStream.flush();
                }
            }
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public static void main(String[] args) {
        new SoaptaClient().start();
    }
}
