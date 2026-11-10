package server.command;

public interface CommandStrategy {
    void update(PrivateMsgStrategy event);
}
