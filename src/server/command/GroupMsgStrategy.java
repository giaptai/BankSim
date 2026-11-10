package server.command;

import java.sql.SQLException;

import common.MessageType;
import common.MyExceptions.InsufficientFundsException;
import common.MyExceptions.InvalidAmountException;
import data.Group;
import data.models.ClientHandler;

public class GroupMsgStrategy extends CreateGroupStrategy {

    public GroupMsgStrategy(Group databaseManager, int accountId, double amount) {
        super(databaseManager, accountId, amount);
    }

    @Override
    protected void validateInput() throws InvalidAmountException {
        if (this.amount <= 0) {
            throw new InvalidAmountException();
        }
    }

    @Override
    protected double calculatePredictedBalanceValue(double currentInitialBalance, double transactionAmount) {
        return currentInitialBalance + transactionAmount;
    }

    @Override
    protected void performSpecificBusinessLogic() throws SQLException, InsufficientFundsException {
        databaseManager.adjustAccountBalance(conn, accountId, amount);
        primaryAccount.setBalance(primaryAccount.getBalance() + amount);
    }

    @Override
    protected void saveTransaction() throws SQLException {
        databaseManager.saveTransaction(conn, new ClientHandler(accountId, getTransactionType(), amount));
    }

    @Override
    protected MessageType getTransactionType() {
        return MessageType.DEPOSIT;
    }

}
