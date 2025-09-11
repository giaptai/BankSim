package models;

import java.time.LocalDateTime;

import resources.Type;

public class Transaction {
    private String transactionId;
    private String accountId;
    private Type type;
    private double amount;
    private LocalDateTime timestamp;

    public Transaction(String accountId, Type type, double amount){
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountId() {
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
}
