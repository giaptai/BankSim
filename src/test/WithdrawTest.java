package test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import business.service.BankService;
import data.DatabaseManager;
import data.models.Account;
import resources.annotations.Test;

public class WithdrawTest {
    public static void main(String[] args) {
        testSimpleConcurrentWithdraw();
    }

    @Test
    public static void testSimpleConcurrentWithdraw() {
        DatabaseManager databaseManager = new DatabaseManager();
        BankService bankService = new BankService(databaseManager);

        int accountIdTest = 1;
        double withdrawAmountPerThread = 10;
        int numberOfthread = 30;

        try {
            Account account = databaseManager.getAccountById(accountIdTest);
            if (account == null) {
                return;
            }
            double initialBalance = account.getBalance();
            System.out.printf("Initial balance for account %s: %s\n", accountIdTest,
                    String.format("%.2f", initialBalance));

            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < numberOfthread; i++) {
                futures.add(bankService.withdraw(accountIdTest, withdrawAmountPerThread));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    e.getCause().printStackTrace();
                }
            }
            
            Account finalAccount = databaseManager.getAccountById(accountIdTest);
            if (finalAccount == null) {
                return;
            }
            double finalBalance = finalAccount.getBalance();
            System.out.println(
                    "Final balance for account " + accountIdTest + ": " + String.format("%.2f", finalBalance));
            double expectedFinalBalance = initialBalance - (withdrawAmountPerThread * numberOfthread);
            System.out.println("Expected final balance: " + String.format("%.2f", expectedFinalBalance));
            // So sánh với một sai số nhỏ do có thể có sai số dấu phẩy động
            if (Math.abs(finalBalance - expectedFinalBalance) < 0.001) {
                System.out.println("Test PASSED: Final balance matches expected balance.");
            } else {
                System.out.println("Test FAILED: Final balance (" + String.format("%.2f", finalBalance)
                        + ") does NOT match expected balance (" + String.format("%.2f", expectedFinalBalance) + ").");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            bankService.close();
            System.out.println("BankService shut down.");
        }
    }
}
