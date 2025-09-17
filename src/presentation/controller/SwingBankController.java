package presentation.controller;

import business.service.BankService;
import resources.MyExceptions;
import resources.MyExceptions.InvalidAmountException;

public class SwingBankController {
    private BankService bankService;

    public SwingBankController(BankService bankService) {
        this.bankService = bankService;
    }

    /**
     * Phương thức để mở tài khoản
     */
    public String openAccount(String ownerName, double initialBalance) {
        try {
            bankService.openAccount(ownerName, initialBalance);
            return "SUCCESS: Open new account";
        } catch (InvalidAmountException e) {
            return "ERROR: " + e.getMessage();
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            return "ERROR: System error when opening account: " + (cause != null ? cause.getMessage() : e.getMessage());
        } catch (Exception e) {
            return "ERROR: Unknown error when opening account: " + e.getMessage();
        }
    }

    /**
     * Phương thức để gửi tiền
     */
    public String deposit(int accountId, double amount) {
        try {
            // Loại bỏ khối try-catch lồng nhau và gọi trực tiếp bankService
            bankService.deposit(accountId, amount);
            return "SUCCESS: Sent " + amount + " into account " + accountId + " successfully.";
        } catch (MyExceptions.InvalidAmountException | MyExceptions.AccountNotFoundException e) { // Business exceptions
            return "ERROR: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "ERROR: Deposit transaction is interrupted.";
        } catch (RuntimeException e) { // Bắt các RuntimeException khác (technical errors từ BankService)
            e.printStackTrace();
            return "ERROR: System error when depositing money: " + e.getMessage();
        }
    }

    // Phương thức để rút tiền
    public String withdraw(int accountId, double amount) {
        try {
            // Loại bỏ khối try-catch lồng nhau và gọi trực tiếp bankService
            bankService.withdraw(accountId, amount);
            return "SUCCESS: Withdrawn " + amount + " from account " + accountId + " successfully.";
        } catch (MyExceptions.InvalidAmountException | MyExceptions.AccountNotFoundException
                | MyExceptions.InsufficientFundsException e) { // Business exceptions
            return "ERROR: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "ERROR: Withdraw transaction is interrupted.";
        } catch (RuntimeException e) { // Bắt các RuntimeException khác (technical errors từ BankService)
            e.printStackTrace();
            return "ERROR: System error when withdrawing money: " + e.getMessage();
        }
    }

    // Phương thức để chuyển khoản
    public String transfer(int fromAccountId, int toAccountId, double amount) {
        try {
            bankService.transfer(fromAccountId, toAccountId, amount);
            return "SUCCESS: Transferred  " + amount + " from account " + fromAccountId + " to account " + toAccountId
                    + " successfully";
        } catch (MyExceptions.InvalidAmountException | MyExceptions.AccountNotFoundException
                | MyExceptions.InsufficientFundsException e) { // Business exceptions
            return "ERROR: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "ERROR: The transfer transaction was interrupted.";
        } catch (RuntimeException e) { // Bắt các RuntimeException khác (technical errors từ BankService)
            e.printStackTrace();
            return "ERROR: System error when transferring money: " + e.getMessage();
        }
    }

    // Phương thức để xem số dư
    public String getAccountDetails(int accountId) {
        try {
            return "SUCCESS: " + bankService.getAccountDetails(accountId);
        } catch (MyExceptions.AccountNotFoundException e) { // Business exception
            return "ERROR: " + e.getMessage();
        } catch (RuntimeException e) { // Bắt các RuntimeException khác (technical errors từ BankService)
            e.printStackTrace(); // Log lỗi hệ thống
            return "ERROR: System error when viewing balance: " + e.getMessage();
        }
    }

    // Phương thức mới để xem lịch sử giao dịch
    public String getTransactionHistory(int accountId) {
        try {
            return "SUCCESS: " + bankService.getTransactionHistory(accountId);
        } catch (MyExceptions.AccountNotFoundException e) {
            return "ERROR: " + e.getMessage();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return "ERROR: System error when getting transaction history: " + e.getMessage();
        }
    }
}
