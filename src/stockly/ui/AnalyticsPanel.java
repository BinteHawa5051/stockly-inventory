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
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Analytics screen: deeper look at inventory composition.
 * <p>
 * KPI cards (products, units, average per product, restock count), a
 * large status-distribution donut, a per-product quantity bar chart
 * that highlights low/out-of-stock items, a status breakdown table and
 * auto-calculated insight cards. Everything is computed live from the
 * inventory — no fabricated trends.
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
    private final JLabel[] kpiValues = new JLabel[4];
    private final JPanel insightsPanel = new JPanel();

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

        // Row 0: four KPI cards
        gbc.gridy = 0;
        gbc.weighty = 0;
        String[] kpiCaptions = {"Total Products", "Total Units",
                "Avg Qty / Product", "Need Restock"};
        Color[] kpiAccents = {Theme.SAGE, Theme.LAVENDER, Theme.PEACH, Theme.ROSE};
        for (int i = 0; i < 4; i++) {
            gbc.gridx = i;
            gbc.weightx = 0.25;
            content.add(kpiCard(kpiCaptions[i], kpiAccents[i], i), gbc);
        }

        // Row 1: donut + breakdown table
        gbc.gridy = 1;
        gbc.weighty = 0.85;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 0.42;
        content.add(card("Stock Distribution", "Products by status", donut), gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0.58;
        content.add(card("Status Breakdown", "Counts and unit totals per status",
                tableScroll()), gbc);

        // Row 2: bar chart + insights
        gbc.gridy = 2;
        gbc.weighty = 1.2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 0.58;
        content.add(card("Quantity Comparison", "Units on hand — low stock highlighted",
                scrollWrap(bars)), gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0.42;
        JPanel insightsCard = card("Inventory Insights", "Computed from live data",
                null);
        insightsCard.add(insightsPanel, BorderLayout.CENTER);
        insightsPanel.setLayout(new java.awt.GridLayout(0, 1, 0, 10));
        insightsPanel.setOpaque(false);
        insightsPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        content.add(insightsCard, gbc);

        add(content, BorderLayout.CENTER);
        refresh();
    }

    private JPanel kpiCard(String caption, Color accent, int index) {
        UIComponents.Card card = new UIComponents.Card();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JPanel iconTile = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);
                g2.dispose();
                String[] icons = {Icon.BOX, Icon.INVENTORY, Icon.ANALYTICS, Icon.WARN};
                Icon.draw((Graphics2D) g.create(), icons[index], 9, 9, 20, accent);
            }
        };
        iconTile.setPreferredSize(new Dimension(38, 38));
        iconTile.setOpaque(false);

        JLabel value = new JLabel("0");
        kpiValues[index] = value;
        value.setFont(Theme.bold(22));
        value.setForeground(Theme.TEXT);
        JLabel cap = new JLabel(caption);
        cap.setFont(Theme.font(11));
        cap.setForeground(Theme.TEXT_MUTED);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        text.add(value);
        text.add(cap);

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.add(text, BorderLayout.CENTER);
        row.add(iconTile, BorderLayout.EAST);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JPanel card(String title, String subtitle, Component body) {
        UIComponents.Card card = new UIComponents.Card();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.add(new UIComponents.SectionTitle(title, subtitle), BorderLayout.NORTH);
        if (body != null) {
            card.add(body, BorderLayout.CENTER);
        }
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
        int totalUnits = manager.totalUnits();
        int avg = totalProducts == 0 ? 0 : totalUnits / totalProducts;
        int restock = manager.countByStatus(StockStatus.LOW_STOCK)
                + manager.countByStatus(StockStatus.OUT_OF_STOCK);
        kpiValues[0].setText(String.valueOf(totalProducts));
        kpiValues[1].setText(String.valueOf(totalUnits));
        kpiValues[2].setText(String.valueOf(avg));
        kpiValues[3].setText(String.valueOf(restock));

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
        rebuildInsights(all);
        revalidate();
        repaint();
    }

    private void rebuildInsights(List<Product> all) {
        insightsPanel.removeAll();
        if (all.isEmpty()) {
            insightsPanel.add(insightCard("No data yet", "Add products to see insights.", Theme.TEXT_MUTED));
        } else {
            Product max = all.stream().max(java.util.Comparator.comparingInt(Product::getQuantity)).orElse(null);
            Product min = all.stream().min(java.util.Comparator.comparingInt(Product::getQuantity)).orElse(null);
            int unavailable = all.stream().filter(p -> p.getQuantity() == 0)
                    .mapToInt(Product::getQuantity).sum();
            int restockCount = manager.countByStatus(StockStatus.LOW_STOCK)
                    + manager.countByStatus(StockStatus.OUT_OF_STOCK);
            int avg = manager.totalUnits() / manager.productCount();

            if (max != null) {
                insightsPanel.add(insightCard("Highest stock", max.getName() + " — " + max.getQuantity() + " units",
                        Theme.CHART_IN_STOCK));
            }
            if (min != null) {
                insightsPanel.add(insightCard("Lowest stock", min.getName() + " — " + min.getQuantity() + " units",
                        min.getStockStatus() == StockStatus.OUT_OF_STOCK
                                ? Theme.CHART_OUT_STOCK : Theme.CHART_LOW_STOCK));
            }
            insightsPanel.add(insightCard("Need restock", restockCount + " products below threshold",
                    Theme.CHART_LOW_STOCK));
            insightsPanel.add(insightCard("Unavailable units", String.valueOf(unavailable),
                    Theme.CHART_OUT_STOCK));
            insightsPanel.add(insightCard("Average per product", avg + " units", Theme.LAVENDER));
        }
        insightsPanel.revalidate();
        insightsPanel.repaint();
    }

    private JPanel insightCard(String title, String detail, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(accent);
                g2.fillRoundRect(0, 2, 4, getHeight() - 4, 2, 2);
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(4, 0));
        bar.setOpaque(false);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(Theme.bold(12));
        t.setForeground(Theme.TEXT);
        JLabel d = new JLabel(detail);
        d.setFont(Theme.font(11));
        d.setForeground(Theme.TEXT_MUTED);
        text.add(t);
        text.add(d);

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(bar, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        card.add(row, BorderLayout.CENTER);
        return card;
    }
}
