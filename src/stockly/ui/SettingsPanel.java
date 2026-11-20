package stockly.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import java.io.File;

/**
 * Settings / About screen.
 * <p>
 * Shows application information, the data-file location and a short
 * description of how stock status is classified.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class SettingsPanel extends JPanel {

    /**
     * Creates the settings/about screen.
     *
     * @param dataFile the CSV file used for persistence
     */
    public SettingsPanel(File dataFile) {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 24, 28));

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 14, 0);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // brand card
        UIComponents.Card brand = new UIComponents.Card();
        brand.setLayout(new BorderLayout());
        brand.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        gbc.gridy = 0;
        content.add(brand, gbc);

        JPanel brandInner = new JPanel(new BorderLayout(16, 0));
        brandInner.setOpaque(false);
        JPanel logoMark = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.FOREST);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                Icon.draw((Graphics2D) g.create(), Icon.LOGO, 10, 10, getWidth() - 20, Theme.PEACH);
            }
        };
        logoMark.setPreferredSize(new Dimension(52, 52));
        logoMark.setOpaque(false);

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new javax.swing.BoxLayout(brandText, javax.swing.BoxLayout.Y_AXIS));
        JLabel name = new JLabel("STOCKLY");
        name.setFont(Theme.bold(20));
        name.setForeground(Theme.TEXT);
        name.setAlignmentX(LEFT_ALIGNMENT);
        JLabel tag = new JLabel("Premium Inventory Management — v1.0");
        tag.setFont(Theme.font(12));
        tag.setForeground(Theme.TEXT_MUTED);
        tag.setAlignmentX(LEFT_ALIGNMENT);
        brandText.add(name);
        brandText.add(javax.swing.Box.createVerticalStrut(3));
        brandText.add(tag);

        brandInner.add(logoMark, BorderLayout.WEST);
        brandInner.add(brandText, BorderLayout.CENTER);
        brand.add(brandInner, BorderLayout.CENTER);

        // info card
        UIComponents.Card info = new UIComponents.Card();
        info.setLayout(new BorderLayout());
        info.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        gbc.gridy = 1;
        content.add(info, gbc);

        JPanel rows = new JPanel(new GridBagLayout());
        rows.setOpaque(false);
        GridBagConstraints r = new GridBagConstraints();
        r.insets = new Insets(6, 0, 6, 0);
        r.anchor = GridBagConstraints.WEST;
        r.fill = GridBagConstraints.HORIZONTAL;

        addRow(rows, r, 0, "Data file", dataFile.getAbsolutePath());
        addRow(rows, r, 1, "Low stock threshold", "1 – 5 units (adjust LOW_STOCK_THRESHOLD in Product.java)");
        addRow(rows, r, 2, "Status rules", "0 = Out of Stock · 1–5 = Low Stock · 6+ = In Stock");
        addRow(rows, r, 3, "Persistence", "CSV format — edit inventory.csv or delete it to reset demo data");

        info.add(rows, BorderLayout.CENTER);
        add(content, BorderLayout.NORTH);
    }

    private void addRow(JPanel parent, GridBagConstraints gbc, int row, String key, String value) {
        gbc.gridy = row * 2;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel k = new JLabel(key);
        k.setFont(Theme.bold(12));
        k.setForeground(Theme.TEXT);
        parent.add(k, gbc);

        gbc.gridy = row * 2 + 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        JLabel v = new JLabel(value);
        v.setFont(Theme.font(12));
        v.setForeground(Theme.TEXT_MUTED);
        parent.add(v, gbc);
        gbc.gridwidth = 1;
    }
}
