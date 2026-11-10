package server.command;

import server.ClientHandler;

@FunctionalInterface
public interface CommandStrategy {
    void execute(ClientHandler sender, String args);
}
