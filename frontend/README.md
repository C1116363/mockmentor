# 3. frontend — the React app

React 19 + Vite. **This app is for candidates only** — students and working
professionals who want a mock interview.

Login, signup, book an interview, track it. That's the whole app.

Mentors and admins still exist in the system, but their work (assigning a
mentor, accepting a request, managing accounts) happens through the API, not
through this interface. If a mentor or admin logs in here they get a polite
"nothing here for you yet" screen.

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
| `pages/StudentDashboard.jsx` | the whole app once you're logged in |
| `components/RequestCard.jsx` | the card used in the request list |
| `components/StatusBadge.jsx` | the coloured status pill |
| `App.jsx` | decides: loading / logged out / logged in |
| `App.css` | all the styling |

### Common edits

| I want to... | File |
| --- | --- |
| Add a new backend call | `api/client.js`, then use it in a page |
| Change the signup fields | `pages/AuthPage.jsx` |
| Change the experience dropdown options | `pages/StudentDashboard.jsx` → `EXPERIENCE_LEVELS` |
| Change colours | `src/index.css` → the `:root { }` block |
| Change what shows on a request card | `components/RequestCard.jsx` |
| Point at a different API URL | `api/client.js` → `BASE_URL` |

### Bringing mentor / admin screens back later

Nothing was removed from the backend — every endpoint still works. To add a
mentor dashboard again you'd add the calls back to `api/client.js`
(`/requests/pending`, `/requests/{id}/accept`, …), create the page, and branch
on `user.role` in `App.jsx`.
