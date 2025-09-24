package test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

import business.service.BankService;
import data.DatabaseManager;
import presentation.ui.ThreadTrackerGUI;

public class GUIDepositTest {
    private static Logger LOGGER = Logger.getLogger(GUIDepositTest.class.getName());

    public static void main(String[] args) {

        try (InputStream inp = GUIDepositTest.class.getClassLoader()
                .getResourceAsStream("resources/logging.properties")) {
            if (inp != null) {
                // download all configurations
                LogManager.getLogManager().readConfiguration(inp);
                LOGGER.info("Logging configuration loaded successfully.");
            } else {
                System.err.println("WARNING: logging.properties not found. Using default logging configuration.");
            }
        } catch (IOException e) {
            System.err.println("WARNING: Could not load logging.properties: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            Files.createDirectories(Paths.get("logs"));
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

        DatabaseManager databaseManager = new DatabaseManager();

        ThreadTrackerGUI trackerGUI = new ThreadTrackerGUI();
        trackerGUI.setVisible(true);
        BankService bankService = new BankService(databaseManager, trackerGUI);

        int accountTestId = 1;
        int amountPerTransaction = 10;
        int totalTransaction = 1000;

        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < totalTransaction; i++) {
                futures.add(
                        bankService.deposit(accountTestId, amountPerTransaction));
            }

            //
            LOGGER.info("Submitted " + totalTransaction + " deposit tasks. Waiting for completion...");
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    LOGGER.warning("Error in one of the deposit tasks: " + e.getMessage());
                }
            }
            LOGGER.info("All deposit tasks completed.");
            //

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warning("Test interrupted: " + e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.warning("Text RuntimeException: " + e.getMessage());
        } finally {
            bankService.close();
        }
    }
}