# Firebase Setup Guide for AuralignSpaces - Saved Designs & Thumbnails

## Overview
You need to set up:
1. **Firestore Database** - to store design metadata and saved design documents
2. **Firebase Storage** - to store design screenshot thumbnail images
3. **Firebase Security Rules** - to protect user data

---

## Step 1: Firestore Database Setup

### 1.1 Navigate to Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **AuralignSpaces** project
3. Click on **Firestore Database** from the left menu

### 1.2 Create/Verify Database
- If you don't have a Firestore database, click **Create Database**
- Choose **Start in production mode**
- Select region: **asia-south1** (India) or closest to your users
- Click **Create**

---

## Step 2: Create Firestore Collections Structure

### 2.1 Collection: `users`
This collection stores user documents (likely already exists).

**Steps:**
1. In Firestore, click **Start Collection**
2. Collection ID: `users`
3. Click **Next**
4. Click **Auto ID** to create first document (auto-generated)
5. Add these fields for each user:
   ```
   id: (string) - user's Firebase UID
   name: (string) - user's display name
   email: (string) - user's email
   createdAt: (timestamp) - account creation date
   ```
6. Click **Save**

### 2.2 Sub-Collection: `users > {userId} > designs`
This stores all saved designs for each user.

**Steps:**
1. Click on a document in `users` collection (or create one with ID = Firebase UID)
2. Click **Add Collection**
3. Collection ID: `designs`
4. Click **Next**
5. Click **Auto ID** to create first document
6. Add these fields:
   ```
   id: (string) - design document ID (will auto-populate)
   userId: (string) - owner's Firebase UID
   name: (string) - design name (e.g., "Living Room Design")
   roomType: (string) - "Living Room", "Bedroom", "Kitchen", etc.
   widthM: (number) - room width in meters (e.g., 5.5)
   lengthM: (number) - room length in meters (e.g., 4.2)
   heightM: (number) - room height in meters (e.g., 3.0)
   budget: (number) - initial budget in â‚¹ (e.g., 50000)
   budgetUsed: (number) - actual spent amount (e.g., 32000)
   wallPaintId: (string) - selected wall paint ID (e.g., "paint_ocean")
   wallColorHex: (string) - wall color hex code (e.g., "#1A2744")
   floorMaterialId: (string) - selected floor tile ID (e.g., "tile_oak")
   thumbnailUrl: (string) - EMPTY for now (will be filled after save)
   placedItems: (array) - empty array [] initially
   createdAt: (timestamp) - auto timestamp
   updatedAt: (timestamp) - auto timestamp
   ```
7. Click **Save**

### 2.3 Example Document Structure in Firestore

```
users/
  â””â”€â”€ abc123def456ghi789 (uid)
      â”œâ”€â”€ id: "abc123def456ghi789"
      â”œâ”€â”€ name: "John Designer"
      â”œâ”€â”€ email: "john@example.com"
      â””â”€â”€ designs/ (sub-collection)
          â”œâ”€â”€ design_001
          â”‚   â”œâ”€â”€ id: "design_001"
          â”‚   â”œâ”€â”€ userId: "abc123def456ghi789"
          â”‚   â”œâ”€â”€ name: "Living Room Design"
          â”‚   â”œâ”€â”€ roomType: "Living Room"
          â”‚   â”œâ”€â”€ widthM: 5.5
          â”‚   â”œâ”€â”€ lengthM: 4.2
          â”‚   â”œâ”€â”€ heightM: 3.0
          â”‚   â”œâ”€â”€ budget: 50000
          â”‚   â”œâ”€â”€ budgetUsed: 32000
          â”‚   â”œâ”€â”€ wallPaintId: "paint_ocean"
          â”‚   â”œâ”€â”€ wallColorHex: "#1A2744"
          â”‚   â”œâ”€â”€ floorMaterialId: "tile_oak"
          â”‚   â”œâ”€â”€ thumbnailUrl: "https://..." (URL from Firebase Storage)
          â”‚   â”œâ”€â”€ placedItems: [...]
          â”‚   â”œâ”€â”€ createdAt: (timestamp)
          â”‚   â””â”€â”€ updatedAt: (timestamp)
          â””â”€â”€ design_002
              â””â”€â”€ ... (similar structure)
```

---

## Step 3: Firebase Storage Setup for Thumbnails

### 3.1 Navigate to Storage
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **AuralignSpaces** project
3. Click on **Storage** from the left menu

### 3.2 Create/Verify Storage Bucket
- If you don't have Cloud Storage, click **Get Started**
- Choose location: **asia-south1** (India)
- Click **Done**

### 3.3 Create Folder Structure in Storage

**You need to create this manually or let the app create it:**

```
users/
  â””â”€â”€ {userId}/
      â””â”€â”€ design_thumbnails/
          â”œâ”€â”€ design_001.jpg
          â”œâ”€â”€ design_002.jpg
          â””â”€â”€ ... (one jpg per saved design)
```

**Manual Step (Optional - can let app auto-create):**
1. In Firebase Storage, click **Upload Folder**
2. Or just let the app auto-create folders when you save a design

The app will automatically upload to:
```
users/{userId}/design_thumbnails/{designId}.jpg
```

---

## Step 4: Firebase Storage Security Rules

### 4.1 Go to Storage Rules
1. In Firebase Storage, click **Rules** tab
2. Replace the existing rules with:

```rules
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Allow authenticated users to upload their own thumbnails
    match /users/{userId}/design_thumbnails/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Allow authenticated users to access other storage
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

3. Click **Publish**

---

## Step 5: Firestore Security Rules

### 5.1 Go to Firestore Rules
1. In Firestore Database, click **Rules** tab
2. Replace the existing rules with:

```rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User data - only accessible to the user themselves
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;

      // User's designs - only accessible to the user
      match /designs/{designId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }

    // Public furniture catalog - readable by all authenticated users
    match /furniture_catalog/{itemId} {
      allow read: if request.auth != null;
      allow write: if false; // Admin only (via console)
    }

    // Wall paints - readable by all authenticated users
    match /wall_paints/{paintId} {
      allow read: if request.auth != null;
      allow write: if false; // Admin only (via console)
    }

    // Floor materials - readable by all authenticated users
    match /floor_materials/{floorId} {
      allow read: if request.auth != null;
      allow write: if false; // Admin only (via console)
    }
  }
}
```

3. Click **Publish**

---

## Step 6: Test the Setup Manually

### 6.1 Create Test Data in Firestore
1. Open Firestore Console
2. Create a test user document:
   ```
   Collection: users
   Document ID: (your-firebase-uid)
   Fields:
   - id: your-firebase-uid
   - name: "Test User"
   - email: "test@example.com"
   ```

3. Create a test design document:
   ```
   Sub-collection: designs (under your user)
   Document ID: test_design_001
   Fields:
   - id: "test_design_001"
   - userId: "your-firebase-uid"
   - name: "Test Living Room"
   - roomType: "Living Room"
   - widthM: 5.0
   - lengthM: 4.0
   - heightM: 3.0
   - budget: 50000
   - budgetUsed: 0
   - wallPaintId: "paint_pearl"
   - wallColorHex: "#F5F5F0"
   - floorMaterialId: "tile_oak"
   - thumbnailUrl: "" (leave empty)
   - placedItems: [] (empty array)
   - createdAt: (auto timestamp)
   - updatedAt: (auto timestamp)
   ```

### 6.2 Test Storage Upload
1. In Firebase Console, go to **Storage**
2. Click **Upload Files**
3. Upload a test image (.jpg)
4. Navigate to: `users/{your-uid}/design_thumbnails/test_design.jpg`
5. Click on the file
6. Copy the **Download URL** (you'll see it in the details panel)
7. Go back to Firestore
8. Edit your test design document
9. Paste the URL in the `thumbnailUrl` field
10. Click **Update**

---

## Step 7: App Auto-Flow (What Happens Automatically)

When you save a design in the app:

### 7.1 Save Flow
1. **App saves design doc** to Firestore:
   ```
   users/{userId}/designs/{designId}
   ```
2. **App captures screenshot** of AR view
3. **App uploads screenshot** to Storage:
   ```
   users/{userId}/design_thumbnails/{designId}.jpg
   ```
4. **App gets download URL** from Storage
5. **App updates Firestore doc** with thumbnail URL:
   ```
   users/{userId}/designs/{designId} -> thumbnailUrl = "https://..."
   ```

### 7.2 Load Flow
1. **App fetches saved designs** list from Firestore
2. **App displays thumbnail image** from `thumbnailUrl` in saved designs screen
3. When you click a saved design:
   - App loads the design doc from Firestore
   - App reconstructs the 3D AR scene with same items/positions
   - **Thumbnail image is NOT shown in designer screen** (it's just for the list preview)

---

## Step 8: Collections Summary Table

| Collection Path | Type | Purpose | Who Can Access |
|---|---|---|---|
| `users` | Collection | Stores user profiles | Only authenticated users (own doc) |
| `users/{uid}/designs` | Sub-collection | Stores user's saved designs | Only the owner user |
| `furniture_catalog` | Collection | Stores all furniture items | All authenticated users (read-only) |
| `wall_paints` | Collection | Stores wall paint options | All authenticated users (read-only) |
| `floor_materials` | Collection | Stores floor tile options | All authenticated users (read-only) |
| Storage: `users/{uid}/design_thumbnails/` | Folder | Stores design screenshot JPGs | Only the owner user |

---

## Step 9: Common Questions

### Q1: Do I need to manually create all furniture items?
**No.** The app has mock data (hardcoded furniture items). You can:
- Keep using mock data (no Firestore furniture_catalog needed)
- OR manually add items to Firestore collection `furniture_catalog` with this structure:
  ```
  id: (string) - unique item ID
  name: (string) - item name
  brand: (string) - brand name
  category: (string) - "FURNITURE", "LIGHTING", "DECOR", etc.
  price: (number) - price in â‚¹
  emoji: (string) - emoji representation
  description: (string) - item description
  widthM: (number) - width in meters
  depthM: (number) - depth in meters
  modelUrl: (string) - URL to 3D model (.glb file)
  colorOptions: (array) - list of hex colors
  rating: (number) - rating 0-5
  ```

### Q2: Do I need to manually create wall paints and floor tiles?
**No.** Same as above - mock data is built-in. But if you want Firestore:
- Add to `wall_paints` collection
- Add to `floor_materials` collection

### Q3: What if I don't want thumbnails?
**You can disable it.** But the current code requires it. Thumbnails are optional in the UI (if `thumbnailUrl` is empty, it just shows a placeholder).

### Q4: Can I test without uploading a real design?
**Yes.** Use the test data steps in Step 6.

### Q5: Where do I find my Firebase UID?
1. Go to Firebase Console â†’ Authentication
2. Click on a user
3. Copy the **User UID** value

---

## Step 10: Firestore Data Types Reference

When creating documents, use these data types:

| Field | Type | Example |
|---|---|---|
| `id` | String | "design_001" |
| `userId` | String | "abc123def456" |
| `name` | String | "Living Room Design" |
| `budget` | Number | 50000 |
| `budgetUsed` | Number | 32000 |
| `widthM` | Number | 5.5 |
| `createdAt` | Timestamp | (server timestamp) |
| `placedItems` | Array | [{ itemId: "1", posX: 0, ... }] |
| `thumbnailUrl` | String | "https://storage.googleapis.com/..." |
| `wallPaintId` | String | "paint_ocean" |

---

## Step 11: Check Your Setup

**Firestore Check:**
1. Go to Firestore Console
2. Verify collection structure exists:
   - âœ… `users` collection
   - âœ… `designs` sub-collection under user
   - âœ… Test document with all fields

**Storage Check:**
1. Go to Storage Console
2. Verify folder exists:
   - âœ… `users/` folder
   - âœ… `{userId}/` folder inside
   - âœ… `design_thumbnails/` folder inside

**Rules Check:**
1. Go to Firestore Rules â†’ Review (should show your security rules)
2. Go to Storage Rules â†’ Review (should show your storage rules)
3. Click **Publish** if not already published

---

## Step 12: Quick Checklist

Before running the app:

- [ ] Firestore database created
- [ ] `users` collection exists
- [ ] `designs` sub-collection exists under `users/{uid}`
- [ ] All required fields added to design documents
- [ ] Storage bucket created
- [ ] Storage folder structure ready (`users/{uid}/design_thumbnails/`)
- [ ] Firestore security rules published
- [ ] Storage security rules published
- [ ] Test data created in Firestore (optional but recommended)
- [ ] Test image uploaded to Storage (optional but recommended)

---

## Next Steps

After setup:
1. Rebuild and run the app
2. Sign in with your Firebase account
3. Create a new design in the app
4. Click **Save Design**
5. Wait for screenshot to upload
6. Go to **Saved Designs**
7. You should see the design with thumbnail preview
8. Click it to open and verify items/positions are restored

If something doesn't work, check:
- Firestore rules allow your user to read/write
- Storage rules allow upload
- Design document has all fields
- Check app logs for errors
