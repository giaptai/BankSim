package business.service;

import java.util.Optional;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import business.service.transaction.template.DepositProcessor;
import business.service.transaction.template.SingleAccTxTemplate;
import business.service.transaction.template.WithdrawProcessor;
import data.IDatabaseManager;
import data.models.Account;
import data.models.Transaction;
import presentation.ui.ThreadTrackerGUI;

import java.util.concurrent.TimeUnit;

import resources.MyExceptions;
import resources.MyExceptions.InvalidAmountException;
import resources.MyExceptions.AccountNotFoundException;
import resources.MyExceptions.InsufficientFundsException;
import resources.Type;
import resources.annotations.Service;

@Service
public class BankService {
    private Logger LOGGER = Logger.getLogger(BankService.class.getName());
    private static ExecutorService transactionExecutor;
    private IDatabaseManager databaseManager;
    private final Map<Integer, Lock> accountLocks = new ConcurrentHashMap<>();
    private ThreadTrackerGUI trackerGUI;
    private final int MAX_THREADS = 100;

    private BankService(IDatabaseManager databaseManager) {
        if (transactionExecutor == null) {
            transactionExecutor = Executors.newFixedThreadPool(MAX_THREADS);
        }
        this.databaseManager = databaseManager;
    }

    public BankService(IDatabaseManager databaseManager, ThreadTrackerGUI trackerGUI) {
        this(databaseManager);
        this.trackerGUI = trackerGUI;
    }

    /**
     * Open Account
     * 
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
                        Connection conn = null;
                        try {
                            // conn = databaseManager.createConnection();
                            conn = databaseManager.createHikariConnection();
                            // Account account = new Account(ownName, initialBalance);
                            Account account = Account.builder()
                                    .ownerName(ownName)
                                    .balance(initialBalance)
                                    .build();

                            return databaseManager.saveAccount(conn, account);
                        } finally {
                            if (conn != null) {
                                try {
                                    conn.setAutoCommit(true);
                                    conn.close();
                                } catch (SQLException e) {
                                    LOGGER.log(Level.WARNING,
                                            "Error closing connection in openAccount: " + e.getMessage());
                                }
                            }
                        }
                    }
                });

        try {
            return fu.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InvalidAmountException) {
                throw (InvalidAmountException) cause;
            }
            LOGGER.log(Level.WARNING, cause.getMessage(), cause);
            throw new RuntimeException("Error opening account: ", e);
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    public Future<?> deposit(Integer accountId, double amount) throws InterruptedException, RuntimeException {
        return transactionExecutor.submit(
                () -> {
                    SingleAccTxTemplate depositProcessor = new DepositProcessor(databaseManager, trackerGUI, this,
                            accountId, amount);
                    try {
                        depositProcessor.execute();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOGGER.log(Level.WARNING, "Deposit task interrupted: " + e.getMessage(), e);
                        throw new RuntimeException("Deposit task interrupted", e);
                    } catch (RuntimeException e) {
                        LOGGER.log(Level.WARNING, "Deposit task failed: " + e.getMessage(), e);
                        throw e;
                    }
                });
    }

    public Future<?> withdraw(Integer accountId, double amount) throws InterruptedException, RuntimeException {
        Future<?> fu = transactionExecutor.submit(
                () -> {
                    SingleAccTxTemplate withdrawProcessor = new WithdrawProcessor(databaseManager, trackerGUI, this,
                            accountId, amount);
                    try {
                        withdrawProcessor.execute();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOGGER.log(Level.WARNING, "Withdraw task interrupted: " + e.getMessage(), e);
                        throw new RuntimeException("Withdraw task interrupted", e);
                    } catch (RuntimeException e) {
                        LOGGER.log(Level.WARNING, "Withdraw task failed: " + e.getMessage(), e);
                        throw e;
                    }
                });
        return fu;
    }

    public Future<?> transfer(int fromAccountId, int toAccountId, double amount)
            throws InterruptedException, RuntimeException {
        Future<?> fu = transactionExecutor.submit(
                () -> {
                    String currThreadName = Thread.currentThread().getName();
                    LocalDateTime startTime = LocalDateTime.now();
                    double initialBalance = 0.0;
                    double predictedBalance = 0.0;
                    Connection conn = null;
                    try {
                        if (amount <= 0) {
                            throw new InvalidAmountException();
                        }
                        if (fromAccountId == toAccountId) {
                            throw new InvalidAmountException("Cannot transfer to the same account.");
                        }

                        Lock fromLock = accountLocks.computeIfAbsent(fromAccountId, k -> new ReentrantLock());
                        Lock toLock = accountLocks.computeIfAbsent(toAccountId, k -> new ReentrantLock());

                        // Lock theo thứ tự để tránh deadlock
                        Lock firstLock, secondLock;
                        if (fromAccountId < toAccountId) {
                            firstLock = fromLock;
                            secondLock = toLock;
                        } else {
                            firstLock = toLock;
                            secondLock = fromLock;
                        }

                        firstLock.lock();
                        try {
                            secondLock.lock();
                            try {
                                // conn = databaseManager.createConnection();
                                conn = databaseManager.createHikariConnection();
                                conn.setAutoCommit(false);
                                // Luôn đọc tài khoản từ DB để kiểm tra sự tồn tại và số dư mới nhất
                                Account fromAccount = databaseManager.getAccountById(conn, fromAccountId, true);
                                Account toAccount = databaseManager.getAccountById(conn, toAccountId, true);

                                if (fromAccount == null || toAccount == null) {
                                    throw new AccountNotFoundException();
                                }
                                if (fromAccount.getBalance() < amount) {
                                    throw new InsufficientFundsException();
                                }

                                initialBalance = toAccount.getBalance();
                                predictedBalance = initialBalance + amount;

                                if (trackerGUI != null) {
                                    trackerGUI.updateThreadRow(
                                            currThreadName,
                                            Type.TRANSFER.name(),
                                            String.valueOf(fromAccountId),
                                            String.valueOf(toAccountId),
                                            amount,
                                            predictedBalance,
                                            -1.0,
                                            startTime,
                                            "Pending",
                                            "");
                                }

                                databaseManager.saveTransaction(conn, fromAccount, toAccount, amount);
                                conn.commit();
                                Account finalAccount = databaseManager.getAccountById(conn, toAccountId);
                                if (trackerGUI != null && finalAccount != null) {
                                    trackerGUI.updateThreadRow(
                                            currThreadName,
                                            Type.TRANSFER.name(),
                                            String.valueOf(fromAccountId),
                                            String.valueOf(toAccountId),
                                            amount,
                                            predictedBalance,
                                            finalAccount.getBalance(),
                                            LocalDateTime.now(),
                                            "Completed",
                                            "");
                                }
                            } finally {
                                secondLock.unlock();
                            }
                        } finally {
                            firstLock.unlock();
                        }
                    } catch (AccountNotFoundException | InsufficientFundsException | InvalidAmountException e) {
                        if (conn != null) {
                            try {
                                conn.rollback();
                            } catch (SQLException rollBackEx) {
                                LOGGER.log(Level.WARNING, "Error rolling back transaction: " + rollBackEx.getMessage(),
                                        rollBackEx);
                            }
                        }
                        if (trackerGUI != null) {
                            trackerGUI.updateThreadRow(
                                    currThreadName,
                                    Type.TRANSFER.name(),
                                    String.valueOf(fromAccountId),
                                    String.valueOf(toAccountId),
                                    amount,
                                    predictedBalance,
                                    -1.0,
                                    startTime,
                                    "Failed",
                                    e.getMessage());
                        }
                        throw e;
                    } catch (IOException | ClassNotFoundException | SQLException e) {
                        if (conn != null) {
                            try {
                                conn.rollback();
                            } catch (SQLException rollBackEx) {
                                LOGGER.log(Level.WARNING, "Error rolling back transaction: " + rollBackEx.getMessage(),
                                        rollBackEx);
                            }
                        }
                        if (trackerGUI != null) {
                            trackerGUI.updateThreadRow(
                                    currThreadName,
                                    Type.TRANSFER.name(),
                                    String.valueOf(fromAccountId),
                                    String.valueOf(toAccountId),
                                    amount,
                                    predictedBalance,
                                    -1.0,
                                    startTime,
                                    "Failed",
                                    e.getMessage());
                        }
                        LOGGER.log(Level.SEVERE, "System error during transfer: " + e.getMessage(), e);
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            if (conn != null) {
                                conn.setAutoCommit(true);
                                conn.close();
                            }
                        } catch (SQLException e) {
                            LOGGER.log(Level.WARNING, "Closed connection: " + e.getMessage(), e);
                        }
                    }
                });
        return fu;
    }

    public String getAccountDetails(int accountId) throws RuntimeException {
        Connection conn = null;
        try {
            conn = databaseManager.createHikariConnection();
            return Optional.ofNullable(databaseManager.getAccountById(conn, accountId)).map(Account::toString)
                    .orElseThrow(() -> new MyExceptions.AccountNotFoundException());
        } catch (ClassNotFoundException | IOException | AccountNotFoundException | SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting account details: " + e.getMessage(), e);
            throw new RuntimeException("Error getting account details: ", e);
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Closed connection: " + e.getMessage(), e);
            }
        }
    }

    public List<Transaction> getTransactionHistory(int accountId) throws RuntimeException {
        Connection conn = null;
        List<Transaction> transactions = new ArrayList<>();
        try {
            conn = databaseManager.createHikariConnection();
            transactions = databaseManager.getTransactionsByAccountId(conn, accountId);
            return transactions;
        } catch (ClassNotFoundException | IOException | SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting transaction history: " + e.getMessage(), e);
            throw new RuntimeException("Error getting transaction history: ", e);
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Closed connection: " + e.getMessage(), e);
            }
        }
    }

    public void close() {
        transactionExecutor.shutdown();
        try {
            if (!transactionExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                transactionExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            transactionExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "BankService shutdown interrupted: " + e.getMessage(), e);
        }
        databaseManager.close();
    }
}
