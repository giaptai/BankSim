package test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.Formatter;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import business.service.IBankService;
import resources.Constants;

public class SimRunner {
    private static Logger LOGGER = Logger.getLogger(SimRunner.class.getName());

    private IBankService bankService;

    static {
        loadLogConfig();
    }

    private static void loadLogConfig() {
        try (InputStream is = SimRunner.class.getClassLoader()
                .getResourceAsStream(Constants.LOGGING_PROPERTIES_PATH)) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
                LOGGER.info("Logging configuration loaded successfully.");
            } else {
                LOGGER.warning("WARNING: logging.properties not found. Using default logging configuration.");
            }
        } catch (IOException e) {
            LOGGER.warning("WARNING: Could not load logging.properties: " + e.getMessage());
        }
        // setting log
        try {
            Files.createDirectories(Paths.get("logs"));
            // Tạo tên file log hoàn chỉnh với ngày tháng động (ví dụ: "logs/banksim_2025-09-30_%g.log").
            // Đây là điều mà logging.properties không thể làm trực tiếp
            String datePattern = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String logFileName = "logs/banksim_" + datePattern + "_%g.log";
            FileHandler fileHandler = new FileHandler(logFileName, 5 * 1024 * 1024, 5, true); // 5MB, 5 files, append
            fileHandler.setLevel(Level.ALL);
            Formatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            Logger rootLogger = Logger.getLogger(""); // Logger gốc
            rootLogger.addHandler(fileHandler);
            LOGGER.info("Date-based FileHandler configured successfully.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error setting up date-based FileHandler: " + e.getMessage(), e);
        }
    }

    public SimRunner() {
    }

    public SimRunner(IBankService bankService) {
        this.bankService = bankService;
    }

    // Phương thức cho DEPOSIT và WITHDRAW (chỉ cần 1 accountId)
    public void runConcurrentTransactions(int accountTestId, double amountPerTransaction,
            resources.Type transactionType,
            int totalTransaction) {
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < totalTransaction; i++) {
                switch (transactionType) {
                    case DEPOSIT -> futures.add(bankService.deposit(accountTestId, amountPerTransaction));
                    case WITHDRAW -> futures.add(bankService.withdraw(accountTestId, amountPerTransaction));
                    case TRANSFER -> LOGGER.log(Level.WARNING,
                            "Transfer not fully supported from this runner without a 'toAccountId' input.");
                    default -> LOGGER.log(Level.WARNING, "Unsupported transaction type: " + transactionType);
                }
            }
            LOGGER.info("Submitted " + totalTransaction + " deposit tasks. Waiting for completion...");
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    LOGGER.warning("Error in one of the deposit tasks: " + e.getMessage());
                }
            }
            LOGGER.info("All " + totalTransaction + " " + transactionType.name() + " tasks completed.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Test interrupted: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Test RuntimeException: " + e.getMessage(), e);
        }
    }

    // Phương thức mới cho TRANSFER (cần 2 accountId)
    public void runConcurrentTransfers(int fromAccountId, int toAccountId, double amountPerTransaction,
            int totalTransaction) {
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < totalTransaction; i++) {
                futures.add(bankService.transfer(fromAccountId, toAccountId, amountPerTransaction));
            }
            LOGGER.info("Submitted " + totalTransaction + " TRANSFER tasks. Waiting for completion...");
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.log(Level.WARNING, "Error in one of the TRANSFER tasks: " + e.getMessage(), e);
                }
            }
            LOGGER.info("All " + totalTransaction + " TRANSFER tasks completed.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Transfer test interrupted: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Transfer test RuntimeException: " + e.getMessage(), e);
        }
    }
}