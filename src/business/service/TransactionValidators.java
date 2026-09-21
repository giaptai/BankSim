package business.service;

/**
 * Utility class providing pre-defined transaction validators.
 * <p>
 * This class cannot be instantiated and provides static factory methods
 * for creating common transaction validation rules.
 * 
 * @see ITransactionValidator
 */
public final class TransactionValidators {
    private TransactionValidators() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    public static final ITransactionValidator POSITIVE_AMOUNT = (balance, amount) -> amount > 0;
    public static final ITransactionValidator SUFFICIENT_FUNDS = (balance, amount) -> balance >= amount;
}
