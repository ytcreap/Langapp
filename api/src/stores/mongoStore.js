export class MongoStore {
  constructor({ uri, dbName, collectionName }) {
    this.uri = uri;
    this.dbName = dbName;
    this.collectionName = collectionName;
    this.client = null;
    this.collection = null;
  }

  async init() {
    if (!this.uri) {
      throw new Error("MONGODB_URI is required when DATA_SOURCE=mongo");
    }

    const { MongoClient } = await import("mongodb");
    this.client = new MongoClient(this.uri);
    await this.client.connect();
    this.collection = this.client.db(this.dbName).collection(this.collectionName);

    const existing = await this.collection.findOne({ _id: "root" });
    if (!existing) {
      await this.collection.insertOne({ _id: "root", data: { Lessons: {}, Users: {}, UserProgress: {} } });
    }

    return this;
  }

  async close() {
    if (this.client) {
      await this.client.close();
    }
  }

  async read() {
    const document = await this.collection.findOne({ _id: "root" });
    return document?.data ?? { Lessons: {}, Users: {}, UserProgress: {} };
  }

  async write(mutator) {
    const current = await this.read();
    const result = mutator(current);
    await this.collection.updateOne(
      { _id: "root" },
      { $set: { data: current, updatedAt: new Date() } },
      { upsert: true }
    );
    return result;
  }
}
