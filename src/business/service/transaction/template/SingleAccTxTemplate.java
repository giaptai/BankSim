package business.service.transaction.template;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import business.service.transaction.observer.TransactionEvent;
import resources.Type;
import data.IDatabaseManager;
import data.models.Account;
import resources.MyExceptions.AccountNotFoundException;
import resources.MyExceptions.InsufficientFundsException;
import resources.MyExceptions.InvalidAmountException;
import resources.TransactionStatus;

public abstract class SingleAccTxTemplate {
    private static Logger LOGGER = Logger.getLogger(SingleAccTxTemplate.class.getName());
    protected IDatabaseManager databaseManager;
    protected Connection conn;
    protected String currThreadName;
    protected LocalDateTime startTime;
    protected double amount;
    protected double initialBalance;
    protected double predictedBalance;
    protected int accountId;
    protected Account primaryAccount;

    public SingleAccTxTemplate(IDatabaseManager databaseManager, int accountId, double amount) {
        this.databaseManager = databaseManager;
        this.accountId = accountId;
        this.amount = amount;
    }

    public final TransactionEvent execute() throws InterruptedException, RuntimeException {
        currThreadName = Thread.currentThread().getName();
        startTime = LocalDateTime.now();
        TransactionEvent finalEvent = null;
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

            performSpecificBusinessLogic();

            saveTransaction();

            conn.commit();

            finalEvent = TransactionEvent.builder()
                    .currThreadName(currThreadName)
                    .type(getTransactionType().name())
                    .fromAccountId(String.valueOf(accountId))
                    .toAccountId(null)
                    .amount(amount)
                    .predictedBalance(predictedBalance)
                    .actualBalance(primaryAccount.getBalance())
                    .startTime(startTime)
                    .status(TransactionStatus.COMPLETED)
                    .message("")
                    .build();
        } catch (InsufficientFundsException | InvalidAmountException | AccountNotFoundException e) {
            rollbackAndLog(e, Level.WARNING);
            finalEvent = TransactionEvent.builder()
                    .currThreadName(currThreadName)
                    .type(Type.TRANSFER.name())
                    .fromAccountId(String.valueOf(accountId))
                    .toAccountId(null)
                    .amount(amount)
                    .predictedBalance(predictedBalance)
                    .actualBalance(-1.0)
                    .startTime(startTime)
                    .status(TransactionStatus.FAILED)
                    .message(e.getMessage())
                    .build();
            throw e;
        } catch (IOException | ClassNotFoundException | SQLException e) {
            rollbackAndLog(e, Level.SEVERE);
            finalEvent = TransactionEvent.builder()
                    .currThreadName(currThreadName)
                    .type(Type.TRANSFER.name())
                    .fromAccountId(String.valueOf(accountId))
                    .toAccountId(null)
                    .amount(amount)
                    .predictedBalance(predictedBalance)
                    .actualBalance(-1.0)
                    .startTime(startTime)
                    .status(TransactionStatus.FAILED)
                    .message("System error")
                    .build();
            throw new RuntimeException("System error during transaction: " + e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return finalEvent;
    }

    protected abstract void validateInput() throws InvalidAmountException, InsufficientFundsException;

    protected abstract double calculatePredictedBalanceValue(double currentInitialBalance, double transactionAmount);

    protected abstract void performSpecificBusinessLogic() throws SQLException, InsufficientFundsException;

    protected abstract void saveTransaction() throws SQLException;

    protected abstract Type getTransactionType();

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
