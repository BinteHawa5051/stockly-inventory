package stockly.ui;

import stockly.model.InventoryManager;
import stockly.model.Product;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.nio.file.Paths;

/**
 * Application shell: sidebar + top bar + card-switched content pages.
 * <p>
 * Owns the shared {@link InventoryManager} and the four pages
 * (Dashboard, Inventory, Add Item, Analytics, Settings). Every page
 * refreshes itself through the manager's {@code InventoryListener} so
 * statistics, tables and charts always show current data.
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
        setSize(1240, 780);
        setLocationRelativeTo(null);

        // root layout: sidebar west, right side = topbar + content
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);

        Sidebar sidebar = new Sidebar(this::navigate);
        this.sidebarRef = sidebar;
        root.add(sidebar, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Theme.BG);

        content.setBackground(Theme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        dashboard = new DashboardPanel(manager);
        inventory = new InventoryPanel(manager, new InventoryPanel.ProductActions() {
            @Override public void onAdd() { ProductDialog.show(MainFrame.this, manager, null); }
            @Override public void onEdit(String id) { ProductDialog.show(MainFrame.this, manager, id); }
            @Override public void onDelete(String id) { confirmDelete(id); }
        });
        analytics = new AnalyticsPanel(manager);
        settings = new SettingsPanel(Paths.get("inventory.csv").toFile());

        topBar = new TopBar(event -> {
            // global search: jump to inventory and apply the filter
            inventory.setGlobalSearch(event.getActionCommand());
            navigateById(Sidebar.INVENTORY);
        });
        right.add(topBar, BorderLayout.NORTH);

        // Add Item page hosts the same form in a dedicated panel
        content.add(dashboard, Sidebar.DASHBOARD);
        content.add(inventory, Sidebar.INVENTORY);
        content.add(buildAddPage(), Sidebar.ADD_ITEM);
        content.add(analytics, Sidebar.ANALYTICS);
        content.add(settings, Sidebar.SETTINGS);

        // refresh everything whenever inventory changes
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

    /** Dedicated "Add New Item" page wrapping the product dialog inline. */
    private JPanel buildAddPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(Theme.BG);
        page.setBorder(BorderFactory.createEmptyBorder(20, 28, 24, 28));

        UIComponents.Card card = new UIComponents.Card();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

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
        // update sidebar highlight — find via the listener trick:
        // Sidebar keeps its own active state; we re-set it here.
        sidebarSetActive(id);
        switch (id) {
            case Sidebar.DASHBOARD -> topBar.setPage("Dashboard", "Your inventory at a glance");
            case Sidebar.INVENTORY -> topBar.setPage("Inventory", "Manage products, stock and status");
            case Sidebar.ADD_ITEM -> topBar.setPage("Add New Item", "Create a product record");
            case Sidebar.ANALYTICS -> topBar.setPage("Analytics", "Distribution and trends");
            case Sidebar.SETTINGS -> topBar.setPage("Settings", "About STOCKLY");
        }
    }

    private Sidebar sidebarRef;

    private void sidebarSetActive(String id) {
        if (sidebarRef != null) {
            sidebarRef.setActive(id);
        }
    }

    /** Delete flow: confirmation dialog naming the product, then refresh. */
    private void confirmDelete(String id) {
        Product p = manager.findById(id);
        if (p == null) {
            JOptionPane.showMessageDialog(this,
                    "This product no longer exists — it may already have been deleted.",
                    "Product Not Found", JOptionPane.INFORMATION_MESSAGE);
            inventory.refresh();
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete \"" + p.getName() + "\" (" + p.getId() + ")?\n"
                        + "This will remove the product and its stock record.",
                "Delete Product", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                manager.deleteProduct(id);
            } catch (InventoryManager.InventoryException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
