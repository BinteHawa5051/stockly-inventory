package stockly.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Top navigation bar shown above every page.
 * <p>
 * Displays the current page title and contextual subtitle on the left,
 * and a global search field, notification bell and today's date on the
 * right.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class TopBar extends JPanel {

    private final JLabel title = new JLabel(" ");
    private final JLabel subtitle = new JLabel(" ");
    private final JTextField search = new JTextField();

    /**
     * Creates the top bar.
     *
     * @param onSearch callback receiving the search text (empty string clears)
     */
    public TopBar(ActionListener onSearch) {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(18, 28, 14, 28));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        title.setFont(Theme.bold(20));
        title.setForeground(Theme.TEXT);
        subtitle.setFont(Theme.font(12));
        subtitle.setForeground(Theme.TEXT_MUTED);
        subtitle.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        left.add(title, BorderLayout.NORTH);
        left.add(subtitle, BorderLayout.SOUTH);
        add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);

        search.setPreferredSize(new Dimension(210, 34));
        search.setFont(Theme.font(12));
        search.setForeground(Theme.TEXT);
        search.setCaretColor(Theme.FOREST);
        search.setToolTipText("Search products");
        SearchField field = new SearchField(search);
        right.add(field);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { fire(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { fire(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { fire(); }
            private void fire() {
                onSearch.actionPerformed(new java.awt.event.ActionEvent(
                        search.getText().trim(), java.awt.event.ActionEvent.ACTION_PERFORMED, "search"));
            }
        });

        right.add(new BellButton());
        right.add(new DateChip());
        add(right, BorderLayout.EAST);
    }

    /** Updates the title and contextual subtitle for the visible page. */
    public void setPage(String titleText, String subtitleText) {
        title.setText(titleText);
        subtitle.setText(subtitleText);
    }

    /** Clears the global search field. */
    public void clearSearch() {
        search.setText("");
    }

    /** @return the raw search field (for external listeners if needed) */
    public JTextField getSearchField() {
        return search;
    }

    /** Search input with a leading magnifier icon and rounded border. */
    public static class SearchField extends JPanel {
        SearchField(JTextField field) {
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            add(new IconPad(Icon.SEARCH, 12, Theme.TEXT_MUTED), BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.SURFACE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 17, 17);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 17, 17);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Small square panel hosting a single icon with padding. */
    public static class IconPad extends JPanel {
        private final String name;
        private final int pad;
        private final Color color;

        IconPad(String name, int pad, Color color) {
            this.name = name;
            this.pad = pad;
            this.color = color;
            setOpaque(false);
            setPreferredSize(new Dimension(34, 34));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Icon.draw(g2, name, pad, pad, 18, color);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Notification bell with a small sage dot. */
    private static class BellButton extends JPanel {
        private boolean hover;

        BellButton() {
            setPreferredSize(new Dimension(38, 38));
            setOpaque(false);
            setToolTipText("Notifications");
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (hover) {
                g2.setColor(Theme.HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            Icon.draw(g2, Icon.BELL, 9, 8, 20, Theme.TEXT);
            g2.setColor(Theme.CHART_LOW_STOCK);
            g2.fillOval(getWidth() - 13, 7, 7, 7);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Rounded chip showing today's date. */
    private static class DateChip extends JPanel {
        DateChip() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setPreferredSize(new Dimension(0, 34));
            JLabel label = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")));
            label.setFont(Theme.font(12));
            label.setForeground(Theme.TEXT_MUTED);
            add(label, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.SURFACE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 17, 17);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 17, 17);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
