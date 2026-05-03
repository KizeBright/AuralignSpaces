const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

const catalogPath = path.resolve(__dirname, "..", "furniture_catalog_items.json");
const serviceAccountPath = process.env.GOOGLE_APPLICATION_CREDENTIALS ||
  path.resolve(__dirname, "..", "serviceAccountKey.json");

if (!fs.existsSync(serviceAccountPath)) {
  console.error(`Missing service account key: ${serviceAccountPath}`);
  console.error("Create/download a Firebase service account JSON and either:");
  console.error("  1. Save it as serviceAccountKey.json in the project root, or");
  console.error("  2. Set GOOGLE_APPLICATION_CREDENTIALS to its full path.");
  process.exit(1);
}

const serviceAccount = require(serviceAccountPath);
const items = JSON.parse(fs.readFileSync(catalogPath, "utf8"));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const db = admin.firestore();

function cleanItem(item) {
  const width = Number(item.width ?? item.widthM ?? 0);
  const modelUrl = String(item.modelUrl || "").trim();

  return {
    id: String(item.id),
    name: String(item.name || ""),
    brand: String(item.brand || ""),
    category: String(item.category || "FURNITURE"),
    price: Number(item.price || 0),
    emoji: String(item.emoji || ""),
    modelId: String(item.modelId || item.id),
    thumbnailUrl: String(item.thumbnailUrl || ""),
    description: String(item.description || item.name || ""),
    widthM: width,
    depthM: Number(item.depth ?? item.depthM ?? width),
    colorOptions: Array.isArray(item.colorOptions) ? item.colorOptions : [],
    rating: Number(item.rating || 4.5),
    modelUrl,
  };
}

async function seed() {
  const batch = db.batch();

  for (const rawItem of items) {
    const item = cleanItem(rawItem);
    if (!item.id) throw new Error(`Catalog item is missing id: ${JSON.stringify(rawItem)}`);
    batch.set(db.collection("furniture_catalog").doc(item.id), item, { merge: true });
    console.log(`Queued ${item.id}: ${item.name} - INR ${item.price}`);
  }

  await batch.commit();
  console.log(`Saved ${items.length} furniture catalog items.`);
}

seed().catch((error) => {
  console.error(error);
  process.exit(1);
});
