# DeskSharing Deployment

This guide describes how to run DeskSharing with Docker Compose.

DeskSharing is a Spring Boot and Vaadin web application.  
The application runs inside the container on port `8080`.

---

## 1. Requirements

The following tools are required:

```text
Docker
Docker Compose
```

---

## 2. Configuration

Copy the example environment file:

```bash
cp .env.example .env
```

Then adjust the values in `.env`.

Example:

```env
APP_PORT=8080

POSTGRES_DB=desksharing
POSTGRES_USER=desksharing_user
POSTGRES_PASSWORD=CHANGE_ME
---

## Database migrations

DeskSharing uses Flyway to manage the PostgreSQL database schema.

Database migrations are located in:

src/main/resources/db/migration

Flyway automatically applies pending migrations when the application starts.

Hibernate only validates the database schema and does not create or modify tables automatically.

Do not edit migrations that have already been applied. Schema changes must be added as new versioned migration files, for example:

V2__example_change.sql
V3__another_change.sql

MICROSOFT_CLIENT_ID=CHANGE_ME
MICROSOFT_CLIENT_SECRET=CHANGE_ME
MICROSOFT_TENANT_ID=CHANGE_ME
```

---

## 3. Start the application

Run:

```bash
docker compose up -d --build
```

Docker Compose will build the application image and start:

```text
desksharing-app
desksharing-db
```

---

## 4. View logs

```bash
docker compose logs -f
```

---

## 5. Stop the application

```bash
docker compose down
```

---

## 6. Database

The PostgreSQL database runs as a Docker container.

Data is stored in a Docker volume:

```text
desksharing_postgres_data
```

This keeps the database data even if the containers are stopped or recreated.

---

## 7. Microsoft Entra Redirect URL

If Microsoft Login is used, add this redirect URL to your Microsoft Entra App Registration:

```text
http://localhost:8080/login/oauth2/code/microsoft
```

For production, use your own domain:

```text
https://your-domain.example/login/oauth2/code/microsoft
```

The redirect URL must match the configured registration id:

```text
microsoft
```

---

## 8. Reverse Proxy

For production, DeskSharing can run behind a reverse proxy such as Nginx.

Example:

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

---

## 9. Update

Pull the latest changes and rebuild:

```bash
git pull
docker compose down
docker compose up -d --build
docker compose logs -f
```