package business.service.transaction.observer;

public interface Subject {
    public abstract void attach(Observer observer);

    public abstract void detach(Observer observer);

    void notifyObservers(TransactionEvent event);
}
