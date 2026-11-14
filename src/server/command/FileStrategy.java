package server.command;

import server.ChatHub;
import server.ClientHandler;

public class FileStrategy implements CommandStrategy {

    @Override
    public void execute(ClientHandler sender, String args) {
        if (args == null || args.isBlank()) {
            sender.notifyMessage("[ERROR] Usage: /file <send|accept|decline|requests>");
            return;
        }
        String[] parts = args.split("\\s+", 2);
        String subArgs = parts.length > 1 ? parts[1].trim() : ""; // thông tin file
        ChatHub hub = ChatHub.getInstance();
        switch (parts[0].toLowerCase()) {
            case "accept":
                hub.fileAccept(sender, subArgs);
                break;
            case "decline":
                hub.fileDecline(sender, subArgs);
                break;
            case "requests":
                hub.fileRequest(sender, subArgs);
                break;
            case "send":
                hub.fileSend(sender, subArgs);
                break;
            default:
                sender.notifyMessage("[ERROR] Unknown command. Type /help for assistance.");
                break;
        }
    }
}