import javax.swing.SwingUtilities;

import business.service.BankService;
import data.DatabaseManager;
import presentation.console.Menu;
import presentation.controller.BankController;
import presentation.controller.SwingBankController;
import presentation.ui.BankSwingGUI;

public class App {
    public static void main(String[] args) {
        // 1. Initital Data access layer
        DatabaseManager databaseManager = new DatabaseManager();

        // 2. Initial Business Logic layer
        BankService bankService = new BankService(databaseManager);

        // 3. Initial Presentation layer
        // Menu menu = new Menu();
        // BankController bankController = new BankController(bankService, menu);

        // start
        // bankController.start();

        SwingBankController swingController = new SwingBankController(bankService);
        BankSwingGUI bankSwingGUI = new BankSwingGUI(swingController);
        SwingUtilities.invokeLater(() -> bankSwingGUI.setVisible(true));
    }
}
