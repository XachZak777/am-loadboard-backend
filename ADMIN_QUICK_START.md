# Quick Start: Admin Account Setup

## Step 1: Create First Admin (Bootstrap)
No authentication needed for the first admin.

```bash
curl -X POST http://localhost:8080/api/auth/register-admin \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "YourSecurePassword123!"
  }'
```

Save the returned `token` for later use.

## Step 2: Login with Admin Account
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "YourSecurePassword123!"
  }'
```

## Step 3: Create Additional Admins (Optional)
```bash
curl -X POST http://localhost:8080/api/auth/register-admin \
  -H "Authorization: Bearer <your_admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin2@example.com",
    "password": "AnotherSecurePassword456!"
  }'
```

## Testing with REST Client (VS Code)
Use the examples in `request.http`:
- Register first admin: Line 195
- Register additional admin: Line 205

## Error Cases
| Status | Error | Solution |
|--------|-------|----------|
| 400 | Email already registered | Use a different email |
| 403 | Only admins can create new admins | Provide valid admin token |
| 500 | Any other error | Check server logs |

## Notes
- JWT tokens expire based on your JwtUtil configuration
- First admin registration bypasses all authentication checks
- Subsequent admins require valid admin token in Authorization header
- Passwords are BCrypt-encoded and cannot be retrieved

See `fe-instructions.md` for complete integration guide.
