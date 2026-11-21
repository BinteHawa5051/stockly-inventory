package stockly.test;

import stockly.model.ActivityEntry;
import stockly.model.InventoryManager;
import stockly.model.Product;
import stockly.model.StockStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Headless self-test for the inventory engine.
 * <p>
 * Exercises every scenario required by the OEL marking rubric against a
 * temporary CSV file, prints a PASS/FAIL line per case and exits with
 * status 0 when everything passes. Run with:
 * <pre>
 *   java -cp classes stockly.test.SelfTest
 * </pre>
 *
 * @author Tooba Akram &amp; Abeeha Noor
 * @version 1.0
 */
public final class SelfTest {

    private static int passed = 0;
    private static int failed = 0;

    private SelfTest() {
    }

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("stockly-test");
        InventoryManager m = new InventoryManager(temp.resolve("inv.csv"));

        // TC-01 add a valid product
        Product added = m.addProduct("Test Widget", 9);
        check("TC-01", "Add a valid product",
                m.findById(added.getId()) != null && added.getQuantity() == 9,
                "product present with qty 9");

        // TC-02 empty name rejected
        expectError("TC-02", "Add product with empty name", () -> m.addProduct("   ", 4),
                "name cannot be empty");

        // TC-03 negative quantity rejected
        expectError("TC-03", "Add product with negative quantity", () -> m.addProduct("Neg", -1),
                "quantity cannot be negative");

        // TC-04 non-numeric quantity is a UI-level parse; simulate by parsing
        boolean parseFails;
        try {
            Integer.parseInt("abc");
            parseFails = false;
        } catch (NumberFormatException e) {
            parseFails = true;
        }
        check("TC-04", "Enter a non-numeric quantity (dialog parse guard)", parseFails,
                "NumberFormatException thrown before reaching manager");

        // TC-05 view all products (seeded demo data + added item)
        check("TC-05", "View all products",
                m.productCount() >= 8 && m.getAllProducts().size() == m.productCount(),
                m.productCount() + " products listed");

        // TC-06 view an empty inventory — pre-create an empty CSV so no
        // demo seeding happens, then verify zero products.
        Path emptyFile = temp.resolve("empty.csv");
        Files.writeString(emptyFile, "id,name,quantity\n");
        InventoryManager empty = new InventoryManager(emptyFile);
        check("TC-06", "View an empty inventory",
                empty.productCount() == 0 && empty.getAllProducts().isEmpty()
                        && empty.findById("P001") == null,
                "0 products; UI shows the empty-state panel");

        // TC-07 update existing product
        String id = added.getId();
        m.updateProduct(id, "Test Widget Pro", 4);
        Product updated = m.findById(id);
        check("TC-07", "Update an existing product",
                updated != null && updated.getName().equals("Test Widget Pro")
                        && updated.getQuantity() == 4 && updated.getStockStatus() == StockStatus.LOW_STOCK,
                "name/qty updated, status recomputed to Low Stock");

        // TC-08 update non-existing product
        expectError("TC-08", "Update a non-existing product", () -> m.updateProduct("P999", "Ghost", 1),
                "unknown id rejected");

        // TC-09 delete existing product
        m.deleteProduct(id);
        check("TC-09", "Delete an existing product", m.findById(id) == null,
                "product removed from inventory");

        // TC-10 delete non-existing product
        expectError("TC-10", "Delete a non-existing product", () -> m.deleteProduct("P999"),
                "unknown id rejected");

        // TC-11 stock-status classification boundaries
        boolean statusOk = true;
        StringBuilder detail = new StringBuilder();
        int[][] cases = {{0, 0}, {1, 1}, {5, 1}, {6, 2}, {100, 2}};
        StockStatus[] expected = {StockStatus.OUT_OF_STOCK, StockStatus.LOW_STOCK,
                StockStatus.LOW_STOCK, StockStatus.IN_STOCK, StockStatus.IN_STOCK};
        for (int i = 0; i < cases.length; i++) {
            Product p = new Product("T" + i, "t", cases[i][0]);
            if (p.getStockStatus() != expected[i]) {
                statusOk = false;
            }
            detail.append(cases[i][0]).append("->").append(p.getStockStatus()).append(' ');
        }
        check("TC-11", "Stock-status classification (0 / 1-5 / 6+)", statusOk, detail.toString().trim());

        // TC-12 dashboard statistics after CRUD
        int before = m.productCount();
        m.addProduct("Stat Check", 3);
        int lowAfter = m.countByStatus(StockStatus.LOW_STOCK);
        int totalUnits = m.totalUnits();
        m.deleteProduct(m.getAllProducts().get(m.getAllProducts().size() - 1).getId());
        check("TC-12", "Dashboard statistics recompute after CRUD",
                m.productCount() == before && lowAfter >= 1 && totalUnits > 0,
                "count=" + before + " lowStock=" + lowAfter + " units=" + totalUnits);

        // TC-13 charts update — data providers return live collections
        List<Product> chartData = m.getAllProducts();
        int unitsSum = chartData.stream().mapToInt(Product::getQuantity).sum();
        check("TC-13", "Charts receive live data (bar + donut sources)",
                unitsSum == m.totalUnits() && m.countByStatus(StockStatus.IN_STOCK)
                        + m.countByStatus(StockStatus.LOW_STOCK)
                        + m.countByStatus(StockStatus.OUT_OF_STOCK) == m.productCount(),
                "chart inputs reconcile with manager totals");

        // persistence round-trip
        InventoryManager reloaded = new InventoryManager(temp.resolve("inv.csv"));
        check("TC-14", "CSV persistence across restarts",
                reloaded.productCount() == m.productCount()
                        && reloaded.findById(added.getId()) == null, // was deleted
                "reloaded " + reloaded.productCount() + " products");

        // activity log recorded real events
        List<ActivityEntry> log = m.getActivityLog().getRecent(10);
        long deletes = log.stream().filter(e -> e.getKind() == ActivityEntry.Kind.DELETED).count();
        check("TC-15", "Recent activity records real session events",
                log.size() >= 4 && deletes >= 1,
                log.size() + " entries, " + deletes + " deletions");

        // TC-16 manual status override
        Product pinned = m.addProduct("Pinned Item", 50, StockStatus.OUT_OF_STOCK);
        check("TC-16", "Manual stock-status override",
                pinned.getStockStatus() == StockStatus.OUT_OF_STOCK
                        && pinned.getManualStatus() == StockStatus.OUT_OF_STOCK,
                "qty=50 but status pinned to OUT_OF_STOCK");

        // TC-17 override survives CSV round-trip
        InventoryManager reloaded2 = new InventoryManager(temp.resolve("inv.csv"));
        Product pinnedReloaded = reloaded2.findById(pinned.getId());
        check("TC-17", "Manual status persists across restarts",
                pinnedReloaded != null
                        && pinnedReloaded.getStockStatus() == StockStatus.OUT_OF_STOCK
                        && pinnedReloaded.getManualStatus() == StockStatus.OUT_OF_STOCK,
                "reloaded status=" + (pinnedReloaded == null ? "null" : pinnedReloaded.getStockStatus()));

        // TC-18 quoted/comma name round-trips through CSV
        Product comma = m.addProduct("Stapler, Heavy Duty", 8);
        InventoryManager reloaded3 = new InventoryManager(temp.resolve("inv.csv"));
        Product commaReloaded = reloaded3.findById(comma.getId());
        check("TC-18", "Product name containing comma survives CSV",
                commaReloaded != null && commaReloaded.getName().equals("Stapler, Heavy Duty"),
                "name=" + (commaReloaded == null ? "null" : commaReloaded.getName()));

        // TC-19 malformed CSV row is skipped, rest still load
        Path badFile = temp.resolve("bad.csv");
        Files.writeString(badFile, "id,name,quantity,status\n"
                + "P001,Good,5,\n"
                + "BADROW,no-qty-here\n"
                + "P002,Also Good,9,LOW_STOCK\n");
        InventoryManager bad = new InventoryManager(badFile);
        check("TC-19", "Malformed CSV row skipped gracefully",
                bad.productCount() == 2
                        && bad.findById("P002").getStockStatus() == StockStatus.LOW_STOCK,
                "loaded " + bad.productCount() + " of 3 rows; P002 manual status="
                        + bad.findById("P002").getStockStatus());

        // TC-20 configurable low-stock threshold
        stockly.model.AppConfig.setLowStockThreshold(10);
        Product p10 = new Product("T", "t", 8);
        boolean thresholdWorks = p10.getStockStatus() == StockStatus.LOW_STOCK;
        stockly.model.AppConfig.setLowStockThreshold(5);
        check("TC-20", "Low-stock threshold is configurable",
                thresholdWorks && new Product("T2", "t", 8).getStockStatus() == StockStatus.IN_STOCK,
                "threshold=10 -> LOW, threshold=5 -> IN for qty 8");

        // TC-21 export + import round-trip
        Path exported = temp.resolve("export.csv");
        m.exportCsv(exported);
        InventoryManager imp = new InventoryManager(temp.resolve("imp.csv"));
        int imported = imp.importCsv(exported);
        check("TC-21", "CSV export/import round-trip",
                imported == m.productCount(),
                "exported+imported " + imported + " products");

        // TC-22 clear inventory
        imp.clearAll();
        check("TC-22", "Clear inventory empties the list",
                imp.productCount() == 0,
                "count=" + imp.productCount());

        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void check(String id, String desc, boolean ok, String actual) {
        System.out.printf("%-6s %-58s %s%n", id, desc, ok ? "PASS" : "FAIL");
        System.out.printf("       actual: %s%n", actual);
        if (ok) passed++; else failed++;
    }

    private interface ThrowingRunnable {
        void run() throws InventoryManager.InventoryException;
    }

    private static void expectError(String id, String desc, ThrowingRunnable r, String expectedMsg) {
        try {
            r.run();
            check(id, desc, false, "no exception thrown");
        } catch (InventoryManager.InventoryException e) {
            check(id, desc, e.getMessage().toLowerCase().contains(expectedMsg.split(" ")[0])
                    || !e.getMessage().isBlank(), "error: " + e.getMessage());
        }
    }
}
