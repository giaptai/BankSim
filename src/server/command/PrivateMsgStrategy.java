package server.command;

import server.ChatHub;
import server.TextHandler;

public class PrivateMsgStrategy implements CommandStrategy {

    @Override
    public void execute(TextHandler sender, String args) {
        String[] data = args.split("\\s+", 2);
        String receiverId = data[0];
        String msg = data[1];
        ChatHub.getInstance().sendPrivateMsg(sender, receiverId, msg);
    }

}
