package stockly.ui;

import stockly.model.InventoryManager;
import stockly.model.Product;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Modal dialog for adding a new product or editing an existing one.
 * <p>
 * Fields: product name and quantity. Validation mirrors
 * {@link InventoryManager}: the name must not be blank and the
 * quantity must be a non-negative integer. Errors are shown inline in
 * a friendly message area. On success the dialog closes and the
 * manager has already persisted and broadcast the change.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class ProductDialog extends JDialog {

    private final InventoryManager manager;
    private final String editingId; // null → add mode
    private final PromptField nameField = new PromptField("e.g. Ceramic Mug");
    private final PromptField qtyField = new PromptField("e.g. 12");
    private final javax.swing.JComboBox<String> statusBox = new javax.swing.JComboBox<>(
            new String[]{"Auto (from quantity)", "In Stock", "Low Stock", "Out of Stock"});
    private final JLabel statusPreview = new JLabel(" ");
    private final JLabel errorLabel = new JLabel(" ");
    private boolean saved;

    /**
     * Creates the dialog.
     *
     * @param owner     parent frame (for modality and centering)
     * @param manager   shared inventory data source
     * @param editingId id of the product being edited, or {@code null} to add
     */
    public ProductDialog(java.awt.Window owner, InventoryManager manager, String editingId) {
        super(owner, editingId == null ? "Add New Product" : "Edit Product",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        this.manager = manager;
        this.editingId = editingId;

        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setResizable(false);

        // header strip
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(22, 28, 6, 28));
        JLabel heading = new JLabel(editingId == null ? "Add New Product" : "Edit Product");
        heading.setFont(Theme.bold(18));
        heading.setForeground(Theme.TEXT);
        header.add(heading, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // form body
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(10, 28, 10, 28));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        body.add(fieldLabel("Product Name"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        nameField.setPreferredSize(new Dimension(280, 36));
        styleField(nameField);
        body.add(nameField, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        body.add(fieldLabel("Quantity"), gbc);
        gbc.gridx = 1;
        qtyField.setPreferredSize(new Dimension(120, 36));
        styleField(qtyField);
        body.add(qtyField, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.weightx = 0;
        body.add(fieldLabel("Stock Status"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        styleCombo(statusBox);
        statusBox.addActionListener(e -> updatePreview());
        body.add(statusBox, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel previewRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        previewRow.setOpaque(false);
        JLabel previewCaption = new JLabel("Stock status:");
        previewCaption.setFont(Theme.font(12));
        previewCaption.setForeground(Theme.TEXT_MUTED);
        statusPreview.setFont(Theme.bold(12));
        previewRow.add(previewCaption);
        previewRow.add(statusPreview);
        body.add(previewRow, gbc);

        gbc.gridy = 4;
        errorLabel.setFont(Theme.font(12));
        errorLabel.setForeground(new Color(0xB0534A));
        errorLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        body.add(errorLabel, gbc);

        add(body, BorderLayout.CENTER);

        // footer buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 28, 20, 28));

        UIComponents.GhostButton cancel = new UIComponents.GhostButton("Cancel");
        cancel.addActionListener(e -> dispose());
        UIComponents.PrimaryButton save = new UIComponents.PrimaryButton("Save Product");
        save.addActionListener(e -> attemptSave());
        footer.add(cancel);
        footer.add(save);
        add(footer, BorderLayout.SOUTH);

        // prefill for edit mode
        if (editingId != null) {
            Product p = manager.findById(editingId);
            if (p != null) {
                nameField.setText(p.getName());
                qtyField.setText(String.valueOf(p.getQuantity()));
                if (p.getManualStatus() != null) {
                    statusBox.setSelectedItem(p.getManualStatus().getLabel());
                }
            } else {
                showError("This product no longer exists. It may have been deleted.");
                save.setEnabled(false);
            }
        }

        // live stock-status preview — status is always derived from quantity
        qtyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
        });
        updatePreview();

        // Enter key saves
        nameField.addActionListener(e -> attemptSave());
        qtyField.addActionListener(e -> attemptSave());

        pack();
        setLocationRelativeTo(owner);
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(12));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    private void styleField(JTextField f) {
        f.setFont(Theme.font(13));
        f.setForeground(Theme.TEXT);
        f.setCaretColor(Theme.FOREST);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    /** Shows the status that will actually be saved. */
    private void updatePreview() {
        String selected = (String) statusBox.getSelectedItem();
        if (selected != null && !selected.startsWith("Auto")) {
            for (stockly.model.StockStatus s : stockly.model.StockStatus.values()) {
                if (s.getLabel().equals(selected)) {
                    statusPreview.setText(s.getLabel());
                    statusPreview.setForeground(s.getColor());
                    return;
                }
            }
        }
        try {
            int q = Integer.parseInt(qtyField.getText().trim());
            if (q < 0) {
                statusPreview.setText("—");
                statusPreview.setForeground(Theme.TEXT_MUTED);
                return;
            }
            stockly.model.StockStatus s = new stockly.model.Product("tmp", "", q).getStockStatus();
            statusPreview.setText(s.getLabel() + " (auto)");
            statusPreview.setForeground(s.getColor());
        } catch (NumberFormatException e) {
            statusPreview.setText("—");
            statusPreview.setForeground(Theme.TEXT_MUTED);
        }
    }

    /** Maps the combo selection to a manual status, or {@code null} for auto. */
    private stockly.model.StockStatus selectedManualStatus() {
        String selected = (String) statusBox.getSelectedItem();
        if (selected == null || selected.startsWith("Auto")) {
            return null;
        }
        for (stockly.model.StockStatus s : stockly.model.StockStatus.values()) {
            if (s.getLabel().equals(selected)) {
                return s;
            }
        }
        return null;
    }

    private void styleCombo(javax.swing.JComboBox<String> combo) {
        combo.setFont(Theme.font(13));
        combo.setForeground(Theme.TEXT);
        combo.setBackground(Theme.SURFACE);
        combo.setPreferredSize(new Dimension(280, 36));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
    }

    private void clearError() {
        errorLabel.setText(" ");
    }

    /** Validates input and commits through the manager. */
    private void attemptSave() {
        clearError();
        String name = nameField.getText().trim();
        String qtyText = qtyField.getText().trim();

        if (name.isEmpty()) {
            showError("Product name cannot be empty.");
            nameField.requestFocusInWindow();
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(qtyText);
        } catch (NumberFormatException e) {
            showError("Quantity must be a whole number.");
            qtyField.requestFocusInWindow();
            return;
        }
        if (qty < 0) {
            showError("Quantity cannot be negative.");
            qtyField.requestFocusInWindow();
            return;
        }

        try {
            if (editingId == null) {
                manager.addProduct(name, qty, selectedManualStatus());
            } else {
                manager.updateProduct(editingId, name, qty, selectedManualStatus());
            }
            saved = true;
            String savedName = editingId == null ? name : manager.findById(editingId).getName();
            dispose();
            Dialogs.notice(getOwner(), "Product Saved",
                    "\"" + savedName + "\" has been saved to your inventory.");
        } catch (InventoryManager.InventoryException ex) {
            showError(ex.getMessage());
        }
    }

    /** @return {@code true} when the user saved successfully */
    public boolean wasSaved() {
        return saved;
    }

    /** Convenience: shows the dialog modally for add or edit. */
    public static void show(java.awt.Window owner, InventoryManager manager, String editingId) {
        ProductDialog d = new ProductDialog(owner, manager, editingId);
        d.setVisible(true);
    }
}
