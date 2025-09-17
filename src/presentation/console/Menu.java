package presentation.console;

import java.util.List;
import java.util.Scanner;

import data.models.Transaction;

public class Menu {
    private Scanner sc;

    public Menu() {
        sc = new Scanner(System.in);
    }

    public void displayWelcome() {
        System.out.println("\n===============================");
        System.out.println("| >>>>>>>>> Bank Sim <<<<<<<< |");
        System.out.println("===============================");
        System.out.println("Welcome to the BankSim!");
    }

    public void displayMainMenu() {
        System.out.println("""
                \n===============================
                | >>>>>>>>> Bank Sim <<<<<<<< |
                |-----------------------------|
                | 1. Open New account         |
                | 2. Deposit                  |
                | 3. Withdraw                 |
                | 4. Tranfer                  |
                | 5. See detail account       |
                | 6. See history transaction  |
                | 0. Exit                     |
                ===============================\n
                """);
    }

    public void displayGoodbye() {
        System.out.println("Thank you for using BankSim. Goodbye!");
    }

    public void displaySuccessMessage(String message) {
        System.out.println("Success: " + message);
    }

    public void displayErrorMessage(String message) {
        System.out.println("Error: " + message);
    }

    public void displayAccountDetails(String details) {
        System.out.println("\n--- Account Details ---");
        System.out.println(details);
        System.out.println("-----------------------");
    }

    public void displayTransactionHistory(List<Transaction> transactions) {
        System.out.println("\n--- Transaction History ---");
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
        } else {
            for (Transaction t : transactions) {
                System.out.println(t.toString());
            }
        }
        System.out.println("---------------------------");
    }

    /**
     * Thu thập lựa chọn menu từ người dùng.
     * Đảm bảo người dùng nhập một số nguyên hợp lệ.
     * 
     * @return Lựa chọn của người dùng.
     */
    public int getMenuChoice() {
        System.out.print("Enter your choice: ");
        while (!sc.hasNextInt()) {
            System.out.println("Wrong input");
            sc.next();
            System.out.print("Enter your choice: ");
        }
        int choice = sc.nextInt();
        sc.nextLine();
        return choice;
    }

    /**
     * Thu thập tên chủ tài khoản từ người dùng.
     * 
     * @return Tên chủ tài khoản.
     */
    public String getOwnerNameInput() {
        System.out.print("Enter account name: ");
        return sc.nextLine();
    }

    /**
     * Thu thập số dư ban đầu từ người dùng.
     * Đảm bảo người dùng nhập một số thực hợp lệ.
     * 
     * @return Số dư ban đầu.
     */
    public double getInitialBalanceInput() {
        System.out.print("Enter inital balance: ");
        while (!sc.hasNextDouble()) {
            System.out.println("Wrong input");
            sc.next();
            System.out.print("Enter inital balance: ");
        }
        double balance = sc.nextDouble();
        sc.nextLine();
        return balance;
    }

    /**
     * Thu thập ID tài khoản từ người dùng.
     * Đảm bảo người dùng nhập một số nguyên hợp lệ.
     * 
     * @return ID tài khoản.
     */
    public int getAccountIdInput() {
        System.out.print("Enter account Id: ");
        while (!sc.hasNextInt()) {
            System.out.println("Wrong input");
            sc.next();
            System.out.print("Enter account Id: ");
        }

        int id = sc.nextInt();
        sc.nextLine();
        return id;
    }

    /**
     * Thu thập số tiền từ người dùng (cho gửi/rút/chuyển khoản).
     * Đảm bảo người dùng nhập một số thực hợp lệ.
     * 
     * @return Số tiền.
     */
    public double getAmountInput() {
        System.out.print("Enter amount: ");
        while (!sc.hasNextDouble()) {
            System.out.println("Wrong input");
            sc.next();
            System.out.print("Enter amount: ");
        }
        double amount = sc.nextDouble();
        sc.nextLine();
        return amount;
    }

    /**
     * Thu thập ID tài khoản nguồn cho giao dịch chuyển khoản.
     * 
     * @return ID tài khoản nguồn.
     */
    public int getFromAccountIdInput() {
        System.out.print("Enter from account Id: ");
        while (!sc.hasNextInt()) {
            System.out.println("Wrong input");
            sc.next();
            System.out.print("Enter from account Id: ");
        }

        int fromAccountId = sc.nextInt();
        sc.nextLine();
        return fromAccountId;
    }

    /**
     * Thu thập ID tài khoản đích cho giao dịch chuyển khoản.
     * 
     * @return ID tài khoản đích.
     */
    public int getToAccountIdInput() {
        System.out.print("Enter to account Id: ");
        while (!sc.hasNextInt()) {
            System.out.println("Wrong input");
            sc.next();
            System.out.print("Enter to account Id: ");
        }

        int toAccountId = sc.nextInt();
        sc.nextLine();
        return toAccountId;
    }
}
