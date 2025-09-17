package business.service;

import java.util.Map;
import java.util.Optional;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import data.DatabaseManager;
import data.models.Account;
import data.models.Transaction;

import java.util.concurrent.TimeUnit;

import resources.MyExceptions;
import resources.MyExceptions.InvalidAmountException;
import resources.MyExceptions.AccountNotFoundException;
import resources.MyExceptions.InsufficientFundsException;
import resources.Type;
import resources.annotations.Service;

@Service
public class BankService {
    private Map<Integer, Account> accounts;
    private ExecutorService transactionExecutor;
    private DatabaseManager databaseManager;

    public BankService(DatabaseManager databaseManager) {
        accounts = new ConcurrentHashMap<>();
        transactionExecutor = Executors.newFixedThreadPool(13);
        this.databaseManager = databaseManager;
    }

    /**
     * Open Account
     * @param ownName
     * @param initialBalance
     * @return
     * @throws InterruptedException
     * @throws RuntimeException
     */
    public Account openAccount(String ownName, double initialBalance) throws InterruptedException, RuntimeException {
        if (initialBalance < 0) {
            throw new InvalidAmountException();
        }
        Future<Account> fu = transactionExecutor.submit(
                new Callable<Account>() {
                    @Override
                    public Account call() throws IOException, ClassNotFoundException, SQLException {
                        Account account = new Account(ownName, initialBalance);
                        Account saved = databaseManager.saveAccount(account);
                        if (saved != null) {
                            accounts.put(saved.getAccountId(), saved);
                        }
                        return saved;
                    }
                });

        try {
            return fu.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InvalidAmountException) {
                throw (InvalidAmountException) cause;
            }
            throw new RuntimeException("Error opening account: ", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    public void deposit(Integer accountId, double amount) throws InterruptedException, RuntimeException {
        Future<?> fu = transactionExecutor.submit(
                () -> {
                    try {
                        if (amount < 0) {
                            throw new InvalidAmountException();
                        }
                        Account account = databaseManager.getAccountById(accountId);
                        if (account != null) {
                            account.deposit(amount);
                            databaseManager.updateAccount(account);
                            databaseManager.saveTransaction(new Transaction(accountId, Type.DEPOSIT, amount));
                            accounts.put(account.getAccountId(), account);
                        } else {
                            throw new AccountNotFoundException();
                        }
                    } catch (InvalidAmountException | AccountNotFoundException e) {
                        throw e;
                    } catch (IOException | ClassNotFoundException | SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
        try {
            fu.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InvalidAmountException) {
                throw (InvalidAmountException) cause;
            }
            if (cause instanceof AccountNotFoundException) {
                throw (AccountNotFoundException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Unexpacted error during deposit: ", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    public void withdraw(Integer accountId, double amount) throws InterruptedException, RuntimeException {
        Future<?> fu = transactionExecutor.submit(
                () -> {
                    try {
                        if (amount < 0) {
                            throw new InvalidAmountException();
                        }

                        Account account = databaseManager.getAccountById(accountId);
                        if (account != null) {
                            if (account.getBalance() < amount) {
                                throw new InsufficientFundsException();
                            }
                            account.withdraw(amount);
                            databaseManager.updateAccount(account);
                            databaseManager.saveTransaction(new Transaction(accountId, Type.WITHDRAW, amount));
                            accounts.put(account.getAccountId(), account);
                        } else {
                            throw new AccountNotFoundException();
                        }
                    } catch (InsufficientFundsException | InvalidAmountException | AccountNotFoundException e) {
                        throw e;
                    } catch (IOException | ClassNotFoundException | SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
        try {
            fu.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InsufficientFundsException) {
                throw (InsufficientFundsException) cause;
            }
            if (cause instanceof AccountNotFoundException) {
                throw (AccountNotFoundException) cause;
            }
            if (cause instanceof InvalidAmountException) {
                throw (InvalidAmountException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Unexpacted error during deposit: ", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    public void transfer(int fromAccountId, int toAccountId, double amount)
            throws InterruptedException, RuntimeException {
        Future<?> fu = transactionExecutor.submit(
                () -> {
                    try {
                        if (amount < 0) {
                            throw new InvalidAmountException();
                        }
                        Account fromAccount = databaseManager.getAccountById(fromAccountId);
                        Account toAccount = databaseManager.getAccountById(toAccountId);

                        if (fromAccount == null || toAccount == null) {
                            throw new AccountNotFoundException();
                        }

                        // Lock theo thứ tự để tránh deadlock
                        Lock firstLock, secondLock;

                        if (fromAccount.getAccountId() < toAccount.getAccountId()) {
                            firstLock = fromAccount.getAccountLock();
                            secondLock = toAccount.getAccountLock();
                        } else {
                            firstLock = toAccount.getAccountLock();
                            secondLock = fromAccount.getAccountLock();
                        }

                        firstLock.lock();
                        secondLock.lock();

                        try {
                            if (fromAccount.getBalance() < amount) {
                                throw new InsufficientFundsException();
                            }
                            fromAccount.withdraw(amount);
                            toAccount.deposit(amount);
                            databaseManager.saveTransaction(fromAccount, toAccount, amount);
                            accounts.put(fromAccount.getAccountId(), fromAccount);
                            accounts.put(toAccount.getAccountId(), toAccount);
                        } finally {
                            secondLock.unlock();
                            firstLock.unlock();
                        }
                    } catch (AccountNotFoundException | InsufficientFundsException | InvalidAmountException e) {
                        throw e;
                    } catch (IOException | ClassNotFoundException | SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
        try {
            fu.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AccountNotFoundException) {
                throw (AccountNotFoundException) cause;
            }
            if (cause instanceof InsufficientFundsException) {
                throw (InsufficientFundsException) cause;
            }
            if (cause instanceof InvalidAmountException) {
                throw (InvalidAmountException) cause;
            }
            throw new RuntimeException("Unexpacted error during tranfer: ", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    public String getAccountDetails(int accountId) throws RuntimeException {
        try {
            return Optional.ofNullable(databaseManager.getAccountById(accountId)).map(Account::toString)
                    .orElseThrow(() -> new MyExceptions.AccountNotFoundException());
        } catch (ClassNotFoundException | AccountNotFoundException | IOException | SQLException e) {
            throw new RuntimeException("Error getting account details: ", e);
        }
    }

    public List<Transaction> getTransactionHistory(int accountId) throws RuntimeException {
        List<Transaction> transactions = new ArrayList<>();
        try {
            transactions = databaseManager.getTransactionsByAccountId(accountId);
        } catch (ClassNotFoundException | IOException | SQLException e) {
            throw new RuntimeException("Error getting transaction history: ", e);
        }
        return transactions;
    }

    public void close() {
        transactionExecutor.shutdown();
        try {
            if (!transactionExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                transactionExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            transactionExecutor.shutdownNow(); // Buộc tắt nếu bị gián đoạn
            Thread.currentThread().interrupt(); // Đặt lại trạng thái ngắt
            System.err.println("BankService shutdown interrupted: " + e.getMessage());
        }
    }
}
