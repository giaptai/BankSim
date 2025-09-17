package test;

import business.service.BankService;
import data.DatabaseManager;
import data.models.Account;
import resources.MyExceptions;
import resources.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DepositTest {

    public static void main(String[] args) {
        testSimpleConcurrentDeposit(); // Gọi phương thức kiểm thử đơn giản
    }

    @Test
    public static void testSimpleConcurrentDeposit() {
        System.out.println("Starting simple concurrent deposit test with new Thread() and join()...");

        DatabaseManager databaseManager = new DatabaseManager();
        BankService bankService = new BankService(databaseManager);

        int accountIdToTest = 1; // Giả sử tài khoản ID 1 đã tồn tại trong cơ sở dữ liệu của bạn
        double depositAmountPerThread = 10.0; // Số tiền mỗi luồng sẽ gửi
        int numberOfThreads = 30; // Số lượng thao tác gửi tiền đồng thời

        try {
            // Lấy số dư ban đầu
            Account initialAccount = databaseManager.getAccountById(accountIdToTest);
            if (initialAccount == null) {
                System.out.println(
                        "Error: Account " + accountIdToTest + " not found. Please ensure it exists in the database.");
                return;
            }
            double initialBalance = initialAccount.getBalance();
            System.out.println(
                    "Initial balance for account " + accountIdToTest + ": " + String.format("%.2f", initialBalance));

            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < numberOfThreads; i++) {
                futures.add(bankService.deposit(accountIdToTest, depositAmountPerThread));
            }

            // Chờ tất cả các luồng hoàn thành
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    e.getCause().printStackTrace();
                }
            }

            // Lấy số dư cuối cùng
            Account finalAccount = databaseManager.getAccountById(accountIdToTest);
            if (finalAccount == null) {
                System.out.println("Error: Account " + accountIdToTest + " not found after deposits.");
                return;
            }
            double finalBalance = finalAccount.getBalance();
            System.out.println(
                    "Final balance for account " + accountIdToTest + ": " + String.format("%.2f", finalBalance));

            double expectedFinalBalance = initialBalance + (depositAmountPerThread * numberOfThreads);

            System.out.println("Expected final balance: " + String.format("%.2f", expectedFinalBalance));

            // So sánh với một sai số nhỏ do có thể có sai số dấu phẩy động
            if (Math.abs(finalBalance - expectedFinalBalance) < 0.001) {
                System.out.println("Test PASSED: Final balance matches expected balance.");
            } else {
                System.out.println("Test FAILED: Final balance (" + String.format("%.2f", finalBalance)
                        + ") does NOT match expected balance (" + String.format("%.2f", expectedFinalBalance) + ").");
            }

        } catch (MyExceptions.AccountNotFoundException e) {
            System.out.println(
                    "Error: Account " + accountIdToTest + " not found. Please ensure it exists in the database.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Test interrupted: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Runtime error during test: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Unexpected error during test setup/teardown: " + e.getMessage());
            e.printStackTrace();
        } finally {
            bankService.close(); // Đảm bảo ExecutorService của BankService được tắt
            System.out.println("BankService shut down.");
        }
    }
}