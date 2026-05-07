import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const apiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const jsonPath = path.resolve(apiRoot, process.env.JSON_DB_PATH ?? "./languageguidev2-default-rtdb-export (1).json");
const uri = process.env.MONGODB_URI;
const dbName = process.env.MONGODB_DB_NAME ?? "langapp";
const collectionName = process.env.MONGODB_COLLECTION ?? "database";

if (!uri) {
  console.error("MONGODB_URI is required");
  process.exit(1);
}

const { MongoClient } = await import("mongodb");
const data = JSON.parse(await fs.readFile(jsonPath, "utf8"));
const client = new MongoClient(uri);

try {
  await client.connect();
  await client
    .db(dbName)
    .collection(collectionName)
    .updateOne(
      { _id: "root" },
      { $set: { data, importedAt: new Date(), source: path.basename(jsonPath) } },
      { upsert: true }
    );
  console.log(`Imported ${jsonPath} into ${dbName}.${collectionName}`);
} finally {
  await client.close();
}
