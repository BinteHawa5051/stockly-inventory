package stockly.test;

import stockly.model.InventoryManager;
import stockly.model.Product;
import stockly.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures genuine screenshots of EVERY STOCKLY screen and state for
 * the OEL report. Drives the real {@link MainFrame} on screen with
 * generous delays so nothing is blurred, and uses {@link Robot} for
 * pixel capture — nothing is mocked.
 * <p>
 * Covers: all 5 pages, search, status filter, sort, add-form states
 * (empty / filled / validation error), success notice, edit dialog,
 * details dialog, delete confirmation, analytics, settings, bell
 * notification, needs-attention filter and the empty-inventory state.
 * Output: {@code screenshots/NN_name.png}.
 *
 * @author Tooba Akram and Abeeha Noor
 * @version 1.0
 */
public class ReportShots {

    private static final Robot ROBOT;
    private static MainFrame frame;
    private static InventoryManager manager;

    static {
        try {
            ROBOT = new Robot();
            ROBOT.setAutoDelay(250);
        } catch (AWTException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void main(String[] args) throws Exception {
        Path out = Paths.get("screenshots");
        Files.createDirectories(out);

        manager = new InventoryManager(Paths.get("inventory.csv"));
        SwingUtilities.invokeAndWait(() -> {
            frame = new MainFrame(manager);
            frame.setSize(1280, 800);
            frame.setLocation(40, 30);
            frame.setVisible(true);
        });
        settle(1800);

        // ---------- PAGE SHOTS ----------
        pageShot(out, "01_dashboard", Sidebar.DASHBOARD);
        pageShot(out, "02_inventory", Sidebar.INVENTORY);
        pageShot(out, "10_add_item_empty", Sidebar.ADD_ITEM);
        pageShot(out, "13_analytics", Sidebar.ANALYTICS);
        pageShot(out, "14_settings", Sidebar.SETTINGS);

        // ---------- INVENTORY INTERACTIONS ----------
        searchShot(out);
        statusFilterShot(out);
        sortShot(out);
        attentionShot(out);

        // ---------- ADD ITEM FORM STATES ----------
        filledFormShot(out);
        validationShot(out);
        successShot(out);

        // ---------- DIALOGS ----------
        editDialogShot(out);
        detailsDialogShot(out);
        deleteShot(out);
        bellShot(out);

        // ---------- EMPTY STATE (last — restores CSV afterwards) ----------
        emptyStateShot(out);

        frame.dispose();
        System.out.println("DONE — all screenshots in screenshots/");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void settle(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }

    private static void pageShot(Path out, String name, String pageId) throws Exception {
        SwingUtilities.invokeAndWait(() -> frame.showPage(pageId));
        settle(1400);
        save(out, name + ".png", frame.getBounds());
        System.out.println("captured " + name);
    }

    private static void save(Path out, String name, Rectangle bounds) throws Exception {
        BufferedImage img = ROBOT.createScreenCapture(bounds);
        ImageIO.write(img, "png", out.resolve(name).toFile());
    }

    private static List<Component> allComponents(Container root) {
        List<Component> list = new ArrayList<>();
        collect(root, list);
        return list;
    }

    private static void collect(Container c, List<Component> out) {
        for (Component k : c.getComponents()) {
            out.add(k);
            if (k instanceof Container cc) collect(cc, out);
        }
    }

    private static Window findDialog(String titlePart) {
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.isVisible()
                    && d.getTitle() != null && d.getTitle().contains(titlePart)) {
                return w;
            }
        }
        return null;
    }

    private static JButton findButton(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof AbstractButton b && text.equals(b.getText())) return (JButton) b;
            if (c instanceof Container cc) {
                JButton r = findButton(cc, text);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static void typeIntoSearch(String text) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame.showPage(Sidebar.INVENTORY);
            for (Component c : allComponents(frame)) {
                if (c instanceof TableKit.SearchBox box) {
                    box.getField().setText(text);
                    return;
                }
            }
        });
    }

    private static void clearSearch() throws Exception {
        typeIntoSearch("");
    }

    private static void selectComboItem(String firstItem, String wanted) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (Component c : allComponents(frame)) {
                if (c instanceof JComboBox<?> cb && cb.getItemCount() > 0
                        && firstItem.equals(cb.getItemAt(0))
                        && wanted.equals(cb.getSelectedItem())) {
                    cb.setSelectedItem(wanted);
                    return;
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Shots
    // ------------------------------------------------------------------

    private static void searchShot(Path out) throws Exception {
        typeIntoSearch("mug");
        settle(1200);
        save(out, "03_inventory_search.png", frame.getBounds());
        System.out.println("captured 03_inventory_search");
        clearSearch();
        settle(800);
    }

    private static void statusFilterShot(Path out) throws Exception {
        selectComboItem("All Statuses", "Low Stock");
        settle(1200);
        save(out, "04_inventory_status_filter.png", frame.getBounds());
        System.out.println("captured 04_inventory_status_filter");
        selectComboItem("All Statuses", "All Statuses");
        settle(800);
    }

    private static void sortShot(Path out) throws Exception {
        selectComboItem("Sort: Name A–Z", "Sort: Qty High→Low");
        settle(1200);
        save(out, "05_inventory_sorted.png", frame.getBounds());
        System.out.println("captured 05_inventory_sorted");
        selectComboItem("Sort: Name A–Z", "Sort: Name A–Z");
        settle(800);
    }

    private static void attentionShot(Path out) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame.showPage(Sidebar.INVENTORY);
            // find InventoryPanel and call filterAttention via public API
            for (Component c : allComponents(frame)) {
                if (c instanceof InventoryPanel ip) {
                    ip.filterAttention();
                    return;
                }
            }
        });
        settle(1200);
        save(out, "06_inventory_needs_attention.png", frame.getBounds());
        System.out.println("captured 06_inventory_needs_attention");
        selectComboItem("All Statuses", "All Statuses");
        settle(800);
    }

    private static void filledFormShot(Path out) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame.showPage(Sidebar.ADD_ITEM);
            List<JTextField> fields = new ArrayList<>();
            addItemFields().forEach(fields::add);
            if (fields.size() >= 2) {
                fields.get(0).setText("Wireless Keyboard");
                fields.get(1).setText("8");
            }
        });
        settle(1200);
        save(out, "11_add_item_filled.png", frame.getBounds());
        System.out.println("captured 11_add_item_filled");
    }

    private static void validationShot(Path out) throws Exception {
        // clear fields then click Save -> real inline validation error
        SwingUtilities.invokeAndWait(() -> {
            frame.showPage(Sidebar.ADD_ITEM);
            List<JTextField> fields = new ArrayList<>();
            addItemFields().forEach(fields::add);
            for (JTextField f : fields) f.setText("");
            JButton save = findButton(frame, "Save Product");
            if (save != null) save.doClick();
        });
        settle(1400);
        save(out, "12_validation_error.png", frame.getBounds());
        System.out.println("captured 12_validation_error");
    }

    private static void successShot(Path out) throws Exception {
        // fill + save -> modal notice appears; driver captures it
        Thread driver = new Thread(() -> {
            try {
                for (int i = 0; i < 40; i++) {
                    Thread.sleep(200);
                    for (Window w : Window.getWindows()) {
                        if (w instanceof JDialog d && d.isVisible()
                                && "Product Saved".equals(d.getTitle())) {
                            settle(500);
                            save(out, "09_success_notification.png", d.getBounds());
                            System.out.println("captured 09_success_notification");
                            JButton ok = findButton(d, "OK");
                            if (ok != null) SwingUtilities.invokeLater(ok::doClick);
                            return;
                        }
                    }
                }
            } catch (Exception e) { System.out.println("success: " + e); }
        });
        driver.setDaemon(true);
        driver.start();
        SwingUtilities.invokeAndWait(() -> {
            frame.showPage(Sidebar.ADD_ITEM);
            List<JTextField> fields = new ArrayList<>();
            addItemFields().forEach(fields::add);
            if (fields.size() >= 2) {
                fields.get(0).setText("Photo Frame Oak");
                fields.get(1).setText("14");
            }
            JButton save = findButton(frame, "Save Product");
            if (save != null) save.doClick();
        });
        driver.join(12000);
        settle(800);
    }

    private static void editDialogShot(Path out) throws Exception {
        Product p = manager.getAllProducts().get(0);
        Thread driver = new Thread(() -> {
            try {
                settle(700);
                Window dlg = findDialog("Edit Product");
                if (dlg != null) {
                    settle(500);
                    save(out, "07_edit_product.png", dlg.getBounds());
                    System.out.println("captured 07_edit_product");
                    JButton cancel = findButton(dlg, "Cancel");
                    if (cancel != null) SwingUtilities.invokeLater(cancel::doClick);
                }
            } catch (Exception e) { System.out.println("edit: " + e); }
        });
        driver.setDaemon(true);
        driver.start();
        SwingUtilities.invokeAndWait(
                () -> ProductDialog.show(frame, manager, p.getId()));
        driver.join(8000);
        settle(600);
    }

    private static void detailsDialogShot(Path out) throws Exception {
        Product p = manager.getAllProducts().get(1);
        Thread driver = new Thread(() -> {
            try {
                settle(700);
                Window dlg = findDialog("Product Details");
                if (dlg != null) {
                    settle(500);
                    save(out, "08_product_details.png", dlg.getBounds());
                    System.out.println("captured 08_product_details");
                    JButton close = findButton(dlg, "Close");
                    if (close != null) SwingUtilities.invokeLater(close::doClick);
                }
            } catch (Exception e) { System.out.println("details: " + e); }
        });
        driver.setDaemon(true);
        driver.start();
        SwingUtilities.invokeAndWait(
                () -> ProductDetailsDialog.show(frame, manager, p.getId()));
        driver.join(8000);
        settle(600);
    }

    private static void deleteShot(Path out) throws Exception {
        Product p = manager.getAllProducts().get(2);
        Thread driver = new Thread(() -> {
            try {
                settle(700);
                Window dlg = findDialog("Delete Product");
                if (dlg != null) {
                    settle(500);
                    save(out, "15_delete_confirmation.png", dlg.getBounds());
                    System.out.println("captured 15_delete_confirmation");
                    JButton cancel = findButton(dlg, "Cancel");
                    if (cancel != null) SwingUtilities.invokeLater(cancel::doClick);
                }
            } catch (Exception e) { System.out.println("delete: " + e); }
        });
        driver.setDaemon(true);
        driver.start();
        Method m = MainFrame.class.getDeclaredMethod("confirmDelete", String.class);
        m.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try { m.invoke(frame, p.getId()); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        driver.join(8000);
        settle(600);
    }

    private static void bellShot(Path out) throws Exception {
        Thread driver = new Thread(() -> {
            try {
                for (int i = 0; i < 40; i++) {
                    Thread.sleep(200);
                    for (Window w : Window.getWindows()) {
                        if (w instanceof JDialog d && d.isVisible()
                                && "Notifications".equals(d.getTitle())) {
                            settle(500);
                            save(out, "16_notifications.png", d.getBounds());
                            System.out.println("captured 16_notifications");
                            JButton ok = findButton(d, "OK");
                            if (ok != null) SwingUtilities.invokeLater(ok::doClick);
                            return;
                        }
                    }
                }
            } catch (Exception e) { System.out.println("bell: " + e); }
        });
        driver.setDaemon(true);
        driver.start();
        Method m = MainFrame.class.getDeclaredMethod("showNotifications");
        m.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try { m.invoke(frame); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        driver.join(12000);
        settle(600);
    }

    private static void emptyStateShot(Path out) throws Exception {
        // backup CSV, clear inventory, capture empty state, restore
        Path csv = Paths.get("inventory.csv");
        Path backup = Paths.get("inventory-backup.csv");
        Files.copy(csv, backup, StandardCopyOption.REPLACE_EXISTING);
        SwingUtilities.invokeAndWait(() -> manager.clearAll());
        SwingUtilities.invokeAndWait(() -> frame.showPage(Sidebar.INVENTORY));
        settle(1400);
        save(out, "17_empty_inventory.png", frame.getBounds());
        System.out.println("captured 17_empty_inventory");
        // restore data file for future runs
        Files.copy(backup, csv, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(backup);
    }

    /** Editable text fields inside the AddItemPanel only (hidden pages
     *  like the inventory search box must not be touched). */
    private static List<JTextField> addItemFields() {
        List<JTextField> fields = new ArrayList<>();
        for (Component c : allComponents(frame)) {
            if (c instanceof AddItemPanel panel) {
                collectText(panel, fields);
                break;
            }
        }
        return fields;
    }

    private static void collectText(Container root, List<JTextField> out) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTextField tf && tf.isEditable()) out.add(tf);
            if (c instanceof Container cc) collectText(cc, out);
        }
    }
}
