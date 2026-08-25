# Hosting ConfirmPlacement

Three pieces, three different kinds of hosting. Only one of them costs money.

| Piece | What it needs | Cost |
| --- | --- | --- |
| **Website** (`website/`) | any static host | free |
| **Frontend** (`frontend/`) | any static host | free |
| **Backend** (`backend/`) | a server that stays running, + MySQL, + disk | **₹0–600/month** |

The website is already live at
<https://c1116363.github.io/mockmentor/> from the `gh-pages` branch.

- **[Read this first](#read-this-first)** — five things that will break if you skip them
- **[Step by step](#step-by-step)**
- **[Where to host the backend](#where-to-host-the-backend)**
- **[The full environment](#the-full-environment)**
- **[When it doesn't work](#when-it-doesnt-work)**

---

## Read this first

Five things are fine locally and are **not** fine on the internet. Four of them
fail loudly. One does not, and it is the dangerous one.

### 1. The JWT secret — do this before anything else

`application.properties` ships a working default so a fresh clone runs with no
setup. **That value is committed to a public repository.** Anyone who can read
the repo can sign a token for any account on any server still using it,
including an admin account.

Nothing looks broken. The app works perfectly. That is exactly what makes it
the most serious item on this page.

```bash
openssl rand -base64 32
```

Set the result as `JWT_SECRET`. The backend prints a large warning at startup
while the default is still in place — if you see that banner on a hosted
server, stop and fix it.

### 2. CORS — the reason a fresh deploy looks completely dead

`app.cors.allowed-origins` defaults to the two localhost ports. Your deployed
frontend is not on those, so **every** browser call fails while `curl` against
the same API works perfectly. It looks like the frontend is broken; it isn't.

```
CORS_ORIGINS=https://your-frontend-url,https://your-website-url
```

No trailing slashes. Origins only — scheme, host and port, no path.

### 3. Uploaded files vanish on redeploy

CVs, payment screenshots and study material are written to `./uploads/`. On
most platforms the filesystem is **ephemeral** — it is recreated on every
deploy, so every uploaded file disappears while the database rows still point
at them.

Two honest options:

- **Attach a persistent volume** and point `UPLOAD_ROOT` at it. Railway, Fly and
  any VPS can do this. Simplest correct answer.
- **Accept it for now** if you are only demoing. Do not accept it once real
  candidates are uploading CVs — losing somebody's CV is not a tidiness problem.

Object storage (S3, Cloudflare R2) is the real answer at scale and is not
implemented; the three `Storage` classes would each need a new backend.

### 4. `ddl-auto=update` should become `validate`

Let it create the schema on the very first boot, then switch:

```
DDL_AUTO=validate
```

`validate` refuses to start if the code and the database disagree, instead of
silently altering a live database. It turns a data problem into a startup
error, which is the trade you want.

### 5. HTTPS is not optional

Razorpay will not send webhooks to plain HTTP, and browsers block mixed
content. Every host below terminates TLS for you — just never wire the frontend
to an `http://` API.

---

## Step by step

### 1. Database first

Create a MySQL 8 database. Every option in the next section can give you one,
or use a managed provider (Aiven and Railway both have small free tiers).

Keep three things: **host**, **user**, **password**. You want a JDBC URL like:

```
jdbc:mysql://HOST:3306/interview_mentor?serverTimezone=UTC
```

> Drop `createDatabaseIfNotExist=true` in production — a web app should not
> hold permission to create databases.

### 2. Backend

Build it:

```bash
cd backend
mvn clean package -DskipTests
# -> target/interview-mentor-0.0.1-SNAPSHOT.jar
```

Run it with the environment from [below](#the-full-environment):

```bash
java -jar target/interview-mentor-0.0.1-SNAPSHOT.jar
```

Check it before moving on:

```bash
curl https://your-api-url/api/public/plans     # 200 + the seeded plans
curl https://your-api-url/actuator/health      # if you enable actuator
```

**Write down the API URL.** The next two steps both need it.

### 3. Frontend

The API URL is baked in **at build time**, so it must be set before you build:

```bash
cd frontend
VITE_API_URL=https://your-api-url/api npm run build
# -> dist/
```

Upload `dist/` to any static host. Vercel, Netlify and Cloudflare Pages all
have generous free tiers and connect straight to the repo — set `VITE_API_URL`
in their dashboard and point the build at `frontend/`.

> Changing the API URL later means **rebuilding**, not editing a config file.
> `import.meta.env` is substituted during the build; there is nothing to change
> in `dist/`.

### 4. Website

Two edits at the top of the `<script>` in `website/index.html`:

```js
const DEPLOYED_APP_URL = "https://your-frontend-url";
const DEPLOYED_API_URL = "https://your-api-url/api";
```

**Until you set these, every button on the live site is inert** and answers
"not live yet". That is deliberate — better than dumping visitors on a dead
link — but it also means the published page currently has no working call to
action.

Then publish. The current process copies `website/` onto the `gh-pages` branch:

```bash
git worktree add /tmp/ghp gh-pages
cp website/index.html website/README.md website/serve.sh /tmp/ghp/
cd /tmp/ghp && git add -A && git commit -m "Publish website" && git push origin gh-pages
git worktree remove /tmp/ghp
```

`.github/workflows/ci.yml` automates this, but it has never been pushed —
pushing anything under `.github/workflows/` needs the `workflow` token scope:

```bash
gh auth refresh -s workflow
```

Then commit the workflow and set **Settings → Pages → Source → GitHub Actions**.

### 5. Point Razorpay at the real URL

Only if you are taking card payments. Dashboard → Settings → Webhooks:

```
https://your-api-url/api/webhooks/razorpay
```

Events: `order.paid`, `payment.captured`, `payment.failed`. Full detail in
[PAYMENTS.md](PAYMENTS.md).

---

## Where to host the backend

This is the only piece that needs a real server: it is a long-running JVM with
a database and a disk.

| Option | Cost | Good for | Watch out for |
| --- | --- | --- | --- |
| **Railway** | ~$5/mo | easiest by far — MySQL, volumes and deploys in one place | usage-based, so watch the meter |
| **Render** | free / $7/mo | simple, connects to the repo | **the free tier sleeps.** First request after idle takes ~50s. Unusable for anything paid. |
| **Fly.io** | ~$5/mo | volumes, regions close to India | CLI-driven, more to learn |
| **Oracle Cloud Free Tier** | genuinely ₹0 | 4 ARM cores and 24 GB RAM, free indefinitely | you install and patch everything yourself |
| **VPS** (Hostinger, DigitalOcean) | ₹400–600/mo | full control, one box for API + MySQL + files | you own the updates, backups and TLS |

**If you want it working this week:** Railway. Managed MySQL, a persistent
volume, HTTPS and environment variables in one dashboard.

**If budget is the constraint:** Oracle's free tier is real and permanent — but
budget one evening for the setup.

**Avoid Render's free tier for this app.** A student clicking Pay and waiting
50 seconds for a cold start will assume it is broken, and the Razorpay webhook
can time out against a sleeping server — which means a payment that succeeded
at the bank never activates.

---

## The full environment

```ini
# ---- Database (required) ----
DATABASE_URL=jdbc:mysql://HOST:3306/interview_mentor?serverTimezone=UTC
DB_USER=confirmplacement
DB_PASSWORD=...

# ---- Security (required) ----
# openssl rand -base64 32   — never the committed default
JWT_SECRET=...

# ---- URLs (required) ----
# No trailing slashes. Get this wrong and the whole frontend appears dead.
CORS_ORIGINS=https://your-frontend-url,https://your-website-url
# Where password-reset links point — the frontend, not the API
FRONTEND_URL=https://your-frontend-url

# ---- Production hygiene ----
DDL_AUTO=validate        # after the first boot has created the schema
SHOW_SQL=false           # log noise, and query parameters can contain personal data
UPLOAD_ROOT=/data/uploads   # a PERSISTENT volume, or uploads vanish on deploy

# ---- Payments (optional; manual UPI works without any of this) ----
PAYMENT_PROVIDER=manual
UPI_ID=yourname@okhdfcbank
UPI_PAYEE=Your Name
# PAYMENT_PROVIDER=razorpay
# RAZORPAY_KEY_ID=
# RAZORPAY_KEY_SECRET=
# RAZORPAY_WEBHOOK_SECRET=

# ---- Email (optional; defaults to printing to the log) ----
MAIL_PROVIDER=log
# MAIL_PROVIDER=smtp
# MAIL_USERNAME=you@gmail.com
# MAIL_PASSWORD=16-char-app-password
# MAIL_FROM=you@gmail.com

# ---- GitHub collaborator invites (optional) ----
GITHUB_TOKEN=
```

Frontend, at **build** time only:

```ini
VITE_API_URL=https://your-api-url/api
```

---

## When it doesn't work

### The frontend loads but every request fails

CORS, nine times out of ten. Open the browser console — if it mentions
`Access-Control-Allow-Origin`, your frontend URL is missing from
`CORS_ORIGINS`. Check for a trailing slash and for `http` where it should be
`https`.

Confirm the API itself is fine:

```bash
curl https://your-api-url/api/public/plans
```

If that returns data, the API is healthy and this is purely CORS.

### Everything 401s straight after logging in

The token is being rejected. Either `JWT_SECRET` changed between deploys —
which invalidates every existing token, and is expected once — or you are
running two instances with different secrets, so tokens work only on whichever
one happens to answer.

### Uploaded files 404 after a deploy

The ephemeral disk. See [item 3](#3-uploaded-files-vanish-on-redeploy). The
rows are still in the database; the bytes are gone.

### Payments succeed but nothing activates

The webhook is not arriving. Razorpay's dashboard has a per-webhook delivery
log — look there first, then:

```sql
SELECT event_id, event_type, outcome, received_at
FROM webhook_events ORDER BY received_at DESC LIMIT 20;
```

An empty table means nothing ever reached you. See
[PAYMENTS.md](PAYMENTS.md#when-it-doesnt-work).

### The backend starts, then dies

Almost always the database. `Communications link failure` means it cannot
reach `DATABASE_URL`; `Access denied` means the credentials are wrong. Both
appear in the first twenty lines of the log.

### First boot fails with `DDL_AUTO=validate`

Expected — there is no schema yet to validate against. Boot once with
`DDL_AUTO=update`, let it create the tables, then switch to `validate`.
