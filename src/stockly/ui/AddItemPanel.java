package stockly.ui;

import stockly.model.InventoryManager;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Dedicated "Add New Item" page — an inline version of the product
 * form so the sidebar destination does real work instead of opening a
 * dialog. Shares the same validation rules as {@link ProductDialog}.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class AddItemPanel extends JPanel {

    private final InventoryManager manager;
    private final Runnable onSavedToInventory;
    private final Runnable onSavedToDashboard;

    private final JTextField nameField = new JTextField();
    private final JTextField qtyField = new JTextField();
    private final JLabel statusPreview = new JLabel(" ");
    private final JLabel errorLabel = new JLabel(" ");
    private final JLabel successLabel = new JLabel(" ");

    /**
     * Creates the inline add-product form.
     *
     * @param manager             shared inventory data source
     * @param onSavedToInventory  callback to jump to the inventory page
     * @param onSavedToDashboard  callback to jump to the dashboard
     */
    public AddItemPanel(InventoryManager manager, Runnable onSavedToInventory, Runnable onSavedToDashboard) {
        this.manager = manager;
        this.onSavedToInventory = onSavedToInventory;
        this.onSavedToDashboard = onSavedToDashboard;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0;
        form.add(fieldLabel("Product Name"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        nameField.setPreferredSize(new Dimension(320, 38));
        styleField(nameField);
        form.add(nameField, gbc);

        gbc.gridy = 1; gbc.gridx = 0; gbc.weightx = 0;
        form.add(fieldLabel("Quantity"), gbc);
        gbc.gridx = 1; gbc.weightx = 0;
        qtyField.setPreferredSize(new Dimension(140, 38));
        styleField(qtyField);
        form.add(qtyField, gbc);

        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 1;
        JPanel previewRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        previewRow.setOpaque(false);
        JLabel previewCaption = new JLabel("Stock status:");
        previewCaption.setFont(Theme.font(12));
        previewCaption.setForeground(Theme.TEXT_MUTED);
        statusPreview.setFont(Theme.bold(12));
        previewRow.add(previewCaption);
        previewRow.add(statusPreview);
        previewRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        form.add(previewRow, gbc);

        gbc.gridy = 3;
        errorLabel.setFont(Theme.font(12));
        errorLabel.setForeground(new Color(0xB0534A));
        errorLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        form.add(errorLabel, gbc);

        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2;
        successLabel.setFont(Theme.font(12));
        successLabel.setForeground(Theme.CHART_IN_STOCK);
        form.add(successLabel, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        UIComponents.PrimaryButton save = new UIComponents.PrimaryButton("Save Product");
        save.addActionListener(e -> attemptSave());
        UIComponents.GhostButton viewInventory = new UIComponents.GhostButton("View Inventory");
        viewInventory.addActionListener(e -> onSavedToInventory.run());
        UIComponents.GhostButton viewDashboard = new UIComponents.GhostButton("View Dashboard");
        viewDashboard.addActionListener(e -> onSavedToDashboard.run());
        buttons.add(save);
        buttons.add(viewInventory);
        buttons.add(viewDashboard);

        gbc.gridy = 5; gbc.gridx = 0; gbc.gridwidth = 2;
        form.add(buttons, gbc);

        // live stock-status preview — status is always derived from quantity
        qtyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
        });
        updatePreview();

        add(form, BorderLayout.NORTH);
        nameField.addActionListener(e -> attemptSave());
        qtyField.addActionListener(e -> attemptSave());
    }

    /** Shows the status the current quantity will produce. */
    private void updatePreview() {
        try {
            int q = Integer.parseInt(qtyField.getText().trim());
            if (q < 0) {
                statusPreview.setText("—");
                statusPreview.setForeground(Theme.TEXT_MUTED);
                return;
            }
            stockly.model.StockStatus s = new stockly.model.Product("tmp", "", q).getStockStatus();
            statusPreview.setText(s.getLabel());
            statusPreview.setForeground(s.getColor());
        } catch (NumberFormatException e) {
            statusPreview.setText("—");
            statusPreview.setForeground(Theme.TEXT_MUTED);
        }
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

    private void attemptSave() {
        errorLabel.setText(" ");
        successLabel.setText(" ");
        String name = nameField.getText().trim();
        String qtyText = qtyField.getText().trim();

        if (name.isEmpty()) {
            errorLabel.setText("Product name cannot be empty.");
            nameField.requestFocusInWindow();
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(qtyText);
        } catch (NumberFormatException e) {
            errorLabel.setText("Quantity must be a whole number.");
            qtyField.requestFocusInWindow();
            return;
        }
        if (qty < 0) {
            errorLabel.setText("Quantity cannot be negative.");
            qtyField.requestFocusInWindow();
            return;
        }

        try {
            stockly.model.Product p = manager.addProduct(name, qty);
            successLabel.setText("Saved \"" + p.getName() + "\" as " + p.getId()
                    + " — " + p.getStockStatus().getLabel() + ".");
            nameField.setText("");
            qtyField.setText("");
            nameField.requestFocusInWindow();
        } catch (InventoryManager.InventoryException ex) {
            errorLabel.setText(ex.getMessage());
        }
    }
}
