package business.service;

import java.util.List;
import java.util.concurrent.Future;

import data.models.Account;
import data.models.Transaction;

public interface IBankService {
    Account openAccount(String ownName, double initialBalance) throws InterruptedException, RuntimeException;

    Future<?> deposit(Integer accountId, double amount) throws InterruptedException, RuntimeException;

    Future<?> withdraw(Integer accountId, double amount) throws InterruptedException, RuntimeException;

    Future<?> transfer(int fromAccountId, int toAccountId, double amount)
            throws InterruptedException, RuntimeException;

    String getAccountDetails(int accountId) throws RuntimeException;

    List<Transaction> getTransactionHistory(int accountId) throws RuntimeException;

    void close();
}
