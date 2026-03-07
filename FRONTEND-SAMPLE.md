Frontend integration guide — Loadboard Backend

This file describes the minimal endpoints, request/response shapes, and UX flow the frontend needs to implement the validation -> review -> save workflow.

Overview
- Validate (external FMCSA lookup): frontend calls backend validate endpoints to fetch FMCSA data and receives a preview (cached server-side) with a validationId.
- Review: show the returned parsed fields to the user for manual verification.
- Save (register): frontend calls save endpoint with validationId + user credentials; backend persists the record and returns a JWT.

Cache behavior
- Validation preview data is stored in a server-side cache with TTL = 15 minutes.
- After a successful save (registration), the cache entry is removed.
- If the cache entry expires before save, the backend attempts a fall-back to persisted validation records (if any) — but frontend should expect expiry.

Endpoints (required)
1) POST /api/validate/carrier
- Purpose: lookup by MC or DOT and return parsed FMCSA fields for preview.
- Request body:
  {
    "lookupValue": "123456",
    "lookupType": "MC" // or "DOT"
  }
- Response (200) — preview (example):
  {
    "validationId":"11111111-2222-3333-4444-555555555555",
    "lookupType":"MC",
    "lookupValue":"123456",
    "dotNumber":"987654321",
    "mcNumber":"123456",
    "legalName":"ACME Transport Inc",
    "dbaName":"ACME",
    "operatingStatus":"A",
    "allowedToOperate":"Y",
    "phyStreet":"100 Logistics Way",
    "phyCity":"Chicago",
    "phyState":"IL",
    "phyZip":"60601",
    "phyCountry":"US",
    "totalDrivers":25,
    "totalPowerUnits":12,
    "brokerAuthorityActive": null
  }

Alternative: register-with-preview (frontend-provided preview fields)
- POST /api/carriers/register-with-preview
  - Request body (example):
    {
      "email":"carrier@example.com",
      "password":"StrongP@ssw0rd",
      "dotNumber":"987654321",
      "mcNumber":"123456",
      "legalName":"ACME Transport Inc",
      "dbaName":"ACME",
      "operatingStatus":"A",
      "allowedToOperate":"Y",
      "phyStreet":"100 Logistics Way",
      "phyCity":"Chicago",
      "phyState":"IL",
      "phyZip":"60601",
      "phyCountry":"US",
      "totalDrivers":25,
      "totalPowerUnits":12
    }
  - Response: { "token":"<jwt>" }

Note: the preferred (canonical) flow is still validate -> save (validationId) because it ensures the server-side preview is authoritative. Use register-with-preview only if your frontend wants to POST the preview fields directly (the server will persist them as-is).

2) POST /api/validate/broker
- Purpose: lookup broker MC number and return parsed fields and brokerAuthorityActive boolean.
- Request body:
  {
    "mcNumber": "123456"
  }
- Response: same preview schema as carrier, `brokerAuthorityActive` will be true/false.

3) POST /api/validate/carrier/save
- Purpose: persist the previewed carrier and create a user for login.
- Request body:
  {
    "validationId":"11111111-2222-3333-4444-555555555555",
    "email":"carrier@example.com",
    "password":"StrongP@ssw0rd"
  }
- Response (200): LoginResponse
  { "token": "<jwt>" }

4) POST /api/validate/broker/save
- Same as carrier save but registers a broker user.

5) POST /api/auth/login
- Purpose: login returning a JWT for existing users.
- Request body:
  { "email":"user@example.com","password":"pass" }
- Response: { "token":"<jwt>" }

Protected endpoints (examples)
- GET /api/carriers/me  — Authorization: Bearer <jwt>
- POST /api/brokers/loads — Authorization: Bearer <jwt>

Frontend responsibilities (recommended)
- Treat DOT/MC values as strings (backend stores them as TEXT).
- After receiving preview, present fields: legalName, operatingStatus, allowedToOperate, address (phyStreet/City/State/Zip), totalDrivers/PowerUnits.
- Block save until user confirms (explicit "Confirm and Save" button).
- Show clear error if save fails due to cache expiry and show a "Re-run validation" action.
- Securely handle the password — POST it directly only over HTTPS.

Sample UI flow (short)
1) User enters MC/DOT and clicks "Lookup" -> frontend POSTs to /api/validate/carrier.
2) Display returned preview and ask the user to confirm.
3) On confirm, POST to /api/validate/carrier/save with validationId + credentials.
4) On success, store the returned JWT (secure storage) and continue the onboarding flow.

Edge cases
- Cache expiry before save: frontend must handle 404/expired errors gracefully and offer to re-run validation.
- Partial or missing FMCSA fields: preview may contain nulls; highlight missing fields to user.
- Duplicate email: registration can fail if email exists — show meaningful message from backend.
