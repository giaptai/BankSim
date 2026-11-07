package presentation.console;

import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import data.protocol.Message;
import data.protocol.MessageSerializer;
import data.protocol.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.io.IOException;

public class Menu {
    private Scanner sc;
    private final Socket socket;
    private final String name;
    private final Object consoleLock = new Object();

    public Menu(Socket socket, String name) {
        sc = new Scanner(System.in);
        this.socket = socket;
        this.name = name;
    }

    public void Login(String name) {
        System.out.printf("\nWelcome, %s!\n", name);
        while (true) {
            agenda();
        }
    }

    protected void agenda() {
        System.out.println();
        System.out.println("1. Private Chat");
        System.out.println("2. Group Chat");
        System.out.println("3. Create Group");
        System.out.println("4. My Conversations");
        System.out.println("5. Notification");
        System.out.println("6. File Transfer");
        System.out.println("7. Exit");
        Short choose = null;
        while (choose == null || choose < 1 || choose > 7) {
            System.out.print("\nChoose: ");
            choose = sc.nextShort();
            sc.nextLine();
        }

        switch (choose) {
            case 1:
                privateChat();
                break;
            case 2:
                groupChat();
                break;
            case 3:
                System.out.println("Create Group");
                break;
            case 4:
                System.out.println("My Conversations");
                break;
            case 5:
                System.out.println("Notification");
                break;
            case 6:
                fileTransfer();
                break;
            case 7:
                System.exit(0);
        }
    }

    protected void privateChat() {
        String[] users = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        try {
            System.err.println();
            System.out.println("==============================");
            System.out.printf("%21s\n", "PRIVATE CHAT");
            System.out.println("==============================");
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            MessageSerializer.write(out, new Message(MessageType.CHAT, "LIST_USERS"));
            Message res = MessageSerializer.read(in);
            users = res.getPayload().split(", ");
            System.out.printf("\nOnline users: %d\n\n", users.length);
            for (int i = 0; i < users.length; i++) {
                String username = users[i];
                if (this.name.equals(username)) {
                    System.out.printf("%d. %s (You)\n", (i + 1), username);
                } else {
                    System.out.printf("%d. %s\n", (i + 1), username);
                }
            }

            Short select = null;
            while (select == null) {
                System.out.print("\nSelect user: ");
                select = sc.nextShort();
                sc.nextLine();
            }
            String recipient = users[select - 1];
            System.out.printf("\nConnected to, %s\n", recipient);
            System.out.println("\n-----------------------------");
            Thread receiver = new Thread(() -> {
                try {
                    DataInputStream ips = new DataInputStream(socket.getInputStream());

                    while (!socket.isClosed()) {
                        Message incom = MessageSerializer.read(ips);
                        synchronized (consoleLock) {
                            System.out.print("\r\u001B[2K");
                            System.out.println(incom.getPayload());
                            System.out.print("You > ");
                        }
                    }
                } catch (IOException e) {
                }
            });

            receiver.setDaemon(true);
            receiver.start();

            while (true) {
                synchronized (consoleLock) {
                    System.out.print("You > ");
                }
                String content = sc.nextLine();
                if (content.equals("/exit")) {
                    break;
                }
                MessageSerializer.write(out, new Message(MessageType.CHAT, recipient + ": " + content));
            }
        } catch (IOException e) {

        }
    }

    protected void groupChat() {
        System.out.println("==============================");
        System.out.printf("%20s\n", "GROUP CHAT");
        System.out.println("==============================\n");

        System.out.println("1. Java Developers (15 members)");
        System.out.println("2. Backend Team (8 members)");
        System.out.println("3. Gaming (25 members)");

        Short select = null;
        while (select == null) {
            System.out.print("\nSelect group: ");
            select = sc.nextShort();
            sc.nextLine();
        }
        System.out.printf("\nWelcome to, %s!\n", select);
        System.out.println("\n-----------------------------");
    }

    protected void fileTransfer() {
        System.out.println("==============================");
        System.out.printf("%21s\n", "FILE TRANSFER");
        System.out.println("==============================\n");

        System.out.println("1. Send file to user");
        System.out.println("2. Send file to group");
        System.out.println("3. View transfer history");

        Short select = null;
        while (select == null) {
            System.out.print("\nSelect: ");
            select = sc.nextShort();
            sc.nextLine();
        }
        System.out.printf("\nRecipient: %s\n", select);
        System.out.printf("\nFile path: %s\n", "C:\\Files\\document.zip");
        System.out.printf("\nFile size: %s\n", "150MB");
        System.out.println("\nUploading...");

    }

    public static void main(String[] args) {
        // new Menu().Login();
        System.out.println(Thread.activeCount());
    }
}
