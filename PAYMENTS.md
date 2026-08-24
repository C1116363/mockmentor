# Payments — turning on Razorpay

The code is written and tested. Nothing is switched on until you paste three
keys into `backend/.env`. This page is the whole checklist.

- **[Where the keys go](#where-the-keys-go)** — the four-line answer
- **[Getting the keys](#getting-the-keys)** — what Razorpay asks for
- **[Testing without real money](#testing-without-real-money)**
- **[How it works](#how-it-works)** — worth reading before you take real money
- **[When it doesn't work](#when-it-doesnt-work)**

---

## Where the keys go

One file: **`backend/.env`**. It is gitignored, so nothing in it reaches GitHub.

```ini
PAYMENT_PROVIDER=razorpay

RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxxxxxx
RAZORPAY_KEY_SECRET=xxxxxxxxxxxxxxxxxxxxxxxx
RAZORPAY_WEBHOOK_SECRET=whatever-long-random-string-you-chose
```

Restart the backend. That is the entire switch-on.

To go back to manual UPI, set `PAYMENT_PROVIDER=manual`. Nothing else changes —
the manual flow is never removed.

### The three keys are not interchangeable

This is the one thing worth getting right, because getting it wrong produces a
symptom that looks like something else entirely.

| Key | Where it comes from | Who may see it |
| --- | --- | --- |
| `RAZORPAY_KEY_ID` | Dashboard → Account & Settings → API Keys | **Public.** Sent to the browser by design — the checkout cannot open without it. |
| `RAZORPAY_KEY_SECRET` | Shown **once**, next to the Key ID, when you generate the pair | Server only, forever |
| `RAZORPAY_WEBHOOK_SECRET` | Dashboard → Settings → Webhooks — **you type this one in yourself** | Server only, forever |

> **The classic mistake:** putting the *key secret* in `RAZORPAY_WEBHOOK_SECRET`.
> Every webhook then fails its signature check, so payments succeed at the bank
> and never activate here — and the server log fills with rejected webhooks that
> look exactly like somebody attacking you.

If any of the three is blank, gateway checkout stays **off** and the app quietly
offers manual UPI instead. It does not crash, and it does not show a Pay button
that cannot work.

---

## Getting the keys

### 1. The account

Razorpay needs a real business, because RBI requires it — no Indian gateway
skips this.

- Business PAN (or your personal PAN, for a sole proprietorship)
- Bank account in the business name, plus a cancelled cheque
- Address proof
- GST number if you have one — not required below the threshold

Approval is usually 2–5 working days.

### 2. The API keys

Dashboard → **Account & Settings → API Keys → Generate Key**.

The **secret is displayed exactly once**. Copy it into `.env` at that moment; if
you lose it you have to regenerate the pair, which invalidates the old one.

### 3. The webhook

Dashboard → **Settings → Webhooks → Add New Webhook**:

| | |
| --- | --- |
| **URL** | `https://your-domain/api/webhooks/razorpay` |
| **Secret** | any long random string — paste the same value into `.env` |
| **Events** | `order.paid`, `payment.captured`, `payment.failed` |

**Razorpay cannot reach `localhost`.** For development, put a tunnel in front:

```bash
cloudflared tunnel --url http://localhost:8080
```

Use the `https://…trycloudflare.com` URL it prints as the webhook URL.

### 4. Four pages Razorpay requires on your public website

Activation is refused without these, and the marketing site currently has none:

- Terms & Conditions
- Privacy Policy
- **Refund / Cancellation Policy** ← the one people forget
- Contact Us, with a real address and phone number

### What it costs

About **2% + 18% GST on the fee** (so ~2.36% all-in), and money settles to your
bank on **T+2**. Manual UPI costs 0% and arrives instantly, which is why both
are kept.

---

## Testing without real money

Test keys start `rzp_test_`. They open a real checkout that moves no money.

Test card: **4111 1111 1111 1111**, any future expiry, any CVV, OTP `1234`.

You can also exercise the webhook directly, with no Razorpay account at all:

```bash
SECRET=your_webhook_secret
BODY='{"event":"order.paid","payload":{"payment":{"entity":{"id":"pay_TEST","order_id":"order_TEST","amount":49900}}}}'
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$SECRET" -r | cut -d' ' -f1)

curl -i -X POST http://localhost:8080/api/webhooks/razorpay \
  -H 'Content-Type: application/json' \
  -H "X-Razorpay-Signature: $SIG" \
  -H "X-Razorpay-Event-Id: evt_test_001" \
  -d "$BODY"
```

Change one character of `$BODY` without re-signing and it must answer **403**.
If it answers 200, stop and fix that before taking real money.

---

## How it works

### The pieces

| What | Where |
| --- | --- |
| The gateway, behind an interface | `payment/PaymentGateway.java` |
| Razorpay (no SDK — RestClient + JDK HMAC) | `payment/RazorpayGateway.java` |
| The existing manual flow | `payment/ManualUpiGateway.java` |
| How each purchase is priced and activated | `payment/PurchaseSettlement.java` |
| The orchestration, and all the hard bits | `service/impl/CheckoutServiceImpl.java` |
| One attempt to pay for one thing | `model/PaymentIntent.java` |
| The idempotency key **and** the audit log | `model/WebhookEvent.java` |
| Student endpoints | `controller/CheckoutController.java` |
| The webhook | `controller/PaymentWebhookController.java` |
| The browser half | `frontend/src/features/checkout/` |

### The flow

```
student clicks Pay
   │
   ├─ POST /api/checkout/PLAN/14
   │     server reads the price OFF THE ROW, opens a Razorpay order,
   │     and writes a PaymentIntent before anyone can pay
   │
   ├─ Razorpay's checkout window opens in the browser
   │
   ├─ money moves
   │
   ├─ browser  ─── POST /api/checkout/confirm ────┐
   │                                              ├── whichever arrives first
   └─ Razorpay ─── POST /api/webhooks/razorpay ───┘    settles it. Once.
```

### The five rules the correctness rests on

1. **The amount is never sent by the client.** It is read from the row being
   paid for. There is no amount field on any request DTO, and there must never
   be one — a checkout that accepts a price from the browser is a shop where the
   customer writes their own price tag.

2. **The webhook is the authority, not the browser.** The callback only arrives
   if the student's browser survives the round trip. Closed tabs and dead phones
   are ordinary, and those students have paid.

3. **The signature is checked against the raw bytes.** Binding the body to an
   object and re-serialising it changes whitespace and key order, and the HMAC
   no longer matches. Hence `byte[]` in the webhook controller.

4. **Settlement happens exactly once**, guaranteed two ways: a `SELECT … FOR
   UPDATE` on the intent so the callback and the webhook cannot both win, and a
   unique constraint on the webhook event id so a retry cannot re-run one. Both
   are enforced by the database, because a read-then-write in Java loses this
   race — and the prize for losing is granting paid access twice.

5. **A duplicate webhook is answered 200.** Razorpay retries anything that is
   not 2xx, so returning an error for a delivery you have already handled makes
   it retry forever.

### What is deliberately *not* built

- **Refunds.** Razorpay's dashboard does them in two clicks, and the volume here
  does not justify code that moves money outwards.
- **Auto-reconciliation against settlements.** Worth adding when the volume is
  such that nobody is reading the payment list any more.
- **Saved cards / subscriptions.** Nothing here recurs.

---

## When it doesn't work

### The Pay button never appears

The server is not offering a gateway. Check what it thinks:

```bash
curl -s http://localhost:8080/api/checkout/options -H "Authorization: Bearer $TOKEN"
```

`gatewayReady: false` means `PAYMENT_PROVIDER=razorpay` but at least one of the
three keys is blank. The backend says which at startup:

```
WARN  RazorpayGateway : app.payment.provider=razorpay but the keys are missing.
```

### Every webhook returns 403

The signature is not matching. In order of likelihood:

1. **You used the key secret as the webhook secret.** They are different values.
2. The secret in `.env` does not match the one typed into the dashboard —
   check for a trailing space.
3. Something between Razorpay and the app is rewriting the body. A proxy that
   reformats JSON breaks the signature; it must be passed through byte for byte.

### Payments succeed but nothing activates

The webhook is not arriving. Razorpay's dashboard has a delivery log per
webhook — look there first.

- Is the URL reachable from the internet? `localhost` never is.
- Is the tunnel still up? `cloudflared` prints a **new** URL each restart, and
  the dashboard still points at the old one.
- Are the three events ticked?

Everything received is recorded, so this answers it from your side:

```sql
SELECT event_id, event_type, gateway_order_id, outcome, received_at
FROM webhook_events ORDER BY received_at DESC LIMIT 20;
```

An empty table means nothing ever arrived — the problem is between Razorpay and
your server, not in the app.

### "Amount mismatch" in the log

What we asked for and what was captured disagree, so it refused to activate and
left it for a person. Not a bug — this is the check doing its job. Compare:

```sql
SELECT i.amount, i.gateway_order_id, e.outcome
FROM payment_intents i JOIN webhook_events e
  ON e.gateway_order_id = i.gateway_order_id
WHERE e.outcome LIKE 'refused%';
```

### A student says they paid and got nothing

```sql
SELECT * FROM payment_intents WHERE gateway_order_id = 'order_…';
SELECT * FROM webhook_events  WHERE gateway_order_id = 'order_…';
```

- Intent `PAID`, purchase not active → a bug. The log line naming that order id
  is the place to start.
- Intent `CREATED`, no webhook rows → the money never reached us, **or** the
  webhook never did. Razorpay's dashboard settles which.
- No intent at all → they never got as far as the checkout.
