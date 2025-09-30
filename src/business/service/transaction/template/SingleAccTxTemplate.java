package business.service.transaction.template;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import business.service.BankService;
import resources.Type;

import data.IDatabaseManager;
import data.models.Account;
import presentation.ui.ThreadTrackerGUI;
import resources.MyExceptions.AccountNotFoundException;
import resources.MyExceptions.InsufficientFundsException;
import resources.MyExceptions.InvalidAmountException;

public abstract class SingleAccTxTemplate {
    private Logger LOGGER = Logger.getLogger(SingleAccTxTemplate.class.getName());
    protected IDatabaseManager databaseManager;
    protected ThreadTrackerGUI trackerGUI;
    protected BankService bankService;
    protected Connection conn;
    protected String currThreadName;
    protected LocalDateTime startTime;
    protected double amount;
    protected double initialBalance;
    protected double predictedBalance;
    protected int accountId;
    protected Account primaryAccount;

    public SingleAccTxTemplate(IDatabaseManager databaseManager, ThreadTrackerGUI trackerGUI, BankService bankService) {
        this.databaseManager = databaseManager;
        this.trackerGUI = trackerGUI;
        this.bankService = bankService;
    }

    public final void execute() throws InterruptedException, RuntimeException {
        currThreadName = Thread.currentThread().getName();
        startTime = LocalDateTime.now();

        try {
            conn = databaseManager.createHikariConnection();
            conn.setAutoCommit(false);

            primaryAccount = databaseManager.getAccountById(conn, accountId, true);

            if (primaryAccount == null) {
                throw new AccountNotFoundException();
            }

            validateInput();

            initialBalance = primaryAccount.getBalance();
            predictedBalance = calculatePredictedBalanceValue(initialBalance, amount);

            updateGUI("Pending", "");

            performSpecificBusinessLogic();

            saveTransaction();

            conn.commit();

            updateGUI("Completed", "");
        } catch (InsufficientFundsException | InvalidAmountException | AccountNotFoundException e) {
            rollbackAndLog(e, Level.WARNING);
            updateGUI("Failed", e.getMessage());
            throw e;
        } catch (IOException | ClassNotFoundException | SQLException e) {
            rollbackAndLog(e, Level.SEVERE);
            updateGUI("Failed", "System Error");
            throw new RuntimeException("System error during transaction: " + e.getMessage(), e);
        } finally {
            closeConnection();
        }
    }

    protected abstract void validateInput() throws InvalidAmountException, InsufficientFundsException;

    protected abstract double calculatePredictedBalanceValue(double currentInitialBalance, double transactionAmount);

    protected abstract void performSpecificBusinessLogic() throws SQLException, InsufficientFundsException;

    protected abstract void saveTransaction() throws SQLException;

    protected abstract Type getTransactionType();

    protected void updateGUI(String status, String message) {
        if (trackerGUI != null) {
            trackerGUI.updateThreadRow(
                    currThreadName,
                    getTransactionType().name(),
                    String.valueOf(accountId),
                    "N/A",
                    amount,
                    predictedBalance,
                    "Pending".equals(status) ? -1.0 : primaryAccount.getBalance(),
                    startTime,
                    status,
                    message);
        }
    }

    protected void rollbackAndLog(Exception e, Level level) {
        try {
            if (conn != null) {
                conn.rollback();
            }
        } catch (SQLException rollbackEx) {
            LOGGER.log(Level.SEVERE, "Error during rollback: " + rollbackEx.getMessage(), rollbackEx);
        }
        LOGGER.log(level, "Transaction error: " + e.getMessage(), e);
    }

    protected void closeConnection() {
        try {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing connection: " + e.getMessage(), e);
        }
    }
}
