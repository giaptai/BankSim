package client.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Level;
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

                System.out.println(in.readLine());
                System.out.println(in.readLine());
                System.out.println();

                // tiep nhan tin nhan tu server
                Thread.ofPlatform().daemon(true).start(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String msgFromServer;
                            while ((msgFromServer = in.readLine()) != null) {
                                System.out.print("\r\033[K");
                                System.out.println(msgFromServer);
                                System.out.print("> ");
                            }
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                });
                
                //gui tin toi server
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
                    out.println(input);
                } while (!"/exit".equalsIgnoreCase(input));

            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
            }
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        }
    }

    public static void main(String[] args) {
        new SoaptaClient().start();
    }
}
