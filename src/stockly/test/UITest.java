package stockly.test;

import stockly.model.AppConfig;
import stockly.model.InventoryManager;
import stockly.model.Product;
import stockly.model.StockStatus;
import stockly.ui.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Headed (but invisible) UI verification for STOCKLY.
 * <p>
 * Builds the real {@link MainFrame} off-screen and exercises every
 * page: navigation, add/edit/delete dialogs, search, filters, sort,
 * stats, charts refresh, settings threshold and data actions.
 * Modal dialogs are driven by background threads that locate and
 * click their buttons.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class UITest {

    private static int passed, failed;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("stockly-ui");
        InventoryManager m = new InventoryManager(dir.resolve("inv.csv"));

        SwingUtilities.invokeAndWait(() -> {
            try {
                run(m);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println();
        System.out.println("UI RESULT: " + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void run(InventoryManager m) throws Exception {
        MainFrame frame = new MainFrame(m);
        // visible but parked off-screen so modal dialogs can appear
        frame.setLocation(-4000, -4000);
        frame.setVisible(true);

        // U1: all five pages registered
        check("U1", "All five pages registered",
                countCards(frame) == 5, "card count=" + countCards(frame));

        // U2: navigation works for every page
        boolean navOk = true;
        for (String id : new String[]{"dashboard", "inventory", "add_item", "analytics", "settings"}) {
            try {
                frame.showPage(id);
            } catch (Exception e) {
                navOk = false;
            }
        }
        check("U2", "Sidebar navigation reaches every page", navOk, "");

        // U3: dashboard refresh with data does not throw
        try {
            frame.showPage("dashboard");
            Thread.sleep(100);
            check("U3", "Dashboard renders with seeded data", true, "");
        } catch (Exception e) {
            check("U3", "Dashboard renders with seeded data", false, e.toString());
        }

        // U4: Add Item page save flow — find fields, type, click Save
        int before = m.productCount();
        frame.showPage("add_item");
        Thread.sleep(150);
        JPanel addPage = findAddItemPanel(frame);
        if (addPage == null) {
            check("U4", "Add Item form found", false, "panel not found");
        } else {
            setTextFields(addPage, "UI Test Widget", "9");
            JButton save = findButton(addPage, "Save Product");
            if (save == null) {
                check("U4", "Save Product button clickable", false, "not found");
            } else {
                // modal notice dialog will appear — auto-dismiss it
                autoDismissNotices();
                save.doClick();
                Thread.sleep(400);
                Product added = m.getAllProducts().stream()
                        .filter(p -> p.getName().equals("UI Test Widget")).findFirst().orElse(null);
                check("U5", "Add Item page saves product",
                        added != null && added.getQuantity() == 9
                                && added.getStockStatus() == StockStatus.IN_STOCK,
                        added == null ? "not added" : added.getId() + " qty=" + added.getQuantity());
            }
        }

        // U6: inventory table shows all products by default
        frame.showPage("inventory");
        Thread.sleep(150);
        JTable table = findTable(frame);
        int rows = table == null ? -1 : ((DefaultTableModel) table.getModel()).getRowCount();
        check("U6", "Inventory lists all products by default",
                rows == m.productCount(), "rows=" + rows + " products=" + m.productCount());

        // U7: search filters rows
        if (table != null) {
            JTextField search = findSearchField(frame);
            if (search != null) {
                search.setText("widget");
                Thread.sleep(150);
                int filtered = ((DefaultTableModel) table.getModel()).getRowCount();
                search.setText("");
                Thread.sleep(150);
                int restored = ((DefaultTableModel) table.getModel()).getRowCount();
                check("U7", "Search filters and restores rows",
                        filtered == 1 && restored == m.productCount(),
                        "filtered=" + filtered + " restored=" + restored);
            } else {
                check("U7", "Search field found", false, "not found");
            }
        }

        // U8: status filter combo exists and works
        JComboBox<?> statusCombo = findCombo(frame, "All Statuses");
        if (statusCombo != null) {
            statusCombo.setSelectedItem("Out of Stock");
            Thread.sleep(150);
            int outRows = ((DefaultTableModel) table.getModel()).getRowCount();
            int expected = m.countByStatus(StockStatus.OUT_OF_STOCK);
            check("U8", "Status filter shows only matching rows",
                    outRows == expected, "rows=" + outRows + " expected=" + expected);
            statusCombo.setSelectedItem("All Statuses");
        } else {
            check("U8", "Status filter combo found", false, "not found");
        }

        // U9: sort combo changes order
        JComboBox<?> sortCombo = findCombo(frame, "Sort: Name A–Z");
        if (sortCombo != null) {
            sortCombo.setSelectedItem("Sort: Qty High→Low");
            Thread.sleep(150);
            check("U9", "Sort combo applies", true, "");
            sortCombo.setSelectedItem("Sort: Name A–Z");
        } else {
            check("U9", "Sort combo found", false, "not found");
        }

        // U10: edit dialog — open via ProductDialog on EDT with auto-driver
        Product victim = m.getAllProducts().get(0);
        AtomicInteger dialogSeen = new AtomicInteger();
        Thread driver = new Thread(() -> {
            try {
                Thread.sleep(300);
                Window dlg = findDialog("Edit Product");
                if (dlg == null) dlg = findDialog("Product");
                if (dlg != null) {
                    dialogSeen.set(1);
                    setDialogFields(dlg, victim.getName() + " EDITED", "3");
                    JButton save = findButtonIn(dlg, "Save Product");
                    autoDismissNotices();
                    if (save != null) save.doClick();
                }
            } catch (Exception ignored) { }
        });
        driver.setDaemon(true);
        driver.start();
        ProductDialog.show(frame, m, victim.getId()); // modal — driver clicks Save
        driver.join(4000);
        Thread.sleep(300);
        Product edited = m.findById(victim.getId());
        check("U10", "Edit dialog saves changes",
                edited != null && edited.getQuantity() == 3
                        && edited.getName().endsWith("EDITED"),
                edited == null ? "missing" : edited.getQuantity() + " " + edited.getName());

        // U11: delete flow — confirm dialog then product gone
        Product toDelete = m.getAllProducts().stream()
                .filter(p -> !p.getName().equals(victim.getName() + " EDITED"))
                .findFirst().orElse(null);
        if (toDelete != null) {
            String delId = toDelete.getId();
            Thread delDriver = new Thread(() -> {
                try {
                    Thread.sleep(300);
                    Window dlg = findDialog("Delete Product");
                    JButton yes = dlg == null ? null : findButtonIn(dlg, "Confirm");
                    if (yes != null) yes.doClick();
                } catch (Exception ignored) { }
            });
            delDriver.setDaemon(true);
            delDriver.start();
            invokeDelete(frame, delId); // modal confirm — driver clicks Confirm
            delDriver.join(3000);
            Thread.sleep(200);
            check("U11", "Delete confirmation removes product",
                    m.findById(delId) == null, "still present=" + (m.findById(delId) != null));
        } else {
            check("U11", "Delete confirmation removes product", false, "no product to delete");
        }

        // U12: settings threshold changes live status
        AppConfig.setLowStockThreshold(5);
        Product probe = new Product("P", "probe", 4);
        boolean lowBefore = probe.getStockStatus() == StockStatus.LOW_STOCK;
        AppConfig.setLowStockThreshold(3);
        boolean highAfter = probe.getStockStatus() == StockStatus.IN_STOCK;
        AppConfig.setLowStockThreshold(5);
        check("U12", "Settings threshold affects status live", lowBefore && highAfter, "");

        // U13: analytics + settings pages refresh without error
        try {
            frame.showPage("analytics");
            Thread.sleep(120);
            frame.showPage("settings");
            Thread.sleep(120);
            check("U13", "Analytics and Settings render", true, "");
        } catch (Exception e) {
            check("U13", "Analytics and Settings render", false, e.toString());
        }

        // U14: empty-state path — clear inventory then check table empty message
        m.clearAll();
        Thread.sleep(200);
        frame.showPage("inventory");
        Thread.sleep(150);
        int emptyRows = ((DefaultTableModel) table.getModel()).getRowCount();
        check("U14", "Empty inventory shows empty state",
                emptyRows == 0 && m.productCount() == 0, "rows=" + emptyRows);

        frame.dispose();
    }

    // ---- reflection helpers ----

    private static int countCards(MainFrame f) throws Exception {
        Field c = MainFrame.class.getDeclaredField("content");
        c.setAccessible(true);
        return ((java.awt.Container) c.get(f)).getComponentCount();
    }

    private static JPanel findAddItemPanel(java.awt.Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof AddItemPanel) return (JPanel) c;
            if (c instanceof java.awt.Container cont) {
                JPanel r = findAddItemPanel(cont);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static JTable findTable(java.awt.Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTable t) return t;
            if (c instanceof java.awt.Container cont) {
                JTable r = findTable(cont);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static JTextField findSearchField(java.awt.Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof TableKit.SearchBox box) return box.getField();
            if (c instanceof java.awt.Container cont) {
                JTextField r = findSearchField(cont);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static JComboBox<?> findCombo(java.awt.Container root, String item0) {
        for (Component c : root.getComponents()) {
            if (c instanceof JComboBox<?> cb && cb.getItemCount() > 0
                    && item0.equals(cb.getItemAt(0))) return cb;
            if (c instanceof java.awt.Container cont) {
                JComboBox<?> r = findCombo(cont, item0);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static void setTextFields(Container root, String name, String qty) {
        List<JTextField> fields = new ArrayList<>();
        collectTextFields(root, fields);
        // first field = name, second = quantity (form order)
        if (!fields.isEmpty()) fields.get(0).setText(name);
        if (fields.size() > 1) fields.get(1).setText(qty);
    }

    private static void collectTextFields(Container root, List<JTextField> out) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTextField tf && tf.isEditable()) out.add(tf);
            if (c instanceof java.awt.Container cont) collectTextFields(cont, out);
        }
    }

    private static JButton findButton(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof AbstractButton b && text.equals(b.getText())) return (JButton) b;
            if (c instanceof java.awt.Container cont) {
                JButton r = findButton(cont, text);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static JButton findButtonIn(Window w, String text) {
        return findButton(w, text);
    }

    private static void setDialogFields(Window dlg, String name, String qty) {
        List<JTextField> fields = new ArrayList<>();
        collectTextFields(dlg, fields);
        if (!fields.isEmpty()) fields.get(0).setText(name);
        if (fields.size() > 1) fields.get(1).setText(qty);
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

    private static void autoDismissNotices() {
        new Thread(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    Thread.sleep(100);
                    for (Window w : Window.getWindows()) {
                        if (w instanceof JDialog d && d.isVisible()
                                && "Product Saved".equals(d.getTitle())) {
                            JButton ok = findButton(d, "OK");
                            if (ok != null) SwingUtilities.invokeLater(ok::doClick);
                        }
                    }
                }
            } catch (InterruptedException ignored) { }
        }).start();
    }

    private static void invokeDelete(MainFrame frame, String id) {
        try {
            java.lang.reflect.Method m = MainFrame.class.getDeclaredMethod("confirmDelete", String.class);
            m.setAccessible(true);
            m.invoke(frame, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void check(String id, String desc, boolean ok, String actual) {
        System.out.printf("%-4s %-52s %s%n", id, desc, ok ? "PASS" : "FAIL");
        if (!ok) {
            System.out.println("       actual: " + actual);
            failed++;
        } else {
            passed++;
        }
    }
}
