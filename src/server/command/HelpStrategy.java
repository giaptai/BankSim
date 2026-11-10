package server.command;

import server.ClientHandler;

public class HelpStrategy implements CommandStrategy {
    private static final String HELP_TEXT = """
            Commands:
                /list                     List all active users
                /create <group>           Create a new group
                /join <group>             Join an existing group
                /group <group> <text>     Send a message to a group
                /msg <clientId> <text>    Send a private 1:1 message
                /exit                     Disconnect from server
            """;

    @Override
    public void execute(ClientHandler sender, String args) {
        sender.notifyMessage(HELP_TEXT);
    }

}
