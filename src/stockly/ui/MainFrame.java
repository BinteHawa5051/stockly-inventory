package stockly.ui;

import stockly.model.AppConfig;
import stockly.model.InventoryManager;
import stockly.model.Product;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.io.File;
import java.nio.file.Paths;

/**
 * Application shell: sidebar + top bar + card-switched content pages.
 * <p>
 * Owns the shared {@link InventoryManager} and all pages. Every page
 * refreshes itself through the manager's {@code InventoryListener} so
 * statistics, tables and charts always show current data. Destructive
 * actions route through {@link Dialogs} confirmations and honour the
 * delete-confirmation preference in {@link AppConfig}.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class MainFrame extends JFrame {

    private final InventoryManager manager;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final TopBar topBar;
    private final DashboardPanel dashboard;
    private final InventoryPanel inventory;
    private final AnalyticsPanel analytics;
    private final SettingsPanel settings;
    private final Sidebar sidebarRef;

    /**
     * Creates and shows the main window.
     *
     * @param manager shared inventory data source
     */
    public MainFrame(InventoryManager manager) {
        super("STOCKLY — Inventory Management");
        this.manager = manager;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 680));
        setSize(1280, 800);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);

        Sidebar sidebar = new Sidebar(this::navigate);
        this.sidebarRef = sidebar;
        root.add(sidebar, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Theme.BG);

        content.setBackground(Theme.BG);

        // shared callbacks
        java.util.function.Consumer<String> openEditor =
                id -> ProductDialog.show(this, manager, id);

        dashboard = new DashboardPanel(manager, openEditor);
        inventory = new InventoryPanel(manager, new InventoryPanel.ProductActions() {
            @Override public void onAdd() { ProductDialog.show(MainFrame.this, manager, null); }
            @Override public void onEdit(String id) { ProductDialog.show(MainFrame.this, manager, id); }
            @Override public void onView(String id) { ProductDetailsDialog.show(MainFrame.this, manager, id); }
            @Override public void onDelete(String id) { confirmDelete(id); }
        });
        analytics = new AnalyticsPanel(manager);
        settings = new SettingsPanel(manager, this::exportInventory,
                this::importInventory, this::clearInventory);

        topBar = new TopBar(() -> showNotifications());
        right.add(topBar, BorderLayout.NORTH);

        content.add(dashboard, Sidebar.DASHBOARD);
        content.add(inventory, Sidebar.INVENTORY);
        content.add(buildAddPage(), Sidebar.ADD_ITEM);
        content.add(analytics, Sidebar.ANALYTICS);
        content.add(settings, Sidebar.SETTINGS);

        manager.addListener(() -> {
            dashboard.refresh();
            inventory.refresh();
            analytics.refresh();
        });

        right.add(content, BorderLayout.CENTER);
        root.add(right, BorderLayout.CENTER);
        setContentPane(root);

        navigateById(Sidebar.DASHBOARD);
    }

    /** Dedicated "Add New Item" page with the inline product form. */
    private JPanel buildAddPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(Theme.BG);
        page.setBorder(BorderFactory.createEmptyBorder(20, 28, 24, 28));

        UIComponents.Card card = new UIComponents.Card();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        AddItemPanel form = new AddItemPanel(manager,
                () -> navigateById(Sidebar.INVENTORY),
                () -> navigateById(Sidebar.DASHBOARD));
        card.add(form, BorderLayout.CENTER);
        page.add(card, BorderLayout.NORTH);
        return page;
    }

    /** Switches the visible page programmatically (used by navigation and tests). */
    public void showPage(String id) {
        navigateById(id);
    }

    private void navigate(java.awt.event.ActionEvent e) {
        navigateById(e.getActionCommand());
    }

    private void navigateById(String id) {
        cards.show(content, id);
        sidebarRef.setActive(id);
        switch (id) {
            case Sidebar.DASHBOARD -> topBar.setPage("Dashboard", "Your inventory at a glance");
            case Sidebar.INVENTORY -> topBar.setPage("Inventory Management",
                    "Manage, search and organize your products");
            case Sidebar.ADD_ITEM -> topBar.setPage("Add New Product",
                    "Add a new item to your inventory");
            case Sidebar.ANALYTICS -> topBar.setPage("Inventory Analytics",
                    "Understand your stock distribution and composition");
            case Sidebar.SETTINGS -> topBar.setPage("Settings", "Preferences, data and about");
        }
    }

    // ------------------------------------------------------------------
    // Delete flow
    // ------------------------------------------------------------------

    /** Delete flow honouring the confirm-delete preference. */
    private void confirmDelete(String id) {
        Product p = manager.findById(id);
        if (p == null) {
            Dialogs.notice(this, "Product Not Found",
                    "This product no longer exists — it may already have been deleted.");
            inventory.refresh();
            return;
        }
        boolean proceed = !AppConfig.isConfirmDelete()
                || Dialogs.confirm(this, "Delete Product",
                        "Delete \"" + p.getName() + "\" (" + p.getId() + ")?\n"
                                + "This will remove the product and its stock record.");
        if (proceed) {
            try {
                manager.deleteProduct(id);
            } catch (InventoryManager.InventoryException ex) {
                Dialogs.error(this, "Delete Failed", ex.getMessage());
            }
        }
    }

    /** Bell action: shows the recent session activity as a notification. */
    private void showNotifications() {
        StringBuilder sb = new StringBuilder();
        java.util.List<stockly.model.ActivityEntry> recent =
                manager.getActivityLog().getRecent(5);
        if (recent.isEmpty()) {
            sb.append("No activity yet — add, edit or delete a product.");
        } else {
            for (stockly.model.ActivityEntry e : recent) {
                sb.append("• ").append(e.getDescription())
                        .append("  (").append(e.getTimeLabel()).append(")\n");
            }
        }
        Dialogs.notice(this, "Notifications", sb.toString().trim());
    }

    // ------------------------------------------------------------------
    // Data management (Settings page callbacks)
    // ------------------------------------------------------------------

    private void exportInventory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Inventory");
        chooser.setSelectedFile(new File("stockly-inventory.csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParent(), file.getName() + ".csv");
        }
        try {
            manager.exportCsv(file.toPath());
            Dialogs.notice(this, "Export Complete",
                    "Exported " + manager.productCount() + " products to\n" + file.getAbsolutePath());
        } catch (java.io.IOException ex) {
            Dialogs.error(this, "Export Failed", ex.getMessage());
        }
    }

    private void importInventory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Inventory");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        boolean ok = Dialogs.confirm(this, "Import Inventory",
                "This will REPLACE the entire current inventory with the contents of\n"
                        + file.getName() + ".\n\nContinue?");
        if (!ok) {
            return;
        }
        try {
            int count = manager.importCsv(file.toPath());
            Dialogs.notice(this, "Import Complete",
                    "Imported " + count + " products from " + file.getName());
        } catch (java.io.IOException ex) {
            Dialogs.error(this, "Import Failed", ex.getMessage());
        }
    }

    private void clearInventory() {
        boolean ok = Dialogs.confirm(this, "Clear Inventory",
                "This will permanently delete ALL " + manager.productCount()
                        + " products.\nThis action cannot be undone.\n\nAre you sure?");
        if (!ok) {
            return;
        }
        manager.clearAll();
        Dialogs.notice(this, "Inventory Cleared", "All products have been removed.");
    }
}
