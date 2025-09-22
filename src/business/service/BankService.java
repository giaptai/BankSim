package business.service;

import java.util.Optional;
import java.io.IOException;
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
    private ExecutorService transactionExecutor;
    private DatabaseManager databaseManager;
    private final Map<Integer, Lock> accountLocks = new ConcurrentHashMap<>();
    private ThreadTrackerGUI trackerGUI;
    private final int MAX_THREADS = 13;

    public BankService(DatabaseManager databaseManager) {
        transactionExecutor = Executors.newFixedThreadPool(MAX_THREADS);
        this.databaseManager = databaseManager;
    }

    public BankService(DatabaseManager databaseManager, ThreadTrackerGUI trackerGUI) {
        transactionExecutor = Executors.newFixedThreadPool(MAX_THREADS);
        this.databaseManager = databaseManager;
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
                        Account account = new Account(ownName, initialBalance);
                        return databaseManager.saveAccount(account);
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

    public Future<?> deposit(Integer accountId, double amount) throws InterruptedException, RuntimeException {
        return transactionExecutor.submit(
                () -> {
                    String currentThreadName = Thread.currentThread().getName();
                    LocalDateTime startTime = LocalDateTime.now();
                    double initialBalance = 0;
                    double predictedBalance = 0;

                    try {
                        Account account = databaseManager.getAccountById(accountId);
                        if (account != null) {
                            initialBalance = account.getBalance();
                            predictedBalance = initialBalance + amount;
                        }
                    } catch (IOException | ClassNotFoundException | SQLException e) {
                        System.err.println("Error getting initial balance for deposit: " + e.getMessage());
                    }

                    if (trackerGUI != null) {
                        trackerGUI.updateThreadRow(
                                currentThreadName,
                                Type.DEPOSIT.name(),
                                String.valueOf(accountId),
                                "N/A",
                                amount,
                                predictedBalance,
                                -1.0, // Actual balance chưa có
                                startTime,
                                "Pending",
                                "");
                    }

                    try {
                        System.out.println(Thread.currentThread().getName() + " deposited " + amount);
                        if (amount <= 0) {
                            throw new InvalidAmountException();
                        }
                        Account account = databaseManager.getAccountById(accountId);
                        if (account != null) {
                            databaseManager.adjustAccountBalance(accountId, amount);
                            databaseManager.saveTransaction(new Transaction(accountId, Type.DEPOSIT, amount));
                            // Lấy số dư thực tế sau khi giao dịch hoàn tất
                            Account finalAccount = databaseManager.getAccountById(accountId);
                            if (trackerGUI != null && finalAccount != null) {
                                trackerGUI.updateThreadRow(
                                        currentThreadName,
                                        Type.DEPOSIT.name(),
                                        String.valueOf(accountId),
                                        "N/A",
                                        amount,
                                        predictedBalance,
                                        finalAccount.getBalance(),
                                        startTime,
                                        "Completed",
                                        "");
                            }
                        } else {
                            throw new AccountNotFoundException();
                        }
                    } catch (InvalidAmountException | AccountNotFoundException e) {
                        if (trackerGUI != null) {
                            trackerGUI.updateThreadRow(
                                    currentThreadName,
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
                        if (trackerGUI != null) {
                            trackerGUI.updateThreadRow(
                                    currentThreadName,
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
                        throw new RuntimeException(e);
                    }
                });
        // try {
        // fu.get();
        // } catch (ExecutionException e) {
        // Throwable cause = e.getCause();
        // if (cause instanceof InvalidAmountException) {
        // throw (InvalidAmountException) cause;
        // }
        // if (cause instanceof AccountNotFoundException) {
        // throw (AccountNotFoundException) cause;
        // }
        // if (cause instanceof RuntimeException) {
        // throw (RuntimeException) cause;
        // }
        // throw new RuntimeException("Unexpacted error during deposit: ", cause);
        // } catch (InterruptedException e) {
        // Thread.currentThread().interrupt();
        // throw e;
        // }
    }

    public Future<?> withdraw(Integer accountId, double amount) throws InterruptedException, RuntimeException {
        Future<?> fu = transactionExecutor.submit(
                () -> {
                    try {
                        System.out.println(Thread.currentThread().getName() + " withdrawed " + amount);
                        if (amount <= 0) {
                            throw new InvalidAmountException();
                        }

                        Account account = databaseManager.getAccountById(accountId);
                        if (account != null) {
                            if (account.getBalance() < amount) {
                                throw new InsufficientFundsException();
                            }
                            databaseManager.adjustAccountBalance(accountId, amount * -1);
                            databaseManager.saveTransaction(new Transaction(accountId, Type.WITHDRAW, amount));
                        } else {
                            throw new AccountNotFoundException();
                        }
                    } catch (InsufficientFundsException | InvalidAmountException | AccountNotFoundException e) {
                        throw e;
                    } catch (IOException | ClassNotFoundException | SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
        return fu;
        // try {
        // fu.get();
        // } catch (ExecutionException e) {
        // Throwable cause = e.getCause();
        // if (cause instanceof InsufficientFundsException) {
        // throw (InsufficientFundsException) cause;
        // }
        // if (cause instanceof AccountNotFoundException) {
        // throw (AccountNotFoundException) cause;
        // }
        // if (cause instanceof InvalidAmountException) {
        // throw (InvalidAmountException) cause;
        // }
        // if (cause instanceof RuntimeException) {
        // throw (RuntimeException) cause;
        // }
        // throw new RuntimeException("Unexpacted error during deposit: ", cause);
        // } catch (InterruptedException e) {
        // Thread.currentThread().interrupt();
        // throw e;
        // }
    }

    public void transfer(int fromAccountId, int toAccountId, double amount)
            throws InterruptedException, RuntimeException {
        Future<?> fu = transactionExecutor.submit(
                () -> {
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
                                // Luôn đọc tài khoản từ DB để kiểm tra sự tồn tại và số dư mới nhất
                                Account fromAccount = databaseManager.getAccountById(fromAccountId);
                                Account toAccount = databaseManager.getAccountById(toAccountId);

                                if (fromAccount == null || toAccount == null) {
                                    throw new AccountNotFoundException();
                                }

                                if (fromAccount.getBalance() < amount) {
                                    throw new InsufficientFundsException();
                                }
                                databaseManager.saveTransaction(fromAccount, toAccount, amount);
                            } finally {
                                secondLock.unlock();
                            }
                        } finally {
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
            System.out.println("BankService shutdown interrupted: " + e.getMessage());
        }
    }
}
