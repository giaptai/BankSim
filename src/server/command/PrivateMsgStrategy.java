package server.command;

import server.ChatHub;
import server.ClientHandler;

public class PrivateMsgStrategy implements CommandStrategy {

    @Override
    public void execute(ClientHandler sender, String args) {
        String[] data = args.split("\\s+",3);
        String fromClientId = sender.getClientId();
        String toClientId = data[1];
        String msg = data[2];
        ChatHub.getInstance().sendPrivateMsg(fromClientId, toClientId, msg);
    }

}
