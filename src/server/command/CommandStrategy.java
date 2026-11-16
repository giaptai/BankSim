package server.command;

import server.TextHandler;

@FunctionalInterface
public interface CommandStrategy {
    void execute(TextHandler sender, String args);
}
