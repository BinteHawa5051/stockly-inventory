package stockly;

import stockly.model.InventoryManager;
import stockly.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Paths;

/**
 * STOCKLY — Premium Inventory Management System.
 * <p>
 * Entry point of the application. Configures Swing to use the system
 * look-and-feel (so native widgets like combo boxes and scrollbars
 * match the OS) and launches the main window on the event-dispatch
 * thread as required by Swing.
 *
 * @author Tooba Akram and Abeeha Noor
 * @version 1.0
 */
public final class Main {

    private Main() {
        // utility class — not instantiable
    }

    /**
     * Application entry point.
     *
     * @param args command line arguments (unused)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Keep native chrome but our custom-painted surfaces carry the theme.
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // fall back to the default Swing L&F
            }

            stockly.model.AppConfig.load();
            InventoryManager manager = new InventoryManager(Paths.get("inventory.csv"));
            MainFrame frame = new MainFrame(manager);
            frame.setVisible(true);
        });
    }
}
