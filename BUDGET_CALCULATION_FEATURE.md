# Budget Calculation Feature - Chair/Furniture Selection

## Overview
This document outlines the current implementation of budget calculation when furniture items (especially chairs) are selected, and how prices are fetched from Firestore and displayed in the catalog.

---

## Current Implementation

### 1. **Data Model - FurnitureItem**
**File**: `app/src/main/java/com/auralign/spaces/data/model/FurnitureItem.kt`

```kotlin
data class FurnitureItem(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val category: FurnitureCategory = FurnitureCategory.FURNITURE,
    val price: Double = 0.0,              // ðŸ’° Price from Firestore
    val emoji: String = "ðŸ›‹ï¸",
    val modelId: String = "",
    val thumbnailUrl: String = "",
    val description: String = "",
    val widthM: Double = 0.0,
    val depthM: Double = 0.0,
    val colorOptions: List<String> = emptyList(),
    val rating: Float = 4.5f,
    val modelUrl: String = ""
)
```

### 2. **Firestore Integration**
**File**: `app/src/main/java/com/auralign/spaces/data/repository/DesignRepository.kt`

**Fetching Furniture Catalog from Firestore**:
```kotlin
suspend fun getFurnitureCatalog(): List<FurnitureItem> {
    val items = firestore.collection("furniture_catalog")
        .get()
        .await()
        .toObjects(FurnitureItem::class.java)

    val finalItems = if (items.isEmpty()) getMockCatalog() else items
    // ... model URL injection logic
    return finalItems
}
```

**Collection Path**: `firestore.collection("furniture_catalog")`
- Each document in this collection contains a `FurnitureItem` object with a `price` field
- Example chair in mock data:
  ```
  FurnitureItem(
    id = "8",
    name = "Leather Armchair",
    brand = "LuxurySit",
    category = FURNITURE,
    price = 32000.0,  // â‚¹32,000
    emoji = "ðŸª‘"
  )
  ```

---

## 3. **Budget Calculation Logic**

### State Management
**File**: `app/src/main/java/com/auralign/spaces/ui/designer/DesignerViewModel.kt`

```kotlin
data class DesignerState(
    val roomConfig: RoomConfig = RoomConfig(),          // Contains initial budget
    val placedItems: List<PlacedObject> = emptyList(),  // Furniture items placed
    val wallPaint: WallPaintMaterial = ...,             // Wall cost
    val floorMaterial: FloorTileMaterial = ...,         // Floor cost
    val floorPlaced: Boolean = false,
    val wallPlaced: Boolean = false,
    // ...
) {
    // ðŸ“Š Total spent calculation
    val totalSpent: Double get() =
        placedItems.sumOf { it.item.price } +           // Sum of all item prices
        (if (floorPlaced) floorMaterial.price else 0.0) +
        (if (wallPlaced) wallPaint.price else 0.0)

    // ðŸ’° Budget remaining calculation
    val budgetRemaining: Double get() =
        (roomConfig.budget - totalSpent).coerceAtLeast(0.0)

    // ðŸ“ˆ Budget percentage used
    val budgetPercent: Float get() =
        if (roomConfig.budget > 0)
            (totalSpent / roomConfig.budget).toFloat().coerceIn(0f, 1f)
        else 0f

    // âš ï¸ Over budget status
    val isOverBudget: Boolean get() = totalSpent > roomConfig.budget
}
```

### Adding Item (Chair) - Budget Validation
**File**: `app/src/main/java/com/auralign/spaces/ui/designer/DesignerViewModel.kt`

```kotlin
fun addItem(item: FurnitureItem) {
    // âŒ Don't add if exceeds budget
    if (_state.value.budgetRemaining < item.price) return

    // âœ… Add the item
    when (item.category) {
        FurnitureCategory.FURNITURE -> {
            val newPlacedObject = PlacedObject(
                instanceId = UUID.randomUUID().toString(),
                item = item,  // Contains price
                isPlaced = false
                // ...
            )
            _state.update { state ->
                state.copy(
                    placedItems = state.placedItems + newPlacedObject,
                    pendingPlacementId = newPlacedObject.instanceId
                )
            }
        }
        // ...
    }
}
```

**Key Logic**:
1. When chair is selected: `if (_state.value.budgetRemaining < item.price) return` âŒ
2. Only allows addition if `budget remaining â‰¥ chair price`
3. Automatically updates `totalSpent` and `budgetRemaining`

---

## 4. **Catalog Display with Prices**

### CatalogScreen - Display Chair Prices
**File**: `app/src/main/java/com/auralign/spaces/ui/catalog/CatalogScreen.kt`

```kotlin
@Composable
private fun CatalogItemCard(item: FurnitureItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.72f).clickable { onClick() }
    ) {
        Column {
            // Image section
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                AsyncImage(model = images[item.category])
            }

            // Info section
            Column(Modifier.padding(10.dp)) {
                Text(item.brand.uppercase(), fontSize = 9.sp)
                Text(item.name, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))

                // ðŸ’° PRICE DISPLAY
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("â‚¹${item.price.toInt()}",
                         fontSize = 14.sp,
                         fontWeight = FontWeight.Bold)  // â† Price shown here
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.Star)
                    Text(item.rating.toString(), fontSize = 11.sp)
                }
            }
        }
    }
}
```

**Display Format**: `â‚¹${item.price.toInt()}`

---

## 5. **Budget Display During Design**

### Budget Progress Bar
**File**: `app/src/main/java/com/auralign/spaces/ui/designer/DesignerScreen.kt`

```kotlin
@Composable
fun BudgetProgressBar(
    percent: Float,           // totalSpent / budget
    remaining: Double,        // budgetRemaining
    isOver: Boolean           // isOverBudget
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Budget", color = TextSecondary, fontSize = 10.sp)
            // ðŸ’š Green if under budget, ðŸ”´ Red if over
            Text("â‚¹${remaining.toInt()} left",
                 color = if (isOver) Rose else Emerald,
                 fontSize = 10.sp)
        }
        LinearProgressIndicator(
            progress = { percent },
            color = if (isOver) Rose else Emerald
        )
    }
}
```

**Display**:
- Shows: `â‚¹{budgetRemaining} left`
- Color coding: ðŸŸ¢ Green (under budget) / ðŸ”´ Red (over budget)
- Progress bar fills as budget is used

### Budget Display in Top Bar
```kotlin
Box(modifier = Modifier.padding(top = 16.dp)) {
    Text(
        text = "Budget Remaining: â‚¹${state.budgetRemaining.toInt()}",
        color = if (state.isOverBudget) Color.Red else Color.White
    )
}
```

---

## 6. **Furniture Shelf - Item Selection with Budget Check**

**File**: `app/src/main/java/com/auralign/spaces/ui/designer/DesignerScreen.kt`

```kotlin
@Composable
fun FurnitureShelf(
    items: List<FurnitureItem>,
    budgetRemaining: Double,
    onItemClick: (FurnitureItem) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { item ->
            val canAfford = budgetRemaining >= item.price  // â† Check affordability

            Column(
                modifier = Modifier
                    .width(76.dp)
                    .clickable(enabled = canAfford) { onItemClick(item) }
                    .alpha(if (canAfford) 1f else 0.4f)  // Dim if can't afford
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(SurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.emoji, fontSize = 32.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("â‚¹${item.price.toInt()}", fontSize = 11.sp)  // â† Price shown
            }
        }
    }
}
```

**Features**:
- âœ… Shows `â‚¹{item.price}` for each chair
- ðŸš« Disables item if `budgetRemaining < item.price`
- ðŸ‘ï¸ Dims out unaffordable items (40% opacity)

---

## 7. **Summary Sheet - Budget Breakdown**

**File**: `app/src/main/java/com/auralign/spaces/ui/designer/DesignerScreen.kt`

```kotlin
@Composable
fun SummarySheet(state: DesignerState, ...) {
    ModalBottomSheet {
        Column {
            // Header stats
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryStatCard("Items",
                    state.placedItems.size.toString(),
                    Icons.Default.Inventory)
                SummaryStatCard("Spent",
                    "â‚¹${(state.totalSpent / 1000).toInt()}k",
                    Icons.Default.Payment)
                SummaryStatCard("Left",
                    "â‚¹${(state.budgetRemaining / 1000).toInt()}k",
                    Icons.Default.Savings)
            }

            // Wall/Floor costs
            Card {
                Column {
                    Row {
                        Text("Wall paint", fontSize = 12.sp)
                        Text("â‚¹${state.wallPaint.price.toInt()}",
                             fontWeight = FontWeight.Bold)
                    }
                    Row {
                        Text("Floor tile", fontSize = 12.sp)
                        Text("â‚¹${state.floorMaterial.price.toInt()}",
                             fontWeight = FontWeight.Bold)
                    }
                }
            }

            // List of placed items with prices
            LazyColumn {
                items(state.placedItems) { placed ->
                    Row {
                        Text(placed.item.name, fontSize = 14.sp)
                        Text("â‚¹${placed.item.price.toInt()}",
                             color = Emerald,
                             fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onRemoveItem(placed.instanceId) }) {
                            Icon(Icons.Default.Close)
                        }
                    }
                }
            }
        }
    }
}
```

---

## 8. **Firestore Collections Schema**

### `furniture_catalog` Collection
```
Document: "8"
{
  "id": "8",
  "name": "Leather Armchair",
  "brand": "LuxurySit",
  "category": "FURNITURE",
  "price": 32000.0,                    â† â‚¹32,000 price
  "emoji": "ðŸª‘",
  "modelId": "armchair_leather",
  "description": "Premium Italian leather armchair.",
  "widthM": 0.9,
  "depthM": 0.8,
  "colorOptions": ["#3C2415", "#6B3A2A"],
  "rating": 4.5,
  "modelUrl": "https://raw.githubusercontent.com/..."
}
```

### Query Path
```kotlin
firestore.collection("furniture_catalog").get().await()
```

---

## 9. **User Flow - Chair Selection & Budget**

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ 1. User Sets Budget (e.g., â‚¹50,000)                         â”‚
â”‚    roomConfig.budget = 50000.0                              â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                       â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ 2. Browse Catalog                                           â”‚
â”‚    - See all chairs with prices: "â‚¹32,000", "â‚¹45,000", etc.â”‚
â”‚    - CatalogScreen displays item.price from Firestore      â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                       â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ 3. Select Chair (e.g., â‚¹32,000 Armchair)                   â”‚
â”‚    - Check: budgetRemaining (â‚¹50,000) >= price (â‚¹32,000)?  â”‚
â”‚    - âœ… YES â†’ Add to placedItems                            â”‚
â”‚    - âŒ NO â†’ Block with 0.4 opacity + disabled clickable   â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                       â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ 4. Budget Recalculated                                      â”‚
â”‚    totalSpent = â‚¹32,000 (chair only)                       â”‚
â”‚    budgetRemaining = â‚¹50,000 - â‚¹32,000 = â‚¹18,000          â”‚
â”‚    budgetPercent = 32,000 / 50,000 = 64%                   â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                       â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ 5. UI Updates                                               â”‚
â”‚    - Progress bar: 64% filled ðŸŸ¢ Green                     â”‚
â”‚    - "Budget Remaining: â‚¹18,000 left"                      â”‚
â”‚    - Furniture shelf: Only shows items â‰¤ â‚¹18,000           â”‚
â”‚    - Items > â‚¹18,000 are dimmed (disabled)                 â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                       â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ 6. Add More Items or Floor/Wall                             â”‚
â”‚    - Select floor (â‚¹8,000): OK (18,000 - 8,000 = 10,000)   â”‚
â”‚    - Select wall (â‚¹15,000): âŒ Over budget (10,000 left)    â”‚
â”‚    - System prevents wall placement                         â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                       â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ 7. Summary Sheet Shows Breakdown                            â”‚
â”‚    - Items: 1                                               â”‚
â”‚    - Spent: â‚¹40k                                            â”‚
â”‚    - Left: â‚¹10k                                             â”‚
â”‚    - Chair: â‚¹32,000 âœ• (remove option)                      â”‚
â”‚    - Floor: â‚¹8,000 âœ•                                        â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

---

## 10. **Key Features Summary**

| Feature | Implementation | Status |
|---------|------------------|--------|
| **Fetch chair price from Firestore** | âœ… `getFurnitureCatalog()` from `furniture_catalog` collection | âœ… Working |
| **Display chair price in catalog** | âœ… `CatalogItemCard` shows `â‚¹${item.price.toInt()}` | âœ… Working |
| **Display chair price in shelf** | âœ… `FurnitureShelf` shows `â‚¹${item.price.toInt()}` | âœ… Working |
| **Subtract chair price from budget** | âœ… `totalSpent += chair.price`, `budgetRemaining -= chair.price` | âœ… Working |
| **Prevent over-budget purchases** | âœ… `if (budgetRemaining < item.price) return` | âœ… Working |
| **Visual affordability feedback** | âœ… Dim items (0.4 opacity) when unaffordable | âœ… Working |
| **Budget progress display** | âœ… Color-coded progress bar (green/red) | âœ… Working |
| **Budget display in top bar** | âœ… "Budget Remaining: â‚¹X" | âœ… Working |
| **Budget breakdown in summary** | âœ… Lists all items with prices | âœ… Working |

---

## 11. **Edge Cases Handled**

1. **No Firestore Data**: Falls back to mock catalog
2. **Insufficient Budget**: Item is disabled (0.4 opacity + no clickable)
3. **Zero Budget**: Shows budgetPercent = 0
4. **Over Budget**: Shows âš ï¸ Red warning color
5. **Item Removal**: Budget is recalculated immediately
6. **Multiple Items**: Sum of all prices considered

---

## 12. **Files Involved**

| File | Purpose |
|------|---------|
| `FurnitureItem.kt` | Data model with `price` field |
| `DesignRepository.kt` | Fetches from Firestore `furniture_catalog` |
| `DesignerViewModel.kt` | Budget calculation logic |
| `DesignerScreen.kt` | UI display for budget, items, progress |
| `CatalogScreen.kt` | Catalog display with prices |

---

## 13. **Firestore Rules**

Located in `firestore.rules`:
- Allows authenticated users to read from `furniture_catalog`
- Furniture items have public read access (no authentication required for catalog viewing)
