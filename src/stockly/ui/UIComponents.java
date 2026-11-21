package stockly.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Reusable styled Swing components shared across the application.
 * <p>
 * These components replace default-looking Swing widgets with the
 * Stockly visual language: rounded cards, flat forest buttons, soft
 * hover states and pill-shaped status badges.
 *
 * @author Tooba Akram and Abeeha Noor
 * @version 1.0
 */
public final class UIComponents {

    private UIComponents() {
        // utility class
    }

    /**
     * Rounded surface panel used as the base for cards and sections.
     */
    public static class Card extends JPanel {
        /**
         * Creates an empty card with a borderless {@link java.awt.BorderLayout}.
         */
        public Card() {
            setOpaque(false);
            setLayout(new java.awt.BorderLayout());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.SURFACE);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), Theme.RADIUS, Theme.RADIUS));
            g2.setColor(Theme.BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1,
                    Theme.RADIUS, Theme.RADIUS));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Forest-green primary action button with a subtle hover state.
     */
    public static class PrimaryButton extends JButton {
        /**
         * Creates a primary button in the default forest-green colour.
         *
         * @param text button label
         */
        public PrimaryButton(String text) {
            super(text);
            style(Theme.FOREST, Theme.SURFACE);
        }

        /**
         * Creates a primary button with a custom fill colour.
         *
         * @param text button label
         * @param bg   normal-state background colour (darkened on hover/press)
         */
        public PrimaryButton(String text, Color bg) {
            super(text);
            style(bg, Theme.SURFACE);
        }

        private void style(Color bg, Color fg) {
            setFont(Theme.font(13));
            setForeground(fg);
            setBackground(bg);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(0, 0, 0, 0));

            MouseAdapter hover = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setBackground(bg.darker()); repaint(); }
                @Override public void mouseExited(MouseEvent e) { setBackground(bg); repaint(); }
            };
            addMouseListener(hover);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 28, 38);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = getModel().isPressed() ? getBackground().darker().darker() : getBackground();
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), Theme.RADIUS, Theme.RADIUS));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Quiet secondary button with a soft grey fill.
     */
    public static class GhostButton extends JButton {
        /**
         * Creates a quiet secondary button with the standard grey fill.
         *
         * @param text button label
         */
        public GhostButton(String text) {
            super(text);
            setFont(Theme.font(13));
            setForeground(Theme.TEXT);
            setBackground(Theme.SURFACE_ALT);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(0, 0, 0, 0));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setBackground(Theme.HOVER); repaint(); }
                @Override public void mouseExited(MouseEvent e) { setBackground(Theme.SURFACE_ALT); repaint(); }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 24, 38);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), Theme.RADIUS, Theme.RADIUS));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Pill-shaped status badge showing the stock status.
     */
    public static class StatusBadge extends JPanel {
        /** Centred label inside the pill. */
        private final JLabel label = new JLabel();

        /**
         * Creates a badge showing the given stock status.
         *
         * @param status the stock status whose label and colour to display
         */
        public StatusBadge(stockly.model.StockStatus status) {
            setOpaque(false);
            setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
            setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
            setPreferredSize(new Dimension(0, 24));
            label.setFont(Theme.font(11));
            label.setForeground(status.getColor());
            add(label);
            update(status.getLabel(), status.getColor());
        }

        /**
         * Refreshes the badge text and color.
         *
         * @param text  new badge label
         * @param color new badge accent colour
         */
        public void update(String text, Color color) {
            label.setText(text);
            label.setForeground(color);
        }

        /**
         * Returns the badge width needed to fit its text plus padding.
         *
         * @return preferred width that fits the text plus padding
         */
        @Override
        public Dimension getPreferredSize() {
            Dimension d = label.getPreferredSize();
            return new Dimension(d.width + 22, 24);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = label.getForeground();
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 22));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight() / 2f, getHeight() / 2f));
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 90));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1,
                    getHeight() / 2f, getHeight() / 2f));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Section heading with optional muted subtitle.
     */
    public static class SectionTitle extends JPanel {
        /**
         * Creates a section heading with an optional muted subtitle.
         *
         * @param title    main heading text
         * @param subtitle supporting line below the heading, or {@code null} to omit
         */
        public SectionTitle(String title, String subtitle) {
            setOpaque(false);
            setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

            JLabel t = new JLabel(title);
            t.setFont(Theme.bold(16));
            t.setForeground(Theme.TEXT);
            t.setAlignmentX(LEFT_ALIGNMENT);
            add(t);

            if (subtitle != null && !subtitle.isEmpty()) {
                JLabel s = new JLabel(subtitle);
                s.setFont(Theme.font(12));
                s.setForeground(Theme.TEXT_MUTED);
                s.setAlignmentX(LEFT_ALIGNMENT);
                add(s);
            }
        }
    }

    /**
     * Muted helper text label.
     */
    public static class Caption extends JLabel {
        /**
         * Creates muted helper text in the small UI font.
         *
         * @param text the caption text
         */
        public Caption(String text) {
            super(text);
            setFont(Theme.font(11));
            setForeground(Theme.TEXT_MUTED);
        }
    }

    /**
     * Lays the given labels out in a single centred row.
     *
     * @param labels the labels to display, in order
     * @return a transparent panel containing the centred labels
     */
    public static JPanel centered(JLabel... labels) {
        JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 8, 0));
        p.setOpaque(false);
        for (JLabel l : labels) {
            p.add(l);
        }
        return p;
    }

    /**
     * Formats a number compactly for big stat values, e.g. {@code 1234}
     * becomes {@code "1.2k"}.
     *
     * @param value the raw count to abbreviate
     * @return compact "1.2k" style string
     */
    public static String compact(long value) {
        if (value < 1000) {
            return String.valueOf(value);
        }
        return String.format("%.1fk", value / 1000.0).replace(".0k", "k");
    }

    /**
     * Measures a string's rendered width in pixels.
     *
     * @param text the string to measure
     * @param font the font to measure with
     * @return width of {@code text} in pixels under {@code font}
     */
    public static int textWidth(String text, java.awt.Font font) {
        java.awt.FontMetrics fm = new java.awt.Canvas().getFontMetrics(font);
        return fm.stringWidth(text);
    }
}
