package data.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Logger;

import data.protocol.Message;
import data.protocol.MessageSerializer;
import data.protocol.MessageType;
import presentation.console.Menu;

public class SocketClient {
    private static Logger LOGGER = Logger.getLogger(SocketClient.class.getName());
    private final Scanner sc = new Scanner(System.in);
    private String name;

    public Socket connect() {
        // System.out.print("Hostname: ");
        // String hostname = sc.nextLine();
        // System.out.print("Port: ");
        // Integer port = null;
        try {
            // port = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            LOGGER.info("Port must a number");
            return null;
        }
        try {
            Socket socket = new Socket("localhost", 8080);
            System.out.println("==============================");
            System.out.printf("%18s\n", "SOAPTA");
            System.out.println("==============================");
            System.out.print("\nEnter your display name: ");
            this.name = sc.nextLine();

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            MessageSerializer.write(out, new Message(MessageType.CHAT, name));
            Message res = MessageSerializer.read(in);
            // LOGGER.info(res.getPayload());

            return socket;
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        SocketClient client = new SocketClient();
        try (Socket socket = client.connect()) {
            if (socket == null) {
                return;
            }
            new Menu(socket, client.name).Login(client.name);
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }
    }
}
