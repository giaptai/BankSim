package data.models;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import resources.MyExceptions;

public class Account {
    private int accountId;
    private String ownerName;
    private double balance;
    private Lock accountLock = new ReentrantLock();

    public Account() {
    }

    public Account(int accountId, String ownerName, double balance) {
        this.accountId = accountId;
        this.ownerName = ownerName;
        this.balance = balance;
    }

    public Account(String ownerName, double balance) {
        this.ownerName = ownerName;
        this.balance = balance;
    }

    public int getAccountId() {
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

    public void deposit(Double amount) {
        accountLock.lock();
        try {
            this.balance += amount;
        } finally {
            accountLock.unlock();
        }
    }

    public void withdraw(Double amount) {
        accountLock.lock();
        try {
            this.balance -= amount;
        } finally {
            accountLock.unlock();
        }
    }

    @Override
    public String toString() {
        return "Account [" + "Account Id = " + accountId + ", Owner Name = " + ownerName + ", balance = " + balance
                + "]";
    }
}
