# Environment Variables Reference

## Production Environment Variables Required

All variables must be set before starting the application with `spring.profiles.active=prod`.

### Database Configuration

| Variable | Type | Required | Example | Notes |
|----------|------|----------|---------|-------|
| `DATASOURCE_URL` | String | ✅ Yes | `jdbc:postgresql://db.example.com:5432/loadboard` | PostgreSQL connection string |
| `DATASOURCE_USERNAME` | String | ✅ Yes | `loadboard_user` | Database user with full permissions |
| `DATASOURCE_PASSWORD` | String | ✅ Yes | `SecurePassword123!` | Strong password (min 12 chars) |

### Security & JWT

| Variable | Type | Required | Example | Notes |
|----------|------|----------|---------|-------|
| `JWT_SECRET` | String | ✅ Yes | `a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6` | Min 32 bytes for HS256. Generate: `openssl rand -hex 32` |
| `JWT_EXPIRATION_MS` | Long | ✅ Yes | `86400000` | Token lifetime in milliseconds (86400000 = 24 hours) |

### Email (SMTP)

| Variable | Type | Required | Example | Notes |
|----------|------|----------|---------|-------|
| `MAIL_USERNAME` | String | ✅ Yes | `noreply@yourdomain.com` | Gmail or SMTP server user |
| `MAIL_PASSWORD` | String | ✅ Yes | `abcd efgh ijkl mnop` | Gmail app-specific password (NOT regular password) |

### External APIs

| Variable | Type | Required | Example | Notes |
|----------|------|----------|---------|-------|
| `FMCSA_BASE_URL` | String | ✅ Yes | `https://api.fmcsa.dot.gov` | FMCSA API endpoint |
| `FMCSA_API_KEY` | String | ✅ Yes | `your-api-key-123` | Obtain from FMCSA portal |

### Frontend Configuration

| Variable | Type | Required | Example | Notes |
|----------|------|----------|---------|-------|
| `FRONTEND_BASE_URL` | String | ✅ Yes | `https://yourdomain.com` | Used in verification/reset email links |
| `ALLOWED_ORIGINS` | String | ✅ Yes | `https://yourdomain.com` | CORS allowed origins (comma-separated if multiple) |

### Cookie Security

| Variable | Type | Required | Example | Default |
|----------|------|----------|---------|---------|
| `COOKIE_SECURE` | Boolean | ❌ No | `true` | `true` |
| `COOKIE_SAME_SITE` | String | ❌ No | `None` | `None` |

**Note:** Set these to `false` and `Lax` respectively for local HTTP development only.

---

## Setting Variables in Different Environments

### Docker (Recommended for Production)

```bash
docker run \
  -e DATASOURCE_URL="jdbc:postgresql://db:5432/loadboard" \
  -e DATASOURCE_USERNAME="loadboard_user" \
  -e DATASOURCE_PASSWORD="SecurePassword123!" \
  -e JWT_SECRET="a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6" \
  -e JWT_EXPIRATION_MS="86400000" \
  -e MAIL_USERNAME="noreply@yourdomain.com" \
  -e MAIL_PASSWORD="abcd efgh ijkl mnop" \
  -e FMCSA_BASE_URL="https://api.fmcsa.dot.gov" \
  -e FMCSA_API_KEY="your-api-key-123" \
  -e FRONTEND_BASE_URL="https://yourdomain.com" \
  -e ALLOWED_ORIGINS="https://yourdomain.com" \
  -p 8080:8080 \
  loadboard-backend:latest
```

### Kubernetes (Environment Variables)

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: loadboard-backend-config
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
type: Opaque
stringData:
  DATASOURCE_USERNAME: loadboard_user
  DATASOURCE_PASSWORD: SecurePassword123!
  JWT_SECRET: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6
  MAIL_USERNAME: noreply@yourdomain.com
  MAIL_PASSWORD: abcd efgh ijkl mnop
  FMCSA_API_KEY: your-api-key-123
```

### AWS ECS Task Definition

```json
{
  "containerDefinitions": [
    {
      "name": "loadboard-backend",
      "image": "your-registry/loadboard-backend:latest",
      "environment": [
        {
          "name": "DATASOURCE_URL",
          "value": "jdbc:postgresql://db.example.com:5432/loadboard"
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
        }
      ]
    }
  ]
}
```

### .env File (Local Development ONLY)

```bash
# .env (never commit to git!)
DATASOURCE_URL=jdbc:postgresql://localhost:5432/loadboard
DATASOURCE_USERNAME=loadboard_user
DATASOURCE_PASSWORD=loadboard_pass
JWT_SECRET=test_secret_key_min_32_bytes_12345678
JWT_EXPIRATION_MS=86400000
MAIL_USERNAME=test@gmail.com
MAIL_PASSWORD=app_password
FMCSA_BASE_URL=https://api.fmcsa.dot.gov
FMCSA_API_KEY=test-api-key
FRONTEND_BASE_URL=http://localhost:5173
ALLOWED_ORIGINS=http://localhost:5173
COOKIE_SECURE=false
COOKIE_SAME_SITE=Lax
```

**Then load with:** `source .env && java -jar app.jar`

---

## Validation

### Check All Required Variables Are Set

```bash
#!/bin/bash

REQUIRED_VARS=(
  "DATASOURCE_URL"
  "DATASOURCE_USERNAME"
  "DATASOURCE_PASSWORD"
  "JWT_SECRET"
  "JWT_EXPIRATION_MS"
  "MAIL_USERNAME"
  "MAIL_PASSWORD"
  "FMCSA_BASE_URL"
  "FMCSA_API_KEY"
  "FRONTEND_BASE_URL"
  "ALLOWED_ORIGINS"
)

MISSING=()
for var in "${REQUIRED_VARS[@]}"; do
  if [ -z "${!var}" ]; then
    MISSING+=("$var")
  fi
done

if [ ${#MISSING[@]} -ne 0 ]; then
  echo "❌ Missing environment variables:"
  printf '  - %s\n' "${MISSING[@]}"
  exit 1
else
  echo "✅ All required environment variables are set"
fi
```

### Application Startup Verification

The application will automatically validate all required variables on startup with the `prod` profile.

```bash
java -Dspring.profiles.active=prod -jar loadboard-backend.jar
```

Expected output if all variables are set:
```
INFO 12345 --- [main] a.l.c.EnvironmentValidator : All required environment variables validated
INFO 12345 --- [main] o.s.b.w.e.t.TomcatWebServer : Tomcat initialized with port(s): 8080
INFO 12345 --- [main] o.s.b.w.e.t.TomcatWebServer : Tomcat started on port(s): 8080
```

---

## Security Best Practices

### JWT_SECRET Generation

```bash
# Generate a cryptographically secure random 32-byte hex string
openssl rand -hex 32

# Example output:
# a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a1b2c3d4

# Store securely in your secret management system:
# - AWS Secrets Manager
# - HashiCorp Vault
# - Azure Key Vault
# - GCP Secret Manager
```

### Password Requirements

- **Database Password:** Min 12 characters, mixed case + numbers + symbols
- **SMTP Password:** Use app-specific password (Gmail), NOT account password
- **JWT Secret:** Min 32 bytes (256 bits) for HS256

### Rotation Strategy

- **JWT_SECRET:** Rotate every 90 days. During rotation, app continues accepting old tokens until expiration.
- **Database Password:** Rotate every 30 days. Update in secret manager, then update running services.
- **MAIL_PASSWORD:** Rotate when provider recommends (Gmail: every 6 months).

---

## Troubleshooting

### "Missing required environment variables" Error

**Cause:** One or more required variables not set  
**Solution:** Check output for list of missing variables, set them, restart application

### JWT Validation Failure

**Cause:** JWT_SECRET mismatch or less than 32 bytes  
**Solution:** Regenerate secret with `openssl rand -hex 32`, ensure all instances use same secret

### Email Not Sending

**Cause:** MAIL_USERNAME or MAIL_PASSWORD incorrect  
**Solution:** Verify credentials in Gmail security settings; use app-specific password, not account password

### Database Connection Error

**Cause:** DATASOURCE_URL, DATASOURCE_USERNAME, or DATASOURCE_PASSWORD incorrect  
**Solution:** Test connection: `psql -h host -U user -d dbname`

### CORS Origin Blocked

**Cause:** Frontend URL not in ALLOWED_ORIGINS  
**Solution:** Set `ALLOWED_ORIGINS=https://yourdomain.com` (comma-separated for multiple origins)
