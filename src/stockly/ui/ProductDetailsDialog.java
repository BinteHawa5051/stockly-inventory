package stockly.ui;

import stockly.model.InventoryManager;
import stockly.model.Product;
import stockly.model.StockStatus;

import javax.swing.BorderFactory;
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

/**
 * Read-only product details dialog.
 * <p>
 * Opened from the inventory table's view action. Shows every stored
 * attribute of a product plus buttons to edit or close.
 *
 * @author Tooba Akram and Abeeha Noor
 * @version 1.0
 */
public class ProductDetailsDialog extends javax.swing.JDialog {

    private ProductDetailsDialog(java.awt.Window owner, InventoryManager manager, String id) {
        super(owner, "Product Details",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        Product p = manager.findById(id);
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setResizable(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 30, 8, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 0, 7, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        if (p == null) {
            JLabel missing = new JLabel("This product no longer exists.");
            missing.setFont(Theme.font(13));
            missing.setForeground(Theme.TEXT_MUTED);
            gbc.gridx = 0; gbc.gridy = 0;
            content.add(missing, gbc);
        } else {
            addRow(content, gbc, 0, "Product ID", p.getId());
            addRow(content, gbc, 1, "Product Name", p.getName());
            addRow(content, gbc, 2, "Quantity", String.valueOf(p.getQuantity()));
            addRow(content, gbc, 3, "Stock Status", p.getStockStatus().getLabel());
            String mode = p.getManualStatus() == null
                    ? "Auto (from quantity)" : "Manual: " + p.getManualStatus().getLabel();
            addRow(content, gbc, 4, "Status Mode", mode);
        }

        // footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        UIComponents.GhostButton close = new UIComponents.GhostButton("Close");
        close.addActionListener(e -> dispose());
        footer.add(close);
        if (p != null) {
            UIComponents.PrimaryButton edit = new UIComponents.PrimaryButton("Edit Product");
            edit.addActionListener(e -> {
                dispose();
                ProductDialog.show(owner, manager, id);
            });
            footer.add(edit);
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(content, BorderLayout.CENTER);
        wrapper.add(footer, BorderLayout.SOUTH);
        setContentPane(wrapper);
        pack();
        setLocationRelativeTo(owner);
    }

    private void addRow(JPanel parent, GridBagConstraints gbc, int row,
                        String key, String value) {
        gbc.gridy = row * 2;
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel k = new JLabel(key);
        k.setFont(Theme.bold(12));
        k.setForeground(Theme.TEXT_MUTED);
        parent.add(k, gbc);

        gbc.gridy = row * 2 + 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        JLabel v = new JLabel(value);
        v.setFont(Theme.font(14));
        v.setForeground(Theme.TEXT);
        parent.add(v, gbc);
        gbc.gridwidth = 1;
    }

    /** Convenience: shows the dialog modally. */
    public static void show(java.awt.Window owner, InventoryManager manager, String id) {
        new ProductDetailsDialog(owner, manager, id).setVisible(true);
    }
}
