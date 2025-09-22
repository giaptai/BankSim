package presentation.ui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ThreadTrackerGUI extends JFrame {
    private DefaultTableModel model;
    // Map để lưu trữ chỉ số hàng của từng luồng worker dựa trên Thread.getName()
    private Map<String, Integer> threadRowMap;
    // Map để lưu trữ tổng số giao dịch đã xử lý bởi mỗi luồng
    private Map<String, Integer> threadTransactionCount;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final int maxThreads;

    public ThreadTrackerGUI(int maxThreads) {
        this.maxThreads = maxThreads;
        setTitle("BankSim - Thread Tracker");
        setSize(1200, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        threadRowMap = new HashMap<>();
        threadTransactionCount = new HashMap<>();
        initComponents();
    }

    public ThreadTrackerGUI() {
        this(13);
    }

    public void initComponents() {
        String[] colName = { "Thread", "Type", "Source account", "Target account",
                "Transaction amount", "Predicted balance", "Actual balance", "Start at", "Status", "Total transaction"
        };
        this.model = new DefaultTableModel(colName, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        JTableHeader header = table.getTableHeader();
        table.setRowHeight(30);
        // Tạo renderer căn giữa
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        // Đổi font, màu nền, màu chữ
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(header.getWidth(), 28));
        header.setBackground(Color.DARK_GRAY);
        header.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Cập nhật hoặc thêm một hàng cho một luồng worker cụ thể.
     * Nếu luồng đã có hàng, cập nhật hàng đó. Nếu chưa, thêm hàng mới (tối đa
     * maxThreads).
     *
     * @param threadId          ID của luồng worker (ví dụ: "pool-1-thread-1").
     * @param type              Loại giao dịch (DEPOSIT, WITHDRAW, TRANSFER).
     * @param sourceAccountId   ID tài khoản nguồn (hoặc "N/A").
     * @param targetAccountId   ID tài khoản đích (hoặc "N/A").
     * @param transactionAmount Số tiền giao dịch.
     * @param predictedBalance  Số dư dự kiến sau giao dịch.
     * @param actualBalance     Số dư thực tế sau giao dịch (hoặc "N/A" nếu chưa
     *                          hoàn thành).
     * @param startAt           Thời gian bắt đầu giao dịch.
     * @param status            Trạng thái của giao dịch (Pending, Completed,
     *                          Failed).
     * @param message           Thông báo bổ sung.
     */
    public void updateThreadRow(String threadId, String type, String sourceAccountId, String targetAccountId,
            double transactionAmount, double predictedBalance, double actualBalance,
            LocalDateTime startAt, String status, String message) {
        SwingUtilities.invokeLater(() -> {
            Integer rowIndex = threadRowMap.get(threadId);

            // Tăng bộ đếm giao dịch cho luồng này nếu trạng thái là Completed/Failed
            if ("Completed".equals(status) || "Failed".equals(status)) {
                threadTransactionCount.merge(threadId, 1, Integer::sum);
            }
            int currentTransactionCount = threadTransactionCount.getOrDefault(threadId, 0);

            Object[] rowData = {
                    threadId,
                    type,
                    sourceAccountId,
                    targetAccountId,
                    String.format("%.2f", transactionAmount),
                    String.format("%.2f", predictedBalance),
                    (actualBalance == -1.0) ? "N/A" : String.format("%.2f", actualBalance), // -1.0 là giá trị mặc định
                                                                                            // cho N/A
                    startAt != null ? startAt.format(FORMATTER) : "N/A",
                    status,
                    currentTransactionCount
            };

            if (rowIndex == null) {
                // Nếu luồng chưa có hàng và chưa đạt đến số lượng hàng tối đa
                if (model.getRowCount() < maxThreads) {
                    model.addRow(rowData);
                    threadRowMap.put(threadId, model.getRowCount() - 1);
                } else {
                    // Đây là trường hợp không mong muốn nếu ExecutorService hoạt động đúng
                    // Có thể log lỗi hoặc bỏ qua
                    System.err.println("Attempted to add more rows than maxThreads for thread: " + threadId);
                }
            } else {
                // Cập nhật hàng hiện có
                for (int i = 0; i < rowData.length; i++) {
                    model.setValueAt(rowData[i], rowIndex, i);
                }
            }
        });
    }

    /**
     * Xóa thông tin giao dịch khỏi một hàng của luồng, chuẩn bị cho tác vụ mới.
     * Thường gọi khi một luồng hoàn thành tác vụ và sẵn sàng cho tác vụ tiếp theo.
     *
     * @param threadId ID của luồng worker.
     */
    public void clearThreadRow(String threadId) {
        SwingUtilities.invokeLater(() -> {
            Integer rowIndex = threadRowMap.get(threadId);
            if (rowIndex != null && rowIndex < model.getRowCount()) {
                model.setValueAt("", rowIndex, 1); // Type
                model.setValueAt("", rowIndex, 2); // Source Account
                model.setValueAt("", rowIndex, 3); // Target Account
                model.setValueAt("", rowIndex, 4); // Transaction Amount
                model.setValueAt("", rowIndex, 5); // Predicted Balance
                model.setValueAt("", rowIndex, 6); // Actual Balance
                model.setValueAt("", rowIndex, 7); // Start At
                model.setValueAt("Idle", rowIndex, 8); // Status

                // Giữ nguyên số lượng giao dịch đã xử lý trong cột "Total transaction"
                int currentTransactionCount = threadTransactionCount.getOrDefault(threadId, 0);
                model.setValueAt(currentTransactionCount, rowIndex, 9); // Total transaction
            }
        });
    }
}
