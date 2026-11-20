package stockly.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Top navigation bar shown above every page.
 * <p>
 * Displays the current page title and contextual subtitle on the left,
 * and a notification bell plus today's date on the right. Search lives
 * in the Inventory page toolbar — there is exactly one search field.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class TopBar extends JPanel {

    private final JLabel title = new JLabel(" ");
    private final JLabel subtitle = new JLabel(" ");

    /**
     * Creates the top bar.
     *
     * @param onBell callback fired when the notification bell is clicked
     */
    public TopBar(Runnable onBell) {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(18, 28, 14, 28));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        title.setFont(Theme.bold(24));
        title.setForeground(Theme.TEXT);
        subtitle.setFont(Theme.font(12));
        subtitle.setForeground(Theme.TEXT_MUTED);
        subtitle.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        left.add(title, BorderLayout.NORTH);
        left.add(subtitle, BorderLayout.SOUTH);
        add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(new BellButton(onBell));
        right.add(new DateChip());
        add(right, BorderLayout.EAST);
    }

    /** Updates the title and contextual subtitle for the visible page. */
    public void setPage(String titleText, String subtitleText) {
        title.setText(titleText);
        subtitle.setText(subtitleText);
    }

    /** Notification bell with a dot; opens the activity summary. */
    private static class BellButton extends JPanel {
        private boolean hover;
        private final Runnable onBell;

        BellButton(Runnable onBell) {
            this.onBell = onBell;
            setPreferredSize(new Dimension(38, 38));
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setToolTipText("Notifications");
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { onBell.run(); }
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
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            setPreferredSize(new Dimension(0, 34));
            JLabel label = new JLabel(LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")));
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
