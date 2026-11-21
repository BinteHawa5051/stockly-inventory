package stockly.ui;

import stockly.model.StockStatus;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Shared table-chrome helpers for STOCKLY.
 * <p>
 * Centralises every custom-painted piece used by data tables so the
 * inventory screen stays readable and consistent:
 * <ul>
 *   <li>{@link StatusPill} — fully rounded stock-status badge with
 *       centred text and status-appropriate pastel colours.</li>
 *   <li>{@link StatusCellRenderer} — table-cell renderer producing a
 *       correctly sized {@link StatusPill}.</li>
 *   <li>{@link ActionCellRenderer} — row action buttons (view / edit / delete)
 *       drawn with the vector {@link Icon} set.</li>
 *   <li>{@link StyledComboUI} — combo box with a custom-drawn arrow that
 *       matches the text-field styling.</li>
 *   <li>{@link SearchBox} — rounded search field with the magnifier
 *       icon painted inside it.</li>
 *   <li>{@link #rowBackground} — single source for zebra / hover /
 *       selection row colours.</li>
 * </ul>
 *
 * @author Stockly Team
 * @version 1.0
 */
public final class TableKit {

    private TableKit() {
        // utility class
    }

    // ------------------------------------------------------------------
    // Row colours — one source of truth for every column
    // ------------------------------------------------------------------

    /** Zebra stripe colour (odd rows). */
    public static final Color ZEBRA = new Color(0xFAF9F4);
    /** Hover highlight colour. */
    public static final Color HOVER = new Color(0xF4F2EA);
    /** Selection colour. */
    public static final Color SELECTED = new Color(0xE4EADF);

    /**
     * Computes the background colour for a table row so every column —
     * including badges and the actions cell — shares the same stripe,
     * hover and selection state.
     *
     * @param row       view row index
     * @param selected  whether the row is selected
     * @param hoverRow  currently hovered row, or {@code -1}
     * @return the background colour to paint
     */
    public static Color rowBackground(int row, boolean selected, int hoverRow) {
        if (selected) {
            return SELECTED;
        }
        if (row == hoverRow) {
            return HOVER;
        }
        return row % 2 == 1 ? ZEBRA : Theme.SURFACE;
    }

    // ------------------------------------------------------------------
    // Status pill
    // ------------------------------------------------------------------

    /** Pill colours per status: pale fill + dark readable text. */
    private static Color pillBg(StockStatus s) {
        return switch (s) {
            case IN_STOCK -> new Color(0xDFE9DA);   // pale sage
            case LOW_STOCK -> new Color(0xF6E4CE);  // pale peach
            case OUT_OF_STOCK -> new Color(0xF3D9D6); // pale rose
        };
    }

    private static Color pillFg(StockStatus s) {
        return switch (s) {
            case IN_STOCK -> new Color(0x3E5C40);    // dark green
            case LOW_STOCK -> new Color(0x8A5A26);   // dark amber-brown
            case OUT_OF_STOCK -> new Color(0x9E3B38); // dark red
        };
    }

    /**
     * A fully rounded status badge. The label is centred horizontally
     * and vertically inside the pill; the pill is 26 px tall and just
     * wide enough for the text plus padding.
     */
    public static class StatusPill extends JPanel {
        private final StockStatus status;
        private final JLabel label = new JLabel();

        public StatusPill(StockStatus status) {
            this.status = status;
            setOpaque(false);
            setLayout(new GridLayout(1, 1));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            label.setFont(Theme.font(11));
            label.setForeground(pillFg(status));
            label.setText(status.getLabel());
            add(label);
        }

        @Override
        public Dimension getPreferredSize() {
            // text width + horizontal padding, fixed 26 px height
            int w = label.getPreferredSize().width + 22;
            return new Dimension(Math.max(84, w), 26);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(pillBg(status));
            g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2,
                    getHeight(), getHeight()));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Table-cell renderer that returns a {@link StatusPill} sized to
     * fit its text, centred in the cell.
     */
    public static class StatusCellRenderer implements TableCellRenderer {
        private final JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

        public StatusCellRenderer() {
            wrap.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(javax.swing.JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            ProductStatusShim shim = (ProductStatusShim) value;
            StatusPill pill = new StatusPill(shim.status);
            wrap.removeAll();
            wrap.add(pill);
            wrap.setBackground(TableKit.rowBackground(row, isSelected,
                    table.getMousePosition() != null ? table.rowAtPoint(table.getMousePosition()) : -1));
            return wrap;
        }
    }

    /** Tiny carrier so the status column can pass the enum through the model. */
    public record ProductStatusShim(StockStatus status) { }

    // ------------------------------------------------------------------
    // Row action icons
    // ------------------------------------------------------------------

    /**
     * Square icon button (view / edit / delete) drawn with the vector
     * {@link Icon} set. 30×30 px hit target, icon centred, subtle hover
     * rounded background, delete tinted muted red.
     */
    public static class RowIconButton extends JButton {
        private final String iconName;
        private Color iconColor = Theme.TEXT_MUTED;
        private boolean hover;

        public RowIconButton(String iconName) {
            super();
            this.iconName = iconName;
            setPreferredSize(new Dimension(30, 30));
            setMaximumSize(new Dimension(30, 30));
            setMinimumSize(new Dimension(30, 30));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(0, 0, 0, 0));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        /** Tints the icon (used for the muted-red delete glyph). */
        public void setIconColor(Color color) {
            this.iconColor = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            if (hover || getModel().isPressed()) {
                g2.setColor(Theme.HOVER);
                g2.fill(new RoundRectangle2D.Float(2, 2, getWidth() - 4,
                        getHeight() - 4, 8, 8));
            }
            // 18 px icon centred in the 30 px target
            Icon.draw(g2, iconName, (getWidth() - 18) / 2, (getHeight() - 18) / 2,
                    18, iconColor);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Cell renderer producing the three centred row-action buttons.
     * The buttons are visual only — clicks are handled by the table's
     * mouse listener which maps x-position to view/edit/delete.
     */
    public static class ActionCellRenderer implements TableCellRenderer {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));

        public ActionCellRenderer() {
            panel.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(javax.swing.JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            panel.removeAll();
            RowIconButton view = new RowIconButton(Icon.EYE);
            view.setToolTipText("View details");
            RowIconButton edit = new RowIconButton(Icon.EDIT);
            edit.setToolTipText("Edit product");
            RowIconButton del = new RowIconButton(Icon.TRASH);
            del.setIconColor(new Color(0xA9605C));
            del.setToolTipText("Delete product");
            panel.add(view);
            panel.add(edit);
            panel.add(del);
            panel.setBackground(TableKit.rowBackground(row, isSelected,
                    table.getMousePosition() != null ? table.rowAtPoint(table.getMousePosition()) : -1));
            return panel;
        }
    }

    // ------------------------------------------------------------------
    // Styled combo box
    // ------------------------------------------------------------------

    /**
     * Combo-box UI with a custom-drawn triangular arrow and flat
     * ivory styling that matches the rounded search field.
     */
    public static class StyledComboUI extends BasicComboBoxUI {

        @Override
        protected JButton createArrowButton() {
            JButton btn = new BasicArrowButton(BasicArrowButton.SOUTH) {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(30, getHeight());
                }

                @Override
                public Dimension getMinimumSize() {
                    return new Dimension(24, 0);
                }

                @Override
                public Dimension getMaximumSize() {
                    return new Dimension(30, Integer.MAX_VALUE);
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    // custom chevron
                    g2.setColor(Theme.TEXT_MUTED);
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    g2.fillPolygon(new int[]{cx - 5, cx + 5, cx},
                            new int[]{cy - 3, cy - 3, cy + 3}, 3);
                    g2.dispose();
                }
            };
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder());
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            return btn;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds,
                                                boolean hasFocus) {
            // flat ivory background — no native button chrome
            g.setColor(Theme.SURFACE);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        protected ComboPopup createPopup() {
            BasicComboPopup popup = (BasicComboPopup) super.createPopup();
            popup.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                    BorderFactory.createEmptyBorder(4, 0, 4, 0)));
            return popup;
        }
    }

    // ------------------------------------------------------------------
    // Search box — magnifier inside the field
    // ------------------------------------------------------------------

    /**
     * Rounded search field: magnifier icon painted inside the left
     * padding, borderless text field filling the rest, optional
     * placeholder text when empty.
     */
    public static class SearchBox extends JPanel {
        private final javax.swing.JTextField field;
        private final String placeholder;

        /**
         * Creates the search box.
         *
         * @param placeholder hint text shown while empty (may be null)
         */
        public SearchBox(String placeholder) {
            this.placeholder = placeholder;
            setLayout(new java.awt.BorderLayout());
            setOpaque(false);
            field = new javax.swing.JTextField() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (getText().isEmpty() && placeholder != null && !isFocusOwner()) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setColor(Theme.TEXT_MUTED);
                        g2.setFont(getFont());
                        java.awt.FontMetrics fm = g2.getFontMetrics();
                        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                        g2.drawString(placeholder, getInsets().left, y);
                        g2.dispose();
                    }
                }
            };
            field.setOpaque(false);
            field.setBorder(BorderFactory.createEmptyBorder(0, 34, 0, 12));
            field.setFont(Theme.font(12));
            field.setForeground(Theme.TEXT);
            field.setCaretColor(Theme.FOREST);
            add(field, BorderLayout.CENTER);
        }

        /** @return the inner text field */
        public javax.swing.JTextField getField() {
            return field;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.SURFACE);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
            g2.setColor(Theme.BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1,
                    getHeight() - 1, 18, 18));
            // magnifier inside the field's left edge
            Icon.draw(g2, Icon.SEARCH, 11, (getHeight() - 16) / 2, 16,
                    field.isFocusOwner() ? Theme.FOREST : Theme.TEXT_MUTED);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
