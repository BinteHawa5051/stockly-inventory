package stockly.ui.charts;

import stockly.model.Product;
import stockly.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Horizontal bar chart showing each product's stock quantity.
 * <p>
 * Products are listed top-to-bottom with their names on the left and a
 * pastel bar whose length is proportional to the quantity. A subtle
 * max-value gridline and numeric value label complete each row.
 * When the product list is long the chart scrolls vertically.
 *
 * @author Tooba Akram &amp; Abeeha Noor
 * @version 1.0
 */
public class QuantityBarChart extends JPanel {

    private List<Product> products = List.of();
    private int maxQuantity = 1;
    private final JLabel tip = new JLabel();

    /** Creates an empty chart; call {@link #setData(List)} to populate. */
    public QuantityBarChart() {
        setOpaque(false);
        setLayout(null);
        tip.setFont(Theme.font(11));
        tip.setForeground(Theme.SURFACE);
        tip.setBackground(new Color(0x30332F));
        tip.setOpaque(true);
        tip.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        tip.setVisible(false);
        add(tip);
        attachTooltip();
    }

    private int rowAt(int my) {
        int rowH = 30;
        int top = 14;
        int idx = (my - top) / rowH;
        return idx >= 0 && idx < products.size() ? idx : -1;
    }

    private void attachTooltip() {
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int idx = rowAt(e.getY());
                if (idx < 0) {
                    tip.setVisible(false);
                    return;
                }
                Product p = products.get(idx);
                tip.setText(p.getName() + " — " + p.getQuantity() + " units ("
                        + p.getStockStatus().getLabel() + ")");
                tip.setSize(tip.getPreferredSize());
                int tx = Math.min(e.getX() + 14, getWidth() - tip.getWidth() - 4);
                int ty = Math.max(4, e.getY() - tip.getHeight() - 8);
                tip.setLocation(tx, ty);
                tip.setVisible(true);
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseExited(java.awt.event.MouseEvent e) { tip.setVisible(false); }
        });
    }

    /**
     * Replaces the chart data and repaints.
     *
     * @param data products to display, in display order
     */
    public void setData(List<Product> data) {
        this.products = data;
        this.maxQuantity = Math.max(1, data.stream().mapToInt(Product::getQuantity).max().orElse(1));
        int rowH = 30;
        int preferred = Math.max(140, data.size() * rowH + 20);
        setPreferredSize(new Dimension(10, preferred));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int nameW = Math.min(150, w / 3);
        int valueW = 44;
        int barX = nameW + 12;
        int barW = w - barX - valueW - 8;
        int rowH = 30;
        int top = 14;

        g2.setFont(Theme.font(11));
        FontMetrics fm = g2.getFontMetrics();

        if (products.isEmpty()) {
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(Theme.font(12));
            String msg = "No products to chart yet";
            g2.drawString(msg, (w - g2.getFontMetrics().stringWidth(msg)) / 2, h / 2);
            g2.dispose();
            return;
        }

        int y = top;
        int i = 0;
        for (Product p : products) {
            // product name (ellipsized if needed)
            String name = p.getName();
            while (name.length() > 4 && fm.stringWidth(name) > nameW - 8) {
                name = name.substring(0, name.length() - 2) + "…";
            }
            g2.setColor(Theme.TEXT);
            g2.drawString(name, 4, y + rowH / 2 + 4);

            // bar coloured by stock status — agrees with the rest of the UI
            int fill = (int) Math.round((p.getQuantity() / (double) maxQuantity) * barW);
            Color barColor = switch (p.getStockStatus()) {
                case IN_STOCK -> Theme.CHART_IN_STOCK;
                case LOW_STOCK -> Theme.CHART_LOW_STOCK;
                case OUT_OF_STOCK -> Theme.CHART_OUT_STOCK;
            };
            g2.setColor(Theme.SURFACE_ALT);
            g2.fill(new RoundRectangle2D.Float(barX, y + 6, barW, rowH - 13, 5, 5));
            if (fill > 0) {
                g2.setColor(barColor);
                g2.fill(new RoundRectangle2D.Float(barX, y + 6, Math.max(6, fill), rowH - 13, 5, 5));
            }

            // quantity value right-aligned in a fixed column
            g2.setColor(Theme.TEXT_MUTED);
            String qty = String.valueOf(p.getQuantity());
            g2.drawString(qty, barX + barW + valueW - fm.stringWidth(qty) - 4,
                    y + rowH / 2 + 4);

            // row separator
            g2.setColor(Theme.BORDER);
            g2.drawLine(4, y + rowH, w - 8, y + rowH);

            y += rowH;
            i++;
        }
        g2.dispose();
    }
}
