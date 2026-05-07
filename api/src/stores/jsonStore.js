import fs from "node:fs/promises";
import path from "node:path";

export class JsonStore {
  constructor(filePath, writeDebounceMs = 150) {
    this.filePath = filePath;
    this.writeDebounceMs = writeDebounceMs;
    this.data = null;
    this.writeTimer = null;
    this.pendingWrite = null;
  }

  async init() {
    const raw = await fs.readFile(this.filePath, "utf8");
    this.data = JSON.parse(raw);
    return this;
  }

  async close() {
    if (this.writeTimer) {
      clearTimeout(this.writeTimer);
      this.writeTimer = null;
    }
    if (this.pendingWrite) {
      await this.pendingWrite;
    }
    await this.flush();
  }

  async read() {
    return this.data;
  }

  async write(mutator) {
    const result = mutator(this.data);
    this.scheduleFlush();
    return result;
  }

  scheduleFlush() {
    if (this.writeTimer) {
      return;
    }

    this.writeTimer = setTimeout(() => {
      this.writeTimer = null;
      this.pendingWrite = this.flush().finally(() => {
        this.pendingWrite = null;
      });
    }, this.writeDebounceMs);
  }

  async flush() {
    if (!this.data) {
      return;
    }

    const tmpPath = `${this.filePath}.tmp`;
    const payload = JSON.stringify(this.data, null, 2);
    await fs.mkdir(path.dirname(this.filePath), { recursive: true });
    await fs.writeFile(tmpPath, payload, "utf8");
    await fs.rename(tmpPath, this.filePath);
  }
}
