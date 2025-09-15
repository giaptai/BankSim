package resources;

public class MyExceptions {
    public static class InsufficientFundsException extends Exception {
        public InsufficientFundsException() {
            super("Your balance is not enough !");
        }

        public InsufficientFundsException(String message) {
            super(message);
        }

        public InsufficientFundsException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class AccountNotFoundException extends Exception {
        public AccountNotFoundException() {
            super("Account not found !!!");
        }

        public AccountNotFoundException(String message) {
            super(message);
        }

        public AccountNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class InvalidAmountException extends Exception {
        public InvalidAmountException() {
            super("Value of money is not valid !");
        }

        public InvalidAmountException(String message) {
            super(message);
        }

        public InvalidAmountException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
