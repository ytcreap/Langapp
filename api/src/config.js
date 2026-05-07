import path from "node:path";
import fs from "node:fs";
import { fileURLToPath } from "node:url";

const apiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

function loadEnvFile() {
  const envPath = path.join(apiRoot, ".env");
  if (!fs.existsSync(envPath)) {
    return;
  }

  const lines = fs.readFileSync(envPath, "utf8").split(/\r?\n/);
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      continue;
    }

    const separator = trimmed.indexOf("=");
    if (separator === -1) {
      continue;
    }

    const key = trimmed.slice(0, separator).trim();
    let value = trimmed.slice(separator + 1).trim();
    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    process.env[key] ??= value;
  }
}

function env(name, fallback) {
  const value = process.env[name];
  return value === undefined || value === "" ? fallback : value;
}

loadEnvFile();

export const config = {
  apiRoot,
  host: env("HOST", "0.0.0.0"),
  port: Number(env("PORT", "3000")),
  dataSource: env("DATA_SOURCE", "json").toLowerCase(),
  jsonDbPath: path.resolve(apiRoot, env("JSON_DB_PATH", "./languageguidev2-default-rtdb-export (1).json")),
  jsonWriteDebounceMs: Number(env("JSON_WRITE_DEBOUNCE_MS", "150")),
  mongoUri: env("MONGODB_URI", ""),
  mongoDbName: env("MONGODB_DB_NAME", "langapp"),
  mongoCollection: env("MONGODB_COLLECTION", "database"),
  corsOrigin: env("CORS_ORIGIN", "*")
};
