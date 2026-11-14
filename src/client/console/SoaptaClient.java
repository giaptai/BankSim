package client.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.logging.Logger;

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

    public SoaptaClient() {
        this.host = "127.0.0.1";
        this.port = 8080;
    }

    public void start() {
        try {
            System.out.printf("Connecting to SOAPTA Server (%s:%d)...\n", this.host, this.port);
            this.socket = new Socket(this.host, this.port);
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
                                    String role = null;
                                    if (msgFromServer.contains("You accepted the file transfer request")) {
                                        role = "RECEIVER";
                                    } else if (msgFromServer.contains("accepted your file transfer request")) {
                                        role = "SENDER";
                                    }
                                    if (role != null) {
                                        String transferId = msgFromServer.substring(
                                                msgFromServer.lastIndexOf("[ID: ") + 5, msgFromServer.lastIndexOf("]"));
                                        connFileServer(transferId, role);
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
            out.println(String.format("/file send %s %d %s\n", receiverId, fileSize, fileName));
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }
    }

    private void connFileServer(String transferId, String role) {
        try {
            Socket socket = new Socket(this.host, 8081);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            out.println(String.format("[%s] %s %s", role, transferId, this.clientId));
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        }
    }

    public static void main(String[] args) {
        new SoaptaClient().start();
    }
}
