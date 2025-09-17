package presentation.ui.panels;

import javax.swing.*;
import java.awt.*;

public class MainMenuPanel extends JPanel {

    private JTextField menuInputField; // Thêm trường nhập liệu cho menu

    public MainMenuPanel() {
        setLayout(new GridBagLayout()); // Sử dụng GridBagLayout để căn giữa
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER; // Mỗi thành phần trên một hàng mới
        gbc.anchor = GridBagConstraints.CENTER; // Căn giữa
        gbc.insets = new Insets(5, 0, 5, 0); // Khoảng cách giữa các dòng

        JLabel title = new JLabel("<html><font size='+2'><b>HỆ THỐNG NGÂN HÀNG</b></font></html>");
        add(title, gbc);

        add(new JLabel(" "), gbc); // Khoảng trống

        add(new JLabel("1. Open account"), gbc);
        add(new JLabel("2. Deposit"), gbc);
        add(new JLabel("3. Withdraw"), gbc);
        add(new JLabel("4. Tranfer"), gbc);
        add(new JLabel("5. See detail account"), gbc);
        add(new JLabel("6. See history transaction"), gbc);
        add(new JLabel("0. Exit"), gbc);

        add(new JLabel(" "), gbc); // Khoảng trống

        JLabel instruction = new JLabel("<html><i>Enter number and click ENTER to choose</i></html>");
        add(instruction, gbc);

        // Thêm trường nhập liệu cho menu
        gbc.insets = new Insets(15, 0, 5, 0); // Khoảng cách lớn hơn
        menuInputField = new JTextField(5); // Kích thước nhỏ, chỉ để nhập số menu
        menuInputField.setEditable(false); // Không cho phép nhập trực tiếp bằng bàn phím
        menuInputField.setHorizontalAlignment(SwingConstants.CENTER);
        menuInputField.setFont(new Font("SansSerif", Font.BOLD, 16));
        menuInputField.setBackground(Color.WHITE); // Nền trắng để dễ nhìn
        add(menuInputField, gbc);
    }

    public JTextField getMenuInputField() {
        return menuInputField;
    }

    public void clearMenuInput() {
        menuInputField.setText("");
    }
}