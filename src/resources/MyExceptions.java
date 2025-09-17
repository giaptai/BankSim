package resources;

public class MyExceptions {
    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException() {
            super("Your balance is not enough !");
        }
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException() {
            super("Account not found !!!");
        }
    }

    public static class InvalidAmountException extends RuntimeException {
        public InvalidAmountException() {
            super("Value of money is not valid !");
        }
    }
}
