# NGAFID 2.0 Deployment Guide

This document outlines the step-by-step process to **deploy**, **update**, and **manage** the NGAFID 2.0 application on the production server.

---

## 🔐 1. SSH into the Server

You must have SSH access to the NGAFID beta server.

```bash
ssh {username}@ngafidbeta.rit.edu
```

Once connected, switch to the **ngafid** user:
```bash
sudo su - ngafid
```

---

## 📁 2. Navigate to the NGAFID Repository

Move to the main project directory:
```bash
cd ngafid2.0
```

Update the local codebase:
```bash
git pull
```

Checkout a specific branch:
```bash
git checkout <branch-name>
```

---

## 🧱 3. Build the Frontend (React)

Navigate to the frontend directory:
```bash
cd ngafid2.0/ngafid-frontend
```

Build the production-ready frontend:
```bash
npm run build
```

This generates the optimized React build under:
```
ngafid-frontend/build/
```

The server will automatically serve this build.

---

## ⚙️ 4. Restart NGAFID Services

After pulling the latest changes and building the frontend, restart the NGAFID services to apply updates.

### Stop all NGAFID services:
```bash
systemctl --user stop ngafid-web ngafid-upload ngafid-event ngafid-observer ngafid-email ngafid-charts
```

### Start all NGAFID services:
```bash
systemctl --user start ngafid-web ngafid-upload ngafid-event ngafid-observer ngafid-email ngafid-charts
```

### (Optional) Restart a specific service:
```bash
systemctl --user restart ngafid-web
```

### Check service status:
```bash
systemctl --user status ngafid-web ngafid-upload ngafid-event ngafid-observer ngafid-email ngafid-charts
```

Each service corresponds to a backend component:

| Service Name       | Description                              |
|--------------------|------------------------------------------|
| `ngafid-web`       | Web server (Javalin backend)             |
| `ngafid-upload`    | Kafka upload consumer                    |
| `ngafid-event`     | Kafka event consumer                     |
| `ngafid-observer`  | Kafka event observer                     |
| `ngafid-email`     | Email notification service               |
| `ngafid-charts`    | Chart generation / analytics service     |

---

## 🗄️ 5. Accessing the Database

To connect to the MySQL database:

```bash
mysql -u <username> -p
```

The database **username** and **password** are stored in:
```
ngafid2.0/ngafid-db/src/liquibase.properties
```
in the server

Once logged in, select the NGAFID database:
```sql
USE ngafid;
```

You can then run SQL queries as needed, for example:
```sql
SHOW TABLES;
SELECT COUNT(*) FROM flights;
```

---

## 🧩 Notes


- You can verify that the deployment succeeded by visiting:
  ```
  http://ngafidbeta.rit.edu:8181
  ```
- The charts are served at:
  ```
  http://ngafidbeta.rit.edu:8187
  ```

---

