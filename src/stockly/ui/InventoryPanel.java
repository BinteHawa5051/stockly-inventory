package stockly.ui;

import stockly.model.InventoryManager;
import stockly.model.Product;
import stockly.model.StockStatus;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.OverlayLayout;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Inventory screen: searchable, filterable, sortable product table.
 * <p>
 * Columns are Product ID, Name, Quantity, Stock Status (auto-derived
 * badge) and row actions (edit / delete). The toolbar offers a name
 * search field, a status filter, a sort selector and an add button.
 * All mutations go through {@link InventoryManager}; this panel only
 * renders state and forwards user intent.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class InventoryPanel extends JPanel {

    /** Callback interface so the frame can open the product dialog. */
    public interface ProductActions {
        /** Open the add-product dialog. */
        void onAdd();
        /** Open the edit dialog for the given product id. */
        void onEdit(String id);
        /** Ask for confirmation then delete the given product id. */
        void onDelete(String id);
    }

    private final InventoryManager manager;
    private final ProductActions actions;
    private final DefaultTableModel model;
    private final JTable table;
    private final JPanel emptyState;
    private final javax.swing.JTextField searchField = new javax.swing.JTextField();
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"All Statuses", "In Stock", "Low Stock", "Out of Stock"});
    private final JComboBox<String> sortBox = new JComboBox<>(
            new String[]{"Sort: Name A–Z", "Sort: Name Z–A", "Sort: Qty High→Low", "Sort: Qty Low→High"});

    private String globalSearch = "";

    /**
     * Creates the inventory screen.
     *
     * @param manager shared inventory data source
     * @param actions callbacks for add / edit / delete
     */
    public InventoryPanel(InventoryManager manager, ProductActions actions) {
        this.manager = manager;
        this.actions = actions;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 24, 28));

        model = new DefaultTableModel(new Object[]{"Product ID", "Product Name", "Quantity", "Stock Status", ""}, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setRowHeight(46);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0xE4EADF));
        table.setSelectionForeground(Theme.TEXT);
        table.setFont(Theme.font(13));
        table.getTableHeader().setFont(Theme.bold(11));
        table.getTableHeader().setForeground(Theme.TEXT_MUTED);
        table.getTableHeader().setBackground(Theme.SURFACE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(4).setMaxWidth(110);
        table.getColumnModel().getColumn(4).setMinWidth(110);
        table.getColumnModel().getColumn(0).setMaxWidth(90);
        table.getColumnModel().getColumn(0).setMinWidth(80);
        table.getColumnModel().getColumn(2).setMaxWidth(100);
        table.getColumnModel().getColumn(2).setMinWidth(90);
        table.getColumnModel().getColumn(3).setMaxWidth(140);
        table.getColumnModel().getColumn(3).setMinWidth(130);

        styleRenderers();
        attachRowActions();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.setBackground(Theme.SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        wrapper.add(scroll, BorderLayout.CENTER);

        emptyState = buildEmptyState();
        // OverlayLayout stacks the empty-state panel on top of the table;
        // it is only visible when the (filtered) table has no rows.
        JPanel layered = new JPanel();
        layered.setLayout(new OverlayLayout(layered));
        layered.setOpaque(false);
        layered.add(wrapper);
        layered.add(emptyState);
        add(layered, BorderLayout.CENTER);

        add(buildToolbar(), BorderLayout.NORTH);

        // initial population
        refresh();
    }

    // ------------------------------------------------------------------
    // Toolbar
    // ------------------------------------------------------------------

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new GridBagLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // local search
        searchField.setPreferredSize(new Dimension(220, 36));
        searchField.setFont(Theme.font(12));
        searchField.setForeground(Theme.TEXT);
        searchField.setCaretColor(Theme.FOREST);
        searchField.setToolTipText("Filter by product name");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });
        toolbar.add(new TopBar.SearchField(searchField), gbc);

        // status filter
        styleCombo(statusFilter);
        statusFilter.addActionListener(e -> refresh());
        toolbar.add(statusFilter, gbc);

        // sort
        styleCombo(sortBox);
        sortBox.addActionListener(e -> refresh());
        toolbar.add(sortBox, gbc);

        // spacer
        gbc.weightx = 1;
        toolbar.add(new JPanel() {{ setOpaque(false); }}, gbc);
        gbc.weightx = 0;

        UIComponents.PrimaryButton addBtn = new UIComponents.PrimaryButton("+  Add Product");
        addBtn.addActionListener(e -> actions.onAdd());
        toolbar.add(addBtn, gbc);

        return toolbar;
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setFont(Theme.font(12));
        combo.setForeground(Theme.TEXT);
        combo.setBackground(Theme.SURFACE);
        combo.setPreferredSize(new Dimension(150, 36));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
    }

    // ------------------------------------------------------------------
    // Table renderers
    // ------------------------------------------------------------------

    private void styleRenderers() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSel,
                                                           boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSel, hasFocus, row, col);
                c.setFont(Theme.font(13));
                if (!isSel) {
                    c.setForeground(Theme.TEXT);
                }
                // zebra + padding feel via renderer background
                if (row % 2 == 1 && !isSel) {
                    c.setBackground(new Color(0xFAF9F4));
                } else {
                    c.setBackground(Theme.SURFACE);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return c;
            }
        });

        // centered quantity column
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSel,
                                                           boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSel, hasFocus, row, col);
                c.setFont(Theme.bold(13));
                c.setForeground(Theme.TEXT);
                setHorizontalAlignment(CENTER);
                return c;
            }
        });

        // status badge column
        table.getColumnModel().getColumn(3).setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSel,
                                                           boolean hasFocus, int row, int col) {
                Product p = productAtRow(row);
                if (p == null) {
                    return new JLabel();
                }
                StatusBadgeShim badge = new StatusBadgeShim(p.getStockStatus());
                badge.setOpaque(false);
                return badge;
            }
        });

        // actions column
        table.getColumnModel().getColumn(4).setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSel,
                                                           boolean hasFocus, int row, int col) {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
                p.setOpaque(false);
                UIComponents.IconButton edit = new UIComponents.IconButton(Icon.EDIT);
                UIComponents.IconButton del = new UIComponents.IconButton(Icon.TRASH);
                del.setIconColor(Theme.CHART_OUT_STOCK);
                p.add(edit);
                p.add(del);
                return p;
            }
        });
    }

    private void attachRowActions() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col != 4) {
                    return;
                }
                Product p = productAtRow(row);
                if (p == null) {
                    return;
                }
                // hit-test which half of the actions cell was clicked
                Rectangle cell = table.getCellRect(row, col, false);
                int mid = cell.x + cell.width / 2;
                if (e.getX() < mid) {
                    actions.onEdit(p.getId());
                } else {
                    actions.onDelete(p.getId());
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                table.setCursor(col == 4 ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
    }

    private Product productAtRow(int viewRow) {
        // No row sorter is installed, so view order == model order.
        Object id = model.getValueAt(viewRow, 0);
        return manager.findById(String.valueOf(id));
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    /** Applies the global top-bar search text (may be empty). */
    public void setGlobalSearch(String text) {
        this.globalSearch = text == null ? "" : text.trim().toLowerCase();
        refresh();
    }

    /** Rebuilds the table from current filters and inventory data. */
    public void refresh() {
        String local = searchField.getText().trim().toLowerCase();
        String status = (String) statusFilter.getSelectedItem();
        String sort = (String) sortBox.getSelectedItem();

        List<Product> filtered = new ArrayList<>();
        for (Product p : manager.getAllProducts()) {
            boolean nameMatch = local.isEmpty() || p.getName().toLowerCase().contains(local);
            boolean globalMatch = globalSearch.isEmpty() || p.getName().toLowerCase().contains(globalSearch);
            boolean statusMatch = "All Statuses".equals(status)
                    || p.getStockStatus().getLabel().equals(status);
            if (nameMatch && globalMatch && statusMatch) {
                filtered.add(p);
            }
        }

        Comparator<Product> cmp = Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
        switch (sort) {
            case "Sort: Name Z–A" -> cmp = cmp.reversed();
            case "Sort: Qty High→Low" -> cmp = Comparator.comparingInt(Product::getQuantity).reversed()
                    .thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            case "Sort: Qty Low→High" -> cmp = Comparator.comparingInt(Product::getQuantity)
                    .thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            default -> { /* name A–Z */ }
        }
        filtered.sort(cmp);

        model.setRowCount(0);
        for (Product p : filtered) {
            model.addRow(new Object[]{p.getId(), p.getName(), p.getQuantity(),
                    p.getStockStatus().getLabel(), "actions"});
        }

        boolean empty = filtered.isEmpty();
        emptyState.setVisible(empty);
        emptyState.revalidate();
        emptyState.repaint();
    }

    private JPanel buildEmptyState() {
        JPanel p = new JPanel(new java.awt.GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);

        IconTile icon = new IconTile(Icon.BOX, Theme.SAGE);
        p.add(icon, gbc);
        gbc.gridy = 1;
        JLabel title = new JLabel(manager.productCount() == 0 ? "Your inventory is empty" : "No matching products");
        title.setFont(Theme.bold(15));
        title.setForeground(Theme.TEXT);
        p.add(title, gbc);
        gbc.gridy = 2;
        JLabel hint = new JLabel(manager.productCount() == 0
                ? "Add your first product to get started."
                : "Try a different search or filter.");
        hint.setFont(Theme.font(12));
        hint.setForeground(Theme.TEXT_MUTED);
        p.add(hint, gbc);
        gbc.gridy = 3;
        if (manager.productCount() == 0) {
            UIComponents.PrimaryButton add = new UIComponents.PrimaryButton("+  Add Product");
            add.addActionListener(e -> actions.onAdd());
            gbc.insets = new Insets(14, 0, 0, 0);
            p.add(add, gbc);
        }
        p.setVisible(false);
        return p;
    }

    /** Round tinted icon tile used in the empty state. */
    private static class IconTile extends JPanel {
        private final String icon;
        private final Color color;

        IconTile(String icon, Color color) {
            this.icon = icon;
            this.color = color;
            setOpaque(false);
            setPreferredSize(new Dimension(56, 56));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 26));
            g2.fillOval(0, 0, getWidth(), getHeight());
            Icon.draw(g2, icon, 14, 14, 28, color);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Non-model badge cell mirroring the StatusBadge appearance. */
    private static class StatusBadgeShim extends JPanel {
        StatusBadgeShim(StockStatus status) {
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
            setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
            JLabel label = new JLabel(status.getLabel());
            label.setFont(Theme.font(11));
            label.setForeground(status.getColor());
            add(label);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Component[] kids = getComponents();
            Color c = kids.length > 0 ? kids[0].getForeground() : Theme.TEXT_MUTED;
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 22));
            g2.fillRoundRect(8, 10, getWidth() - 16, getHeight() - 20, 12, 12);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 90));
            g2.drawRoundRect(8, 10, getWidth() - 16, getHeight() - 21, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
