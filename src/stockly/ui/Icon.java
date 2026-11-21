package stockly.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Lightweight vector icon set drawn with {@link Graphics2D}.
 * <p>
 * Each icon is rendered as strokes/fills at draw time, so icons stay
 * crisp at any size and can be tinted per state — no image assets or
 * emoji required. Call {@link #draw(Graphics2D, String, int, int, int, Color)}
 * with one of the supported names.
 *
 * @author Tooba Akram & Abeeha Noor
 * @version 1.0
 */
public final class Icon {

    private Icon() {
        // utility class
    }

    /** Supported icon names. */
    public static final String DASHBOARD = "dashboard";
    public static final String INVENTORY = "inventory";
    public static final String ADD = "add";
    public static final String ANALYTICS = "analytics";
    public static final String SETTINGS = "settings";
    public static final String SEARCH = "search";
    public static final String BELL = "bell";
    public static final String BOX = "box";
    public static final String WARN = "warn";
    public static final String TRASH = "trash";
    public static final String EDIT = "edit";
    public static final String CHECK = "check";
    public static final String CLOSE = "close";
    public static final String EYE = "eye";
    public static final String SORT = "sort";
    public static final String LOGO = "logo";

    /**
     * Draws the named icon centered in a {@code size}×{@code size} box.
     *
     * @param g     graphics context
     * @param name  one of the {@code *} name constants
     * @param x     left of the icon box
     * @param y     top of the icon box
     * @param size  icon box edge length
     * @param color stroke/fill color
     */
    public static void draw(Graphics2D g, String name, int x, int y, int size, Color color) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.translate(x, y);
        float s = size;
        float u = s / 24f; // design grid is 24 units
        gg.scale(u, u);
        gg.setColor(color);
        gg.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (name) {
            case DASHBOARD -> drawDashboard(gg);
            case INVENTORY -> drawInventory(gg);
            case ADD -> drawAdd(gg);
            case ANALYTICS -> drawAnalytics(gg);
            case SETTINGS -> drawSettings(gg);
            case SEARCH -> drawSearch(gg);
            case BELL -> drawBell(gg);
            case BOX -> drawBox(gg);
            case WARN -> drawWarn(gg);
            case TRASH -> drawTrash(gg);
            case EDIT -> drawEdit(gg);
            case CHECK -> drawCheck(gg);
            case CLOSE -> drawClose(gg);
            case EYE -> drawEye(gg);
            case SORT -> drawSort(gg);
            case LOGO -> drawLogo(gg);
            default -> { /* unknown icon: draw nothing */ }
        }
        gg.dispose();
    }

    private static void drawDashboard(Graphics2D g) {
        g.draw(new RoundRectangle2D.Float(3, 3, 8, 8, 2, 2));
        g.draw(new RoundRectangle2D.Float(13, 3, 8, 5, 2, 2));
        g.draw(new RoundRectangle2D.Float(3, 13, 8, 8, 2, 2));
        g.draw(new RoundRectangle2D.Float(13, 10, 8, 11, 2, 2));
    }

    private static void drawInventory(Graphics2D g) {
        // three stacked boxes
        for (int i = 0; i < 3; i++) {
            float y = 4 + i * 6;
            g.draw(new RoundRectangle2D.Float(3, y, 18, 4.4f, 1.6f, 1.6f));
        }
    }

    private static void drawAdd(Graphics2D g) {
        g.draw(new RoundRectangle2D.Float(4, 10.5f, 16, 3, 1.5f, 1.5f));
        g.draw(new Line2D.Float(12, 4, 12, 20));
    }

    private static void drawAnalytics(Graphics2D g) {
        g.draw(new Line2D.Float(4, 20, 4, 4));
        g.draw(new Line2D.Float(4, 20, 21, 20));
        float[] heights = {7, 12, 5, 15};
        for (int i = 0; i < 4; i++) {
            float x = 7 + i * 4.2f;
            g.fill(new RoundRectangle2D.Float(x, 20 - heights[i], 2.8f, heights[i], 1.4f, 1.4f));
        }
    }

    private static void drawSettings(Graphics2D g) {
        g.draw(new Ellipse2D.Float(8.4f, 8.4f, 7.2f, 7.2f));
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            float x1 = 12 + (float) Math.cos(a) * 5.6f;
            float y1 = 12 + (float) Math.sin(a) * 5.6f;
            float x2 = 12 + (float) Math.cos(a) * 8.6f;
            float y2 = 12 + (float) Math.sin(a) * 8.6f;
            g.draw(new Line2D.Float(x1, y1, x2, y2));
        }
    }

    private static void drawSearch(Graphics2D g) {
        g.draw(new Ellipse2D.Float(4.5f, 4.5f, 9.5f, 9.5f));
        g.draw(new Line2D.Float(12.3f, 12.3f, 19, 19));
    }

    private static void drawBell(Graphics2D g) {
        Path2D.Float p = new Path2D.Float();
        p.moveTo(12, 4);
        p.quadTo(6.5f, 4, 6.5f, 11);
        p.quadTo(6.5f, 15, 5, 17);
        p.lineTo(19, 17);
        p.quadTo(17.5f, 15, 17.5f, 11);
        p.quadTo(17.5f, 4, 12, 4);
        g.draw(p);
        g.draw(new Line2D.Float(10, 19.5f, 14, 19.5f));
    }

    private static void drawBox(Graphics2D g) {
        g.draw(new Line2D.Float(4, 8, 12, 4));
        g.draw(new Line2D.Float(12, 4, 20, 8));
        g.draw(new Line2D.Float(4, 8, 4, 16));
        g.draw(new Line2D.Float(20, 8, 20, 16));
        g.draw(new Line2D.Float(4, 16, 12, 20));
        g.draw(new Line2D.Float(12, 20, 20, 16));
    }

    private static void drawWarn(Graphics2D g) {
        Path2D.Float p = new Path2D.Float();
        p.moveTo(12, 4);
        p.lineTo(21, 20);
        p.lineTo(3, 20);
        p.closePath();
        g.draw(p);
        g.draw(new Line2D.Float(12, 10, 12, 14.5f));
        g.draw(new Line2D.Float(12, 17, 12.01f, 17));
    }

    private static void drawTrash(Graphics2D g) {
        g.draw(new Line2D.Float(5, 7, 19, 7));
        g.draw(new Line2D.Float(9, 7, 9, 5));
        g.draw(new Line2D.Float(15, 7, 15, 5));
        g.draw(new Line2D.Float(7, 7, 7, 20));
        g.draw(new Line2D.Float(17, 7, 17, 20));
        g.draw(new Line2D.Float(10, 11, 10, 16));
        g.draw(new Line2D.Float(14, 11, 14, 16));
    }

    private static void drawEdit(Graphics2D g) {
        Path2D.Float p = new Path2D.Float();
        p.moveTo(14.5f, 5.5f);
        p.lineTo(18.5f, 9.5f);
        p.lineTo(9, 19);
        p.lineTo(5, 19.5f);
        p.lineTo(5.5f, 15.5f);
        p.closePath();
        g.draw(p);
        g.draw(new Line2D.Float(13, 7, 17, 11));
    }

    private static void drawCheck(Graphics2D g) {
        g.draw(new Line2D.Float(5, 12.5f, 10, 17.5f));
        g.draw(new Line2D.Float(10, 17.5f, 19, 7));
    }

    private static void drawClose(Graphics2D g) {
        g.draw(new Line2D.Float(6, 6, 18, 18));
        g.draw(new Line2D.Float(18, 6, 6, 18));
    }

    private static void drawEye(Graphics2D g) {
        Path2D.Float p = new Path2D.Float();
        p.moveTo(3, 12);
        p.quadTo(7, 6.5f, 12, 6.5f);
        p.quadTo(17, 6.5f, 21, 12);
        p.quadTo(17, 17.5f, 12, 17.5f);
        p.quadTo(7, 17.5f, 3, 12);
        p.closePath();
        g.draw(p);
        g.draw(new Ellipse2D.Float(9.6f, 9.6f, 4.8f, 4.8f));
    }

    private static void drawSort(Graphics2D g) {
        g.draw(new Line2D.Float(8, 4, 8, 20));
        g.draw(new Line2D.Float(16, 4, 16, 20));
        Path2D.Float up = new Path2D.Float();
        up.moveTo(8, 4); up.lineTo(5, 9); up.lineTo(11, 9); up.closePath();
        Path2D.Float down = new Path2D.Float();
        down.moveTo(16, 20); down.lineTo(13, 15); down.lineTo(19, 15); down.closePath();
        g.fill(up);
        g.fill(down);
    }

    private static void drawLogo(Graphics2D g) {
        // stacked bars forming an abstract "S" mark
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(5, 8, 19, 8));
        g.draw(new Line2D.Float(5, 12, 15, 12));
        g.draw(new Line2D.Float(9, 16, 19, 16));
    }
}
