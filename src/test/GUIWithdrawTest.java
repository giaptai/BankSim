package test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import business.service.BankService;
import data.DatabaseManager;
import presentation.ui.ThreadTrackerGUI;

public class GUIWithdrawTest {
    private static Logger LOGGER = Logger.getLogger(GUIWithdrawTest.class.getName());
    public static void main(String[] args) {
        DatabaseManager databaseManager = new DatabaseManager();

        ThreadTrackerGUI trackerGUI = new ThreadTrackerGUI();
        trackerGUI.setVisible(true);
        BankService bankService = new BankService(databaseManager, trackerGUI);

        int accountTestId = 1;
        int amountPerTransaction = 10;
        int totalTransaction = 10_000;

        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < totalTransaction; i++) {
                futures.add(bankService.withdraw(accountTestId, amountPerTransaction));
            }
            //
            LOGGER.info("Submitted " + totalTransaction + " withdraw tasks. Waiting for completion...");
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    LOGGER.warning("Error in one of the withdraw tasks: " + e.getMessage());
                }
            }
            LOGGER.info("All withdraw tasks completed.");
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