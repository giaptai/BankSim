package data.models;

import java.time.LocalDateTime;

import resources.Type;

public class Transaction {
    private String transactionId;
    private int accountId;
    private Type type;
    private double amount;
    private LocalDateTime timestamp;

    // for save
    public Transaction(int accountId, Type type, double amount) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public Transaction(String transactionId, int accountId, Type type, double amount, LocalDateTime timstamp) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timstamp;
    }

    public Transaction(int accountId, Type type, double amount, LocalDateTime timstamp) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timstamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public int getAccountId() {
        return accountId;
    }

    public Type getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Transaction [transactionId=" + transactionId + ", accountId=" + accountId + ", type=" + type
                + ", amount=" + amount + ", timestamp=" + timestamp + "]\n";
    }

}
