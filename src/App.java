import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.SwingUtilities;

import business.service.BankService;
import data.DatabaseManagerFactory;
import data.IDatabaseManager;
import presentation.ui.ThreadTrackerGUI;
import resources.Constants;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ThreadTrackerGUI trackerGUI = new ThreadTrackerGUI();
            IDatabaseManager databaseManager = DatabaseManagerFactory.create(Constants.DB_TYPE_POSTGRES);
            BankService bankService = new BankService(databaseManager, trackerGUI);

            trackerGUI.setBankService(bankService);
            trackerGUI.setVisible(true);

            trackerGUI.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    bankService.close();
                    System.out.println("BankService closed gracefully.");
                }
            });
        });
    }
}
