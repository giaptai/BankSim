package business.service.transaction.template;

import java.sql.SQLException;

import data.IDatabaseManager;
import data.models.Transaction;
import resources.MyExceptions.InsufficientFundsException;
import resources.MyExceptions.InvalidAmountException;
import resources.Type;

public class WithdrawProcessor extends SingleAccTxTemplate {

    public WithdrawProcessor(IDatabaseManager databaseManager,
            int accountId, double amount) {
        super(databaseManager,accountId, amount);
    }

    @Override
    protected void validateInput() throws InvalidAmountException, InsufficientFundsException {
        if (this.amount <= 0) {
            throw new InvalidAmountException();
        }

        if (primaryAccount.getBalance() < this.amount) {
            throw new InsufficientFundsException();
        }
    }

    @Override
    protected double calculatePredictedBalanceValue(double currentInitialBalance, double transactionAmount) {
        return currentInitialBalance - transactionAmount;
    }

    @Override
    protected void performSpecificBusinessLogic() throws SQLException, InsufficientFundsException {
        databaseManager.adjustAccountBalance(conn, accountId, -amount);
        primaryAccount.setBalance(primaryAccount.getBalance() - amount);
    }

    @Override
    protected void saveTransaction() throws SQLException {
        databaseManager.saveTransaction(conn, new Transaction(accountId, getTransactionType(), amount));
    }

    @Override
    protected Type getTransactionType() {
        return Type.WITHDRAW;
    }

}
