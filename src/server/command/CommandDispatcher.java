package server.command;

public interface CommandDispatcher {
    public abstract void attach(CommandStrategy observer);

    public abstract void detach(CommandStrategy observer);

    void notifyObservers(PrivateMsgStrategy event);
}
