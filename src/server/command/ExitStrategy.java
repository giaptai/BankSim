package server.command;

import server.ClientHandler;

public class ExitStrategy implements CommandStrategy{

    @Override
    public void execute(ClientHandler sender, String args) {
        sender.notifyMessage("[SYSTEM] Disconnected from server. Goodbye!");
        sender.close();
    }
    
}
