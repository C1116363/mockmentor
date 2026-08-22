# 3. frontend — the React app

React 19 + Vite. Three roles, three experiences:

- **Candidates** sign up, book a 1-hour slot, track their requests.
- **Mentors** sign up, fill in a profile (experience, education, KYC, bank
  details), wait for an admin to verify it, then get the interview queue.
- **Admins** log in with a seeded account (the Admin option on the login page
  offers no signup), verify mentors, and assign mentors to student requests.

`MentorGate.jsx` is the interesting bit — it reads `verificationStatus` and
picks the form, the waiting screen, or the dashboard.

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
| `pages/StudentDashboard.jsx` | candidate: book + track |
| `pages/MentorGate.jsx` | picks the mentor screen from verification status |
| `pages/MentorProfileForm.jsx` | the profile a mentor submits |
| `pages/MentorPending.jsx` | the "under verification" screen |
| `pages/MentorDashboard.jsx` | approved mentor: queue + accept + complete |
| `pages/AdminDashboard.jsx` | four tabs: assign requests, verify mentors, users, all requests |
| `components/AssignMentorForm.jsx` | admin picking a mentor for one request |
| `components/SlotPicker.jsx` | date picker + the 1-hour slot grid |
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
| Change the slot grid layout | `components/SlotPicker.jsx` + `.slots` / `.slot` in `App.css` |
| Change the bookable hours or slot length | backend: `service/SlotService.java` and `InterviewRequest.SLOT_MINUTES` |
| Change colours | `src/index.css` → the `:root { }` block |
| Change what shows on a request card | `components/RequestCard.jsx` |
| Point at a different API URL | `api/client.js` → `BASE_URL` |

### Bringing mentor / admin screens back later

Nothing was removed from the backend — every endpoint still works. To add a
mentor dashboard again you'd add the calls back to `api/client.js`
(`/requests/pending`, `/requests/{id}/accept`, …), create the page, and branch
on `user.role` in `App.jsx`.
