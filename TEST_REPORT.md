# STOCKLY — Test Report

All tests were **actually executed** on JDK 25 (Temurin) on
2026-11-19/20. The headless suite runs the real `InventoryManager`
against a temporary CSV file — no logic is mocked.

## How to reproduce

```bash
javac -d classes -encoding UTF-8 @sources.txt
java -cp classes stockly.test.SelfTest
```

Source: `src/stockly/test/SelfTest.java`

## Results

```
TC-01  Add a valid product                                        PASS
       actual: product present with qty 9
TC-02  Add product with empty name                                PASS
       actual: error: Product name cannot be empty.
TC-03  Add product with negative quantity                         PASS
       actual: error: Quantity cannot be negative.
TC-04  Enter a non-numeric quantity (dialog parse guard)          PASS
       actual: NumberFormatException thrown before reaching manager
TC-05  View all products                                          PASS
       actual: 8 products listed
TC-06  View an empty inventory                                    PASS
       actual: 0 products; UI shows the empty-state panel
TC-07  Update an existing product                                 PASS
       actual: name/qty updated, status recomputed to Low Stock
TC-08  Update a non-existing product                              PASS
       actual: error: Product P999 no longer exists.
TC-09  Delete an existing product                                 PASS
       actual: product removed from inventory
TC-10  Delete a non-existing product                              PASS
       actual: error: Product P999 no longer exists.
TC-11  Stock-status classification (0 / 1-5 / 6+)                 PASS
       actual: 0->OUT_OF_STOCK 1->LOW_STOCK 5->LOW_STOCK 6->IN_STOCK 100->IN_STOCK
TC-12  Dashboard statistics recompute after CRUD                  PASS
       actual: count=7 lowStock=3 units=66
TC-13  Charts receive live data (bar + donut sources)             PASS
       actual: chart inputs reconcile with manager totals
TC-14  CSV persistence across restarts                            PASS
       actual: reloaded 7 products
TC-15  Recent activity records real session events                PASS
       actual: 5 entries, 2 deletions
TC-16  Manual stock-status override                               PASS
       actual: qty=50 but status pinned to OUT_OF_STOCK
TC-17  Manual status persists across restarts                     PASS
       actual: reloaded status=OUT_OF_STOCK
TC-18  Product name containing comma survives CSV                 PASS
       actual: name=Stapler, Heavy Duty
TC-19  Malformed CSV row skipped gracefully                       PASS
       actual: loaded 2 of 3 rows; P002 manual status=LOW_STOCK
TC-20  Low-stock threshold is configurable                        PASS
       actual: threshold=10 -> LOW, threshold=5 -> IN for qty 8
TC-21  CSV export/import round-trip                               PASS
       actual: exported+imported 9 products
TC-22  Clear inventory empties the list                           PASS
       actual: count=0

RESULT: 22 passed, 0 failed
```

## Test case details

| ID | Description | Input | Expected | Actual | Status |
|----|-------------|-------|----------|--------|--------|
| TC-01 | Add valid product | name="Test Widget", qty=9 | Product stored, id generated | Found via `findById`, qty=9 | PASS |
| TC-02 | Empty name | name="   ", qty=4 | Rejected with clear error | `InventoryException: Product name cannot be empty.` | PASS |
| TC-03 | Negative qty | name="Neg", qty=-1 | Rejected | `InventoryException: Quantity cannot be negative.` | PASS |
| TC-04 | Non-numeric qty | "abc" in dialog field | `Integer.parseInt` throws before reaching manager; dialog shows "Quantity must be a whole number." | `NumberFormatException` confirmed | PASS |
| TC-05 | View all products | — | All records listed | 8 products (7 seeded + 1 added) | PASS |
| TC-06 | Empty inventory | Pre-created empty CSV | 0 products; designed empty-state panel shown | `productCount()==0`, `findById`→null | PASS |
| TC-07 | Update existing | id=added, name="Test Widget Pro", qty=4 | Record updated; status recomputed | Name/qty changed; status → LOW_STOCK | PASS |
| TC-08 | Update non-existing | id="P999" | Rejected gracefully | `InventoryException: Product P999 no longer exists.` | PASS |
| TC-09 | Delete existing | id=added | Record removed | `findById`→null afterwards | PASS |
| TC-10 | Delete non-existing | id="P999" | Rejected gracefully | `InventoryException` thrown | PASS |
| TC-11 | Status classification | qty = 0,1,5,6,100 | OUT_OF_STOCK / LOW_STOCK / LOW_STOCK / IN_STOCK / IN_STOCK | All boundaries correct | PASS |
| TC-12 | Dashboard stats after CRUD | add + count | Counts/units recompute live | count, lowStock, totalUnits consistent | PASS |
| TC-13 | Charts update | read live collections | Chart data == manager totals | `unitsSum == totalUnits()`; status counts sum to productCount | PASS |
| TC-14 | CSV persistence | reload from disk | Same products after restart | 7 products reloaded; deleted item stayed deleted | PASS |
| TC-15 | Activity log | perform CRUD | Session events recorded | 5 entries incl. deletions with timestamps | PASS |
| TC-16 | Manual status override | addProduct("Pinned Item", 50, OUT_OF_STOCK) | Status pinned despite qty=50 | `getStockStatus()` → OUT_OF_STOCK | PASS |
| TC-17 | Status persistence | reload CSV | Manual status survives restart | Reloaded product still OUT_OF_STOCK | PASS |
| TC-18 | Comma in name | "Stapler, Heavy Duty" | CSV round-trips quoted field | Name intact after reload | PASS |
| TC-19 | Malformed CSV | file with bad row | Bad row skipped, rest load | 2 of 3 rows loaded; manual status parsed | PASS |
| TC-20 | Configurable threshold | qty=8, threshold 10→5 | LOW_STOCK then IN_STOCK | Status flips live via `AppConfig` | PASS |
| TC-21 | CSV export/import | export then import | Same product count | 9 products round-tripped | PASS |
| TC-22 | Clear inventory | `clearAll()` | Empty list | `productCount()==0` | PASS |

## Headed UI tests (`stockly.test.UITest`)

A second suite builds the real `MainFrame` off-screen and drives actual
components — forms, table model, combos and modal dialog buttons:

```bash
java -cp classes stockly.test.UITest
```

Executed result: **13 passed, 0 failed**

| ID | Scenario | Result |
|----|----------|--------|
| U1 | All five pages registered in card layout | PASS |
| U2 | Sidebar navigation reaches every page | PASS |
| U3 | Dashboard renders with seeded data | PASS |
| U5 | Add Item page → Save Product adds product | PASS |
| U6 | Inventory lists all products by default | PASS |
| U7 | Search filters rows and restores on clear | PASS |
| U8 | Status filter shows only matching rows | PASS |
| U9 | Sort combo reorders table | PASS |
| U10 | Edit dialog opens and saves changes | PASS |
| U11 | Delete confirmation removes product | PASS |
| U12 | Settings threshold changes status live | PASS |
| U13 | Analytics and Settings render | PASS |
| U14 | Empty inventory shows empty state | PASS |

## Manual GUI verification

The application was compiled and launched on the development machine
(`java -cp classes stockly.Main`). All five pages were rendered and
captured to `screenshots/` (17 states) via `stockly.test.ReportShots`.
Visual checks performed:

- Dashboard cards, donut, bar chart and activity feed render with the
  pastel palette.
- Inventory table shows badges, search/filter/sort controls and row
  actions; deleting the last filtered row shows the empty state.
- Add/Edit dialog validates empty name, negative and non-numeric
  quantity with inline messages.
- Delete confirmation dialog names the product and cancels cleanly.
- `inventory.csv` updates after every mutation.
