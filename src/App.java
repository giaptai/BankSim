import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.SwingUtilities;

import client.ui.ThreadTrackerGUI;
import common.Constants;
import data.GroupManager;
import server.SoaptaServer;
import data.Group;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ThreadTrackerGUI trackerGUI = new ThreadTrackerGUI();
            Group databaseManager = GroupManager.create(Constants.DB_TYPE_POSTGRES);
            SoaptaServer bankService = new SoaptaServer(databaseManager, trackerGUI);

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
