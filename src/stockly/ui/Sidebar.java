package stockly.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fixed left navigation sidebar for the application.
 * <p>
 * Renders the STOCKLY brand, the five navigation destinations and a
 * footer with the app version and a small profile chip. The active
 * destination gets a forest-tinted highlight.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class Sidebar extends JPanel {

    /** Page identifiers used with the card layout. */
    public static final String DASHBOARD = "dashboard";
    public static final String INVENTORY = "inventory";
    public static final String ADD_ITEM = "addItem";
    public static final String ANALYTICS = "analytics";
    public static final String SETTINGS = "settings";

    private final Map<String, NavButton> buttons = new LinkedHashMap<>();
    private String active = DASHBOARD;

    /**
     * Creates the sidebar.
     *
     * @param onNavigate callback receiving the page id when a nav item is clicked
     */
    public Sidebar(ActionListener onNavigate) {
        setLayout(new BorderLayout());
        setBackground(Theme.FOREST);
        setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));

        add(buildBrand(), BorderLayout.NORTH);
        add(buildNav(onNavigate), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildBrand() {
        JPanel brand = new JPanel(new BorderLayout());
        brand.setOpaque(false);
        brand.setBorder(BorderFactory.createEmptyBorder(22, 20, 18, 20));

        JPanel mark = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PEACH);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                Icon.draw(g2, Icon.LOGO, 9, 9, getWidth() - 18, Theme.FOREST);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        mark.setPreferredSize(new Dimension(36, 36));
        mark.setOpaque(false);

        JPanel text = new JPanel(new BorderLayout());
        text.setOpaque(false);
        JLabel name = new JLabel("STOCKLY");
        name.setFont(Theme.bold(17));
        name.setForeground(Theme.SURFACE);
        name.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        JLabel sub = new JLabel("Inventory, refined");
        sub.setFont(Theme.font(10));
        sub.setForeground(new Color(0xB9C4B4));
        sub.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        text.add(name, BorderLayout.NORTH);
        text.add(sub, BorderLayout.SOUTH);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(mark, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        brand.add(row, BorderLayout.CENTER);
        return brand;
    }

    private JPanel buildNav(ActionListener onNavigate) {
        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new javax.swing.BoxLayout(nav, javax.swing.BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        addNav(nav, DASHBOARD, "Dashboard", Icon.DASHBOARD, onNavigate);
        addNav(nav, INVENTORY, "Inventory", Icon.INVENTORY, onNavigate);
        addNav(nav, ADD_ITEM, "Add New Item", Icon.ADD, onNavigate);
        addNav(nav, ANALYTICS, "Analytics", Icon.ANALYTICS, onNavigate);
        addNav(nav, SETTINGS, "Settings", Icon.SETTINGS, onNavigate);

        nav.add(javax.swing.Box.createVerticalGlue());
        return nav;
    }

    private void addNav(JPanel nav, String id, String label, String icon, ActionListener onNavigate) {
        NavButton btn = new NavButton(id, label, icon);
        btn.addActionListener(e -> onNavigate.actionPerformed(
                new java.awt.event.ActionEvent(id, java.awt.event.ActionEvent.ACTION_PERFORMED, id)));
        buttons.put(id, btn);
        nav.add(btn);
        nav.add(javax.swing.Box.createVerticalStrut(4));
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new javax.swing.BoxLayout(footer, javax.swing.BoxLayout.Y_AXIS));
        footer.setBorder(BorderFactory.createEmptyBorder(12, 20, 16, 20));

        JLabel version = new JLabel("STOCKLY v1.0");
        version.setFont(Theme.font(10));
        version.setForeground(new Color(0x8FA393));
        version.setAlignmentX(LEFT_ALIGNMENT);
        footer.add(version);
        footer.add(javax.swing.Box.createVerticalStrut(10));

        JPanel profile = new JPanel(new BorderLayout(10, 0));
        profile.setOpaque(false);
        profile.setAlignmentX(LEFT_ALIGNMENT);
        profile.setMaximumSize(new Dimension(Theme.SIDEBAR_WIDTH - 40, 44));

        JLabel avatar = new JLabel("TA") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.ROSE);
                g2.fill(new java.awt.geom.Ellipse2D.Float(0, 0, getWidth(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setHorizontalAlignment(JLabel.CENTER);
        avatar.setForeground(Theme.FOREST);
        avatar.setFont(Theme.bold(12));
        avatar.setPreferredSize(new Dimension(34, 34));

        JPanel ptext = new JPanel(new java.awt.GridLayout(2, 1));
        ptext.setOpaque(false);
        JLabel pname = new JLabel("Tooba Akram");
        pname.setFont(Theme.font(12));
        pname.setForeground(Theme.SURFACE);
        JLabel prole = new JLabel("Inventory Admin");
        prole.setFont(Theme.font(10));
        prole.setForeground(new Color(0xB9C4B4));
        ptext.add(pname);
        ptext.add(prole);

        profile.add(avatar, BorderLayout.WEST);
        profile.add(ptext, BorderLayout.CENTER);
        footer.add(profile);
        return footer;
    }

    /** Highlights the given page id and dims the rest. */
    public void setActive(String id) {
        active = id;
        buttons.values().forEach(b -> b.setActive(b.id.equals(id)));
    }

    /** @return id of the currently active page */
    public String getActive() {
        return active;
    }

    /** Navigation button with icon + label and active-state highlight. */
    private static class NavButton extends JButton {
        final String id;
        private boolean active;

        NavButton(String id, String label, String iconName) {
            super(label);
            this.id = id;
            setUI(new javax.swing.plaf.basic.BasicButtonUI());
            setFont(Theme.font(13));
            setForeground(new Color(0xDDE3D6));
            setHorizontalAlignment(JLabel.LEFT);
            setIconTextGap(12);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(0, 46, 0, 12));
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Short.MAX_VALUE, 42));
            setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH - 48, 42));

            // left accent bar + icon painted in paintComponent
            this.iconName = iconName;
        }

        private final String iconName;

        public void setActive(boolean a) {
            this.active = a;
            setForeground(active ? Theme.FOREST : new Color(0xDDE3D6));
            setFont(active ? Theme.bold(13) : Theme.font(13));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (getModel().isRollover() || getModel().isPressed()) {
                g2.setColor(new Color(255, 255, 255, 14));
                g2.fillRoundRect(0, 3, getWidth(), getHeight() - 6, 8, 8);
            }
            if (active) {
                g2.setColor(Theme.ACTIVE_BG);
                g2.fillRoundRect(0, 3, getWidth(), getHeight() - 6, 8, 8);
                g2.setColor(Theme.PEACH);
                g2.fillRoundRect(0, 8, 3, getHeight() - 16, 2, 2);
            }
            Color iconColor = active ? Theme.FOREST : new Color(0xC9D2C2);
            Icon.draw(g2, iconName, 14, (getHeight() - 18) / 2, 18, iconColor);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(Theme.SIDEBAR_WIDTH - 48, 42);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Short.MAX_VALUE, 42);
        }

        @Override
        protected void paintBorder(Graphics g) {
            // no border
        }
    }

}
