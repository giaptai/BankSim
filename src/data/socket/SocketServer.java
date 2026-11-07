package data.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import data.protocol.Message;
import data.protocol.MessageSerializer;
import data.protocol.MessageType;

public class SocketServer {
    private static final int MAX_CONNECTIONS = 5000;
    private static final int HANDSHAKE_TIMEOUT_MS = 10_000;
    private final ExecutorService connExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore connLimit = new Semaphore(MAX_CONNECTIONS);
    private static final Logger LOGGER = Logger.getLogger(SocketServer.class.getName());
    private final int port;
    private final Map<String, Socket> onlineUsers = new ConcurrentHashMap<>();

    public SocketServer(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket ss = new ServerSocket(port)) {
            LOGGER.info("Server listening on port " + port + " (max " + MAX_CONNECTIONS + " connections)");
            while (!ss.isClosed()) {
                Socket client = ss.accept();
                if (!connLimit.tryAcquire()) {
                    LOGGER.warning("Connection limit reached, rejecting " + client.getRemoteSocketAddress());
                    client.close();
                    continue;
                }
                try {
                    connExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
                            String name = null;
                            try (Socket socket = client) {
                                // socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
                                LOGGER.info("Client connected: " + socket.getRemoteSocketAddress());
                                DataInputStream in = new DataInputStream(socket.getInputStream());
                                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                                Message message = MessageSerializer.read(in);
                                name = message.getPayload();

                                Socket existing = onlineUsers.putIfAbsent(name, socket);

                                if (existing != null) {
                                    MessageSerializer.write(out, new Message(MessageType.CHAT, "ERROR|NAME_TAKEN"));
                                    return;
                                }
                                LOGGER.info(name + " connected.");
                                Message res = new Message(MessageType.CHAT, "Welcome, " + name + "!");
                                MessageSerializer.write(out, res);
                                while (!socket.isClosed()) {
                                    Message incoming = MessageSerializer.read(in);
                                    LOGGER.info(name + ": " + incoming.getPayload());

                                    if (incoming.getPayload().equals("LIST_USERS")) {
                                        List<String> users = _getOnlineusers();
                                        String payload = String.join(", ", users);
                                        MessageSerializer.write(out, new Message(MessageType.CHAT, payload));
                                    } else {
                                        String[] payloads = incoming.getPayload().split(":\\s");
                                        Socket userSocket = onlineUsers.get(payloads[0]);
                                        if (userSocket != null) {
                                            DataOutputStream ops = new DataOutputStream(userSocket.getOutputStream());
                                            synchronized (userSocket) {
                                                MessageSerializer.write(ops,
                                                        new Message(MessageType.CHAT, name + ": " + payloads[1]));
                                            }
                                        }
                                    }
                                }
                            } catch (EOFException e) {
                                LOGGER.info("Client " + name + " disconnected.");
                            } catch (IOException e) {
                                LOGGER.warning("Client error " + e.getMessage());
                            } finally {
                                onlineUsers.remove(name);
                                connLimit.release();
                            }
                        }
                    });
                } catch (RejectedExecutionException e) {
                    connLimit.release();
                    client.close();
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Server stopped due to error: " + e);
        } finally {
            connExecutor.shutdown();
        }
    }

    private List<String> _getOnlineusers() {
        return onlineUsers.keySet().stream().toList();
    }

    public static void main(String[] args) {
        new SocketServer(8080).start();
    }
}
