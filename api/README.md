# LangApp API

API заменяет прямое чтение Firebase Realtime Database. По умолчанию сервис читает и записывает файл `languageguidev2-default-rtdb-export (1).json`, который уже лежит в этой папке. При необходимости тот же HTTP-контракт можно переключить на MongoDB через переменные окружения.

## Быстрый старт

```bash
cd api
npm start
```

Проверка:

```bash
curl http://localhost:3000/health
curl http://localhost:3000/api/lessons/elementary/1/sections
curl http://localhost:3000/api/lessons/elementary/1/sections/section_1/tasks
```

Для JSON-режима зависимости устанавливать не обязательно: используется только стандартный Node.js HTTP-сервер.

## Основные эндпоинты

База:

```text
GET /health
GET /api/meta
GET /api/levels
GET /api/raw?path=Lessons/elementary/1
```

Уроки:

```text
GET /api/lessons/:level
GET /api/lessons/:level/:lesson
GET /api/lessons/:level/:lesson/exists
GET /api/lessons/:level/:lesson/sections
GET /api/lessons/:level/:lesson/sections/first
GET /api/lessons/:level/:lesson/sections/:sectionId
GET /api/lessons/:level/:lesson/sections/:sectionId/tasks
GET /api/lessons/:level/:lesson/sections/:sectionId/tasks/:taskId
```

Пользователи и прогресс:

```text
GET /api/users/:userId
POST /api/users/:userId/ensure
PATCH /api/users/:userId
GET /api/users/:userId/progress/:level
PUT /api/users/:userId/progress/:level
```

Пример обновления профиля:

```bash
curl -X PATCH http://localhost:3000/api/users/test-user \
  -H "Content-Type: application/json" \
  -d "{\"fullName\":\"Test User\",\"group\":\"BPO-22-04\"}"
```

Пример обновления прогресса:

```bash
curl -X PUT http://localhost:3000/api/users/test-user/progress/elementary \
  -H "Content-Type: application/json" \
  -d "{\"progress\":5}"
```

## Контракт для Android

Списки секций и заданий возвращаются массивом, где ключ Firebase добавлен в поле `id`:

```json
[
  {
    "id": "section_1",
    "name": "...",
    "description": "...",
    "tasks": {
      "task_1": {
        "type": "MULTIPLE_CHOICE"
      }
    }
  }
]
```

Это позволяет мобильному приложению сохранить текущую модель: данные задания остаются такими же, как в Firebase, а `id` больше не нужно брать из ключа `DataSnapshot`.

## Переменные окружения

См. `.env.example`.

Ключевые настройки:

```text
PORT=3000
DATA_SOURCE=json
JSON_DB_PATH=./languageguidev2-default-rtdb-export (1).json
CORS_ORIGIN=*
```

Для MongoDB:

```text
DATA_SOURCE=mongo
MONGODB_URI=mongodb://127.0.0.1:27017
MONGODB_DB_NAME=langapp
MONGODB_COLLECTION=database
```

Импорт текущего JSON в MongoDB:

```bash
cd api
npm install
npm run mongo:import
DATA_SOURCE=mongo npm start
```

## Важно про медиа

В JSON сейчас остаются ссылки на Firebase Storage для `image`, `sound`, `audio`, `referenceAudio`. Новый API заменяет Realtime Database, но не скачивает и не хостит медиафайлы. Для полного ухода от Firebase Storage нужно отдельно выгрузить файлы и заменить URL в базе.

## Speech API

API распознавания речи лежит отдельно в `neuraldiplo`: см. `api/neuraldiplo/README_API.md`. Android отправляет туда записанный файл для `AudioRecordingTask`, `ImageRecordingTask`, `TextRecordingTask` и их set-типов.
