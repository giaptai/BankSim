package models;

import java.util.concurrent.locks.Lock;

public class Account {
    private String accountId;
    private String ownerName;
    private double balance;
    private Lock accountLock;

    public Account() {
    }

    public Account(String accountId, String ownerName, double balance) {
        this.accountId = accountId;
        this.ownerName = ownerName;
        this.balance = balance;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public double getBalance() {
        return balance;
    }

    public Lock getAccountLock() {
        return accountLock;
    }

    public void deposit() {

    }

    public void withdraw() {

    }

}
