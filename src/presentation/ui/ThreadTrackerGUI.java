package presentation.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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

import business.service.IBankService;
import business.service.transaction.observer.Observer;
import business.service.transaction.observer.TransactionEvent;
import resources.Constants;
import resources.MyExceptions.AccountNotFoundException;
import test.SimRunner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadTrackerGUI extends JFrame implements Observer {
    private static final Logger LOGGER = Logger.getLogger(ThreadTrackerGUI.class.getName());
    private DefaultTableModel model;
    // Map để lưu trữ chỉ số hàng của từng luồng worker dựa trên Thread.getName()
    private Map<String, Integer> threadRowMap;
    // Map để lưu trữ tổng số giao dịch đã xử lý bởi mỗi luồng
    private Map<String, Integer> threadTransactionCount;
    // Map để lưu trữ thời gian cập nhật cuối cùng cho mỗi hàng luồng
    private Map<String, Long> lastUpdateTimes; // Thêm map này

    private DecimalFormat df = new DecimalFormat("#,##0.00");
    private JTextField tfAccountId;
    private JTextField tfToAccountId;
    private JLabel labelToAccountId;
    private JTextField tfAmount;
    private JComboBox<String> boxType;
    private JComboBox<Integer> boxTrans;
    private JLabel statusLabel;
    private JLabel startAtLabel;
    private JLabel finishedAtLabel;
    private IBankService bankService;

    public ThreadTrackerGUI() {
        setTitle("BankSim - Thread Tracker");
        setSize(1250, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        threadRowMap = new HashMap<>();
        threadTransactionCount = new HashMap<>();
        lastUpdateTimes = new ConcurrentHashMap<>();
        initComponents();
    }

    public void setBankService(IBankService bankService) {
        this.bankService = bankService;
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
        table.setForeground(Color.black);
        table.setBackground(Color.WHITE);
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

        // Tạo một panel chứa các control và status/balance labels
        JPanel controlAndStatusPanel = new JPanel(new BorderLayout());
        controlAndStatusPanel.add(jPanel(), BorderLayout.NORTH); // Các control ở phía trên

        JPanel statusLabelsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Panel cho các nhãn trạng thái
        statusLabel = new JLabel("Ready to run transactions...");
        statusLabel.setForeground(Color.BLUE);
        statusLabel.setFont(statusLabel.getFont().deriveFont(16f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        statusLabelsPanel.add(statusLabel);

        controlAndStatusPanel.add(statusLabelsPanel, BorderLayout.SOUTH); // Các nhãn trạng thái ở phía dưới

        add(controlAndStatusPanel, BorderLayout.NORTH); // Thêm panel này vào NORTH của JFrame
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel jPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setPreferredSize(new Dimension(this.getWidth(), 60));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Khởi tạo một JComboBox tạm thời để lấy chiều cao ưu tiên làm chuẩn
        JComboBox<String> dummyBox = new JComboBox<>(new String[] { "Dummy" });
        int componentHeight = dummyBox.getPreferredSize().height;

        // fromAccountId
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel labelId = new JLabel("Account Id: ");
        panel.add(labelId, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        tfAccountId = new JTextField(10);
        tfAccountId.setPreferredSize(new Dimension(tfAccountId.getPreferredSize().width, componentHeight));
        tfAccountId.setMinimumSize(new Dimension(tfAccountId.getPreferredSize().width, componentHeight));
        tfAccountId.setMaximumSize(new Dimension(Integer.MAX_VALUE, componentHeight)); // Cho phép chiều rộng giãn nở
        panel.add(tfAccountId, gbc);

        // toAccountId
        gbc.gridx = 2;
        gbc.gridy = 0;
        labelToAccountId = new JLabel("To Account Id");
        labelToAccountId.setVisible(false);
        panel.add(labelToAccountId);
        gbc.gridx = 3;
        gbc.gridy = 0;
        tfToAccountId = new JTextField(10);
        tfToAccountId.setPreferredSize(new Dimension(tfToAccountId.getPreferredSize().width, componentHeight));
        tfToAccountId.setMinimumSize(new Dimension(tfToAccountId.getPreferredSize().width, componentHeight));
        tfToAccountId.setMaximumSize(new Dimension(Integer.MAX_VALUE, componentHeight));
        tfToAccountId.setVisible(false);
        panel.add(tfToAccountId);

        gbc.gridx = 4;
        gbc.gridy = 0;
        JLabel labelAmount = new JLabel("Amount: ");
        panel.add(labelAmount, gbc);
        gbc.gridx = 5;
        gbc.gridy = 0;
        tfAmount = new JTextField(10);
        // Đặt preferred, minimum và maximum size
        tfAmount.setPreferredSize(new Dimension(tfAmount.getPreferredSize().width, componentHeight));
        tfAmount.setMinimumSize(new Dimension(tfAmount.getPreferredSize().width, componentHeight));
        tfAmount.setMaximumSize(new Dimension(Integer.MAX_VALUE, componentHeight));
        panel.add(tfAmount, gbc);

        gbc.gridx = 6;
        gbc.gridy = 0;
        String[] options = Arrays.asList(resources.Type.values()).stream().map(Enum::name).toArray(String[]::new);
        boxType = new JComboBox<>(options);
        // Đặt preferred, minimum và maximum size (cố định chiều rộng cho ComboBox)
        boxType.setPreferredSize(new Dimension(100, componentHeight));
        boxType.setMinimumSize(new Dimension(100, componentHeight));
        boxType.setMaximumSize(new Dimension(100, componentHeight));
        boxType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedType = (String) boxType.getSelectedItem();
                boolean isTransfer = resources.Type.TRANSFER.name().equals(selectedType);
                labelToAccountId.setVisible(isTransfer);
                tfToAccountId.setVisible(isTransfer);
                // Cập nhật lại panel để bố cục được tính toán lại
                panel.revalidate();
                panel.repaint();
            }
        });
        panel.add(boxType, gbc);

        gbc.gridx = 7;
        gbc.gridy = 0;
        Integer[] numTransaction = { 1000, 10000, 100000, 500000, 1_000_000, 5_000_000 };
        boxTrans = new JComboBox<>(numTransaction);
        // Đặt preferred, minimum và maximum size (cố định chiều rộng cho ComboBox)
        boxTrans.setPreferredSize(new Dimension(100, componentHeight));
        boxTrans.setMinimumSize(new Dimension(100, componentHeight));
        boxTrans.setMaximumSize(new Dimension(100, componentHeight));
        panel.add(boxTrans, gbc);

        // Run Transactions Button
        gbc.gridx = 8;
        gbc.gridy = 0;
        JButton btn = new JButton("Run Transactions");
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, componentHeight));
        btn.setMinimumSize(new Dimension(btn.getPreferredSize().width, componentHeight));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, componentHeight)); // Cho phép chiều rộng giãn nở
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleOkButtonClick();
            }
        });
        panel.add(btn, gbc);

        // Check Balance Button
        gbc.gridx = 9; // Đặt ở cột tiếp theo
        gbc.gridy = 0;
        JButton checkBalanceBtn = new JButton("Check Balance");
        checkBalanceBtn.setPreferredSize(new Dimension(checkBalanceBtn.getPreferredSize().width, componentHeight));
        checkBalanceBtn.setMinimumSize(new Dimension(checkBalanceBtn.getPreferredSize().width, componentHeight));
        checkBalanceBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, componentHeight));
        checkBalanceBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCheckBalanceButtonClick();
            }
        });
        panel.add(checkBalanceBtn, gbc);

        // start at and finish at
        gbc.gridx = 10;
        gbc.gridy = 0;
        JPanel timePanel = new JPanel();
        timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.Y_AXIS));
        startAtLabel = new JLabel("Start At:");
        finishedAtLabel = new JLabel("Finished At:");
        timePanel.add(startAtLabel);
        timePanel.add(finishedAtLabel);

        // Thêm một "glue" để đẩy các thành phần về phía trái nếu có không gian thừa
        gbc.gridx = 11; // Cột cuối cùng
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        panel.add(Box.createHorizontalGlue(), gbc);

        panel.add(timePanel, gbc);

        return panel;
    }

    private void handleCheckBalanceButtonClick() {
        updateBalanceResult("Checking account details...", Color.BLUE);
        try {
            int accountId = Integer.parseInt(tfAccountId.getText());

            if (bankService == null) {
                updateBalanceResult("Error: BankService not initialized.", Color.RED);
                LOGGER.log(Level.SEVERE, "BankService is null in ThreadTrackerGUI.handleCheckBalanceButtonClick()");
                return;
            }

            // Chạy logic kiểm tra số dư trong một luồng riêng để không chặn EDT
            new Thread(() -> {
                try {
                    String accountDetails = bankService.getAccountDetails(accountId);
                    updateBalanceResult(String.format("Account Details for ID %d: %s", accountId, accountDetails),
                            Color.BLACK);
                } catch (AccountNotFoundException ex) {
                    updateBalanceResult("Error: Account " + accountId + " not found.", Color.RED);
                    LOGGER.log(Level.WARNING, "Account not found: " + ex.getMessage(), ex);
                } catch (NumberFormatException ex) {
                    updateBalanceResult("Error: Invalid Account ID. Please enter a valid number.", Color.RED);
                    LOGGER.log(Level.WARNING, "Invalid input for Account ID: " + ex.getMessage(), ex);
                } catch (Exception ex) {
                    updateBalanceResult("Error checking account: " + ex.getMessage(), Color.RED);
                    LOGGER.log(Level.SEVERE, "An unexpected error occurred while checking account: " + ex.getMessage(),
                            ex);
                }
            }).start();

        } catch (NumberFormatException ex) {
            updateBalanceResult("Error: Invalid Account ID. Please enter a valid number.", Color.RED);
            LOGGER.log(Level.WARNING, "Invalid input for Account ID: " + ex.getMessage(), ex);
        }
    }

    private void handleOkButtonClick() {
        model.setRowCount(0);
        threadRowMap.clear();
        threadTransactionCount.clear();
        updateStatus("Starting new test...", Color.BLUE);

        String accountIdTx = tfAccountId.getText().trim();
        String amountTx = tfAmount.getText().trim();
        if (accountIdTx.isEmpty()) {
            updateStatus("Error: Please enter Account ID.", Color.RED);
            return;
        }
        if (amountTx.isEmpty()) {
            updateStatus("Error: Please enter Amount.", Color.RED);
            return;
        }
        if (!amountTx.matches("\\d+(\\.\\d+)?")) {
            updateStatus("Error: Please enter Amount is a number.", Color.RED);
            return;
        }
        if (!accountIdTx.matches("\\d+")) {
            updateStatus("Error: Please enter Account ID is a number.", Color.RED);
            return;
        }
        int fromAccountId = Integer.parseInt(tfAccountId.getText());
        double amountPerTransaction = Double.parseDouble(tfAmount.getText());
        resources.Type transactionType = resources.Type.valueOf((String) boxType.getSelectedItem());
        int totalTransaction = (Integer) boxTrans.getSelectedItem();
        if (amountPerTransaction <= 0) {
            updateStatus("Error: Please enter amount greater than 0", Color.RED);
            LOGGER.log(Level.SEVERE, "Please enter amount greater than 0");
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                startAtLabel.setText("Started at: " + LocalDateTime.now().format(Constants.FORMATTER));
                finishedAtLabel.setText("Finished at: N/A");
            }
        });
        try {
            if (bankService == null) {
                updateStatus("Error: BankService not initialized. Please restart the application.", Color.RED);
                LOGGER.log(Level.SEVERE, "BankService is null in ThreadTrackerGUI.handleOkButtonClick()");
                return;
            }
            new Thread(() -> {
                SimRunner runner = new SimRunner(bankService);
                if (resources.Type.TRANSFER == transactionType) {
                    try {
                        int toAccountId = Integer.parseInt(tfToAccountId.getText());
                        if (fromAccountId == toAccountId) {
                            String errorMessage = "Error: Cannot transfer to the same account.";
                            updateStatus(errorMessage, Color.RED);
                            LOGGER.log(Level.WARNING, errorMessage + " From Account ID: " + fromAccountId
                                    + ", To Account ID: " + toAccountId);
                            return;
                        }
                        runner.runConcurrentTransfers(fromAccountId, toAccountId, amountPerTransaction,
                                totalTransaction);
                        updateStatus(String.format("All %d TRANSFER tasks completed for accounts %d -> %d.",
                                totalTransaction, fromAccountId, toAccountId), new Color(34, 139, 34));
                    } catch (NumberFormatException ex) {
                        updateStatus("Error: Invalid 'To Account ID' for transfer. Please enter a valid number.",
                                Color.RED);
                        LOGGER.log(Level.WARNING, "Invalid 'To Account ID' input for transfer: " + ex.getMessage(), ex);
                    } catch (Exception ex) {
                        updateStatus("An unexpected error occurred during transfer: " + ex.getMessage(), Color.RED);
                        LOGGER.log(Level.SEVERE, "An unexpected error occurred during transfer: " + ex.getMessage(),
                                ex);
                    }
                } else {
                    runner.runConcurrentTransactions(fromAccountId, amountPerTransaction, transactionType,
                            totalTransaction);
                    updateStatus(String.format("All %d %s tasks completed for account %d.", totalTransaction,
                            transactionType.name(), fromAccountId), new Color(34, 139, 34));
                }
                SwingUtilities.invokeLater(() -> {
                    finishedAtLabel.setText("Finished at: " + LocalDateTime.now().format(Constants.FORMATTER));
                });
            }).start();

        } catch (NumberFormatException ex) {
            updateStatus("Error: Invalid Account ID or Amount. Please enter valid numbers.", Color.RED);
            LOGGER.log(Level.WARNING, "Invalid input for Account ID or Amount: " + ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            updateStatus("Error: Invalid transaction type selected.", Color.RED);
            LOGGER.log(Level.WARNING, "Invalid transaction type: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            updateStatus("An unexpected error occurred: " + ex.getMessage(), Color.RED);
            LOGGER.log(Level.SEVERE, "An unexpected error occurred: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            updateStatus("An unexpected error occurred: " + ex.getMessage(), Color.RED);
            LOGGER.log(Level.SEVERE, "An unexpected error occurred: " + ex.getMessage(), ex);
        }
    }

    // Phương thức để cập nhật statusLabel một cách an toàn trên EDT
    private void updateStatus(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            statusLabel.setForeground(color);
        });
    }

    // Phương thức mới để cập nhật nhãn số dư
    private void updateBalanceResult(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            statusLabel.setForeground(color);
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

    @Override
    public void update(TransactionEvent event) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastUpdateTimes.get(event.getCurrThreadName());

        boolean isFinalStatus = "Completed".equals(event.getStatus().getDisplayName()) || "Failed".equals(event.getStatus().getDisplayName());
        if (isFinalStatus == true || lastTime == null || (currentTime - lastTime) > Constants.MIN_UPDATE_INTERVAL_MS) {
            SwingUtilities.invokeLater(() -> {
                Integer rowIdx = threadRowMap.get(event.getCurrThreadName());
                if ("Completed".equals(event.getStatus().getDisplayName()) || "Failed".equals(event.getStatus().getDisplayName())) {
                    threadTransactionCount.merge(event.getCurrThreadName(), 1, (oldVal, newVal) -> oldVal + newVal);
                }
                int currTransactionCount = threadTransactionCount.getOrDefault(event.getCurrThreadName(), 0);
                Object[] rowData = {
                        event.getCurrThreadName(),
                        event.getType(),
                        event.getFromAccountId(),
                        event.getToAccountId(),
                        df.format(event.getAmount()),
                        df.format(event.getPredictedBalance()),
                        (event.getActualBalance() == -1.0) ? "N/A" : df.format(event.getActualBalance()),
                        event.getStartTime() != null ? event.getStartTime().format(Constants.FORMATTER) : "N/A",
                        event.getStatus(),
                        currTransactionCount,
                        event.getMessage() != null && !event.getMessage().isEmpty() ? " (" + event.getMessage() + ") "
                                : ""
                };
                if (rowIdx == null) {
                    if (model.getRowCount() < Constants.MAX_THREADS) {
                        model.addRow(rowData);
                        threadRowMap.put(event.getCurrThreadName(), model.getRowCount() - 1);
                    } else {
                        LOGGER.warning(
                                "Attempted to add more rows than maxThreads for thread: " + event.getCurrThreadName());
                    }
                } else {
                    for (int i = 0; i < rowData.length; i++) {
                        model.setValueAt(rowData[i], rowIdx, i);
                    }
                }
            });
            lastUpdateTimes.put(event.getCurrThreadName(), currentTime);
        }
    }
}
