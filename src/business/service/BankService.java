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

import data.DatabaseManager;
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
    private ExecutorService transactionExecutor;
    private DatabaseManager databaseManager;
    private final Map<Integer, Lock> accountLocks = new ConcurrentHashMap<>();
    private ThreadTrackerGUI trackerGUI;
    private final int MAX_THREADS = 100;

    public BankService(DatabaseManager databaseManager) {
        transactionExecutor = Executors.newFixedThreadPool(MAX_THREADS);
        this.databaseManager = databaseManager;
    }

    public BankService(DatabaseManager databaseManager, ThreadTrackerGUI trackerGUI) {
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
                            conn = databaseManager.createConnection();
                            Account account = new Account(ownName, initialBalance);
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
        } finally {

        }
    }

    public Future<?> deposit(Integer accountId, double amount) throws InterruptedException, RuntimeException {
        return transactionExecutor.submit(
                () -> {
                    String currThreadName = Thread.currentThread().getName();
                    LocalDateTime startTime = LocalDateTime.now();
                    double initialBalance = 0;
                    double predictedBalance = 0;
                    Connection conn = null;
                    try {
                        conn = databaseManager.createConnection();
                        conn.setAutoCommit(false);

                        // 1. Call to DB: getAccountById (sử dụng conn)
                        Account account = databaseManager.getAccountById(conn, accountId);
                        if (account != null) {
                            initialBalance = account.getBalance();
                            predictedBalance = initialBalance + amount;
                        } else {
                            throw new AccountNotFoundException();
                        }
                        if (trackerGUI != null) {
                            trackerGUI.updateThreadRow(
                                    currThreadName,
                                    Type.DEPOSIT.name(),
                                    String.valueOf(accountId),
                                    "N/A",
                                    amount,
                                    predictedBalance,
                                    -1.0,
                                    startTime,
                                    "Pending",
                                    "");
                        }
                        //
                        LOGGER.info(currThreadName + " deposited " + amount);
                        if (amount <= 0) {
                            throw new InvalidAmountException();
                        }

                        // 2. Call to DB: adjustAccountBalance (sử dụng conn)
                        databaseManager.adjustAccountBalance(conn, accountId, amount);
                        // 3. Call to DB: saveTransaction (sử dụng conn)
                        databaseManager.saveTransaction(conn, new Transaction(accountId, Type.DEPOSIT, amount));

                        conn.commit();

                        // 4. Call to DB: getAccountById (sử dụng conn)
                        Account finalAccount = databaseManager.getAccountById(conn, accountId);
                        if (trackerGUI != null && finalAccount != null) {
                            trackerGUI.updateThreadRow(
                                    currThreadName,
                                    Type.DEPOSIT.name(),
                                    String.valueOf(accountId),
                                    "N/A",
                                    amount,
                                    predictedBalance,
                                    finalAccount.getBalance(),
                                    LocalDateTime.now(),
                                    "Completed",
                                    "");
                        }
                    } catch (InvalidAmountException | AccountNotFoundException e) {

                        if (conn != null) {
                            try {
                                conn.rollback();
                            } catch (SQLException rollBackEx) {
                                LOGGER.log(Level.WARNING, "Error rolling back transaction: " + rollBackEx.getMessage());
                            }
                        }

                        if (trackerGUI != null) {
                            trackerGUI.updateThreadRow(
                                    currThreadName,
                                    Type.DEPOSIT.name(),
                                    String.valueOf(accountId),
                                    "N/A",
                                    amount,
                                    predictedBalance,
                                    -1.0, // Không có số dư thực tế nếu thất bại
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
                                LOGGER.log(Level.WARNING, "Error rolling back transaction: " + rollBackEx.getMessage());
                            }
                        }
                        if (trackerGUI != null) {
                            trackerGUI.updateThreadRow(
                                    currThreadName,
                                    Type.DEPOSIT.name(),
                                    String.valueOf(accountId),
                                    "N/A",
                                    amount,
                                    predictedBalance,
                                    -1.0, // Không có số dư thực tế nếu thất bại
                                    startTime,
                                    "Failed",
                                    e.getMessage());
                        }
                        LOGGER.log(Level.SEVERE, "System error during deposit: " + e.getMessage(), e);
                        throw new RuntimeException(e);
                    } finally {
                        if (conn != null) {
                            try {
                                conn.setAutoCommit(true);
                                conn.close();
                            } catch (SQLException e) {
                                LOGGER.log(Level.WARNING, "Closed connection: " + e.getMessage(), e);
                            }
                        }
                    }
                });
    }

    public Future<?> withdraw(Integer accountId, double amount) throws InterruptedException, RuntimeException {
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
                        conn = databaseManager.createConnection();
                        conn.setAutoCommit(false);
                        boolean forUpdate = true;
                        Account account = databaseManager.getAccountById(conn, accountId, forUpdate);
                        if (account != null) {
                            if (account.getBalance() < amount) {
                                throw new InsufficientFundsException();
                            }
                            initialBalance = account.getBalance();
                            predictedBalance = initialBalance - amount;
                        } else {
                            throw new AccountNotFoundException();
                        }

                        if (trackerGUI != null) {
                            trackerGUI.updateThreadRow(
                                    currThreadName,
                                    Type.WITHDRAW.name(),
                                    String.valueOf(accountId),
                                    "N/A",
                                    amount,
                                    predictedBalance,
                                    -1.0,
                                    startTime,
                                    "Pending",
                                    "");
                        }

                        databaseManager.adjustAccountBalance(conn, accountId, amount * -1);
                        databaseManager.saveTransaction(conn, new Transaction(accountId, Type.WITHDRAW, amount));
                        conn.commit();
                        Account finalAccount = databaseManager.getAccountById(conn, accountId);
                        if (trackerGUI != null && finalAccount != null) {
                            trackerGUI.updateThreadRow(
                                    currThreadName,
                                    Type.WITHDRAW.name(),
                                    String.valueOf(accountId),
                                    "N/A",
                                    amount,
                                    predictedBalance,
                                    finalAccount.getBalance(),
                                    LocalDateTime.now(),
                                    "Completed",
                                    "");
                        }
                    } catch (InsufficientFundsException | InvalidAmountException | AccountNotFoundException e) {
                        if (conn != null) {
                            try {
                                conn.rollback();
                            } catch (SQLException rollBackEx) {
                                LOGGER.log(Level.WARNING, "Error rolling back transaction: " + rollBackEx.getMessage(), rollBackEx);
                            }
                        }
                        if (trackerGUI != null) {
                            trackerGUI.updateThreadRow(
                                    currThreadName,
                                    Type.WITHDRAW.name(),
                                    String.valueOf(accountId),
                                    "N/A",
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
                                 LOGGER.log(Level.WARNING, "Error rolling back transaction: " + rollBackEx.getMessage(), rollBackEx);
                            }
                        }
                        if (trackerGUI != null) {
                            trackerGUI.updateThreadRow(
                                    currThreadName,
                                    Type.WITHDRAW.name(),
                                    String.valueOf(accountId),
                                    "N/A",
                                    amount,
                                    predictedBalance,
                                    -1.0,
                                    startTime,
                                    "Failed",
                                    e.getMessage());
                        }
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            if (conn != null) {
                                conn.setAutoCommit(true);
                                conn.close();
                            }
                        } catch (SQLException e) {
                            LOGGER.log(Level.WARNING, "Closed connection: " + e.getMessage());
                        }
                    }
                });
        return fu;
    }

    public void transfer(int fromAccountId, int toAccountId, double amount)
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
                                conn = databaseManager.createConnection();
                                conn.setAutoCommit(false);
                                // Luôn đọc tài khoản từ DB để kiểm tra sự tồn tại và số dư mới nhất
                                Account fromAccount = databaseManager.getAccountById(conn, fromAccountId);
                                Account toAccount = databaseManager.getAccountById(conn, toAccountId);

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
                                            String.valueOf(fromAccount),
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
                                LOGGER.log(Level.WARNING, "Error rolling back transaction: " + rollBackEx.getMessage(), rollBackEx);
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
                                LOGGER.log(Level.WARNING, "Error rolling back transaction: " + rollBackEx.getMessage(), rollBackEx);
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
            LOGGER.log(Level.SEVERE, "Unexpected error during transfer: " + e.getMessage(), e);
            throw new RuntimeException("Unexpacted error during tranfer: ", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Transfer interrupted: " + e.getMessage(), e);
            throw e;
        }
    }

    public String getAccountDetails(int accountId) throws RuntimeException {
        Connection conn = null;
        try {
            conn = databaseManager.createConnection();
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
            conn = databaseManager.createConnection();
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
            transactionExecutor.shutdownNow(); // Buộc tắt nếu bị gián đoạn
            Thread.currentThread().interrupt(); // Đặt lại trạng thái ngắt
            LOGGER.log(Level.WARNING, "BankService shutdown interrupted: " + e.getMessage(), e);
        }
    }
}
