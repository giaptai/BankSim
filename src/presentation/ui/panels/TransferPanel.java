package presentation.ui.panels;

import javax.swing.*;
import java.awt.*;

public class TransferPanel extends JPanel {
    private JTextField fromAccountIdField;
    private JTextField toAccountIdField;
    private JTextField amountField;

    public TransferPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Transfer Funds"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // From Account ID
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("From Account ID:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        fromAccountIdField = new JTextField(20);
        add(fromAccountIdField, gbc);

        // To Account ID
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        add(new JLabel("To Account ID:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        toAccountIdField = new JTextField(20);
        add(toAccountIdField, gbc);

        // Amount
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        add(new JLabel("Amount:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        amountField = new JTextField(20);
        add(amountField, gbc);
    }

    public String getFromAccountId() {
        return fromAccountIdField.getText();
    }

    public String getToAccountId() {
        return toAccountIdField.getText();
    }

    public String getAmount() {
        return amountField.getText();
    }

    public void clearFields() {
        fromAccountIdField.setText("");
        toAccountIdField.setText("");
        amountField.setText("");
    }

    public void requestInitialFocus() {
        fromAccountIdField.requestFocusInWindow();
    }
}