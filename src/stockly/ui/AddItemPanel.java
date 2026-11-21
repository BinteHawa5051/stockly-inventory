package stockly.ui;

import stockly.model.InventoryManager;
import stockly.model.Product;
import stockly.model.StockStatus;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Dedicated "Add New Item" page.
 * <p>
 * A polished inline form: product name, quantity and an optional stock
 * status override. By default the status combo is set to
 * {@code "Auto (from quantity)"} and the status is derived from the
 * quantity (0 → Out of Stock, 1–5 → Low Stock, 6+ → In Stock); picking a
 * specific status pins it manually. A live preview always shows the
 * status that will be saved. Validation mirrors
 * {@link InventoryManager} and {@link ProductDialog}.
 *
 * @author Tooba Akram and Abeeha Noor
 * @version 1.0
 */
public class AddItemPanel extends JPanel {

    private static final String[] STATUS_OPTIONS = {
            "Auto (from quantity)", "In Stock", "Low Stock", "Out of Stock"
    };

    private final InventoryManager manager;
    private final Runnable onSavedToInventory;
    private final Runnable onSavedToDashboard;

    private final PromptField nameField = new PromptField("e.g. Ceramic Mug");
    private final PromptField qtyField = new PromptField("e.g. 12");
    private final JComboBox<String> statusBox = new JComboBox<>(STATUS_OPTIONS);
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

        // center a readable-width column
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new javax.swing.BoxLayout(column, javax.swing.BoxLayout.Y_AXIS));

        column.add(buildHeader());
        column.add(javax.swing.Box.createVerticalStrut(18));
        column.add(buildForm());

        // fixed readable width, natural height
        column.setPreferredSize(new Dimension(560, column.getPreferredSize().height));
        column.setMaximumSize(new Dimension(560, Integer.MAX_VALUE));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        center.add(column, gbc);
        add(center, BorderLayout.CENTER);

        qtyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
        });
        statusBox.addActionListener(e -> updatePreview());
        updatePreview();

        nameField.addActionListener(e -> attemptSave());
        qtyField.addActionListener(e -> attemptSave());
    }

    // ------------------------------------------------------------------
    // Header
    // ------------------------------------------------------------------

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        header.setAlignmentX(LEFT_ALIGNMENT);

        JPanel mark = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(Theme.PEACH.getRed(), Theme.PEACH.getGreen(), Theme.PEACH.getBlue(), 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                Icon.draw(g2, Icon.ADD, 11, 11, getWidth() - 22, Theme.FOREST);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        mark.setPreferredSize(new Dimension(46, 46));
        mark.setOpaque(false);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Add New Product");
        title.setFont(Theme.bold(20));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = new JLabel("Create a stock record — status is automatic unless you pin it.");
        sub.setFont(Theme.font(12));
        sub.setForeground(Theme.TEXT_MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        text.add(title);
        text.add(javax.swing.Box.createVerticalStrut(2));
        text.add(sub);

        header.add(mark, BorderLayout.WEST);
        header.add(text, BorderLayout.CENTER);
        return header;
    }

    // ------------------------------------------------------------------
    // Form
    // ------------------------------------------------------------------

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(18, 22, 20, 22));
        form.setAlignmentX(LEFT_ALIGNMENT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 0, 7, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Product name
        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0;
        form.add(fieldLabel("Product Name"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        styleField(nameField);
        nameField.setPreferredSize(new Dimension(0, 40));
        form.add(nameField, gbc);

        // Quantity
        gbc.gridy = 1; gbc.gridx = 0; gbc.weightx = 0;
        form.add(fieldLabel("Quantity"), gbc);
        gbc.gridx = 1; gbc.weightx = 0;
        styleField(qtyField);
        qtyField.setPreferredSize(new Dimension(140, 40));
        form.add(qtyField, gbc);

        // Stock status
        gbc.gridy = 2; gbc.gridx = 0; gbc.weightx = 0;
        form.add(fieldLabel("Stock Status"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        styleCombo(statusBox);
        statusBox.setPreferredSize(new Dimension(280, 40));
        form.add(statusBox, gbc);

        // Live preview
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 1;
        JPanel previewRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        previewRow.setOpaque(false);
        JLabel previewCaption = new JLabel("Resulting status:");
        previewCaption.setFont(Theme.font(12));
        previewCaption.setForeground(Theme.TEXT_MUTED);
        statusPreview.setFont(Theme.bold(13));
        previewRow.add(previewCaption);
        previewRow.add(statusPreview);
        previewRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        form.add(previewRow, gbc);

        // Messages
        gbc.gridy = 4;
        errorLabel.setFont(Theme.font(12));
        errorLabel.setForeground(new Color(0xB0534A));
        form.add(errorLabel, gbc);

        gbc.gridy = 5;
        successLabel.setFont(Theme.font(12));
        successLabel.setForeground(Theme.CHART_IN_STOCK);
        form.add(successLabel, gbc);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        UIComponents.PrimaryButton save = new UIComponents.PrimaryButton("Save Product");
        save.addActionListener(e -> attemptSave());
        UIComponents.GhostButton clear = new UIComponents.GhostButton("Clear Form");
        clear.addActionListener(e -> clearForm());
        UIComponents.GhostButton viewInventory = new UIComponents.GhostButton("View Inventory");
        viewInventory.addActionListener(e -> onSavedToInventory.run());
        UIComponents.GhostButton viewDashboard = new UIComponents.GhostButton("View Dashboard");
        viewDashboard.addActionListener(e -> onSavedToDashboard.run());
        buttons.add(save);
        buttons.add(clear);
        buttons.add(viewInventory);
        buttons.add(viewDashboard);

        gbc.gridy = 6;
        form.add(buttons, gbc);
        return form;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

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
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setFont(Theme.font(13));
        combo.setForeground(Theme.TEXT);
        combo.setBackground(Theme.SURFACE);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
    }

    /** Shows the status that will actually be saved. */
    private void updatePreview() {
        String selected = (String) statusBox.getSelectedItem();
        if (selected != null && !selected.startsWith("Auto")) {
            for (StockStatus s : StockStatus.values()) {
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
            StockStatus s = new Product("tmp", "", q).getStockStatus();
            statusPreview.setText(s.getLabel() + " (auto)");
            statusPreview.setForeground(s.getColor());
        } catch (NumberFormatException e) {
            statusPreview.setText("—");
            statusPreview.setForeground(Theme.TEXT_MUTED);
        }
    }

    /** Maps the combo selection to a manual status, or {@code null} for auto. */
    private StockStatus selectedManualStatus() {
        String selected = (String) statusBox.getSelectedItem();
        if (selected == null || selected.startsWith("Auto")) {
            return null;
        }
        for (StockStatus s : StockStatus.values()) {
            if (s.getLabel().equals(selected)) {
                return s;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Save
    // ------------------------------------------------------------------

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
        if (name.length() > 60) {
            errorLabel.setText("Product name is too long (max 60 characters).");
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
            Product p = manager.addProduct(name, qty, selectedManualStatus());
            Dialogs.notice(getTopLevelAncestor() instanceof java.awt.Window w ? w : null,
                    "Product Saved",
                    "\"" + p.getName() + "\" has been saved as " + p.getId()
                            + " — " + p.getStockStatus().getLabel() + ".");
            clearForm();
        } catch (InventoryManager.InventoryException ex) {
            errorLabel.setText(ex.getMessage());
        }
    }

    /** Resets every field, message and the status preview. */
    private void clearForm() {
        nameField.setText("");
        qtyField.setText("");
        statusBox.setSelectedIndex(0);
        errorLabel.setText(" ");
        successLabel.setText(" ");
        updatePreview();
        nameField.requestFocusInWindow();
    }
}
