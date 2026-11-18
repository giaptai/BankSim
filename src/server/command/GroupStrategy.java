package server.command;

import server.ChatHub;
import server.TextHandler;

public class GroupStrategy implements CommandStrategy {

    @Override
    public void execute(TextHandler sender, String args) {
        ChatHub hub = ChatHub.getInstance();
        String[] commands = args.split("\\s+", 2);
        if (commands.length == 0) {
            sender.notifyMessage("[ERROR] Unknown /group command. Type /help for assistance.\n");
        }
        String payload = commands.length > 1 ? commands[1].trim() : "";
        switch (commands[0]) {
            case "list" -> hub.getGroups(sender);
            case "my" -> hub.myGroup(sender);
            case "create" -> hub.createGroupNew(sender, payload);
            case "join" -> hub.joinGroupNew(sender, payload);
            case "leave" -> hub.leaveGroupNew(sender, payload);
            case "members" -> {
                sender.notifyMessage("[SYSTEM] Sorry, this feature is not done yet\n");
            }
            case "send" -> hub.sendGroupMsgNew(sender, payload);
            default -> {
                sender.notifyMessage("[ERROR] Unknown /group command. Type /help for assistance.\n");
            }
        }
    }
}
