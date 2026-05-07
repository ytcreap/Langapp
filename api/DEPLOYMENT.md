# Развертывание LangApp API

## Требования

- Node.js 20 или выше.
- Открытый порт для HTTP, обычно `3000`.
- Для JSON-режима: доступ на чтение и запись к файлу базы.
- Для MongoDB-режима: MongoDB 6/7 или совместимый managed-сервис.

## Вариант 1. JSON-файл на сервере

1. Скопируйте папку `api` на сервер.
2. Убедитесь, что рядом с кодом лежит файл `languageguidev2-default-rtdb-export (1).json`.
3. Создайте `.env` по примеру:

```bash
cp .env.example .env
```

4. Запустите:

```bash
npm start
```

5. Проверьте:

```bash
curl http://SERVER_IP:3000/health
```

Для фонового запуска удобно использовать `pm2`:

```bash
npm install -g pm2
pm2 start src/server.js --name langapp-api
pm2 save
pm2 startup
```

## Вариант 2. JSON-файл через systemd

Создайте `/etc/systemd/system/langapp-api.service`:

```ini
[Unit]
Description=LangApp API
After=network.target

[Service]
WorkingDirectory=/opt/langapp/api
Environment=PORT=3000
Environment=DATA_SOURCE=json
Environment=JSON_DB_PATH=./languageguidev2-default-rtdb-export (1).json
ExecStart=/usr/bin/node src/server.js
Restart=always
RestartSec=5
User=www-data
Group=www-data

[Install]
WantedBy=multi-user.target
```

Затем:

```bash
sudo systemctl daemon-reload
sudo systemctl enable langapp-api
sudo systemctl start langapp-api
sudo systemctl status langapp-api
```

## Вариант 3. MongoDB

1. Установите зависимости:

```bash
cd api
npm install
```

2. Импортируйте JSON:

```bash
export MONGODB_URI=mongodb://127.0.0.1:27017
npm run mongo:import
```

3. Запустите API в MongoDB-режиме:

```bash
export DATA_SOURCE=mongo
export MONGODB_URI=mongodb://127.0.0.1:27017
npm start
```

В MongoDB сервис хранит весь экспорт в документе `{ _id: "root", data: ... }`. Это максимально близко к Realtime Database и упрощает миграцию. Позже можно нормализовать коллекции `lessons`, `sections`, `tasks`, `users`, если появятся админка, поиск или частые частичные обновления.

## Reverse proxy

Пример Nginx:

```nginx
server {
    listen 80;
    server_name api.example.com;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

После подключения HTTPS укажите в Android `https://api.example.com`.

## Обновление базы

JSON-режим пишет изменения профилей и прогресса обратно в JSON-файл. Перед ручной заменой файла:

1. Остановите сервис.
2. Сделайте backup текущего JSON.
3. Замените файл.
4. Запустите сервис.

## Безопасность

Сейчас API подготовлено как замена источника данных, а не как полноценная auth-система. Перед публичным продакшеном нужно добавить:

- проверку JWT или серверную авторизацию;
- ограничение `PATCH /api/users/:userId`, чтобы пользователь менял только свой профиль;
- rate limit;
- HTTPS;
- backup JSON/MongoDB.
