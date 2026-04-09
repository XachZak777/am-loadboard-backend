# Admin Account Setup - Changes Applied

## Summary
Successfully added admin account registration functionality to the loadboard backend. The system now supports creating admin accounts with a bootstrap mechanism for the first admin and authentication-required mechanism for subsequent admins.

## Files Created

### 1. `RegisterAdminRequest.java`
**Path:** `src/main/java/am/loadboardbackend/dto/auth/RegisterAdminRequest.java`

New DTO for admin registration requests with `email` and `password` fields.

### 2. `AuthController.java` (Updated)
**Path:** `src/main/java/am/loadboardbackend/controller/AuthController.java`

**Changes:**
- Added dependency injection for `RegistrationService`
- Added new endpoint: `POST /api/auth/register-admin`
- Returns `201 CREATED` with JWT token on success

## Files Modified

### 1. `UserRepository.java`
**Path:** `src/main/java/am/loadboardbackend/repository/UserRepository.java`

**Changes:**
- Added import: `import am.loadboardbackend.model.UserRole;`
- Added method: `long countByRole(UserRole role);`
- Used to check if any admins exist in the system

### 2. `RegistrationService.java`
**Path:** `src/main/java/am/loadboardbackend/service/RegistrationService.java`

**Changes:**
- Added dependency injection for `AuthService`
- Added new method: `registerAdmin(RegisterAdminRequest request)`
- Implements bootstrap mechanism:
  - First admin: no authentication required
  - Subsequent admins: requires existing admin authentication
- Validates email uniqueness before creating admin
- Returns `LoginResponse` with JWT token

### 3. `SecurityConfig.java`
**Path:** `src/main/java/am/loadboardbackend/security/SecurityConfig.java`

**Changes:**
- Added import: `import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;`
- Added annotation: `@EnableMethodSecurity`
- Enables method-level security for `@PreAuthorize` annotations

### 4. `request.http`
**Path:** `request.http`

**Changes:**
- Added endpoint examples:
  - `POST /api/auth/register-admin` (bootstrap - no auth)
  - `POST /api/auth/register-admin` (with admin token - protected)

### 5. `fe-instructions.md`
**Path:** `fe-instructions.md`

**Changes:**
- Added comprehensive "Admin Account Setup" section at the beginning
- Documents bootstrap process for first admin
- Shows how to create additional admins
- Includes curl/HTTP examples

## How It Works

### Bootstrap Process (First Admin)
```bash
curl -X POST http://localhost:8080/api/auth/register-admin \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "SecurePassword123!"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Creating Additional Admins (Protected)
```bash
curl -X POST http://localhost:8080/api/auth/register-admin \
  -H "Authorization: Bearer <existing_admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newadmin@example.com",
    "password": "AnotherPassword456!"
  }'
```

## Testing
- ✅ All existing tests pass (3 tests)
- ✅ Project compiles without errors
- ✅ No breaking changes to existing functionality

## Business Logic
1. When registering an admin, the system checks if `ROLE_ADMIN` count is zero
2. If zero: allows registration without authentication (bootstrap)
3. If > zero: requires current user to be admin, otherwise throws error
4. Validates email uniqueness to prevent duplicates
5. Returns JWT token for immediate authentication

## Security Considerations
- Bootstrap endpoint allows first admin creation without authentication (necessary for initial setup)
- All subsequent admin creations require admin authentication
- Email validation prevents duplicate accounts
- Passwords are encoded with BCrypt
- JWT token returned for authentication

## Next Steps (Optional)
- Add frontend UI for admin registration/login
- Add admin dashboard endpoints for user management
- Add audit logging for admin actions
- Add role-based access control for sensitive endpoints
