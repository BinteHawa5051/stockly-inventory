package stockly.ui.charts;

import stockly.model.StockStatus;
import stockly.ui.Theme;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Donut chart showing how many products fall into each stock status.
 * <p>
 * Layout: a fixed 140×140 px ring panel on the left and a real legend
 * component on the right, separated by a 32 px gap and both vertically
 * centred. The ring is stroked with an 18 px band, segments drawn
 * clockwise from 12 o'clock in the order In Stock → Low Stock →
 * Out of Stock with small gaps. The total product count is drawn from
 * the exact centre point inside the ring hole. Hovering a segment
 * shows a tooltip.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class DonutChart extends JPanel {

    /** Legend/segment order — must match the drawing order. */
    private static final List<StockStatus> ORDER = List.of(
            StockStatus.IN_STOCK, StockStatus.LOW_STOCK, StockStatus.OUT_OF_STOCK);

    /** Ring outer diameter in pixels (fixed preferred size). */
    private static final int RING_DIAMETER = 140;
    /** Ring stroke thickness in pixels. */
    private static final int RING_STROKE = 18;
    /** Horizontal gap between ring and legend. */
    private static final int LEGEND_GAP = 32;
    /** Angular gap between segments. */
    private static final double GAP = Math.toRadians(3);

    private final Map<StockStatus, Integer> counts = new EnumMap<>(StockStatus.class);
    private int total;
    private final RingPanel ring = new RingPanel();
    private final LegendPanel legend = new LegendPanel();

    /** Creates an empty chart; call {@link #setData(Map)} to populate. */
    public DonutChart() {
        setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, BoxLayout.X_AXIS));
        add(ring);
        add(Box.createHorizontalStrut(LEGEND_GAP));
        add(legend);
        add(Box.createHorizontalGlue());
        attachTooltip();
    }

    /**
     * Replaces the chart data and repaints both parts.
     *
     * @param data product counts keyed by stock status
     */
    public void setData(Map<StockStatus, Integer> data) {
        counts.clear();
        counts.putAll(data);
        total = counts.values().stream().mapToInt(Integer::intValue).sum();
        ring.repaint();
        legend.rebuild();
    }

    @Override
    public Dimension getPreferredSize() {
        // ring 140 + gap 32 + legend ~120 wide, min height keeps card ~220
        return new Dimension(320, 200);
    }

    // ------------------------------------------------------------------
    // Segment geometry shared by painting and hit-testing
    // ------------------------------------------------------------------

    private int countOf(StockStatus s) {
        return counts.getOrDefault(s, 0);
    }

    private int pctOf(StockStatus s) {
        return total == 0 ? 0 : (int) Math.round(countOf(s) * 100.0 / total);
    }

    private static Color colorOf(StockStatus s) {
        return switch (s) {
            case IN_STOCK -> Theme.CHART_IN_STOCK;
            case LOW_STOCK -> Theme.CHART_LOW_STOCK;
            case OUT_OF_STOCK -> Theme.CHART_OUT_STOCK;
        };
    }

    // ------------------------------------------------------------------
    // Ring panel — fixed 140x140, draws arcs + centre label
    // ------------------------------------------------------------------

    /** The donut itself: three stroked arcs + centred total label. */
    private class RingPanel extends JPanel {
        RingPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(RING_DIAMETER, RING_DIAMETER));
            setMaximumSize(new Dimension(RING_DIAMETER, RING_DIAMETER));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight());
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            double outer = size / 2.0 - 2;
            double inner = outer - RING_STROKE;
            double mid = (outer + inner) / 2.0;

            if (total == 0) {
                // empty ring
                g2.setColor(Theme.BORDER);
                g2.setStroke(new BasicStroke(RING_STROKE, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER));
                g2.draw(new Arc2D.Double(cx - mid, cy - mid, mid * 2, mid * 2,
                        0, 360, Arc2D.OPEN));
                drawCentreText(g2, cx, cy, "0", "products");
            } else {
                drawArcs(g2, cx, cy, mid);
                drawCentreText(g2, cx, cy, String.valueOf(total),
                        total == 1 ? "product" : "products");
            }
            g2.dispose();
        }

        private void drawArcs(Graphics2D g2, int cx, int cy, double mid) {
            double start = -Math.PI / 2; // 12 o'clock
            for (StockStatus s : ORDER) {
                int c = countOf(s);
                if (c == 0) {
                    continue;
                }
                double extent = c / (double) total * 2 * Math.PI;
                double a0 = Math.toDegrees(start + GAP / 2);
                double sweep = Math.toDegrees(extent - GAP);
                if (sweep <= 0) {
                    sweep = 0.5;
                }
                g2.setColor(colorOf(s));
                g2.setStroke(new BasicStroke(RING_STROKE, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER));
                g2.draw(new Arc2D.Double(cx - mid, cy - mid, mid * 2, mid * 2,
                        a0, sweep, Arc2D.OPEN));
                start += extent;
            }
        }

        /** Centre label drawn from the exact centre point, inside the hole. */
        private void drawCentreText(Graphics2D g2, int cx, int cy,
                                    String value, String label) {
            // clear the hole so text never touches the ring
            double inner = ringSize() / 2.0 - 2 - RING_STROKE;
            g2.setColor(Theme.SURFACE);
            g2.fillOval((int) (cx - inner), (int) (cy - inner),
                    (int) (inner * 2), (int) (inner * 2));

            g2.setColor(Theme.TEXT);
            g2.setFont(Theme.bold(20));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(value, cx - fm.stringWidth(value) / 2, cy - 2);

            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(Theme.font(11));
            fm = g2.getFontMetrics();
            g2.drawString(label, cx - fm.stringWidth(label) / 2, cy + 15);
        }

        private int ringSize() {
            return Math.min(getWidth(), getHeight());
        }

        /** Status segment under a point (ring-local coordinates). */
        StockStatus statusAt(int mx, int my) {
            int size = ringSize();
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            double outer = size / 2.0 - 2;
            double inner = outer - RING_STROKE;
            double mid = (outer + inner) / 2.0;
            double dx = mx - cx, dy = my - cy;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < mid - RING_STROKE / 2.0 - 4
                    || dist > mid + RING_STROKE / 2.0 + 4 || total == 0) {
                return null;
            }
            double angle = Math.atan2(dy, dx);
            double start = -Math.PI / 2;
            double pos = angle < start ? angle + 2 * Math.PI : angle;
            for (StockStatus s : ORDER) {
                int c = countOf(s);
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
    }

    // ------------------------------------------------------------------
    // Legend panel — three real rows, vertically centred
    // ------------------------------------------------------------------

    /** Legend with one row per status: dot, label, count · percent. */
    private class LegendPanel extends JPanel {
        LegendPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        void rebuild() {
            removeAll();
            add(Box.createVerticalGlue());
            for (StockStatus s : ORDER) {
                add(new LegendRow(s));
                add(Box.createVerticalStrut(6));
            }
            add(Box.createVerticalGlue());
            revalidate();
            repaint();
        }

        /** One legend row: colour dot + status label + "n · pct%". */
        private class LegendRow extends JPanel {
            LegendRow(StockStatus s) {
                setOpaque(false);
                setLayout(new javax.swing.BoxLayout(this, BoxLayout.X_AXIS));
                setAlignmentX(LEFT_ALIGNMENT);
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

                JPanel dot = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(colorOf(s));
                        g2.fillOval(1, 1, 10, 10);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                dot.setPreferredSize(new Dimension(12, 12));
                dot.setMaximumSize(new Dimension(12, 12));
                dot.setOpaque(false);
                dot.setAlignmentY(CENTER_ALIGNMENT);

                JLabel label = new JLabel(s.getLabel());
                label.setFont(Theme.font(12));
                label.setForeground(Theme.TEXT);
                label.setAlignmentY(CENTER_ALIGNMENT);

                JLabel right = new JLabel(countOf(s) + " · " + pctOf(s) + "%");
                right.setFont(Theme.font(11));
                right.setForeground(Theme.TEXT_MUTED);
                right.setHorizontalAlignment(SwingConstants.RIGHT);
                right.setAlignmentY(CENTER_ALIGNMENT);

                add(dot);
                add(Box.createHorizontalStrut(8));
                add(label);
                add(Box.createHorizontalStrut(14));
                add(right);
                add(Box.createHorizontalGlue());
            }
        }
    }

    // ------------------------------------------------------------------
    // Tooltip
    // ------------------------------------------------------------------

    private final JLabel tip = new JLabel();

    private void attachTooltip() {
        tip.setFont(Theme.font(11));
        tip.setForeground(Theme.SURFACE);
        tip.setBackground(new Color(0x30332F));
        tip.setOpaque(true);
        tip.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
        tip.setVisible(false);
        // overlay label on the ring panel (null layout = manual positioning)
        ring.setLayout(null);
        ring.add(tip);

        ring.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                StockStatus s = ring.statusAt(e.getX(), e.getY());
                if (s == null) {
                    tip.setVisible(false);
                    return;
                }
                tip.setText(s.getLabel() + " — " + countOf(s) + " (" + pctOf(s) + "%)");
                tip.setSize(tip.getPreferredSize());
                int tx = Math.min(e.getX() + 12, ring.getWidth() - tip.getWidth() - 4);
                int ty = Math.max(4, e.getY() - tip.getHeight() - 8);
                tip.setLocation(tx, ty);
                tip.setVisible(true);
            }
        });
        ring.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { tip.setVisible(false); }
        });
    }
}
