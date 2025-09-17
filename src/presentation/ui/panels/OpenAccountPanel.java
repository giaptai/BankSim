package presentation.ui.panels;

import javax.swing.*;
import java.awt.*;

public class OpenAccountPanel extends JPanel {
    private JTextField ownerNameField;
    private JTextField initialBalanceField;

    public OpenAccountPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Open account"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Owner Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Owner name:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        ownerNameField = new JTextField(20);
        add(ownerNameField, gbc);

        // Initial Balance
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        add(new JLabel("Initial balance:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        initialBalanceField = new JTextField(20);
        add(initialBalanceField, gbc);
    }

    public String getOwnerName() {
        return ownerNameField.getText();
    }

    public String getInitialBalance() {
        return initialBalanceField.getText();
    }

    public void clearFields() {
        ownerNameField.setText("");
        initialBalanceField.setText("");
    }

    public void requestInitialFocus() {
        ownerNameField.requestFocusInWindow();
    }
}