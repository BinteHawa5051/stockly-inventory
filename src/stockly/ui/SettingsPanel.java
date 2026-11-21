package stockly.ui;

import stockly.model.AppConfig;
import stockly.model.InventoryManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JScrollPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Settings screen organised into clear sections.
 * <p>
 * Application profile, inventory preferences (low-stock threshold and
 * delete confirmation — both live via {@link AppConfig}), appearance
 * palette preview, data management (export / import / clear with
 * confirmations handled by the frame) and an About block.
 *
 * @author Tooba Akram and Abeeha Noor
 * @version 1.0
 */
public class SettingsPanel extends JPanel {

    /**
     * Creates the settings screen.
     *
     * @param manager        shared inventory data source (for counts)
     * @param onExport       callback for CSV export
     * @param onImport       callback for CSV import
     * @param onClear        callback for clearing the inventory
     */
    public SettingsPanel(InventoryManager manager, Runnable onExport,
                         Runnable onImport, Runnable onClear) {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 24, 28));

        JPanel scroll = new JPanel(new GridBagLayout());
        scroll.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 16, 0);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int row = 0;
        gbc.gridy = row++;
        scroll.add(profileCard(), gbc);
        gbc.gridy = row++;
        scroll.add(preferencesCard(), gbc);
        gbc.gridy = row++;
        scroll.add(appearanceCard(), gbc);
        gbc.gridy = row++;
        scroll.add(dataCard(manager, onExport, onImport, onClear), gbc);
        gbc.gridy = row;
        scroll.add(aboutCard(), gbc);

        JScrollPaneDecorator.wrap(this, scroll);
    }

    // ------------------------------------------------------------------
    // Section builders
    // ------------------------------------------------------------------

    private JPanel profileCard() {
        UIComponents.Card card = sectionCard("Application Profile", "STOCKLY identity and administrator");
        JPanel inner = new JPanel(new BorderLayout(16, 0));
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel mark = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.FOREST);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                Icon.draw((Graphics2D) g.create(), Icon.LOGO, 10, 10, getWidth() - 20, Theme.PEACH);
            }
        };
        mark.setPreferredSize(new Dimension(52, 52));
        mark.setOpaque(false);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        JLabel name = new JLabel("STOCKLY");
        name.setFont(Theme.bold(18));
        name.setForeground(Theme.TEXT);
        name.setAlignmentX(LEFT_ALIGNMENT);
        JLabel desc = new JLabel("Premium inventory management for modern retail teams.");
        desc.setFont(Theme.font(12));
        desc.setForeground(Theme.TEXT_MUTED);
        desc.setAlignmentX(LEFT_ALIGNMENT);
        JLabel admin = new JLabel("Administrator: Tooba Akram");
        admin.setFont(Theme.font(11));
        admin.setForeground(Theme.TEXT_MUTED);
        admin.setAlignmentX(LEFT_ALIGNMENT);
        text.add(name);
        text.add(javax.swing.Box.createVerticalStrut(3));
        text.add(desc);
        text.add(javax.swing.Box.createVerticalStrut(4));
        text.add(admin);

        inner.add(mark, BorderLayout.WEST);
        inner.add(text, BorderLayout.CENTER);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private JPanel preferencesCard() {
        UIComponents.Card card = sectionCard("Inventory Preferences",
                "Changes apply immediately across the whole app");
        JPanel rows = new JPanel(new GridBagLayout());
        rows.setOpaque(false);
        rows.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        GridBagConstraints r = new GridBagConstraints();
        r.insets = new Insets(6, 0, 6, 0);
        r.anchor = GridBagConstraints.WEST;
        r.fill = GridBagConstraints.HORIZONTAL;

        // Low-stock threshold
        r.gridy = 0; r.gridx = 0; r.weightx = 0;
        JLabel k1 = prefLabel("Low-stock threshold");
        rows.add(k1, r);
        r.gridy = 1; r.gridx = 0; r.gridwidth = 2; r.weightx = 1;
        JPanel thr = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        thr.setOpaque(false);
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(
                AppConfig.getLowStockThreshold(), 0, 100, 1));
        spinner.setFont(Theme.font(13));
        spinner.setPreferredSize(new Dimension(80, 34));
        spinner.addChangeListener(e -> {
            AppConfig.setLowStockThreshold((Integer) spinner.getValue());
            AppConfig.save();
        });
        JLabel hint = new JLabel("units — items at or below this count as Low Stock");
        hint.setFont(Theme.font(11));
        hint.setForeground(Theme.TEXT_MUTED);
        thr.add(spinner);
        thr.add(hint);
        rows.add(thr, r);
        r.gridwidth = 1;

        // Confirm delete
        r.gridy = 2; r.gridx = 0; r.weightx = 0;
        rows.add(prefLabel("Delete confirmation"), r);
        r.gridy = 3; r.gridx = 0; r.gridwidth = 2; r.weightx = 1;
        JCheckBox confirmBox = new JCheckBox("Ask before deleting a product", AppConfig.isConfirmDelete());
        confirmBox.setFont(Theme.font(12));
        confirmBox.setForeground(Theme.TEXT);
        confirmBox.setOpaque(false);
        confirmBox.addActionListener(e -> {
            AppConfig.setConfirmDelete(confirmBox.isSelected());
            AppConfig.save();
        });
        rows.add(confirmBox, r);
        r.gridwidth = 1;

        // Status mode note
        r.gridy = 4; r.gridx = 0; r.gridwidth = 2;
        JLabel note = new JLabel("Stock status rule: 0 = Out of Stock · 1–threshold = Low Stock · above = In Stock. "
                + "Products can also pin a manual status from the add/edit form.");
        note.setFont(Theme.font(11));
        note.setForeground(Theme.TEXT_MUTED);
        rows.add(note, r);

        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel appearanceCard() {
        UIComponents.Card card = sectionCard("Appearance", "STOCKLY colour palette");
        JPanel swatches = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        swatches.setOpaque(false);
        swatches.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        Object[][] palette = {
                {Theme.BG, "Ivory"}, {Theme.SAGE, "Sage"}, {Theme.FOREST, "Forest"},
                {Theme.ROSE, "Rose"}, {Theme.PEACH, "Peach"}, {Theme.LAVENDER, "Lavender"},
                {Theme.TEXT, "Charcoal"}, {Theme.BORDER, "Grey"}
        };
        for (Object[] entry : palette) {
            swatches.add(new Swatch((Color) entry[0], (String) entry[1]));
        }
        card.add(swatches, BorderLayout.CENTER);
        return card;
    }

    private JPanel dataCard(InventoryManager manager, Runnable onExport,
                            Runnable onImport, Runnable onClear) {
        UIComponents.Card card = sectionCard("Data Management",
                manager.productCount() + " products currently stored in inventory.csv");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.setOpaque(false);
        btns.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));

        UIComponents.PrimaryButton export = new UIComponents.PrimaryButton("Export CSV");
        export.addActionListener(e -> onExport.run());
        UIComponents.GhostButton imp = new UIComponents.GhostButton("Import CSV");
        imp.addActionListener(e -> onImport.run());
        UIComponents.GhostButton clear = new UIComponents.GhostButton("Clear Inventory");
        clear.setForeground(Theme.CHART_OUT_STOCK);
        clear.addActionListener(e -> onClear.run());

        btns.add(export);
        btns.add(imp);
        btns.add(clear);
        card.add(btns, BorderLayout.CENTER);
        return card;
    }

    private JPanel aboutCard() {
        UIComponents.Card card = sectionCard("About STOCKLY", "Application information");
        JPanel rows = new JPanel(new GridBagLayout());
        rows.setOpaque(false);
        rows.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        GridBagConstraints r = new GridBagConstraints();
        r.insets = new Insets(5, 0, 5, 0);
        r.anchor = GridBagConstraints.WEST;
        r.fill = GridBagConstraints.HORIZONTAL;

        String[][] items = {
                {"Application", "STOCKLY — Inventory Management System"},
                {"Version", "1.0"},
                {"Technology", "Java 25 · Swing · Java2D custom charts"},
                {"Data", "CSV persistence (inventory.csv) + settings.properties"},
                {"Developer", "Tooba Akram — OEL Individual Project"},
        };
        for (int i = 0; i < items.length; i++) {
            r.gridy = i * 2; r.gridx = 0; r.weightx = 0;
            JLabel k = new JLabel(items[i][0]);
            k.setFont(Theme.bold(12));
            k.setForeground(Theme.TEXT_MUTED);
            rows.add(k, r);
            r.gridy = i * 2 + 1; r.gridx = 0; r.gridwidth = 2; r.weightx = 1;
            JLabel v = new JLabel(items[i][1]);
            v.setFont(Theme.font(12));
            v.setForeground(Theme.TEXT);
            rows.add(v, r);
            r.gridwidth = 1;
        }
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private UIComponents.Card sectionCard(String title, String subtitle) {
        UIComponents.Card card = new UIComponents.Card();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        card.add(new UIComponents.SectionTitle(title, subtitle), BorderLayout.NORTH);
        return card;
    }

    private JLabel prefLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bold(12));
        l.setForeground(Theme.TEXT);
        return l;
    }

    /** Color swatch with name label. */
    private static class Swatch extends JPanel {
        Swatch(Color color, String name) {
            setLayout(new java.awt.GridLayout(2, 1));
            setOpaque(false);
            JPanel chip = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.setColor(Theme.BORDER);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                    g2.dispose();
                }
            };
            chip.setPreferredSize(new Dimension(34, 22));
            chip.setOpaque(false);
            JLabel l = new JLabel(name, SwingConstants.CENTER);
            l.setFont(Theme.font(9));
            l.setForeground(Theme.TEXT_MUTED);
            add(chip);
            add(l);
        }
    }

    /** Wraps content in a scroll pane filling the panel. */
    private static class JScrollPaneDecorator {
        static void wrap(JPanel target, JPanel content) {
            JScrollPane scroll = new JScrollPane(content);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            target.add(scroll, BorderLayout.CENTER);
        }
    }
}
