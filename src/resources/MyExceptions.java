package resources;

public class MyExceptions {
    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException() {
            super("Your balance is not enough !");
        }

        public InsufficientFundsException(String message) { // Thêm constructor này
            super(message);
        }
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException() {
            super("Account not found !!!");
        }

        public AccountNotFoundException(String message) { // Thêm constructor này
            super(message);
        }
    }

    public static class InvalidAmountException extends RuntimeException {
        public InvalidAmountException() {
            super("Value of money is not valid !");
        }

        public InvalidAmountException(String message) { // Thêm constructor này
            super(message);
        }
    }
}
