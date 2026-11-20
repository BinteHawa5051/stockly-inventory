package stockly.ui.charts;

import stockly.model.StockStatus;
import stockly.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Donut chart showing how many products fall into each stock status.
 * <p>
 * Three proportional arcs drawn clockwise from 12 o'clock in the order
 * In Stock → Low Stock → Out of Stock, each separated by a thin gap.
 * The centre shows the total product count inside the ring hole, and a
 * vertically-centred legend with counts and percentages sits to the
 * right. Hovering a segment shows a tooltip.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class DonutChart extends JPanel {

    /** Legend/segment order — must match the drawing order. */
    private static final List<StockStatus> ORDER = List.of(
            StockStatus.IN_STOCK, StockStatus.LOW_STOCK, StockStatus.OUT_OF_STOCK);

    /** Angular gap between segments, in radians (~2.5 degrees). */
    private static final double GAP = Math.toRadians(2.5);

    private final Map<StockStatus, Integer> counts = new EnumMap<>(StockStatus.class);
    private int total;
    private final JLabel tip = new JLabel();

    /** Creates an empty chart; call {@link #setData(Map)} to populate. */
    public DonutChart() {
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

    /**
     * Replaces the chart data and repaints.
     *
     * @param data product counts keyed by stock status
     */
    public void setData(Map<StockStatus, Integer> data) {
        counts.clear();
        counts.putAll(data);
        total = counts.values().stream().mapToInt(Integer::intValue).sum();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(340, 230);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Measure the legend first so the donut can shrink to make room.
        FontMetrics fm = legendMetrics(g2);
        int legendW = legendWidth(fm);

        // Donut occupies the left area; legend is right-anchored with a
        // guaranteed gap. Shrink the donut if the panel is too narrow.
        int donutSize = donutSizeFor(w, h, legendW);
        int cx = 8 + donutSize / 2;
        int cy = h / 2;
        int outer = donutSize / 2 - 4;
        int ring = Math.max(14, outer / 5);          // thinner ring
        int inner = outer - ring;

        if (total == 0) {
            drawEmpty(g2, cx, cy, outer, inner);
        } else {
            drawRing(g2, cx, cy, outer, inner, ring);
            drawCenter(g2, cx, cy, inner);
        }
        drawLegend(g2, w - legendW - 8, cy, fm);
        g2.dispose();
    }

    private void drawRing(Graphics2D g2, int cx, int cy, int outer, int inner, int ring) {
        // stroke is centred on radius (outer+inner)/2
        double mid = (outer + inner) / 2.0;
        double start = -Math.PI / 2; // 12 o'clock
        for (StockStatus s : ORDER) {
            int count = counts.getOrDefault(s, 0);
            if (count == 0) {
                continue;
            }
            double extent = (count / (double) total) * 2 * Math.PI;
            double a0 = start + GAP / 2;
            double a1 = start + extent - GAP / 2;
            if (a1 <= a0) {
                a1 = a0 + 0.001;
            }
            g2.setColor(s == StockStatus.IN_STOCK ? Theme.CHART_IN_STOCK
                    : s == StockStatus.LOW_STOCK ? Theme.CHART_LOW_STOCK
                    : Theme.CHART_OUT_STOCK);
            g2.setStroke(new BasicStroke(ring, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g2.draw(new Arc2D.Double(cx - mid, cy - mid, mid * 2, mid * 2,
                    Math.toDegrees(a0), Math.toDegrees(-(a1 - a0)), Arc2D.OPEN));
            start += extent;
        }
    }

    private void drawCenter(Graphics2D g2, int cx, int cy, int inner) {
        // clear the hole so centre text never touches the ring
        g2.setColor(Theme.SURFACE);
        g2.fillOval((int) (cx - inner), (int) (cy - inner), inner * 2, inner * 2);

        g2.setColor(Theme.TEXT);
        g2.setFont(Theme.bold(20));
        String value = String.valueOf(total);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(value, cx - fm.stringWidth(value) / 2, cy - 2);

        g2.setColor(Theme.TEXT_MUTED);
        g2.setFont(Theme.font(11));
        String label = total == 1 ? "product" : "products";
        fm = g2.getFontMetrics();
        g2.drawString(label, cx - fm.stringWidth(label) / 2, cy + 16);
    }

    private void drawEmpty(Graphics2D g2, int cx, int cy, int outer, int inner) {
        g2.setColor(Theme.BORDER);
        g2.setStroke(new BasicStroke(Math.max(10, outer - inner)));
        g2.draw(new Arc2D.Double(cx - outer, cy - outer, outer * 2, outer * 2,
                0, 360, Arc2D.OPEN));
        g2.setColor(Theme.SURFACE);
        g2.fillOval(cx - inner, cy - inner, inner * 2, inner * 2);
        g2.setColor(Theme.TEXT_MUTED);
        g2.setFont(Theme.font(12));
        String msg = "No products yet";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, cx - fm.stringWidth(msg) / 2, cy);
    }

    private void drawLegend(Graphics2D g2, int x, int cy, FontMetrics fm) {
        int rowH = 30;
        int y = cy - (rowH * ORDER.size()) / 2;
        g2.setFont(Theme.font(12));
        for (StockStatus s : ORDER) {
            int count = counts.getOrDefault(s, 0);
            int pct = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);

            Color c = s == StockStatus.IN_STOCK ? Theme.CHART_IN_STOCK
                    : s == StockStatus.LOW_STOCK ? Theme.CHART_LOW_STOCK
                    : Theme.CHART_OUT_STOCK;
            g2.setColor(c);
            g2.fillOval(x, y + 6, 10, 10);

            g2.setColor(Theme.TEXT);
            g2.drawString(s.getLabel(), x + 18, y + 15);

            g2.setColor(Theme.TEXT_MUTED);
            String right = count + " · " + pct + "%";
            g2.drawString(right, x + 18 + fm.stringWidth(s.getLabel()) + 14,
                    y + 15);
            y += rowH;
        }
    }

    /** FontMetrics with the legend font applied. */
    private FontMetrics legendMetrics(Graphics2D g2) {
        g2.setFont(Theme.font(12));
        return g2.getFontMetrics();
    }

    /** Widest legend row in pixels. */
    private int legendWidth(FontMetrics fm) {
        int legendW = 0;
        for (StockStatus s : ORDER) {
            int count = counts.getOrDefault(s, 0);
            int pct = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
            int rowW = 18 + fm.stringWidth(s.getLabel()) + 14
                    + fm.stringWidth(count + " · " + pct + "%");
            legendW = Math.max(legendW, rowW);
        }
        return legendW;
    }

    /** Donut side length, shrinking until the legend fits beside it. */
    private int donutSizeFor(int w, int h, int legendW) {
        int donutSize = Math.min(h - 16, (int) (w * 0.52));
        while (donutSize > 90 && 8 + donutSize + 28 + legendW > w - 8) {
            donutSize -= 8;
        }
        return donutSize;
    }

    /** Segment under the mouse, or null. Mirrors paintComponent geometry. */
    private StockStatus statusAt(int mx, int my) {
        int w = getWidth();
        int h = getHeight();
        java.awt.image.BufferedImage tmp = new java.awt.image.BufferedImage(1, 1,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = tmp.createGraphics();
        FontMetrics fm = legendMetrics(g2);
        int legendW = legendWidth(fm);
        g2.dispose();
        int donutSize = donutSizeFor(w, h, legendW);
        int cx = 8 + donutSize / 2;
        int cy = h / 2;
        int outer = donutSize / 2 - 4;
        int ring = Math.max(14, outer / 5);
        int inner = outer - ring;
        double mid = (outer + inner) / 2.0;
        double dx = mx - cx, dy = my - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        // generous hit band around the ring
        if (dist < mid - ring / 2 - 4 || dist > mid + ring / 2 + 4 || total == 0) {
            return null;
        }
        double angle = Math.atan2(dy, dx);
        double start = -Math.PI / 2;
        double pos = angle < start ? angle + 2 * Math.PI : angle;
        for (StockStatus s : ORDER) {
            int c = counts.getOrDefault(s, 0);
            if (c == 0) {
                continue;
            }
            double extent = c / (double) total * 2 * Math.PI;
            if (pos >= start && pos < start + extent) {
                return s;
            }
            start += extent;
        }
        return null;
    }

    private void attachTooltip() {
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                StockStatus s = statusAt(e.getX(), e.getY());
                if (s == null) {
                    tip.setVisible(false);
                    return;
                }
                int c = counts.getOrDefault(s, 0);
                int pct = total == 0 ? 0 : (int) Math.round(c * 100.0 / total);
                tip.setText(s.getLabel() + " — " + c + " (" + pct + "%)");
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
}
