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
import java.util.Map;

/**
 * Donut chart showing how many products fall into each stock status.
 * <p>
 * The chart is drawn entirely with {@link Graphics2D}: three arcs in
 * the Stockly palette plus a centered total. A legend with counts and
 * percentages is rendered beside the ring when space allows.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class DonutChart extends JPanel {

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

    /** Segment lookup used by both painting and hit-testing. */
    private StockStatus statusAt(int mx, int my) {
        int w = getWidth();
        int h = getHeight();
        int donutSize = Math.min(h - 24, w / 2);
        int cx = 12 + donutSize / 2;
        int cy = h / 2;
        int outer = donutSize / 2 - 6;
        int inner = (int) (outer * 0.62);
        double dx = mx - cx, dy = my - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < inner || dist > outer || total == 0) {
            return null;
        }
        double angle = Math.atan2(dy, dx); // 0 = east, positive = south (y down)
        double start = -Math.PI / 2;
        double pos = angle < start ? angle + 2 * Math.PI : angle;
        double acc = start;
        for (StockStatus s : StockStatus.values()) {
            int c = counts.getOrDefault(s, 0);
            if (c == 0) continue;
            double extent = c / (double) total * 2 * Math.PI;
            acc += extent;
            if (pos >= acc - extent && pos < acc) {
                return s;
            }
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
        return new Dimension(320, 220);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Layout: donut on the left, legend on the right.
        int donutSize = Math.min(h - 24, w / 2);
        int cx = 12 + donutSize / 2;
        int cy = h / 2;
        int outer = donutSize / 2 - 6;
        int inner = (int) (outer * 0.62);

        if (total == 0) {
            drawEmpty(g2, cx, cy, outer, inner);
        } else {
            drawRing(g2, cx, cy, outer, inner);
            drawCenter(g2, cx, cy);
        }
        drawLegend(g2, w / 2 + 8, cy, total);
        g2.dispose();
    }

    private void drawRing(Graphics2D g2, int cx, int cy, int outer, int inner) {
        double start = -Math.PI / 2; // 12 o'clock
        for (StockStatus status : StockStatus.values()) {
            int count = counts.getOrDefault(status, 0);
            if (count == 0) {
                continue;
            }
            double extent = (count / (double) total) * 2 * Math.PI;
            g2.setColor(status.getColor());
            g2.setStroke(new BasicStroke(outer - inner, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g2.draw(new Arc2D.Double(cx - outer, cy - outer, outer * 2, outer * 2,
                    Math.toDegrees(start), Math.toDegrees(-extent), Arc2D.OPEN));
            start += extent;
        }
        // soft inner hole edge
        g2.setColor(Theme.SURFACE);
        g2.fillOval(cx - inner, cy - inner, inner * 2, inner * 2);
    }

    private void drawCenter(Graphics2D g2, int cx, int cy) {
        g2.setColor(Theme.TEXT);
        g2.setFont(Theme.bold(22));
        String value = String.valueOf(total);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(value, cx - fm.stringWidth(value) / 2, cy + 8);
        g2.setColor(Theme.TEXT_MUTED);
        g2.setFont(Theme.font(11));
        String label = total == 1 ? "product" : "products";
        fm = g2.getFontMetrics();
        g2.drawString(label, cx - fm.stringWidth(label) / 2, cy + 26);
    }

    private void drawEmpty(Graphics2D g2, int cx, int cy, int outer, int inner) {
        g2.setColor(Theme.BORDER);
        g2.setStroke(new BasicStroke(outer - inner));
        g2.draw(new Arc2D.Double(cx - outer, cy - outer, outer * 2, outer * 2, 0, 360, Arc2D.OPEN));
        g2.setColor(Theme.SURFACE);
        g2.fillOval(cx - inner, cy - inner, inner * 2, inner * 2);
        g2.setColor(Theme.TEXT_MUTED);
        g2.setFont(Theme.font(12));
        String msg = "No products yet";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, cx - fm.stringWidth(msg) / 2, cy - inner / 2);
    }

    private void drawLegend(Graphics2D g2, int x, int cy, int total) {
        int rowH = 26;
        int y = cy - (rowH * 3) / 2;
        g2.setFont(Theme.font(12));
        for (StockStatus status : StockStatus.values()) {
            int count = counts.getOrDefault(status, 0);
            int pct = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);

            g2.setColor(status.getColor());
            g2.fillOval(x, y + 5, 9, 9);

            g2.setColor(Theme.TEXT);
            g2.drawString(status.getLabel(), x + 16, y + 14);

            g2.setColor(Theme.TEXT_MUTED);
            String right = count + " · " + pct + "%";
            g2.drawString(right, x + 16 + g2.getFontMetrics().stringWidth(status.getLabel()) + 12, y + 14);
            y += rowH;
        }
    }
}
