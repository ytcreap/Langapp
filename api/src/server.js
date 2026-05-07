import http from "node:http";
import { URL } from "node:url";

import { config } from "./config.js";
import { HttpError, badRequest, notFound } from "./errors.js";
import { LangAppRepository } from "./repository.js";
import { JsonStore } from "./stores/jsonStore.js";
import { MongoStore } from "./stores/mongoStore.js";

function createStore() {
  if (config.dataSource === "mongo") {
    return new MongoStore({
      uri: config.mongoUri,
      dbName: config.mongoDbName,
      collectionName: config.mongoCollection
    });
  }

  if (config.dataSource !== "json") {
    throw new Error(`Unsupported DATA_SOURCE '${config.dataSource}'. Use 'json' or 'mongo'.`);
  }

  return new JsonStore(config.jsonDbPath, config.jsonWriteDebounceMs);
}

function sendJson(response, status, payload, headers = {}) {
  const body = JSON.stringify(payload);
  response.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(body),
    ...headers
  });
  response.end(body);
}

function corsHeaders(request) {
  const configured = config.corsOrigin;
  const requestOrigin = request.headers.origin;
  const allowOrigin = configured === "*" ? "*" : configured.split(",").map((item) => item.trim()).find((item) => item === requestOrigin) ?? configured.split(",")[0];

  return {
    "Access-Control-Allow-Origin": allowOrigin,
    "Access-Control-Allow-Methods": "GET,POST,PUT,PATCH,OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type,Authorization",
    "Access-Control-Max-Age": "86400"
  };
}

async function readJsonBody(request) {
  const chunks = [];
  for await (const chunk of request) {
    chunks.push(chunk);
  }

  const raw = Buffer.concat(chunks).toString("utf8").trim();
  if (!raw) {
    return {};
  }

  try {
    return JSON.parse(raw);
  } catch (error) {
    throw badRequest("Request body must be valid JSON", error.message);
  }
}

function routeKey(method, parts) {
  return `${method} ${parts.map((part, index) => (index % 2 === 0 ? part : ":")).join("/")}`;
}

async function handleApi(repository, request, url, parts) {
  const method = request.method;

  if (method === "GET" && parts.length === 1 && parts[0] === "meta") {
    return repository.meta();
  }
  if (method === "GET" && parts.length === 1 && parts[0] === "levels") {
    return repository.getLevels();
  }
  if (method === "GET" && parts.length === 1 && parts[0] === "raw") {
    return repository.raw(url.searchParams.get("path") ?? "");
  }
  if (method === "GET" && parts.length === 2 && parts[0] === "lessons") {
    return repository.getLessons(parts[1]);
  }
  if (method === "GET" && parts.length === 3 && parts[0] === "lessons") {
    return repository.getLesson(parts[1], parts[2]);
  }
  if (method === "GET" && parts.length === 4 && parts[0] === "lessons" && parts[3] === "exists") {
    return repository.lessonExists(parts[1], parts[2]);
  }
  if (method === "GET" && parts.length === 4 && parts[0] === "lessons" && parts[3] === "sections") {
    return repository.getSections(parts[1], parts[2]);
  }
  if (method === "GET" && parts.length === 5 && parts[0] === "lessons" && parts[3] === "sections" && parts[4] === "first") {
    return repository.getFirstSection(parts[1], parts[2]);
  }
  if (method === "GET" && parts.length === 5 && parts[0] === "lessons" && parts[3] === "sections") {
    return repository.getSection(parts[1], parts[2], parts[4]);
  }
  if (method === "GET" && parts.length === 6 && parts[0] === "lessons" && parts[3] === "sections" && parts[5] === "tasks") {
    return repository.getTasks(parts[1], parts[2], parts[4]);
  }
  if (method === "GET" && parts.length === 7 && parts[0] === "lessons" && parts[3] === "sections" && parts[5] === "tasks") {
    return repository.getTask(parts[1], parts[2], parts[4], parts[6]);
  }
  if (method === "GET" && parts.length === 2 && parts[0] === "users") {
    return repository.getUser(parts[1]);
  }
  if ((method === "PUT" || method === "PATCH") && parts.length === 2 && parts[0] === "users") {
    return repository.updateUser(parts[1], await readJsonBody(request));
  }
  if (method === "POST" && parts.length === 3 && parts[0] === "users" && parts[2] === "ensure") {
    return repository.ensureUser(parts[1], await readJsonBody(request));
  }
  if (method === "GET" && parts.length === 4 && parts[0] === "users" && parts[2] === "progress") {
    return repository.getUserProgress(parts[1], parts[3]);
  }
  if ((method === "PUT" || method === "PATCH") && parts.length === 4 && parts[0] === "users" && parts[2] === "progress") {
    return repository.updateUserProgress(parts[1], parts[3], await readJsonBody(request));
  }

  throw notFound(`No route for ${routeKey(method, parts)}`);
}

async function main() {
  const store = await createStore().init();
  const repository = new LangAppRepository(store);

  const server = http.createServer(async (request, response) => {
    const headers = corsHeaders(request);

    if (request.method === "OPTIONS") {
      response.writeHead(204, headers);
      response.end();
      return;
    }

    try {
      const url = new URL(request.url ?? "/", `http://${request.headers.host ?? "localhost"}`);
      const parts = url.pathname.split("/").filter(Boolean).map(decodeURIComponent);

      if (request.method === "GET" && url.pathname === "/health") {
        sendJson(response, 200, { ok: true, dataSource: config.dataSource }, headers);
        return;
      }

      if (parts[0] !== "api") {
        throw notFound("Use /api routes");
      }

      const payload = await handleApi(repository, request, url, parts.slice(1));
      sendJson(response, 200, payload, headers);
    } catch (error) {
      const status = error instanceof HttpError ? error.status : 500;
      const message = status === 500 ? "Internal server error" : error.message;
      const details = error instanceof HttpError ? error.details : undefined;
      if (status === 500) {
        console.error(error);
      }
      sendJson(response, status, { error: message, details }, headers);
    }
  });

  server.listen(config.port, config.host, () => {
    console.log(`LangApp API listening on http://${config.host}:${config.port}`);
    console.log(`Data source: ${config.dataSource}`);
  });

  const shutdown = async () => {
    server.close(async () => {
      await store.close();
      process.exit(0);
    });
  };

  process.on("SIGINT", shutdown);
  process.on("SIGTERM", shutdown);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
