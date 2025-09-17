package presentation.ui;

import presentation.controller.SwingBankController;
import presentation.ui.panels.MainMenuPanel;
import presentation.ui.panels.OpenAccountPanel;
import presentation.ui.panels.DepositPanel;
import presentation.ui.panels.WithdrawPanel;
import presentation.ui.panels.TransferPanel;
import presentation.ui.panels.ViewBalancePanel;
import presentation.ui.panels.ViewTransactionHistoryPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class BankSwingGUI extends JFrame {

    private SwingBankController controller;

    // CardLayout components
    private JPanel cardPanel;
    private CardLayout cardLayout;

    // Panels for different views
    private MainMenuPanel mainMenuPanel;
    private OpenAccountPanel openAccountPanel;
    private DepositPanel depositPanel;
    private WithdrawPanel withdrawPanel;
    private TransferPanel transferPanel;
    private ViewBalancePanel viewBalancePanel;
    private ViewTransactionHistoryPanel viewTransactionHistoryPanel;

    // Global UI components
    private JLabel statusLabel;
    private JTextField currentFocusedField; // Để bàn phím số biết nhập vào đâu

    // Constants for card names
    public static final String MAIN_MENU_CARD = "MainMenu";
    public static final String OPEN_ACCOUNT_CARD = "OpenAccount";
    public static final String DEPOSIT_CARD = "Deposit";
    public static final String WITHDRAW_CARD = "Withdraw";
    public static final String TRANSFER_CARD = "Transfer";
    public static final String VIEW_BALANCE_CARD = "ViewBalance";
    public static final String VIEW_HISTORY_CARD = "ViewHistory";

    public BankSwingGUI(SwingBankController controller) {
        this.controller = controller;
        setTitle("BankSim");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        initComponents();
        addEventHandlers();
        pack(); // Tự động điều chỉnh kích thước frame
        showCard(MAIN_MENU_CARD); // Hiển thị menu chính khi khởi động
    }

    private void initComponents() {
        // Status Label at SOUTH
        statusLabel = new JLabel("Ready...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setForeground(Color.BLUE);
        add(statusLabel, BorderLayout.SOUTH);

        // Card Panel at CENTER
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        add(cardPanel, BorderLayout.CENTER);

        // Initialize and add panels to cardPanel
        mainMenuPanel = new MainMenuPanel();
        cardPanel.add(mainMenuPanel, MAIN_MENU_CARD);

        openAccountPanel = new OpenAccountPanel();
        cardPanel.add(openAccountPanel, OPEN_ACCOUNT_CARD);

        depositPanel = new DepositPanel();
        cardPanel.add(depositPanel, DEPOSIT_CARD);

        withdrawPanel = new WithdrawPanel();
        cardPanel.add(withdrawPanel, WITHDRAW_CARD);

        transferPanel = new TransferPanel();
        cardPanel.add(transferPanel, TRANSFER_CARD);

        viewBalancePanel = new ViewBalancePanel();
        cardPanel.add(viewBalancePanel, VIEW_BALANCE_CARD);

        viewTransactionHistoryPanel = new ViewTransactionHistoryPanel();
        cardPanel.add(viewTransactionHistoryPanel, VIEW_HISTORY_CARD);

        // Add mumPad and actionPad to the EAST
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding
        sidePanel.add(mumPad());
        sidePanel.add(Box.createVerticalStrut(20)); // Khoảng cách
        sidePanel.add(actionPad());
        add(sidePanel, BorderLayout.EAST);
    }

    private JPanel mumPad() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Numpad"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        ActionListener numpadListener = e -> {
            String buttonText = ((JButton) e.getSource()).getText();
            if (getCurrentCard() == mainMenuPanel) {
                // Nếu đang ở menu chính, nhập vào trường menuInputField
                mainMenuPanel.getMenuInputField().setText(mainMenuPanel.getMenuInputField().getText() + buttonText);
            } else if (currentFocusedField != null) {
                // Nếu đang ở form, nhập vào trường đang được focus
                currentFocusedField.setText(currentFocusedField.getText() + buttonText);
            }
        };

        int num = 1;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                gbc.gridx = col;
                gbc.gridy = row;
                JButton button = new JButton(String.valueOf(num++));
                button.setPreferredSize(new Dimension(60, 40));
                button.addActionListener(numpadListener);
                panel.add(button, gbc);
            }
        }
        gbc.gridx = 1;
        gbc.gridy = 3;
        JButton zeroButton = new JButton("0");
        zeroButton.setPreferredSize(new Dimension(60, 40));
        zeroButton.addActionListener(numpadListener);
        panel.add(zeroButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        JButton dotButton = new JButton(".");
        dotButton.setPreferredSize(new Dimension(60, 40));
        dotButton.addActionListener(numpadListener);
        panel.add(dotButton, gbc);

        gbc.gridx = 2;
        gbc.gridy = 3;
        JButton backButton = new JButton("<-");
        backButton.setPreferredSize(new Dimension(60, 40));
        backButton.addActionListener(e -> {
            if (getCurrentCard() == mainMenuPanel) {
                // Xóa ký tự cuối cùng trong trường menuInputField
                String currentText = mainMenuPanel.getMenuInputField().getText();
                if (currentText.length() > 0) {
                    mainMenuPanel.getMenuInputField().setText(currentText.substring(0, currentText.length() - 1));
                }
            } else if (currentFocusedField != null && currentFocusedField.getText().length() > 0) {
                // Xóa ký tự cuối cùng trong trường đang được focus
                currentFocusedField.setText(
                        currentFocusedField.getText().substring(0, currentFocusedField.getText().length() - 1));
            }
        });
        panel.add(backButton, gbc);

        return panel;
    }

    private JPanel actionPad() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Operaters"));
        panel.add(Box.createVerticalGlue());

        JButton clearButton = new JButton("Clear");
        JButton cancelButton = new JButton("Cancel");
        JButton enterButton = new JButton("Enter");

        Dimension buttonSize = new Dimension(100, 40);
        clearButton.setMaximumSize(buttonSize);
        clearButton.setPreferredSize(buttonSize);
        clearButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        cancelButton.setMaximumSize(buttonSize);
        cancelButton.setPreferredSize(buttonSize);
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        enterButton.setMaximumSize(buttonSize);
        enterButton.setPreferredSize(buttonSize);
        enterButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add Action Listeners
        clearButton.addActionListener(e -> handleClear());
        cancelButton.addActionListener(e -> handleCancel());
        enterButton.addActionListener(e -> handleEnter());

        panel.add(clearButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(cancelButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(enterButton);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private void addEventHandlers() {
        // Add focus listeners to all text fields in form panels
        // This allows the numpad to know which field to type into
        addFocusListenerToTextFields(openAccountPanel);
        addFocusListenerToTextFields(depositPanel);
        addFocusListenerToTextFields(withdrawPanel);
        addFocusListenerToTextFields(transferPanel);
        addFocusListenerToTextFields(viewBalancePanel);
        addFocusListenerToTextFields(viewTransactionHistoryPanel);
    }

    private void addFocusListenerToTextFields(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JTextField) {
                ((JTextField) comp).addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        currentFocusedField = (JTextField) e.getSource();
                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                        // currentFocusedField = null; // Có thể giữ hoặc bỏ, tùy thuộc vào hành vi mong
                        // muốn
                    }
                });
            } else if (comp instanceof Container) {
                addFocusListenerToTextFields((Container) comp);
            }
        }
    }

    // --- Card Layout Management ---
    public void showCard(String cardName) {
        cardLayout.show(cardPanel, cardName);
        updateStatus("Ready...", Color.BLUE); // Reset status when changing card
        // Request focus for the first input field if it's a form panel
        if (cardName.equals(OPEN_ACCOUNT_CARD)) {
            openAccountPanel.requestInitialFocus();
        }
        if (cardName.equals(DEPOSIT_CARD)) {
            depositPanel.requestInitialFocus();
        }
        if (cardName.equals(WITHDRAW_CARD)) {
            withdrawPanel.requestInitialFocus();
        }
        if (cardName.equals(TRANSFER_CARD)) {
            transferPanel.requestInitialFocus();
        }
        if (cardName.equals(VIEW_BALANCE_CARD)) {
            viewBalancePanel.requestInitialFocus();
        }
        if (cardName.equals(VIEW_HISTORY_CARD)) {
            viewTransactionHistoryPanel.clearFields();
            viewTransactionHistoryPanel.requestInitialFocus();
        }
        // ... (Thêm cho các panel khác sau này)
    }

    // --- Action Pad Handlers ---
    private void handleClear() {
        Component currentCard = getCurrentCard();
        if (currentCard == mainMenuPanel) {
            mainMenuPanel.clearMenuInput(); // Xóa trường nhập liệu menu
            updateStatus("Ready...", Color.BLUE);
        } else if (currentFocusedField != null) {
            currentFocusedField.setText("");
        }
    }

    private void handleCancel() {
        Component currentCard = getCurrentCard();
        if (currentCard == mainMenuPanel) {
            mainMenuPanel.clearMenuInput(); // Xóa trường nhập liệu menu
            updateStatus("Ready...", Color.BLUE);
        } else {
            // Xóa tất cả các trường trong form hiện tại trước khi quay lại menu
            if (currentCard instanceof OpenAccountPanel) {
                ((OpenAccountPanel) currentCard).clearFields();
            } else if (currentCard instanceof DepositPanel) {
                ((DepositPanel) currentCard).clearFields();
            } else if (currentCard instanceof WithdrawPanel) {
                ((WithdrawPanel) currentCard).clearFields();
            } else if (currentCard instanceof TransferPanel) {
                ((TransferPanel) currentCard).clearFields();
            } else if (currentCard instanceof ViewBalancePanel) {
                ((ViewBalancePanel) currentCard).clearFields();
            } else if (currentCard instanceof ViewTransactionHistoryPanel) {
                ((ViewTransactionHistoryPanel) currentCard).clearFields();
            }
            // ... (Thêm cho các panel khác sau này)
            showCard(MAIN_MENU_CARD);
        }
    }

    private void handleEnter() {
        Component currentCard = getCurrentCard();

        if (currentCard == mainMenuPanel) {
            // Logic để chọn mục menu từ bàn phím số
            String input = mainMenuPanel.getMenuInputField().getText();
            if (input.trim().isEmpty()) {
                updateStatus("Error: Please enter menu number.", Color.RED);
                return;
            }
            try {
                int choice = Integer.parseInt(input.trim());
                mainMenuPanel.clearMenuInput(); // Xóa trường nhập liệu menu sau khi xử lý
                switch (choice) {
                    case 1:
                        showCard(OPEN_ACCOUNT_CARD);
                        break;
                    case 2:
                        showCard(DEPOSIT_CARD);
                        break;
                    case 3:
                        showCard(WITHDRAW_CARD);
                        break;
                    case 4:
                        showCard(TRANSFER_CARD);
                        break;
                    case 5:
                        showCard(VIEW_BALANCE_CARD);
                        break;
                    case 6:
                        showCard(VIEW_HISTORY_CARD);
                        break;
                    case 0:
                        handleExit();
                        break;
                    default:
                        updateStatus("Error: Invalid selection.", Color.RED);
                }
            } catch (NumberFormatException ex) {
                updateStatus("Error: Please enter a valid number.", Color.RED);
            }
        } else if (currentCard == openAccountPanel) {
            handleOpenAccountAction();
        } else if (currentCard == depositPanel) {
            handleDepositAction();
        } else if (currentCard == withdrawPanel) {
            handleWithdrawAction();
        } else if (currentCard == transferPanel) {
            handleTransferAction();
        } else if (currentCard == viewBalancePanel) {
            handleViewBalanceAction();
        } else if (currentCard == viewTransactionHistoryPanel) {
            handleViewTransactionHistoryAction();
        }
        // ... (Thêm cho các panel khác sau này)
    }

    private Component getCurrentCard() {
        for (Component comp : cardPanel.getComponents()) {
            if (comp.isVisible()) {
                return comp;
            }
        }
        return null;
    }

    // --- Specific Action Handlers (called by Enter button) ---
    private void handleOpenAccountAction() {
        String ownerName = openAccountPanel.getOwnerName();
        String initialBalanceStr = openAccountPanel.getInitialBalance();

        if (ownerName.trim().isEmpty() || initialBalanceStr.trim().isEmpty()) {
            updateStatus("Error: Account holder name and initial balance cannot be blank.", Color.RED);
            return;
        }

        try {
            double initialBalance = Double.parseDouble(initialBalanceStr);
            String result = controller.openAccount(ownerName, initialBalance);

            if (result.startsWith("SUCCESS:")) {
                // Hiển thị thông báo thành công trong một hộp thoại
                JOptionPane.showMessageDialog(this, result.substring("SUCCESS: ".length()), "Successful",
                        JOptionPane.INFORMATION_MESSAGE);
                openAccountPanel.clearFields(); // Xóa các trường sau khi thành công
                // showCard(MAIN_MENU_CARD); // Quay lại menu chính
            } else {
                // Nếu có lỗi, hiển thị thông báo lỗi trên statusLabel như bình thường
                displayResult(result);
            }
        } catch (NumberFormatException ex) {
            updateStatus("Error: Invalid initial balance.", Color.RED);
        }
    }

    private void handleDepositAction() {
        String accountId = depositPanel.getAccountId();
        String amount = depositPanel.getAmount();

        if (accountId.trim().isEmpty() || amount.trim().isEmpty()) {
            updateStatus("Error: Account Id and amount cannot be blank.", Color.RED);
            return;
        }

        try {
            double initialBalance = Double.parseDouble(amount);
            String result = controller.deposit(Integer.parseInt(accountId), initialBalance);
            if (result.startsWith("SUCCESS:")) {
                JOptionPane.showMessageDialog(this, result.substring("SUCCESS: ".length()), "Successful",
                        JOptionPane.INFORMATION_MESSAGE);
                depositPanel.clearFields();
                // showCard(MAIN_MENU_CARD); // Quay lại menu chính
            } else {
                displayResult(result);
            }
        } catch (NumberFormatException ex) {
            updateStatus("Error: Invalid initial amount.", Color.RED);
        }
    }

    private void handleWithdrawAction() {
        String accountId = withdrawPanel.getAccountId();
        String amount = withdrawPanel.getAmount();

        if (accountId.trim().isEmpty() || amount.trim().isEmpty()) {
            updateStatus("Error: Account Id and amount cannot be blank.", Color.RED);
            return;
        }

        try {
            double initialBalance = Double.parseDouble(amount);
            String result = controller.withdraw(Integer.parseInt(accountId), initialBalance);
            if (result.startsWith("SUCCESS:")) {
                JOptionPane.showMessageDialog(this, result.substring("SUCCESS: ".length()), "Successful",
                        JOptionPane.INFORMATION_MESSAGE);
                withdrawPanel.clearFields();
                // showCard(MAIN_MENU_CARD); // Quay lại menu chính
            } else {
                displayResult(result);
            }
        } catch (NumberFormatException ex) {
            updateStatus("Error: Invalid initial amount.", Color.RED);
        }
    }

    private void handleTransferAction() {
        String fromAccountIdStr = transferPanel.getFromAccountId();
        String toAccountIdStr = transferPanel.getToAccountId();
        String amountStr = transferPanel.getAmount();

        if (fromAccountIdStr.trim().isEmpty() || toAccountIdStr.trim().isEmpty() || amountStr.trim().isEmpty()) {
            updateStatus("Error: All fields must be filled for transfer.", Color.RED);
            return;
        }

        try {
            int fromAccountId = Integer.parseInt(fromAccountIdStr);
            int toAccountId = Integer.parseInt(toAccountIdStr);
            double amount = Double.parseDouble(amountStr);

            String result = controller.transfer(fromAccountId, toAccountId, amount);

            if (result.startsWith("SUCCESS:")) {
                JOptionPane.showMessageDialog(this, result.substring("SUCCESS: ".length()), "Successful",
                        JOptionPane.INFORMATION_MESSAGE);
                transferPanel.clearFields();
            } else {
                displayResult(result);
            }
        } catch (NumberFormatException ex) {
            updateStatus("Error: Invalid Account ID or amount.", Color.RED);
        }
    }

    private void handleViewBalanceAction() {
        String accountIdStr = viewBalancePanel.getAccountId();
        if (accountIdStr.trim().isEmpty()) {
            updateStatus("Error: Account ID cannot be blank.", Color.RED);
            return;
        }
        try {
            int accountId = Integer.parseInt(accountIdStr);
            String result = controller.getAccountDetails(accountId); // Lấy chi tiết tài khoản từ controller
            if (result.startsWith("SUCCESS:")) {
                // Hiển thị chi tiết tài khoản trong detailsArea của ViewBalancePanel
                viewBalancePanel.displayAccountDetails(result.substring("SUCCESS: ".length()));
                updateStatus("Account details loaded.", Color.GREEN);
            } else {
                // Nếu có lỗi, hiển thị thông báo lỗi trên statusLabel và xóa detailsArea
                viewBalancePanel.displayAccountDetails(""); // Xóa nội dung cũ
                displayResult(result);
            }
        } catch (NumberFormatException ex) {
            updateStatus("Error: Invalid Account ID.", Color.RED);
            viewBalancePanel.displayAccountDetails(""); // Xóa nội dung cũ
        }
    }

    private void handleViewTransactionHistoryAction() {
        String accountIdStr = viewTransactionHistoryPanel.getAccountId();
        if (accountIdStr.trim().isEmpty()) {
            updateStatus("Error: Account ID cannot be blank.", Color.RED);
            return;
        }
        try {
            int accountId = Integer.parseInt(accountIdStr);
            String result = controller.getTransactionHistory(accountId); // Lấy lịch sử giao dịch từ controller

            if (result.startsWith("SUCCESS:")) {
                // Hiển thị lịch sử giao dịch trong detailsArea của ViewTransactionHistoryPanel
                viewTransactionHistoryPanel.displayAccountDetails(result.substring("SUCCESS: ".length()));
                updateStatus("Transaction history loaded.", Color.GREEN);
            } else {
                // Nếu có lỗi, hiển thị thông báo lỗi trên statusLabel và xóa detailsArea
                viewTransactionHistoryPanel.displayAccountDetails(""); // Xóa nội dung cũ
                displayResult(result);
            }
        } catch (NumberFormatException ex) {
            updateStatus("Error: Invalid Account ID.", Color.RED);
            viewTransactionHistoryPanel.displayAccountDetails(""); // Xóa nội dung cũ
        }
    }

    private void handleExit() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit the application ?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void displayResult(String result) {
        if (result.startsWith("SUCCESS:")) {
            updateStatus(result.substring("SUCCESS: ".length()), Color.GREEN);
        } else if (result.startsWith("ERROR:")) {
            updateStatus(result.substring("ERROR: ".length()), Color.RED);
        } else {
            updateStatus("Unknown error from controller: " + result, Color.RED);
        }
    }

    private void updateStatus(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            statusLabel.setForeground(color);
        });
    }
}