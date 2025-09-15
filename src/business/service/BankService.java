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
import resources.Type;
import resources.annotations.Service;

@Service
public class BankService {
    private Map<Integer, Account> accounts;
    private ExecutorService transactionExecutor;
    private DatabaseManager databaseManager;

    public BankService(DatabaseManager databaseManager) {
        accounts = new ConcurrentHashMap<>();
        transactionExecutor = Executors.newFixedThreadPool(7);
        // transactionExcutor =
        // Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.databaseManager = databaseManager;
    }

    public Account openAccount(String ownName, double initialBalance)
            throws InvalidAmountException, ExecutionException, InterruptedException, SQLException, IOException,
            ClassNotFoundException {
        if (initialBalance < 0) {
            throw new InvalidAmountException("The initial balance is not negative value");
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
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof ClassNotFoundException) {
                throw (ClassNotFoundException) cause;
            }
            if (cause instanceof SQLException) {
                throw (SQLException) cause;
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    public void deposit(Integer accountId, double amount)
            throws InvalidAmountException, AccountNotFoundException, InterruptedException, ExecutionException {
        Future<?> fu = transactionExecutor.submit(
                () -> {
                    try {
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
                        throw new RuntimeException(e);
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
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    public void withdraw(Integer accountId, double amount) {
        transactionExecutor.submit(
                () -> {
                    try {
                        Account account = databaseManager.getAccountById(accountId);
                        if (account != null) {
                            account.withdraw(amount);
                            databaseManager.updateAccount(account);
                            databaseManager.saveTransaction(new Transaction(accountId, Type.WITHDRAW, amount));
                            accounts.put(account.getAccountId(), account);
                        } else {
                            throw new MyExceptions.AccountNotFoundException();
                        }
                    } catch (MyExceptions.InsufficientFundsException | MyExceptions.InvalidAmountException
                            | MyExceptions.AccountNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IOException | ClassNotFoundException | SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public Future<?> transfer(int fromAccountId, int toAccountId, double amount) {
        return transactionExecutor.submit(
                () -> {
                    try {
                        Account fromAccount = databaseManager.getAccountById(fromAccountId);
                        Account toAccount = databaseManager.getAccountById(toAccountId);

                        if (fromAccount == null || toAccount == null) {
                            throw new MyExceptions.AccountNotFoundException();
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
                                throw new MyExceptions.InsufficientFundsException();
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
                    } catch (MyExceptions.AccountNotFoundException | MyExceptions.InsufficientFundsException
                            | MyExceptions.InvalidAmountException e) {
                        throw new RuntimeException(e);
                    } catch (IOException | ClassNotFoundException | SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public String getAccountDetails(int accountId) {
        try {
            return Optional.ofNullable(databaseManager.getAccountById(accountId)).map(Account::toString)
                    .orElseThrow(() -> new MyExceptions.AccountNotFoundException());
        } catch (ClassNotFoundException | AccountNotFoundException | IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Transaction> getTransactionHistory(int accountId) {
        List<Transaction> transactions = new ArrayList<>();
        try {
            transactions = databaseManager.getTransactionsByAccountId(accountId);
        } catch (ClassNotFoundException | IOException | SQLException e) {
            throw new RuntimeException(e);
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
            e.printStackTrace(System.out);
            System.err.println(e.getMessage());
        }
    }
}
