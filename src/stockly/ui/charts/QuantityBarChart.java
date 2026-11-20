package stockly.ui.charts;

import stockly.model.Product;
import stockly.ui.Theme;

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
 * @author Stockly Team
 * @version 1.0
 */
public class QuantityBarChart extends JPanel {

    private List<Product> products = List.of();
    private int maxQuantity = 1;

    /** Creates an empty chart; call {@link #setData(List)} to populate. */
    public QuantityBarChart() {
        setOpaque(false);
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

            // bar
            int fill = (int) Math.round((p.getQuantity() / (double) maxQuantity) * barW);
            Color barColor = Theme.CHART_SERIES[i % Theme.CHART_SERIES.length];
            g2.setColor(Theme.SURFACE_ALT);
            g2.fill(new RoundRectangle2D.Float(barX, y + 6, barW, rowH - 13, 5, 5));
            if (fill > 0) {
                g2.setColor(barColor);
                g2.fill(new RoundRectangle2D.Float(barX, y + 6, Math.max(6, fill), rowH - 13, 5, 5));
            }

            // quantity value at bar end
            g2.setColor(Theme.TEXT_MUTED);
            String qty = String.valueOf(p.getQuantity());
            g2.drawString(qty, barX + barW + 8, y + rowH / 2 + 4);

            // row separator
            g2.setColor(Theme.BORDER);
            g2.drawLine(4, y + rowH, w - 8, y + rowH);

            y += rowH;
            i++;
        }
        g2.dispose();
    }
}
