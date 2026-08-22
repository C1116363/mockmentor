# 3. frontend — the React app

React 19 + Vite. This is the actual product: login, signup, and one dashboard
per role.

## Run it

```bash
npm install    # first time only
npm run dev
```

Open <http://localhost:5173>. The backend must be running too.

## Where to change things

Everything is under `src/`:

| File / folder | What it does |
| --- | --- |
| `api/client.js` | **every** call to the backend, in one place |
| `auth/AuthContext.jsx` | who is logged in; login, signup, logout |
| `pages/AuthPage.jsx` | the login + signup screen |
| `pages/StudentDashboard.jsx` | what a student sees |
| `pages/MentorDashboard.jsx` | what a mentor sees |
| `pages/AdminDashboard.jsx` | what an admin sees |
| `components/RequestCard.jsx` | the card used in every list |
| `components/StatusBadge.jsx` | the coloured status pill |
| `App.jsx` | picks which dashboard to show, based on role |
| `App.css` | all the styling |

### Common edits

| I want to... | File |
| --- | --- |
| Add a new backend call | `api/client.js`, then use it in a page |
| Change the demo logins shown on the login screen | `pages/AuthPage.jsx` → `DEMO_ACCOUNTS` |
| Change the topic/experience dropdown options | `pages/StudentDashboard.jsx` → `EXPERIENCE_LEVELS` |
| Change colours | `App.css` → the `:root { }` block at the top of `index.css` |
| Change what shows on a request card | `components/RequestCard.jsx` |
| Point at a different API URL | `api/client.js` → `BASE_URL` |
