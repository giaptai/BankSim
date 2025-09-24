package presentation.ui;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import resources.Type;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ThreadTrackerGUI extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(ThreadTrackerGUI.class.getName());
    private DefaultTableModel model;
    // Map để lưu trữ chỉ số hàng của từng luồng worker dựa trên Thread.getName()
    private Map<String, Integer> threadRowMap;
    // Map để lưu trữ tổng số giao dịch đã xử lý bởi mỗi luồng
    private Map<String, Integer> threadTransactionCount;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final int maxThread = 100;

    public ThreadTrackerGUI() {
        setTitle("BankSim - Thread Tracker");
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        threadRowMap = new HashMap<>();
        threadTransactionCount = new HashMap<>();
        initComponents();
    }

    /**
     * 
     */
    public void initComponents() {
        String[] colName = { "Thread", "Type", "Source account", "Target account",
                "Transaction amount", "Predicted balance", "Actual balance", "Start at", "Status", "Total transaction",
                "Log"
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
        add(jPanel(), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel jPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.setPreferredSize(new Dimension(WIDTH, 45));

        JLabel labelId = new JLabel("Account Id: ");
        JTextField tfName = new JTextField(15);
        JButton btn = new JButton("OK");

        JLabel labelAmount = new JLabel("Amount: ");
        JTextField tfAmount = new JTextField(15);

        String[] options = Arrays.asList(resources.Type.values()).stream().map(Enum::name).toArray(String[]::new);
        JComboBox<String> boxType = new JComboBox<>(options);
        boxType.setPreferredSize(new Dimension(100, boxType.getPreferredSize().height));

        Integer[] numTransaction = {1000, 10000, 100000, 500000};
        JComboBox<Integer> boxTrans = new JComboBox<>(numTransaction);
        boxTrans.setPreferredSize(new Dimension(100, boxTrans.getPreferredSize().height));


        panel.add(labelId);
        panel.add(tfName);
        panel.add(labelAmount);
        panel.add(tfAmount);
        panel.add(boxType);
        panel.add(boxTrans);

        panel.add(btn);

        return panel;
    }

    /**
     * 
     * @param threadId
     * @param type
     * @param sourceAccountId
     * @param targetAccountId
     * @param amount
     * @param predictedBalance
     * @param actualBalance
     * @param startAt
     * @param status
     * @param message
     */
    public void updateThreadRow(
            String threadId, String type, String sourceAccountId, String targetAccountId, double amount,
            double predictedBalance, double actualBalance, LocalDateTime startAt, String status, String message) {

        SwingUtilities.invokeLater(() -> {
            Integer rowIdx = threadRowMap.get(threadId);

            if ("Completed".equals(status) || "Failed".equals(status)) {
                threadTransactionCount.merge(threadId, 1, (oldVal, newVal) -> oldVal + newVal);
            }

            int currTransactionCount = threadTransactionCount.getOrDefault(threadId, 0);
            Object[] rowData = {
                    threadId,
                    type,
                    sourceAccountId,
                    targetAccountId,
                    String.format("%.2f", amount),
                    String.format("%.2f", predictedBalance),
                    (actualBalance == -1.0) ? "N/A" : String.format("%.2f", actualBalance),
                    startAt != null ? startAt.format(FORMATTER) : "N/A",
                    status,
                    currTransactionCount,
                    message != null && !message.isEmpty() ? " (" + message + ") " : ""
            };
            if (rowIdx == null) {
                if (model.getRowCount() < maxThread) {
                    model.addRow(rowData);
                    threadRowMap.put(threadId, model.getRowCount() - 1);
                } else {
                    LOGGER.warning("Attempted to add more rows than maxThreads for thread: " + threadId);
                }
            } else {
                for (int i = 0; i < rowData.length; i++) {
                    model.setValueAt(rowData[i], rowIdx, i);
                }
            }
        });
    }

    /**
     * @apiNote Optional
     * @param threadId ID của luồng worker.
     */
    public void clearThreadRow(String threadId) {
        SwingUtilities.invokeLater(() -> {
            Integer rowIdx = threadRowMap.get(threadId);
            if (rowIdx != null && rowIdx < model.getRowCount()) {
                model.setValueAt("", rowIdx, 1);
                model.setValueAt("", rowIdx, 2);
                model.setValueAt("", rowIdx, 3);
                model.setValueAt("", rowIdx, 4);
                model.setValueAt("", rowIdx, 5);
                model.setValueAt("", rowIdx, 6);
                model.setValueAt("", rowIdx, 7);
                model.setValueAt("Idle", rowIdx, 8);
                int currTransactionCount = threadTransactionCount.getOrDefault(threadId, 0);
                model.setValueAt(currTransactionCount, rowIdx, 9);
            }
        });
    }
}
