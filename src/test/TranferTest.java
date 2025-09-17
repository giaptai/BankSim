package test;

import java.util.ArrayList;
import java.util.List;

import business.service.BankService;
import data.DatabaseManager;
import data.models.Account;
import resources.annotations.Test;

public class TranferTest {
    public static void main(String[] args) {
        concurrentTranser();
    }

    @Test
    public static void concurrentTranser() {
        DatabaseManager databaseManager = new DatabaseManager();
        BankService bankService = new BankService(databaseManager);

        int fromAccountId = 1;
        int toAccountId = 2;
        double tranferAmountPerThread = 10;
        int numberOfThreads = 10;

        try {
            Account initialfromAccount = databaseManager.getAccountById(fromAccountId);
            Account initialtoAccount = databaseManager.getAccountById(toAccountId);

            if (initialfromAccount == null || initialtoAccount == null) {
                return;
            }
            double initialfromAccountBalance = initialfromAccount.getBalance();
            double initialtoAccountBalance = initialtoAccount.getBalance();

            System.out.println(
                    "Initial balance for account " + fromAccountId + ": "
                            + String.format("%.2f", initialfromAccountBalance));
            System.out.println(
                    "Initial balance for account " + toAccountId + ": "
                            + String.format("%.2f", initialtoAccountBalance));

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < numberOfThreads; i++) {
                Thread thread = new Thread(() -> {
                    try {
                        bankService.transfer(fromAccountId, toAccountId, tranferAmountPerThread);
                        System.out.println(Thread.currentThread().getName() + " tranfered " + tranferAmountPerThread);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }, "TranferThread-" + i);
                threads.add(thread);
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
            Account finalfromAccount = databaseManager.getAccountById(fromAccountId);
            Account finaltoAccount = databaseManager.getAccountById(toAccountId);
            if (finalfromAccount == null || finaltoAccount == null) {
                return;
            }
            double finalfromBalance = finalfromAccount.getBalance();
            double finaltoBalance = finaltoAccount.getBalance();
            System.out.println(
                    "Final balance for account " + fromAccountId + ": " + String.format("%.2f", finalfromBalance));
            System.out.println(
                    "Final balance for account " + toAccountId + ": " + String.format("%.2f", finaltoBalance));
            double expectedFinalfromBalance = initialfromAccountBalance - (tranferAmountPerThread * numberOfThreads);
            double expectedFinaltoBalance = initialtoAccountBalance + (tranferAmountPerThread * numberOfThreads);
            System.out.println("Expected final from balance: " + String.format("%.2f", expectedFinalfromBalance));
            System.out.println("Expected final to balance: " + String.format("%.2f", expectedFinaltoBalance));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Unexpected error during test setup/teardown: " + e.getMessage());
            e.printStackTrace();
        } finally {
            bankService.close();
            System.out.println("BankService shut down.");
        }
    }
}
