package test;

import business.service.BankService;
import data.DatabaseManager;
import data.models.Account; // Import Account để lấy balance
import resources.MyExceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestMultithread {

    public static void main(String[] args) {
        testConcurrentDeposit();
    }

    public static void testConcurrentDeposit() {
        System.out.println("Starting concurrent deposit test...");

        DatabaseManager databaseManager = new DatabaseManager();

        BankService bankService = new BankService(databaseManager);

        int accountIdToTest = 1; // Giả sử tài khoản ID 1 đã tồn tại trong cơ sở dữ liệu của bạn
        double depositAmountPerThread = 10.0; // Số tiền mỗi luồng sẽ gửi
        int numberOfThreads = 5000; // Số lượng thao tác gửi tiền đồng thời

        try {
            // Lấy số dư ban đầu
            Account initialAccount = databaseManager.getAccountById(accountIdToTest);
            if (initialAccount == null) {
                System.out.println("Error: Account " + accountIdToTest + " not found. Please ensure it exists in the database.");
                return;
            }
            double initialBalance = initialAccount.getBalance();
            System.out.println("Initial balance for account " + accountIdToTest + ": " + String.format("%.2f", initialBalance));

            // Tạo một thread pool cho các tác vụ kiểm tra
            ExecutorService executor = Executors.newFixedThreadPool(10); 
            List<Callable<Void>> tasks = new ArrayList<>();

            for (int i = 0; i < numberOfThreads; i++) {
                tasks.add(() -> {
                    try {
                        bankService.deposit(accountIdToTest, depositAmountPerThread);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println(Thread.currentThread().getName() + " interrupted during deposit: " + e.getMessage());
                    } catch (RuntimeException e) {
                        System.out.println(Thread.currentThread().getName() + " failed to deposit: " + e.getMessage());
                        e.printStackTrace(); // In stack trace để gỡ lỗi
                    }
                    return null;
                });
            }

            // Thực thi tất cả các tác vụ và chờ chúng hoàn thành
            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> future : futures) {
                try {
                    future.get(); // Điều này sẽ ném lại bất kỳ ngoại lệ nào xảy ra trong Callable
                } catch (Exception e) {
                    System.out.println("Error in one of the deposit tasks: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            executor.shutdown();
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.out.println("Executor for test did not terminate in the specified time. Forcing shutdown.");
                executor.shutdownNow();
            }

            // Lấy số dư cuối cùng
            Account finalAccount = databaseManager.getAccountById(accountIdToTest);
            if (finalAccount == null) {
                System.out.println("Error: Account " + accountIdToTest + " not found after deposits.");
                return;
            }
            double finalBalance = finalAccount.getBalance();
            System.out.println("Final balance for account " + accountIdToTest + ": " + String.format("%.2f", finalBalance));

            double expectedFinalBalance = initialBalance + (depositAmountPerThread * numberOfThreads);

            System.out.println("Expected final balance: " + String.format("%.2f", expectedFinalBalance));

            // So sánh với một sai số nhỏ do có thể có sai số dấu phẩy động
            if (Math.abs(finalBalance - expectedFinalBalance) < 0.001) {
                System.out.println("Test PASSED: Final balance matches expected balance.");
            } else {
                System.out.println("Test FAILED: Final balance (" + String.format("%.2f", finalBalance) + ") does NOT match expected balance (" + String.format("%.2f", expectedFinalBalance) + ").");
            }

        } catch (MyExceptions.AccountNotFoundException e) {
            System.out.println("Error: Account " + accountIdToTest + " not found. Please ensure it exists in the database.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Test interrupted: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Runtime error during test: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) { // Bắt các ngoại lệ khác từ databaseManager.getAccountById
            System.out.println("Unexpected error during test setup/teardown: " + e.getMessage());
            e.printStackTrace();
        } finally {
            bankService.close();
            System.out.println("BankService shut down.");
        }
    }
}