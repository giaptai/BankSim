package presentation.ui.panels;

import javax.swing.*;
import java.awt.*;

public class ViewBalancePanel extends JPanel {
    private JTextField accountIdField;
    private JTextArea detailsArea; // Để hiển thị chi tiết tài khoản

    public ViewBalancePanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("See Account Details"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Account ID Input
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Account ID:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        accountIdField = new JTextField(20);
        add(accountIdField, gbc);

        // Details Display Area
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2; // Chiếm 2 cột
        gbc.weighty = 1.0; // Cho phép giãn nở theo chiều dọc
        gbc.fill = GridBagConstraints.BOTH; // Lấp đầy không gian
        detailsArea = new JTextArea(5, 20); // 5 hàng, 20 cột
        detailsArea.setEditable(false); // Không cho phép chỉnh sửa
        detailsArea.setLineWrap(true); // Tự động xuống dòng
        detailsArea.setWrapStyleWord(true); // Xuống dòng theo từ
        JScrollPane scrollPane = new JScrollPane(detailsArea); // Thêm thanh cuộn
        add(scrollPane, gbc);
    }

    public String getAccountId() {
        return accountIdField.getText();
    }

    public void displayAccountDetails(String details) {
        detailsArea.setText(details);
    }

    public void clearFields() {
        accountIdField.setText("");
        detailsArea.setText(""); // Xóa cả khu vực hiển thị chi tiết
    }

    public void requestInitialFocus() {
        accountIdField.requestFocusInWindow();
    }
}