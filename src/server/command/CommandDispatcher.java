package server.command;

import java.util.HashMap;
import java.util.Map;

import server.ClientHandler;

public class CommandDispatcher {
    private final Map<String, CommandStrategy> commands;
    {
        commands = new HashMap<>();
    }

    public CommandDispatcher() {
        commands.put("/help", new HelpStrategy());
        commands.put("/list", new ListUsersStrategy());
        commands.put("/create", new CreateGroupStrategy());
        commands.put("/join", new JoinGroupStrategy());
        commands.put("/group", new GroupMsgStrategy());
        commands.put("/msg", new PrivateMsgStrategy());
        commands.put("/exit", new ExitStrategy());
    }

    public void dispatch(ClientHandler sender, String rawInput) {
        if (rawInput.isBlank()) {
            return;
        }
        String[] data = rawInput.split("\\s+", 2);
        String command = data[0];
        String args = data.length > 1 ? data[1].trim() : "";
        CommandStrategy strategy = commands.get(command);
        if (strategy != null) {
            strategy.execute(sender, args);
        } else {
            sender.notifyMessage("[ERROR] Unknown command. Type /help for assistance.");
        }
    }
}
