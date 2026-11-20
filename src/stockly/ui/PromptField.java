package stockly.ui;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Text field that shows a grey hint when empty.
 * <p>
 * The prompt disappears as soon as the user types or the field gains
 * focus with content; it reappears when the field is cleared.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class PromptField extends JTextField {

    private final String prompt;

    /**
     * Creates a prompt field.
     *
     * @param prompt hint text shown while the field is empty
     */
    public PromptField(String prompt) {
        super();
        this.prompt = prompt;
        setFont(Theme.font(13));
        setForeground(Theme.TEXT);
        setCaretColor(Theme.FOREST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = getInsets().left;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(prompt, x, y);
            g2.dispose();
        }
    }
}
