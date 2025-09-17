package presentation.ui.panels;

import javax.swing.*;
import java.awt.*;

public class WithdrawPanel extends JPanel {
    private JTextField accountIdField;
    private JTextField amountField;

    public WithdrawPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Withdraw"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Owner Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Account Id: "), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        accountIdField = new JTextField(20);
        add(accountIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        add(new JLabel("Amount:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        amountField = new JTextField(20);
        add(amountField, gbc);
    }

    public String getAccountId() {
        return accountIdField.getText();
    }

    public String getAmount() {
        return amountField.getText();
    }

    public void clearFields() {
        accountIdField.setText("");
        amountField.setText("");
    }

    public void requestInitialFocus() {
        accountIdField.requestFocusInWindow();
    }
}