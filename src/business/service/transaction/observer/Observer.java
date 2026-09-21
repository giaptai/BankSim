package business.service.transaction.observer;

public interface Observer {
    void update(TransactionEvent event);
}
