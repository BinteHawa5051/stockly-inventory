package stockly.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;

/**
 * Styled modal dialogs matching the STOCKLY visual identity.
 * <p>
 * Replaces default {@code JOptionPane} chrome with rounded cards,
 * vector icons, consistent typography and themed buttons.
 *
 * @author Tooba Akram & Abeeha Noor
 * @version 1.0
 */
public final class Dialogs {

    private Dialogs() {
    }

    /**
     * Confirmation dialog with a warning icon and Yes / No buttons.
     *
     * @return {@code true} when the user confirms
     */
    public static boolean confirm(Window owner, String title, String message) {
        return confirm(owner, title, message, Icon.WARN, Theme.CHART_LOW_STOCK);
    }

    /**
     * Confirmation dialog with a custom icon tint.
     *
     * @return {@code true} when the user confirms
     */
    public static boolean confirm(Window owner, String title, String message,
                                  String icon, Color tint) {
        JPanel content = body(icon, tint, message);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        UIComponents.GhostButton no = new UIComponents.GhostButton("Cancel");
        UIComponents.PrimaryButton yes = new UIComponents.PrimaryButton("Confirm", Theme.CHART_OUT_STOCK);
        buttons.add(no);
        buttons.add(yes);
        content.add(buttons, BorderLayout.SOUTH);
        content.setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));

        JDialog[] holder = new JDialog[1];
        no.addActionListener(e -> holder[0].dispose());
        final boolean[] result = {false};
        yes.addActionListener(e -> { result[0] = true; holder[0].dispose(); });

        holder[0] = styledDialog(owner, title, content);
        holder[0].setVisible(true);
        return result[0];
    }

    /**
     * Information / success notification with a single OK button.
     */
    public static void notice(Window owner, String title, String message) {
        notice(owner, title, message, Icon.CHECK, Theme.CHART_IN_STOCK);
    }

    /**
     * Information dialog with a custom icon tint.
     */
    public static void notice(Window owner, String title, String message,
                              String icon, Color tint) {
        JPanel content = body(icon, tint, message);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        UIComponents.PrimaryButton ok = new UIComponents.PrimaryButton("OK");
        buttons.add(ok);
        content.add(buttons, BorderLayout.SOUTH);
        content.setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));

        JDialog[] holder = new JDialog[1];
        ok.addActionListener(e -> holder[0].dispose());
        holder[0] = styledDialog(owner, title, content);
        holder[0].setVisible(true);
    }

    /**
     * Error notification (rose icon).
     *
     * @param owner   parent window for modality and centering
     * @param title   dialog title
     * @param message body text shown to the user
     */
    public static void error(Window owner, String title, String message) {
        notice(owner, title, message, Icon.CLOSE, Theme.CHART_OUT_STOCK);
    }

    // ------------------------------------------------------------------

    private static JPanel body(String icon, Color tint, String message) {
        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setOpaque(false);
        content.setBackground(Theme.BG);

        IconBadge badge = new IconBadge(icon, tint);
        badge.setPreferredSize(new Dimension(44, 44));
        content.add(badge, BorderLayout.WEST);

        JLabel label = new JLabel("<html><div style='width:340px'>" + escape(message) + "</div></html>");
        label.setFont(Theme.font(13));
        label.setForeground(Theme.TEXT);
        label.setVerticalAlignment(SwingConstants.TOP);
        JPanel textWrap = new JPanel(new BorderLayout());
        textWrap.setOpaque(false);
        textWrap.add(label, BorderLayout.NORTH);
        content.add(textWrap, BorderLayout.CENTER);
        return content;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    private static JDialog styledDialog(Window owner, String title, JPanel content) {
        JDialog dialog = new JDialog(owner, title,
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        return dialog;
    }

    /** Round tinted icon badge used inside dialogs. */
    private static class IconBadge extends JPanel {
        private final String icon;
        private final Color tint;

        IconBadge(String icon, Color tint) {
            this.icon = icon;
            this.tint = tint;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 26));
            g2.fillOval(0, 0, getWidth(), getHeight());
            Icon.draw(g2, icon, 12, 12, getWidth() - 24, tint);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
