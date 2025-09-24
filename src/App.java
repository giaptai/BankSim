import javax.swing.SwingUtilities;


import presentation.ui.ThreadTrackerGUI;

public class App {
    public static void main(String[] args) {
        // 1. Initital Data access layer
        // DatabaseManager databaseManager = new DatabaseManager();

        // 2. Initial Business Logic layer
        // BankService bankService = new BankService(databaseManager);

        // 3. Initial Presentation layer
        // Menu menu = new Menu();
        // BankController bankController = new BankController(bankService, menu);

        // start
        // bankController.start();

        ThreadTrackerGUI trackerGUI = new ThreadTrackerGUI();
        SwingUtilities.invokeLater(()->trackerGUI.setVisible(true));
    }
}
