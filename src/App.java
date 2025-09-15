import business.service.BankService;
import data.DatabaseManager;
import presentation.console.Menu;
import presentation.controller.BankController;

public class App {
    public static void main(String[] args) {
        // 1. Initital Data access layer
        DatabaseManager databaseManager = new DatabaseManager();

        // 2. Initial Business Logic layer
        BankService bankService = new BankService(databaseManager);

        // 3. Initial Presentation layer
        Menu menu = new Menu();
        BankController bankController = new BankController(bankService, menu);

        // start
        bankController.start();
    }
}
