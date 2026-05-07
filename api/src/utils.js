import { badRequest } from "./errors.js";

export function orderedEntries(value) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return [];
  }

  return Object.entries(value).sort(([left], [right]) => {
    const leftNum = Number(left);
    const rightNum = Number(right);
    if (Number.isInteger(leftNum) && Number.isInteger(rightNum)) {
      return leftNum - rightNum;
    }
    return left.localeCompare(right, "ru");
  });
}

export function withId(id, value) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return { id, value };
  }
  return { id, ...value };
}

export function parseLesson(rawLesson) {
  const lesson = Number(rawLesson);
  if (!Number.isInteger(lesson) || lesson <= 0) {
    throw badRequest("Lesson must be a positive integer");
  }
  return String(lesson);
}

export function nowMs() {
  return Date.now();
}
