# Архитектура взаимодействия

## Сейчас

```text
Android UI/ViewModel
        |
FirebaseRepository
        |
Firebase Realtime Database
```

`FirebaseRepository` подписывается на пути вроде:

```text
Lessons/{level}/{lesson}/sections
Lessons/{level}/{lesson}/sections/{sectionId}/tasks
Users/{userId}
UserProgress/{userId}/{level}
```

## Целевая схема

```text
Android UI/ViewModel
        |
FirebaseRepository временно остается фасадом
        |
LangApp API
        |
JsonStore или MongoStore
        |
languageguidev2-default-rtdb-export (1).json или MongoDB
```

На первом этапе Firebase остается активным источником в Android. В `FirebaseRepository` подготовлены методы для HTTP API, чтобы позже переключить вызовы точечно: сначала секции и задания, затем профиль и прогресс, потом авторизацию.

## Слои API

```text
src/server.js
  HTTP, роутинг, CORS, обработка ошибок

src/repository.js
  бизнес-контракт LangApp: уроки, секции, задания, пользователи, прогресс

src/stores/jsonStore.js
  чтение/запись JSON-файла

src/stores/mongoStore.js
  тот же root-document контракт поверх MongoDB
```

Такой разрез оставляет HTTP-контракт стабильным. Android не должен знать, откуда API берет данные.

## Соответствие Firebase -> API

```text
Firebase path                                      API endpoint
Lessons/{level}                                   GET /api/lessons/{level}
Lessons/{level}/{lesson}                          GET /api/lessons/{level}/{lesson}
Lessons/{level}/{lesson}/sections                 GET /api/lessons/{level}/{lesson}/sections
Lessons/{level}/{lesson}/sections/{section}/tasks GET /api/lessons/{level}/{lesson}/sections/{section}/tasks
Users/{userId}                                    GET/PATCH /api/users/{userId}
UserProgress/{userId}/{level}                     GET/PUT /api/users/{userId}/progress/{level}
```

## Миграционный план

1. Поднять API в JSON-режиме и проверить `/health`, `/api/meta`, секции и задания.
2. В Android задать `API_BASE_URL` в `FirebaseRepository`.
3. Переключить чтение секций и заданий на подготовленные API-методы.
4. Добавить авторизацию для API.
5. Переключить профиль и прогресс.
6. Убрать зависимости Firebase Database из Gradle.
7. Отдельным этапом выгрузить медиа из Firebase Storage и заменить URL в JSON/MongoDB.

## Будущий API распознавания речи

Speech API добавлен отдельным сервисом в `api/neuraldiplo/api_server.py`:

```text
Android
  |-- LangApp Content API: уроки, задания, профиль, прогресс
  |-- Speech API: загрузка аудио, распознавание, оценка произношения
```

Контентный API уже не зависит от speech-части, поэтому распознавание можно добавлять позже без риска для миграции Firebase.

Android отправляет `multipart/form-data` на:

```text
POST /api/speech/check-text
```

Эталонный текст берется из `AudioRecordingTask.targetText`, `ImageRecordingTask.targetText` или `TextRecordingTask.text`. Ответ содержит `success`, `passed`, `score`, `recognizedText` и список расхождений.
