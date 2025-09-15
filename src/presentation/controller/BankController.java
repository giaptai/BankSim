package presentation.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import business.service.BankService;

import data.models.Account;
import data.models.Transaction;

import presentation.console.Menu;
import resources.MyExceptions.AccountNotFoundException;
import resources.MyExceptions.InvalidAmountException;

public class BankController {
    private BankService bankService;
    private Menu menu;

    public BankController(BankService bankService, Menu menu) {
        this.bankService = bankService;
        this.menu = menu;
    }

    public void start() {
        menu.displayWelcome();
        int choice;

        do {
            menu.displayMainMenu();
            choice = menu.getMenuChoice();
            try {
                switch (choice) {
                    case 1:
                        this.handleOpenAccount();
                        break;
                    case 2:
                        this.handleDeposit();
                        break;
                    case 3:
                        this.handleWithdraw();
                        break;
                    case 4:
                        this.handleTransfer();
                        break;
                    case 5:
                        this.handleGetAccountDetails();
                        break;
                    case 6:
                        this.handleGetTransactionHistory();
                        break;
                    case 0:
                        menu.displayGoodbye();
                        break;
                    default:
                        menu.displayErrorMessage("Wrongc choice valid. Please try again !");
                }
            } catch (ClassNotFoundException | InvalidAmountException | ExecutionException | InterruptedException
                    | SQLException | IOException | AccountNotFoundException e) {
                e.printStackTrace();
            }
        } while (choice != 0);

        bankService.close();
    }

    /**
     * Xử lý logic khi người dùng chọn mở tài khoản mới.
     */
    private void handleOpenAccount() throws InvalidAmountException, ExecutionException, InterruptedException,
            SQLException, IOException, ClassNotFoundException {
        String ownerName = menu.getOwnerNameInput();
        double initialBalance = menu.getInitialBalanceInput();
        Account newAccount = bankService.openAccount(ownerName, initialBalance);
        if (newAccount != null) {
            menu.displaySuccessMessage("Open new account successfully");
        } else {
            menu.displayErrorMessage("Can't open new account. Try again");
        }
    }

    /**
     * Xử lý logic khi người dùng chọn gửi tiền.
     */
    private void handleDeposit()
            throws ExecutionException, InvalidAmountException, AccountNotFoundException, InterruptedException {
        int accountId = menu.getAccountIdInput();
        double amount = menu.getAmountInput();
        bankService.deposit(accountId, amount);
        menu.displaySuccessMessage("Deposited money successfully !");
    }

    /**
     * Xử lý logic khi người dùng chọn rút tiền.
     */
    private void handleWithdraw() {
        int accountId = menu.getAccountIdInput();
        double amount = menu.getAmountInput();
        bankService.withdraw(accountId, amount);
        menu.displaySuccessMessage("Withdrawed money successfully !");
    }

    /**
     * Xử lý logic khi người dùng chọn chuyển khoản.
     */
    private void handleTransfer() {
        int fromAccountId = menu.getFromAccountIdInput();
        int toAccountId = menu.getToAccountIdInput();
        double amount = menu.getAmountInput();
        bankService.transfer(fromAccountId, toAccountId, amount);
        menu.displaySuccessMessage("Transfered from" + fromAccountId + "to" + toAccountId + "successfully !");

    }

    /**
     * Xử lý logic khi người dùng chọn xem chi tiết tài khoản.
     */
    private void handleGetAccountDetails() {
        int accountId = menu.getAccountIdInput();
        String details = bankService.getAccountDetails(accountId);
        menu.displayAccountDetails(details);
    }

    /**
     * Xử lý logic khi người dùng chọn xem lịch sử giao dịch.
     */
    private void handleGetTransactionHistory() {
        int accountId = menu.getAccountIdInput();
        List<Transaction> history = bankService.getTransactionHistory(accountId);
        menu.displayTransactionHistory(history);
    }
}
