package server.command;

import server.ChatHub;
import server.TextHandler;

public class GroupMsgStrategy implements CommandStrategy {
    @Override
    public void execute(TextHandler sender, String args) {
        String[] data = args.split("\\s+", 2);
        String fromClientId = sender.getClientId();
        String groupName = data[0];
        String msg = data[1];
        ChatHub.getInstance().sendGroupMsg(fromClientId, groupName, msg);
    }
}
