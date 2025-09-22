package test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import business.service.BankService;
import data.DatabaseManager;
import presentation.ui.ThreadTrackerGUI;

public class GUIDepositTest {
    public static void main(String[] args) {
        DatabaseManager databaseManager = new DatabaseManager();
        
        // Lấy số luồng tối đa từ BankService để truyền vào ThreadTrackerGUI
        // Chúng ta cần một cách để lấy MAX_THREADS mà không cần khởi tạo BankService 2 lần
        // Hoặc đơn giản là hardcode 13 ở đây nếu biết trước
        final int MAX_THREADS = 13; 
        ThreadTrackerGUI trackerGUI = new ThreadTrackerGUI(MAX_THREADS); // Truyền MAX_THREADS vào constructor
        SwingUtilities.invokeLater(() -> trackerGUI.setVisible(true));
        
        BankService bankService = new BankService(databaseManager, trackerGUI);

        int accountIdTest = 1; // ID tài khoản để kiểm thử
        double depositAmountPerThread = 10; // Số tiền gửi mỗi luồng
        int numberOfTransactions = 300; // Tổng số giao dịch muốn thực hiện

        List<Future<?>> futures = new ArrayList<>();
        try {
            System.out.println(
                    "Submitting " + numberOfTransactions + " Deposit transactions to account " + accountIdTest + "...");
            for (int i = 0; i < numberOfTransactions; i++) {
                futures.add(bankService.deposit(accountIdTest, depositAmountPerThread));
            }

            System.out.println("All deposit transactions submitted. Waiting for them to complete...");

            // Chờ tất cả các giao dịch hoàn thành
            for (Future<?> f : futures) {
                try {
                    f.get(); // Chờ từng giao dịch hoàn thành
                } catch (ExecutionException e) {
                    System.err.println("Transaction failed: " + e.getCause().getMessage());
                }
            }
            System.out.println("All deposit transactions completed. Check Thread Tracker GUI.");

        } catch (Exception e) {
             System.err.println("An unexpected error occurred during test setup: " + e.getMessage());
        }finally{
            bankService.close();
            try {
                // Đợi một chút để GUI có thời gian cập nhật trạng thái cuối cùng
                TimeUnit.SECONDS.sleep(2); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Sau khi tất cả hoàn thành, bạn có thể muốn xóa các hàng để hiển thị "Idle"
            // Hoặc để nguyên trạng thái cuối cùng của từng luồng.
            // Nếu muốn xóa:
            // for (int i = 0; i < MAX_THREADS; i++) {
            //     String threadName = "pool-1-thread-" + (i + 1); // Giả định tên luồng
            //     trackerGUI.clearThreadRow(threadName);
            // }
        }
    }
}