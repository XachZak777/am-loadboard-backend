Stripe subscription integration — instructions

Goal

- Allow Carriers to subscribe via Stripe. After successful subscription, set `Carrier.subscriptionActive = true` so the carrier can view all posted loads.

Overview

1. Create Stripe products/prices for your subscription tiers.
2. Expose an endpoint that creates a Checkout Session for the authenticated carrier and returns the session id or session URL.
3. Add a webhook endpoint to receive subscription events from Stripe (invoice.payment_succeeded, customer.subscription.updated, customer.subscription.deleted) and update the Carrier record accordingly.
4. Secure the webhook using Stripe's signing secret. Verify signature on incoming webhook requests.
5. Optionally store Stripe customerId and subscriptionId on the Carrier model to correlate.

Database changes (recommended)

- Add the following optional fields to `Carrier` entity:
  - stripeCustomerId (String)
  - stripeSubscriptionId (String)
  - subscriptionActive (Boolean) — already present

Backend endpoints (examples)

1) POST /api/subscription/create-checkout
- Authenticated (carrier)
- Request: { priceId: "price_xxx" }
- Behavior:
  - If carrier.stripeCustomerId missing: create a Stripe Customer using the carrier email and store the id on the Carrier.
  - Create a Checkout Session for mode=subscription with the given priceId and the customer id.
  - Return session.url (or session.id) to frontend.

2) POST /api/subscription/webhook
- Public endpoint used by Stripe
- Validate signature header `Stripe-Signature` with your webhook secret.
- Parse the event. On subscription created/updated/invoice.payment_succeeded:
  - Find carrier by stripeCustomerId (or by metadata set on session).
  - Set carrier.subscriptionActive = true on successful payment.
  - Save carrier.stripeSubscriptionId = subscription.id for later reference.
- On customer.subscription.deleted or subscription canceled/expired:
  - Set carrier.subscriptionActive = false and clear subscriptionId if desired.

Implementation notes

- Stripe Java SDK: add dependency (Maven):

```xml
<dependency>
  <groupId>com.stripe</groupId>
  <artifactId>stripe-java</artifactId>
  <version>21.16.0</version>
</dependency>
```

- Configuration: set `stripe.apiKey` and `stripe.webhook.secret` in application.properties or environment variables.

- Security: limit who can create checkout sessions — only authenticated carriers for their own account.

- Mapping webhook -> Carrier: prefer storing stripeCustomerId on Carrier when creating the session. Alternatively set a `metadata` field on Session with carrierId.

- Local testing: use Stripe CLI `stripe listen --forward-to localhost:8080/api/subscription/webhook` to forward webhook events to your local server.

Quick example (pseudocode)

1) Create checkout session in controller/service:

- Create Customer if needed:
  CustomerCreateParams params = CustomerCreateParams.builder().setEmail(carrier.email).build();
  Customer customer = Customer.create(params);
  carrier.setStripeCustomerId(customer.getId());
  carrierRepo.save(carrier);

- Create session:
  SessionCreateParams sparams = SessionCreateParams.builder()
    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
    .setCustomer(customer.getId())
    .addLineItem(SessionCreateParams.LineItem.builder().setPrice(priceId).setQuantity(1L).build())
    .setSuccessUrl(successUrl)
    .setCancelUrl(cancelUrl)
    .build();
  Session session = Session.create(sparams);
  return session.getUrl();

2) Webhook handling (simplified):

- Parse and verify payload with webhook secret.
- Switch on event.getType():
  - invoice.payment_succeeded -> extract subscription id and customer id; find carrier by stripeCustomerId; set subscriptionActive=true; save.
  - customer.subscription.deleted -> set subscriptionActive=false; save.

Testing

- Create product/price in Stripe dashboard. Use test API keys.
- Use the Checkout session URL for a test checkout.
- Use Stripe CLI to send `invoice.payment_succeeded` events to your webhook while testing.

Notes

- Keep the webhook secret private and rotate if leaked.
- Consider adding a small retry/backfill job to reconcile subscription states periodically if webhooks are missed.
- If you need I can implement the endpoints and webhook handler in the repo, including DB changes and small integration tests.
