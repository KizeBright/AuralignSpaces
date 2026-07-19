```markdown
# AuralignSpaces

AuralignSpaces is an Android AR interior design app that lets users create room designs, place furniture and materials in augmented reality, calculate budget usage, save designs, and reopen saved layouts later.

The app uses Firebase Authentication and Cloud Firestore for user/design data. For saved design thumbnails, the project can use GitHub CDN instead of Firebase Storage to avoid Firebase Storage premium/billing requirements.

---

## Features

- Firebase Authentication based login
- AR room design experience
- Furniture catalog with categories, prices, ratings, dimensions, and model URLs
- 3D furniture/model placement in AR
- Wall paint and floor material selection
- Live budget calculation
- Prevents or dims items that exceed remaining budget
- Saved designs list
- Saved design thumbnail preview
- Saved design reopening/restoration
- Gemini based AI suggestion support
- GitHub CDN thumbnail upload option

---

## Tech Stack

- Android
- Kotlin
- Jetpack Compose
- Material 3
- Hilt Dependency Injection
- Firebase Authentication
- Cloud Firestore
- Firebase Storage dependency
- ARCore
- SceneView / ARSceneView
- OkHttp
- Gson
- Coil
- CameraX
- DataStore
- Gemini API
- Node.js script for catalog seeding

---

## Project Structure

```text
AuralignSpaces/
├── app/
│   └── src/main/java/com/auralign/spaces/
│       ├── data/
│       │   ├── local/
│       │   ├── model/
│       │   ├── remote/
│       │   ├── repository/
│       │   └── service/
│       ├── di/
│       ├── domain/
│       └── ui/
│           ├── auth/
│           ├── catalog/
│           ├── designer/
│           ├── home/
│           ├── main/
│           ├── profile/
│           ├── saved/
│           └── splash/
├── scripts/
│   └── seedFurnitureCatalog.js
├── unity-project/
├── furniture_catalog_items.json
├── firestore.rules
├── FIREBASE_SETUP_GUIDE.md
├── STORAGE_MIGRATION_GUIDE.md
├── BUDGET_CALCULATION_FEATURE.md
├── package.json
└── README.md
```

---

## Main Screens

- Splash Screen
- Authentication Screen
- Home Screen
- Catalog Screen
- Designer / AR Screen
- Saved Designs Screen
- Thumbnail Viewer Screen
- Profile Screen

---

## Android Requirements

- Android Studio
- JDK 17
- Android SDK
- ARCore supported Android device
- Firebase project
- Internet connection
- Camera permission

The app requires AR hardware:

```xml
<uses-feature android:name="android.hardware.camera.ar" android:required="true" />
```

Required permissions:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

---

## Firebase Setup

Create a Firebase project and enable:

- Authentication
- Cloud Firestore

Recommended Firestore structure:

```text
users/
  {uid}/
    designs/
      {designId}

furniture_catalog/
  {itemId}

wall_paints/
  {paintId}

floor_materials/
  {floorId}
```

Deploy the rules from:

```text
firestore.rules
```

---

## Firestore Saved Design Structure

Saved designs are stored under:

```text
users/{uid}/designs/{designId}
```

Saved design fields:

```kotlin
SavedDesign(
    id = "",
    userId = "",
    name = "",
    roomType = "",
    widthM = 0.0,
    lengthM = 0.0,
    heightM = 0.0,
    budget = 0.0,
    budgetUsed = 0.0,
    placedItems = emptyList(),
    wallPaintId = "",
    wallColorHex = "",
    floorMaterialId = "",
    thumbnailUrl = "",
    createdAt = Timestamp.now(),
    updatedAt = Timestamp.now()
)
```

The most important field for reopening a design is:

```text
placedItems
```

If budget spent is visible but placed furniture does not appear after opening a saved design, the issue is usually with `placedItems` or with catalog item ID matching.

---

## Furniture Catalog Structure

Furniture catalog data is stored in:

```text
furniture_catalog
```

Example document:

```json
{
  "id": "8",
  "name": "Leather Armchair",
  "brand": "LuxurySit",
  "category": "FURNITURE",
  "price": 32000.0,
  "emoji": "Chair",
  "modelId": "armchair_leather",
  "thumbnailUrl": "",
  "description": "Premium Italian leather armchair.",
  "widthM": 0.9,
  "depthM": 0.8,
  "colorOptions": ["#3C2415", "#6B3A2A"],
  "rating": 4.5,
  "modelUrl": "https://raw.githubusercontent.com/mithra-n/auralign-models/main/chair-glb/source/chair%20GLB.glb"
}
```

Important fields:

```text
id
name
category
price
modelUrl
widthM
depthM
```

The saved placed item must use the same `itemId` as the catalog item `id`.

---

## Budget Calculation

The app calculates total spending from:

```text
totalSpent =
    placed furniture item prices
    + floor material price if floor is placed
    + wall paint price if wall is placed
```

It also calculates:

```text
budgetRemaining = room budget - totalSpent
budgetPercent = totalSpent / room budget
isOverBudget = totalSpent > room budget
```

Furniture is disabled or dimmed when:

```text
item.price > budgetRemaining
```

---

## Saved Designs Flow

### Save Flow

```text
1. User creates room design
2. User places furniture/materials
3. App calculates budget used
4. App saves design metadata to Firestore
5. App stores placedItems array
6. App uploads or generates thumbnail URL
7. App updates saved design with thumbnailUrl
```

### Load Flow

```text
1. User opens Saved Designs
2. App fetches designs from Firestore
3. User taps a saved design
4. App loads selected design document
5. App reads placedItems
6. App matches each placed item with furniture_catalog item
7. App restores furniture/model positions in AR
8. App restores budget information
```

---

## GitHub CDN Thumbnail Storage

Firebase Storage can require billing. To avoid that, thumbnails can be uploaded to a public GitHub repository and served through raw GitHub URLs.

Create a public GitHub repository:

```text
auralign-design-thumbnails
```

Create this folder:

```text
thumbnails/
```

Thumbnail files are uploaded as:

```text
thumbnails/{designId}.jpg
```

Public CDN URL format:

```text
https://raw.githubusercontent.com/{repoOwner}/{repoName}/main/thumbnails/{designId}.jpg
```

Current service file:

```text
app/src/main/java/com/auralign/spaces/data/service/GitHubImageService.kt
```

Update these values if needed:

```kotlin
private val repoOwner = "mithra-n"
private val repoName = "auralign-design-thumbnails"
```

---

## GitHub Token Setup

Create a GitHub Personal Access Token with:

```text
public_repo
```

Add it to `local.properties`:

```properties
GITHUB_TOKEN=your_github_token_here
GEMINI_API_KEY=your_gemini_api_key_here
GROQ_API_KEY=your_groq_api_key_here
```

Do not commit `local.properties`.

The app reads these values through Gradle and exposes them using `BuildConfig`.

---

## Local Properties Example

```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
GEMINI_API_KEY=your_gemini_api_key
GROQ_API_KEY=your_groq_api_key
GITHUB_TOKEN=your_github_token
```

---

## Build and Run

Clone the project:

```bash
git clone https://github.com/YOUR_USERNAME/AuralignSpaces.git
cd AuralignSpaces
```

Open in Android Studio.

Sync Gradle.

Build using:

```bash
./gradlew clean build
```

On Windows:

```bash
gradlew.bat clean build
```

Run the app on a real ARCore supported Android device.

---

## Seed Furniture Catalog

The project includes a Node.js script for seeding furniture data into Firestore.

Install Node dependencies:

```bash
npm install
```

Run:

```bash
npm run seed:furniture
```

Or on Windows:

```bash
seed-furniture.cmd
```

The data source is:

```text
furniture_catalog_items.json
```

---

## Fixing Saved Designs Not Showing Placed Models

If Saved Designs shows the amount spent but does not show the model/furniture after opening:

1. Open Firebase Console.
2. Go to Firestore.
3. Open:

```text
users/{uid}/designs/{designId}
```

4. Check whether `placedItems` is empty.

If `placedItems` is empty, the design is saving budget data but not saving placed object data.

If `placedItems` has data, check every item:

```text
placedItems[n].itemId
```

That value must match a document/item ID in:

```text
furniture_catalog
```

Example problem:

```text
placedItems itemId = "chair_01"
furniture_catalog item id = "8"
```

In this case, the app cannot find the model while restoring.

Correct example:

```text
placedItems itemId = "8"
furniture_catalog item id = "8"
```

Also check:

```text
modelUrl
```

The `modelUrl` must be a valid public `.glb` URL.

---

## Common Debug Checklist

- `placedItems` exists in Firestore
- `placedItems` is not an empty array
- Each placed item has a valid `itemId`
- `itemId` exists in `furniture_catalog`
- The matching catalog item has a valid `modelUrl`
- `modelUrl` opens in browser
- User is authenticated before reading saved designs
- Firestore rules allow the user to read their own designs
- Logcat does not show Firestore mapping errors
- Logcat does not show model loading errors

---

## Firestore Rules

Rules are stored in:

```text
firestore.rules
```

Basic expected access:

```text
users/{userId}
users/{userId}/designs/{designId}
```

Only the logged-in user should read/write their own designs.

Catalog collections should be readable by authenticated users.

---

## Git Workflow

Check current changes:

```bash
git status
```

Add files:

```bash
git add .
```

Commit:

```bash
git commit -m "fix: save and restore placed design items"
```

Push:

```bash
git push origin main
```

For GitHub thumbnail storage changes:

```bash
git add app/src/main/java/com/auralign/spaces/data/service/GitHubImageService.kt
git add app/src/main/java/com/auralign/spaces/data/repository/DesignRepository.kt
git add app/build.gradle.kts
git commit -m "feat: use GitHub CDN for saved design thumbnails"
git push origin main
```

---

## Testing Checklist

- Sign in successfully
- Open catalog
- Add a furniture item
- Confirm budget spent updates
- Place item in AR
- Save design
- Open Firestore
- Confirm saved design has `placedItems`
- Confirm saved design has `budgetUsed`
- Confirm saved design has `thumbnailUrl`
- Open Saved Designs
- Tap saved design
- Confirm furniture appears again
- Confirm budget spent is restored correctly
- Confirm thumbnail loads

---

## Notes

Firebase Storage is not required if GitHub CDN is used for thumbnails.

Firestore is still required for:

- Users
- Saved designs
- Placed item data
- Catalog data
- Wall paint data
- Floor material data

The key to restoring saved models is saving and loading the full `placedItems` array correctly.
```
