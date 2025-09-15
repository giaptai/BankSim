package data.models;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import resources.MyExceptions;

public class Account {
    private int accountId;
    private String ownerName;
    private double balance;
    private Lock accountLock;

    public Account() {
        this.accountLock = new ReentrantLock();
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

    public void deposit(Double amount) throws MyExceptions.InvalidAmountException{
        accountLock.lock();
        try {
            if (amount < 0) {
                throw new MyExceptions.InvalidAmountException();
            } else {
                this.balance += amount;
            }
        } finally {
            accountLock.unlock();
        }
    }

    public void withdraw(Double amount) throws MyExceptions.InvalidAmountException, MyExceptions.InsufficientFundsException{
        accountLock.lock();
        try {
            if (amount < 0){
                throw new MyExceptions.InvalidAmountException();
            }
            if (this.balance < amount) {
                throw new MyExceptions.InsufficientFundsException();
            } else {
                this.balance -= amount;
            }
        } finally {
            accountLock.unlock();
        }
    }

}
