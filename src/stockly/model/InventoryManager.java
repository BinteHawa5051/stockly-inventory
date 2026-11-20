package stockly.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central data-access object for the inventory.
 * <p>
 * This class owns the product list and is the single place where
 * inventory rules live: validation, id generation, stock-status
 * classification (via {@link Product#getStockStatus()}), CSV
 * persistence and change notification.
 * <p>
 * The UI never manipulates the product list directly — it calls these
 * methods and re-reads state through the getters, so every screen stays
 * consistent after any change.
 *
 * @author Stockly Team
 * @version 1.0
 */
public class InventoryManager {

    /** Raised when validation fails or an id cannot be resolved. */
    public static class InventoryException extends Exception {
        /** @param message user friendly description of the problem */
        public InventoryException(String message) {
            super(message);
        }
    }

    /** Callback invoked after every successful mutation. */
    public interface InventoryListener {
        /** Called whenever the inventory content changes. */
        void onInventoryChanged();
    }

    private final List<Product> products = new ArrayList<>();
    private final List<InventoryListener> listeners = new ArrayList<>();
    private final ActivityLog activityLog = new ActivityLog();
    private final Path storageFile;

    /**
     * Creates a manager backed by {@code inventory.csv} in the working
     * directory, loads any existing data, and seeds demo products when
     * the file does not exist yet.
     */
    public InventoryManager() {
        this(Paths.get("inventory.csv"));
    }

    /**
     * Creates a manager backed by the given CSV file.
     *
     * @param storageFile path of the CSV persistence file
     */
    public InventoryManager(Path storageFile) {
        this.storageFile = storageFile;
        boolean fileExists = Files.exists(storageFile);
        load();
        // Seed demo data only on the very first run — an existing file
        // (even an empty one) is respected as the user's real inventory.
        if (!fileExists) {
            seedDemoData();
            save();
        }
    }

    // ------------------------------------------------------------------
    // Listeners / activity
    // ------------------------------------------------------------------

    /**
     * Registers a listener notified after each mutation.
     *
     * @param listener the listener to add
     */
    public void addListener(InventoryListener listener) {
        listeners.add(listener);
    }

    /**
     * Returns the session activity log.
     *
     * @return the shared {@link ActivityLog}
     */
    public ActivityLog getActivityLog() {
        return activityLog;
    }

    private void notifyChanged() {
        for (InventoryListener l : listeners) {
            l.onInventoryChanged();
        }
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    /**
     * Returns all products in insertion order.
     *
     * @return unmodifiable view of the product list
     */
    public List<Product> getAllProducts() {
        return Collections.unmodifiableList(products);
    }

    /**
     * Finds a product by id.
     *
     * @param id the product id to look up
     * @return the product, or {@code null} when not found
     */
    public Product findById(String id) {
        for (Product p : products) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** @return number of products in the inventory */
    public int productCount() {
        return products.size();
    }

    /** @return total stock quantity across all products */
    public int totalUnits() {
        int total = 0;
        for (Product p : products) {
            total += p.getQuantity();
        }
        return total;
    }

    /**
     * Counts products in the given stock status.
     *
     * @param status the status to count
     * @return number of matching products
     */
    public int countByStatus(StockStatus status) {
        int count = 0;
        for (Product p : products) {
            if (p.getStockStatus() == status) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------
    // Mutations
    // ------------------------------------------------------------------

    /**
     * Validates product fields shared by add and update.
     *
     * @param name     product name
     * @param quantity stock quantity
     * @throws InventoryException when the name is blank or quantity negative
     */
    private static void validate(String name, int quantity) throws InventoryException {
        if (name == null || name.trim().isEmpty()) {
            throw new InventoryException("Product name cannot be empty.");
        }
        if (quantity < 0) {
            throw new InventoryException("Quantity cannot be negative.");
        }
    }

    /**
     * Adds a new product with a generated id.
     *
     * @param name     product name
     * @param quantity stock quantity
     * @return the created product
     * @throws InventoryException when validation fails
     */
    public Product addProduct(String name, int quantity) throws InventoryException {
        return addProduct(name, quantity, null);
    }

    /**
     * Adds a new product with a generated id and optional manual status.
     *
     * @param name         product name
     * @param quantity     stock quantity
     * @param manualStatus status to pin, or {@code null} for auto classification
     * @return the created product
     * @throws InventoryException when validation fails
     */
    public Product addProduct(String name, int quantity, StockStatus manualStatus)
            throws InventoryException {
        validate(name, quantity);
        Product product = new Product(nextId(), name.trim(), quantity);
        product.setManualStatus(manualStatus);
        products.add(product);
        activityLog.record(ActivityEntry.Kind.ADDED,
                "Added \"" + product.getName() + "\" (" + product.getQuantity() + " units)");
        save();
        notifyChanged();
        return product;
    }

    /**
     * Updates an existing product's name and quantity.
     *
     * @param id       id of the product to update
     * @param name     new product name
     * @param quantity new stock quantity
     * @throws InventoryException when validation fails or the id is unknown
     */
    public void updateProduct(String id, String name, int quantity) throws InventoryException {
        // name/quantity-only update: preserve the existing status mode
        Product existing = findById(id);
        updateProduct(id, name, quantity, existing == null ? null : existing.getManualStatus(),
                true);
    }

    /**
     * Updates an existing product including its status mode.
     *
     * @param id           id of the product to update
     * @param name         new product name
     * @param quantity     new stock quantity
     * @param manualStatus status to pin, or {@code null} for auto classification
     * @throws InventoryException when validation fails or the id is unknown
     */
    public void updateProduct(String id, String name, int quantity, StockStatus manualStatus)
            throws InventoryException {
        updateProduct(id, name, quantity, manualStatus, false);
    }

    private void updateProduct(String id, String name, int quantity,
                               StockStatus manualStatus, boolean keepStatus)
            throws InventoryException {
        validate(name, quantity);
        Product product = findById(id);
        if (product == null) {
            throw new InventoryException("Product " + id + " no longer exists.");
        }
        String oldDesc = product.getName() + " (" + product.getQuantity() + " units)";
        product.setName(name.trim());
        product.setQuantity(quantity);
        if (!keepStatus) {
            product.setManualStatus(manualStatus);
        }
        activityLog.record(ActivityEntry.Kind.UPDATED,
                "Updated \"" + product.getName() + "\" — was " + oldDesc);
        save();
        notifyChanged();
    }

    /**
     * Deletes a product.
     *
     * @param id id of the product to delete
     * @throws InventoryException when the id is unknown
     */
    public void deleteProduct(String id) throws InventoryException {
        Product product = findById(id);
        if (product == null) {
            throw new InventoryException("Product " + id + " no longer exists.");
        }
        products.remove(product);
        activityLog.record(ActivityEntry.Kind.DELETED,
                "Deleted \"" + product.getName() + "\"");
        save();
        notifyChanged();
    }

    /** Generates the next sequential id, e.g. {@code P007}. */
    private String nextId() {
        int max = 0;
        for (Product p : products) {
            String id = p.getId();
            if (id != null && id.startsWith("P")) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(1)));
                } catch (NumberFormatException ignored) {
                    // non-numeric ids are simply skipped
                }
            }
        }
        return String.format("P%03d", max + 1);
    }

    // ------------------------------------------------------------------
    // Persistence (simple CSV: id,name,quantity,status)
    // ------------------------------------------------------------------

    /**
     * Loads products from the CSV file. A malformed line is skipped
     * individually so one bad row never wipes out the whole inventory;
     * the 4th column (manual status) is optional for backward
     * compatibility with older files.
     */
    private void load() {
        if (!Files.exists(storageFile)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(storageFile, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length >= 3) {
                        Product p = new Product(parts[0], parts[1],
                                Integer.parseInt(parts[2].trim()));
                        if (parts.length >= 4 && !parts[3].isBlank()) {
                            try {
                                p.setManualStatus(StockStatus.valueOf(parts[3].trim()));
                            } catch (IllegalArgumentException badStatus) {
                                // unknown status value — keep product, use auto mode
                            }
                        }
                        products.add(p);
                    }
                } catch (IllegalArgumentException e) {
                    // skip this row only — keep loading the rest
                    System.err.println("Skipping malformed inventory row: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load inventory file: " + e.getMessage());
        }
    }

    /**
     * Minimal CSV line parser that understands double-quoted fields
     * with {@code ""} escapes, so product names containing commas or
     * quotes round-trip correctly.
     *
     * @param line a single CSV record
     * @return the field values
     */
    private static String[] parseCsvLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    /** Writes all products back to the CSV file. */
    private void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(storageFile, StandardCharsets.UTF_8)) {
            writer.write("id,name,quantity,status");
            writer.newLine();
            for (Product p : products) {
                String status = p.getManualStatus() == null ? "" : p.getManualStatus().name();
                writer.write(String.join(",", p.getId(), escape(p.getName()),
                        String.valueOf(p.getQuantity()), status));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not save inventory file: " + e.getMessage());
        }
    }

    /** Wraps names containing commas or quotes so CSV stays parseable. */
    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Seeds a small realistic catalogue for the first run. */
    private void seedDemoData() {
        try {
            addQuietly("Notebook A5", 24);
            addQuietly("Ballpoint Pen (Blue)", 3);
            addQuietly("Ceramic Mug", 0);
            addQuietly("Desk Lamp", 12);
            addQuietly("Sticky Notes Pack", 7);
            addQuietly("Wireless Mouse", 2);
            addQuietly("Cotton Tote Bag", 15);
        } catch (InventoryException e) {
            // seed data is constant and valid; this should never happen
        }
    }

    private void addQuietly(String name, int quantity) throws InventoryException {
        Product p = new Product(nextId(), name, quantity);
        products.add(p);
    }
}
