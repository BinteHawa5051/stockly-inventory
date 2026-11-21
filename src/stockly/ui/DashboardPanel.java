package stockly.ui;

import stockly.model.ActivityEntry;
import stockly.model.ActivityLog;
import stockly.model.InventoryManager;
import stockly.model.Product;
import stockly.model.StockStatus;
import stockly.ui.charts.DonutChart;
import stockly.ui.charts.QuantityBarChart;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Landing screen showing live inventory statistics.
 * <p>
 * Everything is computed from the real {@link InventoryManager} state:
 * four equal KPI cards, a donut chart of stock-status distribution, a
 * per-product quantity bar chart (coloured by status), a stock-health
 * overview card, a restock-alerts list and the session activity feed.
 * Call {@link #refresh()} after any inventory change.
 *
 * @author Tooba Akram and Abeeha Noor
 * @version 1.0
 */
public class DashboardPanel extends JPanel {

    /** Shared inventory data source. */
    private final InventoryManager manager;
    /** Donut chart of stock-status distribution. */
    private final DonutChart donut = new DonutChart();
    /** Per-product quantity bar chart. */
    private final QuantityBarChart bars = new QuantityBarChart();
    /** Vertical box holding the recent-activity rows. */
    private final JPanel activityList = new JPanel();
    /** The four KPI cards (total products, units, low stock, out of stock). */
    private final StatCard[] statCards = new StatCard[4];
    /** Stock-health overview card. */
    private final OverviewCard overview = new OverviewCard();
    /** Restock-alerts card listing products needing attention. */
    private final LowStockCard alerts;
    /** Callback receiving a product id to open the quick-edit dialog. */
    private final java.util.function.Consumer<String> onEditProduct;
    /** Callback opening the filtered "needs attention" inventory view. */
    private final Runnable onViewAll;

    /**
     * Creates the dashboard bound to the given inventory.
     *
     * @param manager        shared inventory data source
     * @param onEditProduct  callback receiving a product id for quick edit
     * @param onViewAll      callback to open the filtered inventory view
     */
    public DashboardPanel(InventoryManager manager,
                          java.util.function.Consumer<String> onEditProduct,
                          Runnable onViewAll) {
        this.manager = manager;
        this.onEditProduct = onEditProduct;
        this.onViewAll = onViewAll;
        this.alerts = new LowStockCard(onEditProduct, onViewAll);
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 28, 24, 28));

        buildLayout();
        refresh();
    }

    private void buildLayout() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 20, 20);
        gbc.fill = GridBagConstraints.BOTH;

        // Row 1: four equal KPI cards
        gbc.gridy = 0;
        gbc.weighty = 0;
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 20, 0));
        statsRow.setOpaque(false);
        statCards[0] = new StatCard("Total Products", "Total units", Icon.BOX, Theme.SAGE);
        statCards[1] = new StatCard("Total Units", "Units on hand", Icon.INVENTORY, Theme.LAVENDER);
        statCards[2] = new StatCard("Low Stock Items", "Need restock", Icon.WARN, Theme.PEACH);
        statCards[3] = new StatCard("Out of Stock", "Requires action", Icon.CLOSE, Theme.ROSE);
        for (StatCard c : statCards) {
            statsRow.add(c);
        }
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.weightx = 1;
        content.add(statsRow, gbc);

        // Row 2: donut + overview
        gbc.gridy = 1;
        gbc.weighty = 0.9;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 0.62;
        content.add(card("Inventory Distribution", "Share of products by stock status", donut), gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0.38;
        content.add(overview, gbc);

        // Row 3: bar chart + right column (alerts + activity, equal widths)
        gbc.gridy = 2;
        gbc.weighty = 1.3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 0.60;
        content.add(barCard(), gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0.40;
        JPanel rightCol = new JPanel();
        rightCol.setOpaque(false);
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        alerts.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        alerts.setMaximumSize(new Dimension(Integer.MAX_VALUE, alerts.getPreferredSize().height));
        JPanel act = activityCard();
        act.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        act.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        rightCol.add(alerts);
        rightCol.add(Box.createVerticalStrut(20));
        rightCol.add(act);
        content.add(rightCol, gbc);

        add(content, BorderLayout.CENTER);
    }

    private JPanel card(String title, String subtitle, JPanel chart) {
        UIComponents.Card card = new UIComponents.Card();
        card.setLayout(new BorderLayout());
        card.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 22, 20, 22));
        card.add(new UIComponents.SectionTitle(title, subtitle), BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private JPanel barCard() {
        UIComponents.Card card = new UIComponents.Card();
        card.setLayout(new BorderLayout());
        card.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 22, 20, 22));
        card.add(new UIComponents.SectionTitle("Stock Quantity", "Units on hand per product"),
                BorderLayout.NORTH);
        JScrollPane scroll = ScrollBarKit.wrap(bars);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel activityCard() {
        UIComponents.Card card = new UIComponents.Card();
        card.setLayout(new BorderLayout());
        card.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 22, 20, 22));
        card.add(new UIComponents.SectionTitle("Recent Activity", "This session"), BorderLayout.NORTH);

        activityList.setLayout(new BoxLayout(activityList, BoxLayout.Y_AXIS));
        activityList.setOpaque(false);
        activityList.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JScrollPane scroll = ScrollBarKit.wrap(activityList);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    /** Recomputes every statistic and chart from current inventory data. */
    public void refresh() {
        statCards[0].setValue(manager.productCount(), "unique items");
        statCards[1].setValue(manager.totalUnits(), "units on hand");
        statCards[2].setValue(manager.countByStatus(StockStatus.LOW_STOCK), "need restock");
        statCards[3].setValue(manager.countByStatus(StockStatus.OUT_OF_STOCK), "requires action");

        Map<StockStatus, Integer> dist = new EnumMap<>(StockStatus.class);
        for (StockStatus s : StockStatus.values()) {
            dist.put(s, manager.countByStatus(s));
        }
        donut.setData(dist);
        bars.setData(manager.getAllProducts());
        overview.update(manager);
        alerts.update(manager);
        rebuildActivity();
        revalidate();
        repaint();
    }

    private void rebuildActivity() {
        activityList.removeAll();
        List<ActivityEntry> entries = manager.getActivityLog().getRecent(8);
        if (entries.isEmpty()) {
            JLabel empty = new JLabel("No activity yet — add, edit or delete a product.");
            empty.setFont(Theme.font(12));
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setAlignmentX(LEFT_ALIGNMENT);
            empty.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 4, 8, 4));
            activityList.add(empty);
        } else {
            for (ActivityEntry e : entries) {
                activityList.add(new ActivityRow(e));
                activityList.add(Box.createVerticalStrut(2));
            }
        }
        activityList.revalidate();
        activityList.repaint();
    }

    // ------------------------------------------------------------------
    // Stat card
    // ------------------------------------------------------------------

    /** KPI card: fixed 40px tinted icon tile + big value + muted label. */
    private static class StatCard extends UIComponents.Card {
        private final JLabel value = new JLabel("0");
        private final JLabel caption = new JLabel(" ");
        private final String iconName;
        private final Color accent;

        StatCard(String label, String sub, String iconName, Color accent) {
            this.iconName = iconName;
            this.accent = accent;
            setLayout(new BorderLayout());
            setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18));

            JPanel iconTile = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(),
                            accent.getBlue(), 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    Icon.draw(g2, iconName, 10, 10, 20, accent);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            iconTile.setPreferredSize(new Dimension(40, 40));
            iconTile.setMaximumSize(new Dimension(40, 40));
            iconTile.setOpaque(false);

            value.setFont(Theme.bold(28));
            value.setForeground(Theme.TEXT);
            JLabel name = new JLabel(label);
            name.setFont(Theme.font(12));
            name.setForeground(Theme.TEXT_MUTED);
            caption.setFont(Theme.font(10));
            caption.setForeground(Theme.TEXT_MUTED);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.add(value);
            text.add(Box.createVerticalStrut(2));
            text.add(name);
            text.add(caption);

            JPanel row = new JPanel(new BorderLayout(14, 0));
            row.setOpaque(false);
            row.add(text, BorderLayout.CENTER);
            row.add(iconTile, BorderLayout.EAST);
            add(row, BorderLayout.CENTER);
        }

        void setValue(int v, String sub) {
            value.setText(String.valueOf(v));
            caption.setText(sub);
        }
    }

    // ------------------------------------------------------------------
    // Overview card
    // ------------------------------------------------------------------

    /** Stock-health card: title, lines, segmented bar + legend, glued bottom. */
    private static class OverviewCard extends UIComponents.Card {
        private final JLabel line1 = new JLabel(" ");
        private final JLabel line2 = new JLabel(" ");
        private final JLabel line3 = new JLabel(" ");
        private int[] segments = new int[3];

        OverviewCard() {
            setLayout(new BorderLayout());
            setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 22, 20, 22));

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.add(new UIComponents.SectionTitle("Stock Health",
                    "Overall condition of your inventory"));
            text.add(Box.createVerticalStrut(10));
            for (JLabel l : new JLabel[]{line1, line2, line3}) {
                l.setFont(Theme.font(12));
                l.setForeground(Theme.TEXT);
                l.setAlignmentX(LEFT_ALIGNMENT);
                text.add(l);
                text.add(Box.createVerticalStrut(6));
            }
            text.add(Box.createVerticalStrut(10));
            text.add(new HealthBar(() -> segments));
            text.add(Box.createVerticalStrut(8));
            text.add(healthLegend());
            text.add(Box.createVerticalGlue());
            add(text, BorderLayout.CENTER);
        }

        private JPanel healthLegend() {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            p.setOpaque(false);
            p.setAlignmentX(LEFT_ALIGNMENT);
            StockStatus[] order = {StockStatus.IN_STOCK, StockStatus.LOW_STOCK,
                    StockStatus.OUT_OF_STOCK};
            Color[] cols = {Theme.CHART_IN_STOCK, Theme.CHART_LOW_STOCK, Theme.CHART_OUT_STOCK};
            for (int i = 0; i < 3; i++) {
                JLabel l = new JLabel("● " + order[i].getLabel());
                l.setFont(Theme.font(10));
                l.setForeground(cols[i]);
                p.add(l);
            }
            return p;
        }

        void update(InventoryManager m) {
            int total = m.productCount();
            segments = new int[]{
                    m.countByStatus(StockStatus.IN_STOCK),
                    m.countByStatus(StockStatus.LOW_STOCK),
                    m.countByStatus(StockStatus.OUT_OF_STOCK)};
            int units = m.totalUnits();
            line1.setText("<html><b>" + total + "</b> products &nbsp;·&nbsp; <b>" + units + "</b> units</html>");
            line2.setText("<html><b>" + segments[0] + "</b> fully stocked &nbsp;·&nbsp; <b>"
                    + (segments[1] + segments[2]) + "</b> need attention</html>");
            int healthy = total == 0 ? 0 : (int) Math.round(segments[0] * 100.0 / total);
            line3.setText("<html>Stock health: <b>" + healthy + "%</b> healthy</html>");
            repaint();
        }
    }

    /** Thin stacked bar visualizing in/low/out proportions. */
    private static class HealthBar extends JPanel {
        private final java.util.function.Supplier<int[]> segments;

        HealthBar(java.util.function.Supplier<int[]> segments) {
            this.segments = segments;
            setOpaque(false);
            setPreferredSize(new Dimension(0, 10));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int[] s = segments.get();
            int sum = s[0] + s[1] + s[2];
            int w = getWidth();
            int x = 0;
            Color[] colors = {Theme.CHART_IN_STOCK, Theme.CHART_LOW_STOCK, Theme.CHART_OUT_STOCK};
            for (int i = 0; i < 3; i++) {
                int segW = sum == 0 ? 0 : (int) Math.round(s[i] * (double) w / sum);
                if (segW > 0) {
                    g2.setColor(colors[i]);
                    g2.fillRoundRect(x, 0, Math.max(4, segW), 10, 5, 5);
                }
                x += segW;
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ------------------------------------------------------------------
    // Low-stock alerts card
    // ------------------------------------------------------------------

    /** Card listing up to 4 products needing restock + "View all" link. */
    private static class LowStockCard extends UIComponents.Card {
        private final JPanel list = new JPanel();
        private final java.util.function.Consumer<String> onEdit;
        private final Runnable onViewAll;

        LowStockCard(java.util.function.Consumer<String> onEdit, Runnable onViewAll) {
            this.onEdit = onEdit;
            this.onViewAll = onViewAll;
            setLayout(new BorderLayout());
            setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 22, 16, 22));

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(new UIComponents.SectionTitle("Needs Restock", "Low or out of stock"),
                    BorderLayout.WEST);
            JButton viewAll = new JButton("View all");
            viewAll.setFont(Theme.font(11));
            viewAll.setForeground(Theme.FOREST);
            viewAll.setContentAreaFilled(false);
            viewAll.setBorderPainted(false);
            viewAll.setFocusPainted(false);
            viewAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
            viewAll.addActionListener(e -> onViewAll.run());
            header.add(viewAll, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);

            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            list.setOpaque(false);
            list.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
            add(list, BorderLayout.CENTER);
        }

        void update(InventoryManager m) {
            list.removeAll();
            List<Product> attention = m.getProductsNeedingAttention().stream()
                    .filter(p -> p.getStockStatus() != StockStatus.IN_STOCK)
                    .limit(4)
                    .toList();
            if (attention.isEmpty()) {
                JLabel ok = new JLabel("All products are well stocked.");
                ok.setFont(Theme.font(12));
                ok.setForeground(Theme.CHART_IN_STOCK);
                ok.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 4, 8, 4));
                list.add(ok);
            } else {
                for (Product p : attention) {
                    list.add(new AlertRow(p, onEdit));
                    list.add(Box.createVerticalStrut(4));
                }
            }
            list.revalidate();
            list.repaint();
        }
    }

    /** One restock-alert row: name | "n left" | badge | Update — vertically centred. */
    private static class AlertRow extends JPanel {
        AlertRow(Product p, java.util.function.Consumer<String> onEdit) {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 4, 3, 4));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            setAlignmentX(LEFT_ALIGNMENT);

            // single horizontal line, everything vertically centred
            JPanel line = new JPanel();
            line.setOpaque(false);
            line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));

            JLabel name = new JLabel(p.getName());
            name.setFont(Theme.font(12));
            name.setForeground(Theme.TEXT);
            name.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);

            JLabel qty = new JLabel(p.getQuantity() + " left");
            qty.setFont(Theme.font(11));
            qty.setForeground(Theme.TEXT_MUTED);
            qty.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
            qty.setPreferredSize(new Dimension(52, 0));

            UIComponents.StatusBadge badge = new UIComponents.StatusBadge(p.getStockStatus());
            badge.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);

            UIComponents.GhostButton update = new UIComponents.GhostButton("Update");
            update.setFont(Theme.font(11));
            update.setPreferredSize(new Dimension(update.getPreferredSize().width, 28));
            update.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
            update.addActionListener(e -> onEdit.accept(p.getId()));

            line.add(name);
            line.add(Box.createHorizontalGlue());
            line.add(Box.createHorizontalStrut(10));
            line.add(qty);
            line.add(Box.createHorizontalStrut(10));
            line.add(badge);
            line.add(Box.createHorizontalStrut(8));
            line.add(update);
            add(line, BorderLayout.CENTER);
        }
    }

    // ------------------------------------------------------------------
    // Activity row
    // ------------------------------------------------------------------

    /** Single row of the recent-activity feed. */
    private static class ActivityRow extends JPanel {
        ActivityRow(ActivityEntry entry) {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 4, 5, 4));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            setAlignmentX(LEFT_ALIGNMENT);

            String icon = switch (entry.getKind()) {
                case ADDED -> Icon.ADD;
                case UPDATED -> Icon.EDIT;
                case DELETED -> Icon.TRASH;
            };
            Color tint = switch (entry.getKind()) {
                case ADDED -> Theme.CHART_IN_STOCK;
                case UPDATED -> Theme.LAVENDER;
                case DELETED -> Theme.CHART_OUT_STOCK;
            };

            JPanel glyph = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 26));
                    g2.fillOval(0, 0, 30, 30);
                    Icon.draw(g2, icon, 8, 8, 14, tint);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            glyph.setPreferredSize(new Dimension(30, 30));
            glyph.setOpaque(false);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            JLabel desc = new JLabel(entry.getDescription());
            desc.setFont(Theme.font(12));
            desc.setForeground(Theme.TEXT);
            desc.setAlignmentX(LEFT_ALIGNMENT);
            JLabel time = new JLabel(entry.getTimeLabel());
            time.setFont(Theme.font(10));
            time.setForeground(Theme.TEXT_MUTED);
            time.setAlignmentX(LEFT_ALIGNMENT);
            text.add(desc);
            text.add(time);

            add(glyph, BorderLayout.WEST);
            JPanel center = new JPanel(new BorderLayout());
            center.setOpaque(false);
            center.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
            center.add(text, BorderLayout.CENTER);
            add(center, BorderLayout.CENTER);
        }
    }
}
