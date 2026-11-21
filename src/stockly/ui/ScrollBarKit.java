package stockly.ui;

import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Dimension;

/**
 * Scroll-pane factory producing STOCKLY-styled thin scrollbars.
 * <p>
 * Scrollbars only appear when content overflows and are rendered as a
 * slim sage thumb on a transparent track, matching the app palette.
 *
 * @author Tooba Akram & Abeeha Noor
 * @version 1.0
 */
public final class ScrollBarKit {

    private ScrollBarKit() {
    }

    /**
     * Wraps a component in a JScrollPane with thin themed scrollbars.
     *
     * @param view the component to scroll
     * @return configured scroll pane
     */
    public static JScrollPane wrap(java.awt.Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        style(scroll.getVerticalScrollBar());
        style(scroll.getHorizontalScrollBar());
        return scroll;
    }

    private static void style(javax.swing.JScrollBar bar) {
        bar.setUI(new BasicScrollBarUI() {
            @Override
            public Dimension getPreferredSize(javax.swing.JComponent c) {
                Dimension d = super.getPreferredSize(c);
                return new Dimension(8, d.height);
            }

            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = Theme.SAGE;
                this.trackColor = new java.awt.Color(0, 0, 0, 0);
                this.thumbDarkShadowColor = Theme.SAGE;
                this.thumbHighlightColor = Theme.SAGE;
            }
        });
        bar.setBorder(null);
    }
}
