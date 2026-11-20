package stockly.ui;

import stockly.model.InventoryManager;
import stockly.model.Product;
import stockly.model.StockStatus;
import stockly.ui.charts.DonutChart;
import stockly.ui.charts.QuantityBarChart;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Analytics screen: deeper look at inventory composition.
 * <p>
 * Shows the stock-status donut, a full-width quantity bar chart and a
 * breakdown table with per-status counts, unit totals and share of the
 * catalogue. All figures are computed live from the inventory.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class AnalyticsPanel extends JPanel {

    private final InventoryManager manager;
    private final DonutChart donut = new DonutChart();
    private final QuantityBarChart bars = new QuantityBarChart();
    private final DefaultTableModel breakdownModel;
    private final JTable breakdownTable;

    /**
     * Creates the analytics screen.
     *
     * @param manager shared inventory data source
     */
    public AnalyticsPanel(InventoryManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 24, 28));

        breakdownModel = new DefaultTableModel(
                new Object[]{"Status", "Products", "Units", "Share"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        breakdownTable = new JTable(breakdownModel);
        breakdownTable.setRowHeight(38);
        breakdownTable.setShowGrid(false);
        breakdownTable.setIntercellSpacing(new Dimension(0, 0));
        breakdownTable.setFont(Theme.font(13));
        breakdownTable.getTableHeader().setFont(Theme.bold(11));
        breakdownTable.getTableHeader().setForeground(Theme.TEXT_MUTED);
        breakdownTable.getTableHeader().setBackground(Theme.SURFACE);
        breakdownTable.getTableHeader().setPreferredSize(new Dimension(0, 36));

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 16, 16);
        gbc.fill = GridBagConstraints.BOTH;

        // donut card
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0.42;
        gbc.weighty = 0.8;
        content.add(card("Stock Distribution", "Products by status",
                donut, BorderLayout.CENTER), gbc);

        // breakdown table card
        gbc.gridx = 1;
        gbc.weightx = 0.58;
        content.add(card("Status Breakdown", "Counts and unit totals per status",
                tableScroll(), BorderLayout.CENTER), gbc);

        // bar chart card (full width)
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 1.2;
        gbc.weightx = 1;
        content.add(card("Quantity Overview", "Units on hand for every product",
                scrollWrap(bars), BorderLayout.CENTER), gbc);

        add(content, BorderLayout.CENTER);
        refresh();
    }

    private JPanel card(String title, String subtitle, Component body, String constraint) {
        UIComponents.Card card = new UIComponents.Card();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.add(new UIComponents.SectionTitle(title, subtitle), BorderLayout.NORTH);
        card.add(body, constraint);
        return card;
    }

    private JScrollPane scrollWrap(JPanel inner) {
        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JScrollPane tableScroll() {
        JScrollPane scroll = new JScrollPane(breakdownTable);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.setBackground(Theme.SURFACE);
        return scroll;
    }

    /** Recomputes all analytics from current inventory data. */
    public void refresh() {
        Map<StockStatus, Integer> dist = new EnumMap<>(StockStatus.class);
        for (StockStatus s : StockStatus.values()) {
            dist.put(s, manager.countByStatus(s));
        }
        donut.setData(dist);

        List<Product> all = manager.getAllProducts();
        bars.setData(all);

        int totalProducts = manager.productCount();
        breakdownModel.setRowCount(0);
        for (StockStatus s : StockStatus.values()) {
            int products = manager.countByStatus(s);
            int units = 0;
            for (Product p : all) {
                if (p.getStockStatus() == s) {
                    units += p.getQuantity();
                }
            }
            int share = totalProducts == 0 ? 0 : (int) Math.round(products * 100.0 / totalProducts);
            breakdownModel.addRow(new Object[]{s.getLabel(), products, units, share + "%"});
        }
        revalidate();
        repaint();
    }
}
