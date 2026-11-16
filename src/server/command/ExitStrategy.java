package server.command;

import server.TextHandler;

public class ExitStrategy implements CommandStrategy{
    @Override
    public void execute(TextHandler sender, String args) {
        sender.notifyMessage("[SYSTEM] Disconnected from server. Goodbye!");
    }
}
