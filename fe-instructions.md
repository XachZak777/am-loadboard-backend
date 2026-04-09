FE integration notes — Loadboard Backend

This document summarizes the new load/bid/booking flows and the HTTP endpoints the frontend should use. It assumes the frontend obtains a JWT token from /api/auth/login and includes it as Authorization: Bearer <token>.

## Admin Account Setup

### Creating the First Admin (Bootstrap)
When the system is first deployed, there are no admins yet. Use the bootstrap endpoint to create the first admin account:

```
POST /api/auth/register-admin
Content-Type: application/json

{
  "email": "admin@loadboard.com",
  "password": "SecureAdminPassword123!"
}
```

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

Save the returned JWT token — use it to access the admin dashboard and authenticated endpoints. You can now log in to the admin dashboard with these credentials.

### Creating Additional Admins
Once the first admin exists, only that admin (or other admins) can create new admin accounts:

```
POST /api/auth/register-admin
Authorization: Bearer {{admin_token}}
Content-Type: application/json

{
  "email": "newadmin@loadboard.com",
  "password": "AnotherSecurePassword!"
}
```

### Admin Login
Use the standard login endpoint with admin credentials:

```
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@loadboard.com",
  "password": "SecureAdminPassword123!"
}
```

The response will include a JWT token that can be used for authenticated requests.

---

Key concepts and states
- LoadPosting.status: OPEN | ASSIGNED | CANCELLED | COMPLETED
  - OPEN: visible on the public board and available for bids
  - ASSIGNED: a broker approved a bid and the load is assigned to a carrier (removed from public board)
  - CANCELLED / COMPLETED: final states
- Bids: carriers place bids (including a bookNow flag). Bids are created in PENDING state. Broker must approve a bid to assign the load.
- bookNow: a carrier may set bookNow=true when placing a bid. This does NOT auto-assign the load. It signals higher intent to the broker; the broker must still approve to assign.

Endpoints (minimal)
- POST /api/loads (broker only)
  - Payload (JSON):
    {
      "pickupStreet":"100 Logistics Way",
      "pickupCity":"Chicago",
      "pickupState":"IL",
      "pickupZip":"60601",
      "pickupCountry":"US",
      "deliveryStreet":"200 Freight Ave",
      "deliveryCity":"Los Angeles",
      "deliveryState":"CA",
      "deliveryZip":"90001",
      "deliveryCountry":"US",
      "description":"FTL - dry van",
      "weight":12000,
      "price":1500
    }
  - Returns: created LoadPostingDto (includes id and createdAt)

- GET /api/loads
  - Returns: list of LoadPostingDto (public/open loads)

- POST /api/loads/bid (carrier only)
  - Payload (JSON): { "loadId": "<load-uuid>", "amount": 1400, "bookNow": true }
  - Returns: BidResponse
  - Note: bookNow is a signal only; broker must approve.

- GET /api/loads/{id}/bids (broker only)
  - Returns: list of BidResponse for that load

- POST /api/loads/{id}/approve/{bidId} (broker only)
  - Approves the bid, assigns the load to that carrier, sets load.status=ASSIGNED, sets approved bid.status=APPROVED and other bids=REJECTED
  - Returns: 200 OK

- POST /api/loads/{id}/cancel (broker only)
  - Cancels the assignment (if any): approved bid(s) -> CANCELLED, load.status -> OPEN, assignedCarrier cleared
  - Returns: 200 OK

Frontend behavior / UX recommendations
- Showing the board (public loads)
  - GET /api/loads and show loads with status==="OPEN"
  - Do not show ASSIGNED loads on the main board (they remain in the DB for history)

- Bidding flow (carrier)
  - Carrier selects a load and submits a bid via POST /api/loads/bid with amount and optionally bookNow
  - Show the broker-facing UI a highlighted "book now" indicator when bookNow=true but make it clear this is a request, not an assignment

- Broker review flow
  - Broker visits /api/loads (their own loads) -> view LoadPostingDto
  - Click "View Bids" -> GET /api/loads/{id}/bids
  - Approve desired bid -> POST /api/loads/{id}/approve/{bidId}
  - After approve: frontend should remove the load from the open board and show it in "Assigned" view for that broker

- Cancellation
  - Broker can cancel an assignment via POST /api/loads/{id}/cancel
  - Frontend should show a confirmation dialog since this will reopen the load for bidding

Real-time considerations
- Polling every 10–30s is fine to get bid updates for the broker view.
- If you need near-real-time notifications (new bid/bookNow), consider adding a websocket or server-sent events endpoint later.

Error handling
- 401 Unauthorized: missing or invalid token
- 403 Forbidden: action requires different role (e.g. only carriers can bid, only brokers can post/approve)
- 404 Not Found: invalid loadId or bidId
- 409 Conflict: attempting to bid on an already-assigned load (the backend returns 409)

Testing in dev
- Use request.http in the repo to exercise the flows. Save tokens to the @TOKEN variable after login.
- Sequence to test quickly:
  1) Create broker account and login
  2) Create load (POST /api/loads)
  3) Create carrier account and login
  4) Carrier POST /api/loads/bid with bookNow true or false
  5) Broker GET bids and POST approve
  6) Confirm load no longer appears on GET /api/loads (public)
  7) Broker POST cancel to reopen

If you'd like, I can add a websocket SSE endpoint for bid notifications and/or a small integration test that covers create -> bid -> approve -> cancel. Let me know which you'd prefer next.

Stripe subscription notes
------------------------
- This project supports a soft Stripe integration for carrier subscriptions. If you set the application property `stripe.api.key` (e.g., via environment variable `STRIPE_API_KEY`) the backend will create a Stripe Checkout session when activating a carrier subscription.
- The `SubscriptionService.activate(carrierId)` method returns a Stripe checkout URL when Stripe is configured; the `SubscriptionController.activate` endpoint will return that URL as a plain text response. The frontend should redirect the carrier to that URL to complete payment.
- After a successful payment the frontend should call a backend webhook or the success redirect to mark the carrier subscription active. For now the backend uses a simple approach:
  - If no `stripe.api.key` is configured, activating immediately sets `subscriptionActive=true` (development mode).
  - If Stripe is configured, the backend returns a checkout URL. The frontend should redirect there; on success, implement a webhook or call a backend endpoint to set the flag (we can add this webhook handler next).

Next step options (Stripe)
- I can add a Stripe webhook endpoint (/api/stripe/webhook) to process Checkout Session completed events and mark subscriptions active automatically.
- I can also store Stripe customer IDs on the `Carrier` model and persist subscription records for future billing and cancellations.

Admin Dashboard & Management
-----------
Admins have elevated access to the system and can:
- View all carriers and brokers
- Delete carriers and brokers (and their associated users)
- View audit logs of all user actions
- Create, edit, and delete loads on behalf of any broker
- Monitor all activities in the system via audit trail

### Admin Endpoints

All admin endpoints require `Authorization: Bearer <admin-token>` header and user must have ROLE_ADMIN.

#### Carriers
- `GET /api/admin/carriers` - List all carriers with full details
- `GET /api/admin/carriers/{id}` - Get specific carrier
- `DELETE /api/admin/carriers/{id}` - Delete carrier and associated user

#### Brokers
- `GET /api/admin/brokers` - List all brokers with full details
- `GET /api/admin/brokers/{id}` - Get specific broker
- `DELETE /api/admin/brokers/{id}` - Delete broker and associated user

#### User History & Audit Logs
- `GET /api/admin/users/{id}/history` - Get all actions performed by a user
- `GET /api/admin/entities/{id}/history` - Get all changes to a specific entity (load, carrier, broker, etc.)

#### Load Management (Admin can manage any load)
- `GET /api/admin/loads` - List all loads across all brokers
- `GET /api/admin/loads/{id}` - Get specific load details
- `POST /api/admin/loads/{brokerId}` - Create load for specified broker
  - Payload: LoadPostingDto with all address and shipment details
- `PUT /api/admin/loads/{id}` - Update load details (price, weight, description, addresses)
- `DELETE /api/admin/loads/{id}` - Delete load (removes from board)

### Admin Workflow Example
1. Admin logs in with ROLE_ADMIN credentials
2. Admin dashboard loads and shows three main sections: Carriers, Brokers, Loads
3. Admin can view all carriers/brokers with their metadata and click to see details
4. Admin can delete problematic users by clicking a delete button (triggers audit log)
5. Admin can create test loads for brokers to verify bidding flows
6. Admin can view audit trail for any entity to debug issues
7. All admin actions (create/update/delete) are logged automatically

### Frontend Implementation for Admin
- Add an "Admin Dashboard" link in navigation visible only to ROLE_ADMIN users
- Create three main pages/sections:
  - **Carriers**: List with edit/delete actions, click to view details and audit history
  - **Brokers**: List with edit/delete actions, click to view details and audit history
  - **Loads**: List all loads with create/edit/delete actions, show assigned status and broker
- Add ability to create test loads for any broker (useful for feature testing)
- Show audit logs in a modal/panel when viewing a user/entity
- Use pagination for large lists (GET requests don't support limit/offset yet, but can be added)

### Testing with Admin Account
For development/testing, admins can:
1. Create test loads for any broker without needing to switch accounts
2. Delete test data (carriers, brokers, loads) easily
3. Verify carrier bidding flows by creating loads and viewing bid history
4. Check audit logs to debug user action flows
5. Test cancellation and reassignment by creating/updating loads directly
