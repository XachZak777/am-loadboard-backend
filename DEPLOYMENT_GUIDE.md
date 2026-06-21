# Deployment Guide — Vehicle Transportation Load Board Backend

**Version:** 1.0  
**Date:** May 14, 2026  
**Status:** Production Ready

---

## Quick Start

### Prerequisites

- Java 21 (eclipse-temurin:21-jre or OpenJDK 21)
- PostgreSQL 16+
- Docker & Docker Compose (optional, for containerized deployment)
- Maven 3.9+ (for building from source)

### 1. Build the Application

```bash
# Clone and navigate to backend directory
cd loadboard-backend

# Build JAR (production profile)
mvn clean package -DskipTests -Pprod

# Output: target/loadboard-backend-0.0.1-SNAPSHOT.jar
```

### 2. Prepare PostgreSQL Database

```bash
# Create database and user
psql -U postgres -c "CREATE DATABASE loadboard;"
psql -U postgres -c "CREATE USER loadboard_user WITH PASSWORD 'your-secure-password';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE loadboard TO loadboard_user;"

# Apply schema
psql -h localhost -U loadboard_user -d loadboard -f src/main/resources/migrate/final-ddl.sql

# Apply patches
psql -h localhost -U loadboard_user -d loadboard -f src/main/resources/migrate/patch.sql
psql -h localhost -U loadboard_user -d loadboard -f src/main/resources/migrate/data.sql
```

### 3. Set Environment Variables

```bash
export DATASOURCE_URL="jdbc:postgresql://localhost:5432/loadboard"
export DATASOURCE_USERNAME="loadboard_user"
export DATASOURCE_PASSWORD="your-secure-password"
export JWT_SECRET="$(openssl rand -hex 32)"
export JWT_EXPIRATION_MS="86400000"
export MAIL_USERNAME="noreply@yourdomain.com"
export MAIL_PASSWORD="app-specific-password"
export FMCSA_BASE_URL="https://api.fmcsa.dot.gov"
export FMCSA_API_KEY="your-api-key"
export FRONTEND_BASE_URL="https://yourdomain.com"
export ALLOWED_ORIGINS="https://yourdomain.com"
```

### 4. Run Application

```bash
# Standalone JAR
java -Dspring.profiles.active=prod -jar target/loadboard-backend-0.0.1-SNAPSHOT.jar

# With VM arguments
java \
  -Dspring.profiles.active=prod \
  -Xmx512m \
  -Xms256m \
  -XX:+UseG1GC \
  -jar target/loadboard-backend-0.0.1-SNAPSHOT.jar
```

**Expected Output:**
```
2026-05-14T10:30:00.000+00:00  INFO 12345 --- [main] o.s.b.w.e.t.TomcatWebServer : Tomcat started on port(s): 8080
2026-05-14T10:30:00.500+00:00  INFO 12345 --- [main] a.l.LoadboardBackendApplication : Started LoadboardBackendApplication
```

### 5. Verify Health Check

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

---

## Docker Deployment

### Build Docker Image

```bash
# From project root
docker build -t loadboard-backend:latest -f loadboard-backend/Dockerfile .

# Tag for registry
docker tag loadboard-backend:latest your-registry.azurecr.io/loadboard-backend:latest
```

### Run with Docker Compose (Local Dev)

```bash
cd loadboard-backend

# Start services
docker-compose up -d

# Verify
docker-compose ps
docker-compose logs -f backend
```

### Run as Container (Production)

```bash
docker run \
  -d \
  --name loadboard-backend \
  --network custom-network \
  -e DATASOURCE_URL="jdbc:postgresql://postgres:5432/loadboard" \
  -e DATASOURCE_USERNAME="loadboard_user" \
  -e DATASOURCE_PASSWORD="your-secure-password" \
  -e JWT_SECRET="<32-byte-hex-string>" \
  -e JWT_EXPIRATION_MS="86400000" \
  -e MAIL_USERNAME="noreply@yourdomain.com" \
  -e MAIL_PASSWORD="app-password" \
  -e FMCSA_BASE_URL="https://api.fmcsa.dot.gov" \
  -e FMCSA_API_KEY="your-api-key" \
  -e FRONTEND_BASE_URL="https://yourdomain.com" \
  -e ALLOWED_ORIGINS="https://yourdomain.com" \
  -p 8080:8080 \
  --health-cmd="curl -f http://localhost:8080/actuator/health || exit 1" \
  --health-interval=30s \
  --health-timeout=10s \
  --health-retries=3 \
  --restart unless-stopped \
  your-registry.azurecr.io/loadboard-backend:latest
```

### Kubernetes Deployment

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: loadboard-backend-config
  namespace: default
data:
  DATASOURCE_URL: "jdbc:postgresql://postgres-service:5432/loadboard"
  JWT_EXPIRATION_MS: "86400000"
  FMCSA_BASE_URL: "https://api.fmcsa.dot.gov"
  FRONTEND_BASE_URL: "https://yourdomain.com"
  ALLOWED_ORIGINS: "https://yourdomain.com"

---
apiVersion: v1
kind: Secret
metadata:
  name: loadboard-backend-secrets
  namespace: default
type: Opaque
stringData:
  DATASOURCE_USERNAME: loadboard_user
  DATASOURCE_PASSWORD: your-secure-password
  JWT_SECRET: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6
  MAIL_USERNAME: noreply@yourdomain.com
  MAIL_PASSWORD: app-password
  FMCSA_API_KEY: your-api-key

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: loadboard-backend
  namespace: default
spec:
  replicas: 2
  selector:
    matchLabels:
      app: loadboard-backend
  template:
    metadata:
      labels:
        app: loadboard-backend
    spec:
      containers:
      - name: backend
        image: your-registry.azurecr.io/loadboard-backend:latest
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 8080
          name: http
        envFrom:
        - configMapRef:
            name: loadboard-backend-config
        - secretRef:
            name: loadboard-backend-secrets
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: loadboard-backend
  namespace: default
spec:
  type: ClusterIP
  selector:
    app: loadboard-backend
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
    name: http
```

---

## AWS ECS Deployment

### 1. Create ECR Repository

```bash
aws ecr create-repository \
  --repository-name loadboard-backend \
  --region us-east-1
```

### 2. Push Docker Image

```bash
# Get login credentials
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Tag and push
docker tag loadboard-backend:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/loadboard-backend:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/loadboard-backend:latest
```

### 3. Create ECS Task Definition

```json
{
  "family": "loadboard-backend",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [
    {
      "name": "loadboard-backend",
      "image": "<account-id>.dkr.ecr.us-east-1.amazonaws.com/loadboard-backend:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "hostPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "DATASOURCE_URL",
          "value": "jdbc:postgresql://loadboard-db.us-east-1.rds.amazonaws.com:5432/loadboard"
        },
        {
          "name": "JWT_EXPIRATION_MS",
          "value": "86400000"
        }
      ],
      "secrets": [
        {
          "name": "DATASOURCE_USERNAME",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789:secret:loadboard/db-user"
        },
        {
          "name": "DATASOURCE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789:secret:loadboard/db-password"
        },
        {
          "name": "JWT_SECRET",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789:secret:loadboard/jwt-secret"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/loadboard-backend",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### 4. Create ECS Service

```bash
aws ecs create-service \
  --cluster loadboard \
  --service-name loadboard-backend \
  --task-definition loadboard-backend \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx,subnet-yyy],securityGroups=[sg-xxx],assignPublicIp=DISABLED}" \
  --load-balancers targetGroupArn=arn:aws:elasticloadbalancing:us-east-1:123456789:targetgroup/loadboard-backend/xxx,containerName=loadboard-backend,containerPort=8080
```

---

## Azure Container Instances (ACI) Deployment

```bash
az container create \
  --resource-group loadboard-rg \
  --name loadboard-backend \
  --image your-registry.azurecr.io/loadboard-backend:latest \
  --cpu 1 \
  --memory 1 \
  --ports 8080 \
  --environment-variables \
    DATASOURCE_URL="jdbc:postgresql://loadboard-db.postgres.database.azure.com:5432/loadboard" \
    JWT_EXPIRATION_MS="86400000" \
  --secure-environment-variables \
    DATASOURCE_USERNAME="loadboard_user@server" \
    DATASOURCE_PASSWORD="$DB_PASSWORD" \
    JWT_SECRET="$JWT_SECRET" \
  --restart-policy OnFailure \
  --registry-login-server your-registry.azurecr.io \
  --registry-username $ACR_USERNAME \
  --registry-password $ACR_PASSWORD
```

---

## GCP Cloud Run Deployment

```bash
# Build and push
gcloud builds submit \
  --tag gcr.io/PROJECT_ID/loadboard-backend \
  --dockerfile loadboard-backend/Dockerfile

# Deploy to Cloud Run
gcloud run deploy loadboard-backend \
  --image gcr.io/PROJECT_ID/loadboard-backend \
  --platform managed \
  --region us-central1 \
  --memory 512Mi \
  --cpu 1 \
  --timeout 3600 \
  --set-env-vars="DATASOURCE_URL=jdbc:postgresql://sql-instance/loadboard,JWT_EXPIRATION_MS=86400000" \
  --set-secrets="DATASOURCE_PASSWORD=loadboard-db-password:latest,JWT_SECRET=jwt-secret:latest" \
  --allow-unauthenticated
```

---

## Rolling Updates & Rollback

### Kubernetes Rolling Update

```bash
# Update image
kubectl set image deployment/loadboard-backend \
  backend=your-registry.azurecr.io/loadboard-backend:v1.1 \
  --record

# Monitor rollout
kubectl rollout status deployment/loadboard-backend

# Rollback if issues
kubectl rollout undo deployment/loadboard-backend
```

### Docker Compose Update

```bash
# Pull latest image
docker-compose pull

# Restart service
docker-compose up -d

# View logs
docker-compose logs -f backend
```

---

## Monitoring & Logging

### Health Check

```bash
# Local
curl http://localhost:8080/actuator/health

# Remote (if exposed via ingress)
curl https://api.yourdomain.com/actuator/health
```

### View Logs

**Docker:**
```bash
docker logs -f loadboard-backend
```

**Kubernetes:**
```bash
kubectl logs -f deployment/loadboard-backend
```

**File-based (if running standalone):**
```bash
tail -f logs/application.log
```

### Key Metrics to Monitor

- **CPU Usage:** Should be < 70% under normal load
- **Memory Usage:** Should be < 60% of allocated
- **Database Connections:** Monitor HikariCP pool (max 10)
- **Response Times:** P95 < 500ms, P99 < 1s
- **Error Rate:** Should be < 0.1%

---

## Troubleshooting

### Application Won't Start

**Error:** `Missing required environment variables`

**Solution:**
```bash
# Verify all required variables are set
echo $DATASOURCE_URL
echo $JWT_SECRET
# Set missing variables and restart
```

### Database Connection Error

**Error:** `Connection refused` or `Failed to initialize pool`

**Solution:**
```bash
# Test PostgreSQL connectivity
psql -h localhost -U loadboard_user -d loadboard -c "SELECT 1"

# Verify environment variables
echo $DATASOURCE_URL
echo $DATASOURCE_USERNAME

# Check PostgreSQL is running
pg_isready -h localhost -p 5432
```

### JWT Secret Too Short

**Error:** `JWT_SECRET must be at least 32 bytes for HS256 security`

**Solution:**
```bash
# Generate new secret
export JWT_SECRET="$(openssl rand -hex 32)"

# Verify length (should be 64 characters for hex)
echo -n "$JWT_SECRET" | wc -c  # Output: 64
```

### High Memory Usage

**Solution:**
```bash
# Increase JVM heap
java -Xmx1g -Xms512m -Dspring.profiles.active=prod -jar app.jar

# OR adjust container limits in Kubernetes/Docker
```

### Slow Queries

**Solution:**
1. Enable slow query logging in PostgreSQL:
   ```sql
   ALTER SYSTEM SET log_min_duration_statement = 1000;  -- 1 second
   SELECT pg_reload_conf();
   ```
2. Check application logs for query times
3. Add database indexes as needed

---

## Post-Deployment Checklist

- [ ] Verify application health: `/actuator/health` returns UP
- [ ] Verify database connectivity: Can query tables
- [ ] Verify JWT token generation: Login endpoint works
- [ ] Verify email sending: Test password reset flow
- [ ] Verify CORS: Frontend can communicate with backend
- [ ] Verify file uploads: Documents can be uploaded and downloaded
- [ ] Monitor error logs: No exceptions in first hour
- [ ] Load test: Run synthetic traffic to verify performance
- [ ] Security scan: Verify HTTPS only, no exposed endpoints
- [ ] Backup database: Create backup before production load

---

## Support & Maintenance

### Regular Maintenance Tasks

| Task | Frequency | Command |
|------|-----------|---------|
| Database backups | Daily | `pg_dump -h host -U user -d loadboard > backup.sql` |
| Log rotation | Weekly | Handled by log shipper or OS |
| Dependency updates | Monthly | `mvn versions:display-dependency-updates` |
| Security patches | As needed | Monitor CVE feeds, update immediately |
| Performance review | Weekly | Check metrics and error rates |

### Emergency Contacts

- **Database Admin:** [email]
- **DevOps Lead:** [email]
- **Security Team:** [email]
- **On-Call Engineer:** [rotation schedule]

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-05-14 | Initial production deployment guide |

For questions or issues, contact the DevOps team or refer to the README.md in the project repository.
