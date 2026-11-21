package stockly.test;

import stockly.model.InventoryManager;
import stockly.ui.MainFrame;
import stockly.ui.Sidebar;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Captures real screenshots of every application page using
 * {@link Robot} screen capture. Run while a display is available:
 * <pre>
 *   java -cp classes stockly.test.Screenshot
 * </pre>
 * Output PNGs are written to {@code docs/screenshots/}.
 *
 * @author Tooba Akram &amp; Abeeha Noor
 * @version 1.0
 */
public final class Screenshot {

    private Screenshot() {
    }

    public static void main(String[] args) throws Exception {
        Path outDir = Paths.get("docs", "screenshots");
        Files.createDirectories(outDir);

        InventoryManager manager = new InventoryManager(Paths.get("inventory.csv"));
        MainFrame frame = new MainFrame(manager);
        frame.setSize(1280, 800);
        frame.setLocation(60, 40);
        frame.setVisible(true);
        Thread.sleep(1200);

        Robot robot = new Robot();
        robot.setAutoDelay(150);

        String[][] pages = {
                {Sidebar.DASHBOARD, "dashboard"},
                {Sidebar.INVENTORY, "inventory"},
                {Sidebar.ADD_ITEM, "add-item"},
                {Sidebar.ANALYTICS, "analytics"},
                {Sidebar.SETTINGS, "settings"},
        };

        for (String[] page : pages) {
            frame.showPage(page[0]);
            frame.toFront();
            Thread.sleep(900);
            Rectangle bounds = frame.getBounds();
            BufferedImage img = robot.createScreenCapture(bounds);
            ImageIO.write(img, "png", outDir.resolve(page[1] + ".png").toFile());
            System.out.println("captured " + page[1] + ".png");
        }

        frame.dispose();
        System.out.println("done");
    }
}
