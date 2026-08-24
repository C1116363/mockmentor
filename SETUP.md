# Setup — from a clean laptop to a running app

Three servers, one database. Budget 15 minutes the first time, mostly downloads.

- **[Quick version](#quick-version)** — if you already have Java, Node and MySQL
- **[Full version](#full-version)** — step by step, from nothing
- **[Verify it worked](#verify-it-worked)**
- **[When it doesn't work](#when-it-doesnt-work)** — every error I have actually hit

---

## What you are starting

| Part | Port | What it is |
| --- | --- | --- |
| Backend | 8080 | Spring Boot API + Swagger docs |
| Frontend | 5173 | The React app people log into |
| Website | 3000 | The marketing page (static, no build) |
| MySQL | 3306 | The database |

---

## Quick version

```bash
git clone https://github.com/C1116363/mockmentor.git
cd mockmentor

cp backend/.env.example backend/.env     # then put your MySQL password in it
./start.sh
```

`start.sh` checks your tools, creates the database if it is missing, installs
frontend dependencies on first run, and starts all three servers. Ctrl+C stops
everything.

Then open **<http://localhost:5173>** and log in as `admin@example.com` /
`password123`.

If `start.sh` complains about something missing, the full version below covers it.

---

## Full version

### 1. Install the tools

You need **Java 21+**, **Maven**, **Node 18+** and **MySQL 8**.

**macOS** (with [Homebrew](https://brew.sh)):

```bash
brew install openjdk@21 maven node mysql
brew services start mysql          # starts MySQL now, and on every reboot
```

If `java -version` still shows an old version afterwards, Homebrew keeps JDK 21
out of the way on purpose:

```bash
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk \
             /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

**Ubuntu / Debian:**

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven nodejs npm mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql        # start on every reboot
```

**Windows:** use [WSL2](https://learn.microsoft.com/windows/wsl/install) and
follow the Ubuntu steps inside it. Everything here — `./start.sh`, `./run.sh` —
assumes a Unix shell.

Check all four:

```bash
java -version     # want 21 or higher
mvn -version
node -version     # want 18 or higher
mysql --version
```

### 2. Get the code

```bash
git clone https://github.com/C1116363/mockmentor.git
cd mockmentor
```

### 3. Set up MySQL

**Do you need to create the database by hand?** No. The JDBC URL says
`createDatabaseIfNotExist=true`, so the backend creates `interview_mentor` on
first boot, and `ddl-auto=update` creates every table inside it. `start.sh`
creates it up front too, so a permissions problem surfaces immediately rather
than as a stack trace.

What you **do** need is a MySQL user the app can log in as.

**Option A — use root** (fine for local development):

```bash
mysql -u root -p -e "SELECT 'MySQL is reachable' AS status;"
```

If root has no password, press Enter at the prompt.

**Option B — a dedicated user** (what you would do on a server):

```sql
-- mysql -u root -p
CREATE DATABASE IF NOT EXISTS interview_mentor
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'prehire'@'localhost' IDENTIFIED BY 'choose-a-password';

-- ALL on that one schema only. Not *.* - a web app has no business being able
-- to read or drop every other database on the box.
GRANT ALL PRIVILEGES ON interview_mentor.* TO 'prehire'@'localhost';
FLUSH PRIVILEGES;
```

### 4. Tell the backend your credentials

```bash
cp backend/.env.example backend/.env
```

Then edit `backend/.env`:

```ini
DB_USER=root                  # or prehire, if you made a dedicated user
DB_PASSWORD=your-password     # leave blank if root has no password

# Only needed if you want the payment screens to show your own UPI ID.
UPI_ID=yourname@okhdfcbank
UPI_PAYEE=Your Name
```

`.env` is gitignored, so your password never reaches GitHub.

### 5. Start it

**All three at once:**

```bash
./start.sh
```

**Or one terminal each**, which is easier when you are working on a single part:

```bash
cd backend  && ./run.sh          # :8080  — loads .env, then mvn spring-boot:run
cd frontend && npm install       # first time only
cd frontend && npm run dev       # :5173
cd website  && ./serve.sh        # :3000
```

First backend start takes a minute or two while Maven downloads dependencies.
You will know it is ready when the log says:

```
Seeded demo accounts. Every account uses the password: password123
Seeded 4 plans and their starter study material.
Tomcat started on port 8080
```

---

## Verify it worked

```bash
# 1. the API is up, and the seeded plans came back
curl -s http://localhost:8080/api/public/plans | head -c 120

# 2. login works and returns a token
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"password123"}'

# 3. all three servers answer
for p in 3000 5173 8080; do
  echo "$p -> $(curl -s -o /dev/null -w '%{http_code}' http://localhost:$p/)"
done
```

Expect `200`, `200`, `401` — the 401 on 8080 is correct. The API root needs a
token, and answering 401 rather than 404 is deliberate so it does not reveal
which endpoints exist.

Then open these:

| | |
| --- | --- |
| The app | <http://localhost:5173> |
| Marketing site | <http://localhost:3000> |
| **API docs (all 60 endpoints)** | <http://localhost:8080/swagger-ui.html> |

### Demo logins

Every account uses the password **`password123`**.

| Email | Role | Good for trying |
| --- | --- | --- |
| `admin@example.com` | Admin | Verifying payments, setting plan prices, sending study material |
| `ananya@example.com` | Mentor | The open queue, accepting a session, writing a scorecard |
| `vikram@example.com` | Mentor | Same, second mentor |
| `neha@example.com` | Mentor | Same, third mentor |
| `arjun@example.com` | Mentor | **Onboarding** — profile is incomplete, so you see the form and the admin review queue |
| `rahul@example.com` | Student | Booking, paying, buying a plan, reading material |
| `priya@example.com` | Student | Same, second student — use it to check one student cannot see another's material |

### A five-minute tour

1. Log in as **rahul@example.com** → book a mentoring session → you land on the
   payment screen. Enter any UTR and attach any JPG/PNG.
2. Log in as **admin@example.com** → *Payments* → confirm it. The booking is now
   open to mentors.
3. Log in as **ananya@example.com** → *Open queue* → accept it → write it up.
4. Back as **admin** → *Plans & prices* → change a price. Reload as rahul and the
   new number is there.
5. As **admin** → *Study material* → send a file to rahul only. Log in as priya
   and confirm it is not in her list.

---

## When it doesn't work

### `Communications link failure` / `Connection refused` on startup

MySQL is not running.

```bash
brew services start mysql                    # macOS
sudo systemctl start mysql                   # Linux
nc -z 127.0.0.1 3306 && echo "MySQL is up"   # check
```

### `Access denied for user 'root'@'localhost'`

Wrong password in `backend/.env`. Test it directly:

```bash
mysql -u root -p -e "SELECT 1;"
```

If root genuinely has no password, `DB_PASSWORD=` (empty) is correct — but note
that some MySQL installs use `auth_socket` for root, which the app cannot use.
Make a dedicated user instead (Option B above).

### `Port 8080 was already in use` (or 5173, or 3000)

Something is still running from last time.

```bash
lsof -ti:8080 | xargs kill        # swap the port as needed
```

### Backend starts, but the frontend shows "Is the backend running?"

That message means the frontend got no usable response. Check in this order:

1. `curl http://localhost:8080/api/public/plans` — is the API actually up?
2. Browser console for a **CORS** error. The allowed origins are in
   `backend/src/main/resources/application.properties` under
   `app.cors.allowed-origins`. If you run the frontend on a different port, add it
   there.
3. Browser Network tab — a 401 on every call means the token was rejected. Log out
   and back in.

### `Unknown column` or `Cannot add column` on startup

`ddl-auto=update` only *adds* things. It never drops or relaxes a column, so a
field you removed or changed can leave the schema in a state Hibernate cannot
reconcile. Simplest fix locally is to throw the schema away — the seeder rebuilds
the demo data:

```bash
mysql -u root -p -e "DROP DATABASE interview_mentor;"
```

**This deletes all local data.** Fine for a dev box; on anything you care about
you would use Flyway migrations instead.

### Frontend: `Failed to resolve import`

Dependencies were never installed, or are stale after a pull:

```bash
cd frontend && rm -rf node_modules && npm install
```

### The plans page is empty

Plans are seeded only when the `plans` table is empty, so a database created
before that feature existed has none. Either add one through the admin panel
(*Plans & prices* → **+ New plan**), or drop the schema and let the seeder run.

### Logged out every time I switch tabs

Working as intended. The token lives in `sessionStorage`, so each tab is its own
session and closing a tab logs you out. Refreshing keeps you in. See
[frontend/README.md](frontend/README.md#session-lifetime).

---

## Where to go next

| I want to… | Read |
| --- | --- |
| Find where something is in the code | [ARCHITECTURE.md](ARCHITECTURE.md) |
| Understand what the app does | [README.md](README.md) |
| Change API behaviour | [backend/README.md](backend/README.md) |
| Change a screen | [frontend/README.md](frontend/README.md) |
| Change the marketing page | [website/README.md](website/README.md) |
