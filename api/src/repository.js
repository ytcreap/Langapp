import { badRequest, notFound } from "./errors.js";
import { nowMs, orderedEntries, parseLesson, withId } from "./utils.js";

function lessonsRoot(data) {
  return data.Lessons ?? {};
}

function usersRoot(data) {
  data.Users ??= {};
  return data.Users;
}

function progressRoot(data) {
  data.UserProgress ??= {};
  return data.UserProgress;
}

function getLevel(data, level) {
  return lessonsRoot(data)[level];
}

function getLesson(data, level, lesson) {
  return getLevel(data, level)?.[parseLesson(lesson)];
}

function getSection(data, level, lesson, sectionId) {
  return getLesson(data, level, lesson)?.sections?.[sectionId];
}

function normalizeProgressValue(value) {
  if (typeof value === "number") {
    return value;
  }

  if (value && typeof value === "object") {
    if (typeof value.progress === "number") {
      return value.progress;
    }
    if (typeof value.completedLessons === "number") {
      return value.completedLessons;
    }
  }

  return 0;
}

export class LangAppRepository {
  constructor(store) {
    this.store = store;
  }

  async meta() {
    const data = await this.store.read();
    const lessons = lessonsRoot(data);
    return {
      source: "langapp-api",
      levels: orderedEntries(lessons).map(([id, levelData]) => ({
        id,
        lessonsNumber: Number(levelData?.lessons_number ?? 0)
      }))
    };
  }

  async getLevels() {
    const data = await this.store.read();
    return orderedEntries(lessonsRoot(data)).map(([id, levelData]) => ({
      id,
      lessonsNumber: Number(levelData?.lessons_number ?? 0)
    }));
  }

  async getLessons(level) {
    const data = await this.store.read();
    const levelData = getLevel(data, level);
    if (!levelData) {
      throw notFound(`Level '${level}' not found`);
    }

    return orderedEntries(levelData)
      .filter(([id]) => id !== "lessons_number")
      .map(([id, lesson]) => withId(id, lesson));
  }

  async getLesson(level, lesson) {
    const data = await this.store.read();
    const lessonData = getLesson(data, level, lesson);
    if (!lessonData) {
      throw notFound(`Lesson '${level}/${lesson}' not found`);
    }
    return withId(parseLesson(lesson), lessonData);
  }

  async lessonExists(level, lesson) {
    const data = await this.store.read();
    return { exists: Boolean(getLesson(data, level, lesson)) };
  }

  async getSections(level, lesson) {
    const data = await this.store.read();
    const lessonData = getLesson(data, level, lesson);
    if (!lessonData) {
      return [];
    }

    return orderedEntries(lessonData.sections).map(([id, section]) => withId(id, section));
  }

  async getFirstSection(level, lesson) {
    const sections = await this.getSections(level, lesson);
    return sections[0] ?? null;
  }

  async getSection(level, lesson, sectionId) {
    const data = await this.store.read();
    const section = getSection(data, level, lesson, sectionId);
    if (!section) {
      throw notFound(`Section '${sectionId}' not found`);
    }
    return withId(sectionId, section);
  }

  async getTasks(level, lesson, sectionId) {
    const data = await this.store.read();
    const section = getSection(data, level, lesson, sectionId);
    if (!section) {
      return [];
    }

    return orderedEntries(section.tasks).map(([id, task]) => withId(id, task));
  }

  async getTask(level, lesson, sectionId, taskId) {
    const data = await this.store.read();
    const task = getSection(data, level, lesson, sectionId)?.tasks?.[taskId];
    if (!task) {
      throw notFound(`Task '${taskId}' not found`);
    }
    return withId(taskId, task);
  }

  async getUser(userId) {
    const data = await this.store.read();
    const user = data.Users?.[userId];
    if (!user) {
      return null;
    }
    return withId(userId, user);
  }

  async ensureUser(userId, payload) {
    if (!userId) {
      throw badRequest("userId is required");
    }

    return this.store.write((data) => {
      const users = usersRoot(data);
      if (!users[userId]) {
        const email = String(payload.email ?? "");
        users[userId] = {
          email,
          fullName: payload.fullName ?? email.split("@")[0] ?? "",
          group: payload.group ?? "Not specified",
          createdAt: nowMs()
        };
      }

      users[userId].lastLogin ??= nowMs();
      return withId(userId, users[userId]);
    });
  }

  async updateUser(userId, payload) {
    if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
      throw badRequest("JSON object body is required");
    }

    return this.store.write((data) => {
      const users = usersRoot(data);
      users[userId] ??= {};
      users[userId] = {
        ...users[userId],
        ...payload,
        updatedAt: nowMs()
      };
      return withId(userId, users[userId]);
    });
  }

  async getUserProgress(userId, level) {
    const data = await this.store.read();
    const value = data.UserProgress?.[userId]?.[level];
    return { userId, level, progress: normalizeProgressValue(value), raw: value ?? 0 };
  }

  async updateUserProgress(userId, level, payload) {
    const progress = typeof payload?.progress === "number" ? payload.progress : Number(payload?.progress);
    if (!Number.isFinite(progress)) {
      throw badRequest("progress must be a number");
    }

    return this.store.write((data) => {
      const root = progressRoot(data);
      root[userId] ??= {};
      root[userId][level] = progress;
      return { userId, level, progress };
    });
  }

  async raw(path) {
    const data = await this.store.read();
    if (!path) {
      return data;
    }

    const result = String(path)
      .split("/")
      .filter(Boolean)
      .reduce((node, part) => (node && typeof node === "object" ? node[part] : undefined), data);

    if (result === undefined) {
      throw notFound(`Path '${path}' not found`);
    }

    return result;
  }
}
