# Azure Deployment Guide - Team Scheduler

## ✅ Successful Deployment Completed

Date: 2026-06-12

---

## Prerequisites

- Azure CLI installed and logged in
- Docker Desktop running
- Docker images pushed to container registry
- PostgreSQL client tools installed

---

## Step-by-Step Deployment

### STEP 1: Create Resource Group

```bash
az group create --name team-scheduler-rg --location eastus
```

**Note:** `eastus` has restrictions. Use `westus` if needed.

---

### STEP 2: Create Azure Container Registry

```bash
az acr create --resource-group team-scheduler-rg --name teamschedulerregistry --sku Basic
```

**Login Server:** `teamschedulerregistry.azurecr.io`

---

### STEP 3: Enable Admin Access

```bash
az acr update --name teamschedulerregistry --admin-enabled true
az acr login --name teamschedulerregistry
```

---

### STEP 4: Build and Push Docker Images

Build backend:
```bash
docker build -t teamschedulerregistry.azurecr.io/team-scheduler-backend:latest .
docker push teamschedulerregistry.azurecr.io/team-scheduler-backend:latest
```

Build frontend:
```bash
docker build -t teamschedulerregistry.azurecr.io/team-scheduler-frontend:latest ./frontend
docker push teamschedulerregistry.azurecr.io/team-scheduler-frontend:latest
```

---

### STEP 5: Get Registry Credentials

```bash
az acr credential show --name teamschedulerregistry --query "[username,passwords[0].value]" -o tsv
```

**Output:**
- Username: `teamschedulerregistry`
- Password: `[MASKED_PASSWORD]`

---

### STEP 6: Create PostgreSQL Database

```bash
az postgres flexible-server create \
  --resource-group team-scheduler-rg \
  --name team-scheduler-db \
  --location westus \
  --admin-user adminuser \
  --admin-password "[MASKED_PASSWORD]" \
  --sku-name Standard_B1ms \
  --storage-size 32 \
  --tier Burstable
```

**Connection Details:**
- Host: `team-scheduler-db.postgres.database.azure.com`
- Admin User: `adminuser`
- Admin Password: `[MASKED_PASSWORD]`

---

### STEP 7: Initialize Database

Connect to PostgreSQL:
```bash
psql -h team-scheduler-db.postgres.database.azure.com -U adminuser -d postgres
```

Create database and user:
```sql
CREATE DATABASE team_scheduler;
CREATE USER scheduler_user WITH PASSWORD '[MASKED_PASSWORD]';
GRANT ALL PRIVILEGES ON DATABASE team_scheduler TO scheduler_user;
ALTER DATABASE team_scheduler OWNER TO scheduler_user;
```

Exit: `\q`

---

### STEP 8: Fix Schema Permissions

Connect to the new database:
```bash
psql -h team-scheduler-db.postgres.database.azure.com -U scheduler_user -d team_scheduler
```

Grant schema permissions:
```sql
GRANT ALL PRIVILEGES ON SCHEMA public TO scheduler_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO scheduler_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO scheduler_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO scheduler_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO scheduler_user;
```

Exit: `\q`

---

### STEP 9: Register Container Instance Provider

```bash
az provider register --namespace Microsoft.ContainerInstance
```

---

### STEP 10: Deploy Backend Container

```bash
az container create \
  --resource-group team-scheduler-rg \
  --name team-scheduler-backend \
  --image teamschedulerregistry.azurecr.io/team-scheduler-backend:latest \
  --cpu 1 \
  --memory 1 \
  --registry-login-server teamschedulerregistry.azurecr.io \
  --registry-username teamschedulerregistry \
  --registry-password [MASKED_PASSWORD] \
  --ports 8080 \
  --dns-name-label team-scheduler-backend \
  --location westus \
  --os-type Linux \
  --environment-variables \
    SPRING_DATASOURCE_URL="jdbc:postgresql://team-scheduler-db.postgres.database.azure.com:5432/team_scheduler" \
    SPRING_DATASOURCE_USERNAME="scheduler_user" \
    SPRING_DATASOURCE_PASSWORD="[MASKED_PASSWORD]" \
    SPRING_PROFILES_ACTIVE="prod"
```

**Backend URL:** `http://team-scheduler-backend.westus.azurecontainer.io:8080`

---

### STEP 11: Add PostgreSQL Firewall Rules

Allow Azure services:
```bash
az postgres flexible-server firewall-rule create \
  --resource-group team-scheduler-rg \
  --name team-scheduler-db \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0
```

---

### STEP 12: Deploy Frontend Container

```bash
az container create \
  --resource-group team-scheduler-rg \
  --name team-scheduler-frontend \
  --image teamschedulerregistry.azurecr.io/team-scheduler-frontend:latest \
  --cpu 1 \
  --memory 1 \
  --registry-login-server teamschedulerregistry.azurecr.io \
  --registry-username teamschedulerregistry \
  --registry-password [MASKED_PASSWORD] \
  --ports 3000 \
  --dns-name-label team-scheduler-frontend \
  --location westus \
  --os-type Linux \
  --environment-variables \
    SPRING_API="http://team-scheduler-backend.westus.azurecontainer.io:8080/api"
```

**Frontend URL:** `http://team-scheduler-frontend.westus.azurecontainer.io:3000`

---

## ✅ Production Deployment Summary

| Component | HTTP URL | Status |
|-----------|----------|--------|
| **Frontend** | http://team-scheduler-frontend.eastus.azurecontainer.io:3000 | ✅ Running |
| **Backend** | http://team-scheduler-backend.eastus.azurecontainer.io:8080 | ✅ Running |
| **Database** | team-scheduler-db.postgres.database.azure.com | ✅ Running |

---

## 🔒 HTTPS Configuration (New)

### Setup SSL Certificate

Create self-signed SSL certificate:
```bash
openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 365 -nodes -subj "/CN=team-scheduler.eastus.azurecontainer.io"
openssl pkcs12 -export -out cert.pfx -inkey key.pem -in cert.pem -password pass:TeamSched@2026#SecureHTTPS!
```

### Deploy nginx Reverse Proxy with HTTPS

Create `nginx.conf`:
```nginx
events {
    worker_connections 1024;
}

http {
    upstream backend {
        server team-scheduler-backend.eastus.azurecontainer.io:8080;
    }

    upstream frontend {
        server team-scheduler-frontend.eastus.azurecontainer.io:3000;
    }

    server {
        listen 443 ssl;
        server_name _;

        ssl_certificate /etc/nginx/certs/cert.pem;
        ssl_certificate_key /etc/nginx/certs/key.pem;

        location /api/ {
            proxy_pass http://backend/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
        }

        location / {
            proxy_pass http://frontend/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
        }
    }

    server {
        listen 80;
        server_name _;
        return 301 https://$host$request_uri;
    }
}
```

Create `Dockerfile.nginx`:
```dockerfile
FROM nginx:alpine

RUN mkdir -p /etc/nginx/certs
COPY nginx.conf /etc/nginx/nginx.conf
COPY cert.pem /etc/nginx/certs/cert.pem
COPY key.pem /etc/nginx/certs/key.pem

RUN chmod 644 /etc/nginx/certs/cert.pem && chmod 644 /etc/nginx/certs/key.pem

EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]
```

Build and push nginx image:
```bash
docker build -t teamschedulerregistry.azurecr.io/team-scheduler-nginx:latest -f Dockerfile.nginx .
docker push teamschedulerregistry.azurecr.io/team-scheduler-nginx:latest
```

Deploy nginx container:
```bash
az container create --resource-group team-scheduler-rg --name team-scheduler-nginx --image teamschedulerregistry.azurecr.io/team-scheduler-nginx:latest --cpu 1 --memory 1 --registry-login-server teamschedulerregistry.azurecr.io --registry-username teamschedulerregistry --registry-password $(az acr credential show --name teamschedulerregistry --query passwords[0].value -o tsv) --ports 80 443 --dns-name-label team-scheduler-nginx --location eastus --os-type Linux
```

### HTTPS URLs

After nginx deployment completes:
- **Frontend (HTTPS):** `https://team-scheduler-nginx.eastus.azurecontainer.io`
- **Backend API (HTTPS):** `https://team-scheduler-nginx.eastus.azurecontainer.io/api`
- **Public IP:** Check nginx container details

⚠️ **Note:** Self-signed certificate will show browser warnings. Replace with real certificate from Let's Encrypt or your CA for production.

---

## Test Users (Seeded in Database)

- **admin** / `admin` (ADMIN role)
- **manager_a** / `password` (MANAGER role)
- **dev_one** / `password` (L3_DEVELOPERS role)

---

## Key Credentials (KEEP SECURE)

| Component | Username | Password |
|-----------|----------|----------|
| Azure Container Registry | `teamschedulerregistry` | `[MASKED]` |
| PostgreSQL Admin | `adminuser` | `[MASKED]` |
| PostgreSQL App User | `scheduler_user` | `[MASKED]` |

---

## Important Notes

1. **Firewall Rules:** Allow Azure services to access PostgreSQL (0.0.0.0 - 0.0.0.0)
2. **Schema Permissions:** Must grant public schema permissions to `scheduler_user`
3. **Environment Variables:** Frontend needs correct `SPRING_API` URL for backend connectivity
4. **Database Initialization:** Hibernate auto-creates tables on first startup
5. **Region:** West US works better than East US for free tier quota

---

## Monitoring & Maintenance

### View Container Logs

Backend:
```bash
az container logs --resource-group team-scheduler-rg --name team-scheduler-backend
```

Frontend:
```bash
az container logs --resource-group team-scheduler-rg --name team-scheduler-frontend
```

### Restart Containers

Backend:
```bash
az container restart --resource-group team-scheduler-rg --name team-scheduler-backend
```

Frontend:
```bash
az container restart --resource-group team-scheduler-rg --name team-scheduler-frontend
```

### Delete Resources

```bash
az group delete --name team-scheduler-rg --yes
```

---

## Troubleshooting

### Backend can't connect to PostgreSQL
- Check firewall rules: `az postgres flexible-server firewall-rule list -n team-scheduler-db -g team-scheduler-rg`
- Verify schema permissions are granted
- Restart backend container

### Frontend returns 404 errors
- Verify `SPRING_API` environment variable is set correctly
- Check backend is running: `curl http://team-scheduler-backend.westus.azurecontainer.io:8080/`
- Restart frontend container

### Login fails with "invalid credentials"
- Verify user exists in database: `SELECT * FROM users;`
- Passwords must be bcrypt-hashed (not plain text)
- Use seeded test users (admin/admin)

---

## Deployment Complete! 🎉

Your application is successfully deployed and running on Azure!
