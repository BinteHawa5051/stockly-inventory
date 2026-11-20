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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
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
 * Landing screen showing live inventory statistics.
 * <p>
 * Everything on this screen is computed from the real
 * {@link InventoryManager} state: four summary cards, a donut chart of
 * stock-status distribution, a per-product quantity bar chart, a
 * compact stock-health overview and the session activity feed.
 * Call {@link #refresh()} after any inventory change.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class DashboardPanel extends JPanel {

    private final InventoryManager manager;
    private final DonutChart donut = new DonutChart();
    private final QuantityBarChart bars = new QuantityBarChart();
    private final JPanel activityList = new JPanel();
    private final StatCard productsCard;
    private final StatCard unitsCard;
    private final StatCard lowCard;
    private final StatCard outCard;
    private final OverviewCard overview = new OverviewCard();

    /**
     * Creates the dashboard bound to the given inventory.
     *
     * @param manager the shared inventory data source
     */
    public DashboardPanel(InventoryManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 28, 24, 28));

        productsCard = new StatCard("Total Products", Icon.BOX, Theme.SAGE);
        unitsCard = new StatCard("Total Units", Icon.INVENTORY, Theme.LAVENDER);
        lowCard = new StatCard("Low Stock Items", Icon.WARN, Theme.PEACH);
        outCard = new StatCard("Out of Stock", Icon.CLOSE, Theme.ROSE);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 16, 16);
        gbc.fill = GridBagConstraints.BOTH;

        // Row 1: four stat cards
        gbc.gridy = 0;
        gbc.weighty = 0;
        addCard(content, gbc, productsCard, 0);
        addCard(content, gbc, unitsCard, 1);
        addCard(content, gbc, lowCard, 2);
        addCard(content, gbc, outCard, 3);
        gbc.weightx = 1;
        for (int i = 0; i < 4; i++) {
            gbc.gridx = i;
            gbc.weightx = 0.25;
        }

        // Row 2: donut (left, taller) + overview (right)
        gbc.gridy = 1;
        gbc.weighty = 0.9;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 0.62;
        content.add(chartCard("Inventory Distribution", "Share of products by stock status", donut), gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0.38;
        content.add(overview, gbc);

        // Row 3: bar chart + recent activity
        gbc.gridy = 2;
        gbc.weighty = 1.4;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 0.66;
        content.add(barCard(), gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0.34;
        content.add(activityCard(), gbc);

        add(content, BorderLayout.CENTER);
        refresh();
    }

    private void addCard(JPanel parent, GridBagConstraints gbc, JPanel card, int col) {
        gbc.gridx = col;
        parent.add(card, gbc);
    }

    private JPanel chartCard(String title, String subtitle, JPanel chart) {
        UIComponents.Card card = new UIComponents.Card();
        card.setLayout(new BorderLayout());
        card.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.add(new UIComponents.SectionTitle(title, subtitle), BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private JPanel barCard() {
        UIComponents.Card card = new UIComponents.Card();
        card.setLayout(new BorderLayout());
        card.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.add(new UIComponents.SectionTitle("Stock Quantity", "Units on hand per product"), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(bars);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel activityCard() {
        UIComponents.Card card = new UIComponents.Card();
        card.setLayout(new BorderLayout());
        card.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.add(new UIComponents.SectionTitle("Recent Activity", "This session"), BorderLayout.NORTH);

        activityList.setLayout(new BoxLayout(activityList, BoxLayout.Y_AXIS));
        activityList.setOpaque(false);
        activityList.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JScrollPane scroll = new JScrollPane(activityList);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    /** Recomputes every statistic and chart from current inventory data. */
    public void refresh() {
        productsCard.setValue(manager.productCount());
        unitsCard.setValue(manager.totalUnits());
        lowCard.setValue(manager.countByStatus(StockStatus.LOW_STOCK));
        outCard.setValue(manager.countByStatus(StockStatus.OUT_OF_STOCK));

        Map<StockStatus, Integer> dist = new EnumMap<>(StockStatus.class);
        for (StockStatus s : StockStatus.values()) {
            dist.put(s, manager.countByStatus(s));
        }
        donut.setData(dist);

        bars.setData(manager.getAllProducts());
        overview.update(manager);
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

    /** Summary card with icon tile, large value and label. */
    private static class StatCard extends UIComponents.Card {
        private final JLabel value = new JLabel("0");
        private final JLabel caption = new JLabel(" ");
        private final String iconName;
        private final Color accent;

        StatCard(String label, String iconName, Color accent) {
            this.iconName = iconName;
            this.accent = accent;
            setLayout(new BorderLayout());
            setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 16, 14, 16));

            JPanel iconTile = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            iconTile.setPreferredSize(new Dimension(40, 40));
            iconTile.setOpaque(false);
            iconTile.add(new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Icon.draw(g2, iconName, 10, 10, 20, accent);
                    g2.dispose();
                    super.paintComponent(g);
                }
            });

            value.setFont(Theme.bold(26));
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

            JPanel south = new JPanel(new BorderLayout());
            south.setOpaque(false);
            south.add(text, BorderLayout.CENTER);
            south.add(iconTile, BorderLayout.EAST);

            add(south, BorderLayout.CENTER);
        }

        void setValue(int v) {
            value.setText(String.valueOf(v));
            caption.setText(captionFor(v));
        }

        private String captionFor(int v) {
            return switch (iconName) {
                case Icon.BOX -> v == 1 ? "unique item" : "unique items";
                case Icon.INVENTORY -> "units on hand";
                case Icon.WARN -> "need restock";
                case Icon.CLOSE -> "requires action";
                default -> " ";
            };
        }
    }

    // ------------------------------------------------------------------
    // Overview card
    // ------------------------------------------------------------------

    /** Compact summary of overall stock health with a proportion bar. */
    private static class OverviewCard extends UIComponents.Card {
        private final JLabel line1 = new JLabel(" ");
        private final JLabel line2 = new JLabel(" ");
        private final JLabel line3 = new JLabel(" ");
        private int[] segments = new int[3];
        private int total;

        OverviewCard() {
            setLayout(new BorderLayout());
            setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 18, 16, 18));

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            for (JLabel l : new JLabel[]{line1, line2, line3}) {
                l.setFont(Theme.font(12));
                l.setForeground(Theme.TEXT);
                l.setAlignmentX(LEFT_ALIGNMENT);
                text.add(l);
                text.add(Box.createVerticalStrut(6));
            }
            text.add(Box.createVerticalStrut(10));
            text.add(new HealthBar(() -> segments));
            text.add(Box.createVerticalGlue());
            add(text, BorderLayout.CENTER);
        }

        void update(InventoryManager m) {
            total = m.productCount();
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
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
