package models;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Bank {
    private Map<String, Account> accounts;
    private ExecutorService transactionExcutor;
    private DatabaseManager dbManager;

    public Bank() {
        accounts = new ConcurrentHashMap<>();
        transactionExcutor = Executors.newFixedThreadPool(10);
    }

    public Account openAccount(String ownName, double initialBalance) {
        return new Account();
    }

    public void deposit(String accountId, double amount){

    }

    public void withdraw(String accountId, double amount){
        
    }

    public void transfer(String fromAccountId, String toAccountId, double amount){

    }

    public String getAccountDetails(String accountId){
        return null;
    }

    public List<Transaction> getTransactionHistory(String accountId){
        return null;
    }
}
