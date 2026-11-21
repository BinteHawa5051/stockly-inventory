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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Inventory screen: searchable, filterable, sortable product table.
 * <p>
 * Layout: compact stats strip, then a single toolbar row (search,
 * status filter, sort, clear-filters, add button) directly above the
 * table card. The table uses {@link TableKit} for status pills, row
 * actions, combo styling and shared row colours. All mutations go
 * through {@link InventoryManager}; this panel only renders state and
 * forwards user intent.
 *
 * @author Tooba Akram & Abeeha Noor
 * @version 1.0
 */
public class InventoryPanel extends JPanel {

    /** Callback interface so the frame can open product dialogs. */
    public interface ProductActions {
        /** Open the add-product dialog. */
        void onAdd();
        /** Open the edit dialog for the given product id. */
        void onEdit(String id);
        /** Open the details dialog for the given product id. */
        void onView(String id);
        /** Ask for confirmation then delete the given product id. */
        void onDelete(String id);
    }

    // Fixed column widths (name column absorbs the rest)
    private static final int W_ID = 80;
    private static final int W_QTY = 90;
    private static final int W_STATUS = 130;
    private static final int W_ACTIONS = 140;

    private final InventoryManager manager;
    private final ProductActions actions;
    private final DefaultTableModel model;
    private final JTable table;
    private final JPanel emptyState;
    private final JLabel[] statValues = new JLabel[4];
    private final TableKit.SearchBox searchBox = new TableKit.SearchBox("Search products...");
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"All Statuses", "Needs Attention", "In Stock", "Low Stock", "Out of Stock"});
    private final JComboBox<String> sortBox = new JComboBox<>(
            new String[]{"Sort: Name A–Z", "Sort: Name Z–A", "Sort: Qty High→Low", "Sort: Qty Low→High"});
    private final UIComponents.GhostButton clearBtn =
            new UIComponents.GhostButton("Clear filters");
    private int hoverRow = -1;

    /**
     * Creates the inventory screen.
     *
     * @param manager shared inventory data source
     * @param actions callbacks for add / edit / view / delete
     */
    public InventoryPanel(InventoryManager manager, ProductActions actions) {
        this.manager = manager;
        this.actions = actions;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 24, 28));

        model = new DefaultTableModel(
                new Object[]{"Product ID", "Product Name", "Quantity", "Stock Status", "Actions"}, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = buildTable();
        styleRenderers();
        attachRowActions();
        attachHover();
        attachColumnSizing();

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(buildStatsStrip(), BorderLayout.NORTH);
        top.add(buildToolbar(), BorderLayout.CENTER);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JPanel layered = new JPanel();
        layered.setLayout(new OverlayLayout(layered));
        layered.setOpaque(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.setBackground(Theme.SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        layered.add(scroll);
        emptyState = buildEmptyState();
        layered.add(emptyState);

        UIComponents.Card card = new UIComponents.Card();
        card.setLayout(new BorderLayout());
        card.add(layered, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(card, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        refresh();
    }

    // ------------------------------------------------------------------
    // Table construction
    // ------------------------------------------------------------------

    private JTable buildTable() {
        JTable t = new JTable(model);
        t.setRowHeight(44);
        t.setShowGrid(false);
        t.setShowHorizontalLines(false);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(TableKit.SELECTED);
        t.setSelectionForeground(Theme.TEXT);
        t.setFont(Theme.font(13));
        t.setRowSelectionAllowed(true);
        t.setColumnSelectionAllowed(false);
        t.setDragEnabled(false);
        t.setBorder(BorderFactory.createEmptyBorder());
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.setFillsViewportHeight(true);

        // quiet header: small muted text, single bottom hairline
        t.getTableHeader().setFont(Theme.bold(11));
        t.getTableHeader().setForeground(Theme.TEXT_MUTED);
        t.getTableHeader().setBackground(Theme.SURFACE);
        t.getTableHeader().setPreferredSize(new Dimension(0, 36));
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                c.setFont(Theme.font(11));
                c.setForeground(Theme.TEXT_MUTED);
                ((javax.swing.JComponent) c).setBorder(
                        BorderFactory.createEmptyBorder(0, 14, 0, 14));
                return c;
            }
        });
        return t;
    }

    /** Name column absorbs all leftover width; others stay fixed. */
    private void attachColumnSizing() {
        Runnable resize = () -> {
            int available = table.getWidth();
            if (available <= 0) {
                return;
            }
            int fixed = W_ID + W_QTY + W_STATUS + W_ACTIONS;
            int nameW = Math.max(160, available - fixed);
            setColWidth(0, W_ID);
            setColWidth(1, nameW);
            setColWidth(2, W_QTY);
            setColWidth(3, W_STATUS);
            setColWidth(4, W_ACTIONS);
        };
        table.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { resize.run(); }
        });
        SwingUtilities2.invokeLater(resize);
    }

    private void setColWidth(int index, int width) {
        TableColumn col = table.getColumnModel().getColumn(index);
        col.setPreferredWidth(width);
        col.setMinWidth(width);
        col.setMaxWidth(index == 1 ? Integer.MAX_VALUE : width);
    }

    // ------------------------------------------------------------------
    // Stats strip
    // ------------------------------------------------------------------

    private JPanel buildStatsStrip() {
        JPanel strip = new JPanel(new java.awt.GridLayout(1, 4, 12, 0));
        strip.setOpaque(false);
        strip.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        String[] captions = {"Total Products", "In Stock", "Low Stock", "Out of Stock"};
        Color[] accents = {Theme.SAGE, Theme.CHART_IN_STOCK, Theme.CHART_LOW_STOCK,
                Theme.CHART_OUT_STOCK};
        for (int i = 0; i < 4; i++) {
            final Color accent = accents[i];
            UIComponents.Card card = new UIComponents.Card();
            card.setLayout(new BorderLayout());
            card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

            JPanel leftBar = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(accent);
                    g2.fillRoundRect(0, 4, 4, getHeight() - 8, 2, 2);
                    g2.dispose();
                }
            };
            leftBar.setPreferredSize(new Dimension(4, 0));
            leftBar.setOpaque(false);

            JLabel value = new JLabel("0");
            value.setFont(Theme.bold(28));
            value.setForeground(Theme.TEXT);
            statValues[i] = value;
            JLabel cap = new JLabel(captions[i]);
            cap.setFont(Theme.font(11));
            cap.setForeground(Theme.TEXT_MUTED);
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
            text.add(value);
            text.add(cap);
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.add(leftBar, BorderLayout.WEST);
            row.add(text, BorderLayout.CENTER);
            card.add(row, BorderLayout.CENTER);
            strip.add(card);
        }
        return strip;
    }

    private void updateStatsStrip() {
        int[] vals = {manager.productCount(), manager.countByStatus(StockStatus.IN_STOCK),
                manager.countByStatus(StockStatus.LOW_STOCK),
                manager.countByStatus(StockStatus.OUT_OF_STOCK)};
        for (int i = 0; i < 4; i++) {
            statValues[i].setText(String.valueOf(vals[i]));
        }
    }

    // ------------------------------------------------------------------
    // Toolbar (single row above the table)
    // ------------------------------------------------------------------

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new GridBagLayout());
        toolbar.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 10);
        gbc.anchor = GridBagConstraints.WEST;

        TableKit.SearchBox box = searchBox;
        box.getField().getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });
        box.setPreferredSize(new Dimension(240, 40));
        toolbar.add(box, gbc);

        statusFilter.setUI(new TableKit.StyledComboUI());
        styleCombo(statusFilter, 150);
        statusFilter.addActionListener(e -> refresh());
        toolbar.add(statusFilter, gbc);

        sortBox.setUI(new TableKit.StyledComboUI());
        styleCombo(sortBox, 170);
        sortBox.addActionListener(e -> refresh());
        toolbar.add(sortBox, gbc);

        clearBtn.setToolTipText("Reset search, filter and sort");
        clearBtn.addActionListener(e -> clearFilters());
        toolbar.add(clearBtn, gbc);

        gbc.weightx = 1;
        toolbar.add(new JPanel() {{ setOpaque(false); }}, gbc);
        gbc.weightx = 0;

        UIComponents.PrimaryButton addBtn = new UIComponents.PrimaryButton("+  Add Product");
        addBtn.setPreferredSize(new Dimension(addBtn.getPreferredSize().width, 40));
        addBtn.addActionListener(e -> actions.onAdd());
        toolbar.add(addBtn, gbc);

        return toolbar;
    }

    private void styleCombo(JComboBox<String> combo, int width) {
        combo.setFont(Theme.font(12));
        combo.setForeground(Theme.TEXT);
        combo.setBackground(Theme.SURFACE);
        combo.setPreferredSize(new Dimension(width, 40));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(4, 12, 4, 8)));
    }

    private void clearFilters() {
        searchBox.getField().setText("");
        statusFilter.setSelectedIndex(0);
        sortBox.setSelectedIndex(0);
        refresh();
    }

    private boolean filtersActive() {
        return !searchBox.getField().getText().isBlank()
                || statusFilter.getSelectedIndex() != 0
                || sortBox.getSelectedIndex() != 0;
    }

    // ------------------------------------------------------------------
    // Renderers
    // ------------------------------------------------------------------

    private void styleRenderers() {
        // default text cell: zebra/hover/selection + thin bottom rule
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value,
                        isSel, hasFocus, row, col);
                c.setFont(Theme.font(13));
                c.setForeground(isSel ? Theme.TEXT : Theme.TEXT);
                c.setBackground(TableKit.rowBackground(row, isSel, hoverRow));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 14, 1, 14, Theme.BORDER),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)));
                return c;
            }
        });

        // product name: full text tooltip in case it ever truncates
        table.getColumnModel().getColumn(1).setCellRenderer(
                new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value,
                        isSel, hasFocus, row, col);
                c.setFont(Theme.font(13));
                c.setForeground(Theme.TEXT);
                c.setBackground(TableKit.rowBackground(row, isSel, hoverRow));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 14, 1, 14, Theme.BORDER),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)));
                setToolTipText(value == null ? null : value.toString());
                return c;
            }
        });

        // quantity: left-aligned under its header, same padding
        table.getColumnModel().getColumn(2).setCellRenderer(
                new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value,
                        isSel, hasFocus, row, col);
                c.setFont(Theme.font(13));
                c.setForeground(Theme.TEXT);
                setHorizontalAlignment(LEFT);
                c.setBackground(TableKit.rowBackground(row, isSel, hoverRow));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 14, 1, 14, Theme.BORDER),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)));
                return c;
            }
        });

        // status pill column — model carries the enum via ProductStatusShim
        table.getColumnModel().getColumn(3).setCellRenderer(
                new TableKit.StatusCellRenderer());

        // actions column — vector icons via TableKit
        table.getColumnModel().getColumn(4).setCellRenderer(
                new TableKit.ActionCellRenderer());
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
                // three equal hit zones: view | edit | delete
                Rectangle cell = table.getCellRect(row, col, false);
                int zone = (e.getX() - cell.x) / Math.max(1, cell.width / 3);
                switch (zone) {
                    case 0 -> actions.onView(p.getId());
                    case 1 -> actions.onEdit(p.getId());
                    default -> actions.onDelete(p.getId());
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                table.setCursor(col == 4 ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
    }

    private void attachHover() {
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoverRow) {
                    hoverRow = row;
                    table.repaint();
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverRow = -1;
                table.repaint();
            }
        });
    }

    private Product productAtRow(int viewRow) {
        if (viewRow < 0 || viewRow >= model.getRowCount()) {
            return null;
        }
        Object id = model.getValueAt(viewRow, 0);
        return manager.findById(String.valueOf(id));
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    /**
     * Applies a search query — mirrors into the toolbar search box so
     * there is exactly one query source.
     */
    public void setGlobalSearch(String text) {
        searchBox.getField().setText(text == null ? "" : text);
    }

    /** Filters the table to low + out-of-stock products (dashboard "View all"). */
    public void filterAttention() {
        statusFilter.setSelectedIndex(1);
        refresh();
    }

    /** Rebuilds the table from current filters and inventory data. */
    public void refresh() {
        String local = searchBox.getField().getText().trim().toLowerCase();
        String status = (String) statusFilter.getSelectedItem();
        String sort = (String) sortBox.getSelectedItem();

        List<Product> filtered = new ArrayList<>();
        for (Product p : manager.getAllProducts()) {
            boolean nameMatch = local.isEmpty() || p.getName().toLowerCase().contains(local);
            boolean statusMatch;
            if ("Needs Attention".equals(status)) {
                statusMatch = p.getStockStatus() == StockStatus.LOW_STOCK
                        || p.getStockStatus() == StockStatus.OUT_OF_STOCK;
            } else {
                statusMatch = "All Statuses".equals(status)
                        || p.getStockStatus().getLabel().equals(status);
            }
            if (nameMatch && statusMatch) {
                filtered.add(p);
            }
        }

        Comparator<Product> cmp = Comparator.comparing(Product::getName,
                String.CASE_INSENSITIVE_ORDER);
        switch (sort) {
            case "Sort: Name Z–A" -> cmp = cmp.reversed();
            case "Sort: Qty High→Low" -> cmp = Comparator.comparingInt(Product::getQuantity)
                    .reversed().thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            case "Sort: Qty Low→High" -> cmp = Comparator.comparingInt(Product::getQuantity)
                    .thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            default -> { /* name A–Z */ }
        }
        filtered.sort(cmp);

        model.setRowCount(0);
        for (Product p : filtered) {
            model.addRow(new Object[]{p.getId(), p.getName(), p.getQuantity(),
                    new TableKit.ProductStatusShim(p.getStockStatus()), "actions"});
        }

        boolean empty = filtered.isEmpty();
        emptyState.setVisible(empty);
        emptyState.repaint();
        updateStatsStrip();
        clearBtn.setVisible(filtersActive());
        revalidate();
        repaint();
    }

    private JPanel buildEmptyState() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);

        IconTile icon = new IconTile(Icon.BOX, Theme.SAGE);
        p.add(icon, gbc);
        gbc.gridy = 1;
        JLabel title = new JLabel(manager.productCount() == 0
                ? "Your inventory is empty" : "No matching products");
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
            UIComponents.PrimaryButton add = new UIComponents.PrimaryButton("+  Add Your First Product");
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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 26));
            g2.fillOval(0, 0, getWidth(), getHeight());
            Icon.draw(g2, icon, 14, 14, 28, color);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Tiny helper: run a runnable after the current layout pass. */
    private static class SwingUtilities2 {
        static void invokeLater(Runnable r) {
            javax.swing.SwingUtilities.invokeLater(r);
        }
    }
}
