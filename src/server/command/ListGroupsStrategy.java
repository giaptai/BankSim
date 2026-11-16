package server.command;

import server.ChatHub;
import server.TextHandler;
import java.util.List;

public class ListGroupsStrategy implements CommandStrategy {

    @Override
    public void execute(TextHandler sender, String args) {
        List<String> groups = ChatHub.getInstance().getGroupsSummary();
        if(groups.isEmpty()){
            sender.notifyMessage("Active Groups (0): No groups created yet. Use /create <group> to start one.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Active Groups (%s):\n", groups.size()));
        for(String group : groups){
            sb.append("- ").append(group).append("\n");
        }
        sender.notifyMessage(sb.toString().trim());
    }
    
}
