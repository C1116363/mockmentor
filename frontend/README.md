# 3. frontend — the React app

React 19 + Vite. Three roles, three experiences:

- **Candidates** sign up, book a 1-hour slot, track their requests.
- **Mentors** sign up, fill in a profile (experience, education, KYC, bank
  details), wait for an admin to verify it, then get the interview queue.
- **Admins** log in with a seeded account (the Admin option on the login page
  offers no signup), verify mentors, and assign mentors to student requests.

Navigation is a **☰ menu button in the header**. Nothing is shown until you
click it; then the sections drop down one under the other, and picking one shows
that screen and closes the menu. Each dashboard **registers its own sections**
with `SectionNav` on mount, so the header knows nothing about roles.

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
| `pages/StudentDashboard.jsx` | candidate: Book / My interviews / History |
| `pages/MentorGate.jsx` | picks the mentor screen from verification status |
| `pages/MentorProfileForm.jsx` | the profile a mentor submits |
| `pages/MentorPending.jsx` | the "under verification" screen |
| `pages/MentorDashboard.jsx` | mentor: Open queue / My interviews / History |
| `pages/AdminDashboard.jsx` | admin: Payments / Assign / Mentors / Users / All requests |
| `components/AssignMentorForm.jsx` | admin picking a mentor for one request |
| `components/MentorProfileCard.jsx` | one mentor, in any verification state |
| `components/UpcomingInterviews.jsx` | the Join banner, shared by student and mentor |
| `components/PayModal.jsx` | the pay-to-book popup: UPI ID, UTR, screenshot |
| `components/PaymentReviewCard.jsx` | admin checking one payment |
| `components/FeedbackModal.jsx` | the mentor's scorecard form |
| `components/StarRating.jsx` | clickable 1-5 stars, keyboard accessible |
| `components/FeedbackCard.jsx` | the scorecard as the candidate reads it |
| `components/SlotPicker.jsx` | date picker + the 1-hour slot grid |
| `components/RequestCard.jsx` | the card used in the request list |
| `components/StatusBadge.jsx` | the coloured status pill |
| `App.jsx` | decides: loading / logged out / logged in |
| `App.css` | all component styling |
| `index.css` | **design tokens** — colours, shadows, radii, dark mode |
| `components/ThemeToggle.jsx` | light / dark switch |
| `nav/SectionNav.jsx` | header menu: the ☰ button, the drop-down list, and the shared state |

### Common edits

| I want to... | File |
| --- | --- |
| Add a new backend call | `api/client.js`, then use it in a page |
| Change the signup fields | `pages/AuthPage.jsx` |
| Change the experience dropdown options | `pages/StudentDashboard.jsx` → `EXPERIENCE_LEVELS` |
| Change the slot grid layout | `components/SlotPicker.jsx` + `.slots` / `.slot` in `App.css` |
| Change the bookable hours or slot length | backend: `service/SlotService.java` and `InterviewRequest.SLOT_MINUTES` |
| Change colours | `src/index.css` → the `:root { }` block, and `[data-theme="dark"]` for dark mode |
| Change what shows on a request card | `components/RequestCard.jsx` |
| Point at a different API URL | `api/client.js` → `BASE_URL` |

### Bringing mentor / admin screens back later

Nothing was removed from the backend — every endpoint still works. To add a
mentor dashboard again you'd add the calls back to `api/client.js`
(`/requests/pending`, `/requests/{id}/accept`, …), create the page, and branch
on `user.role` in `App.jsx`.

## Structure

The same four layers as the backend, using the frontend's honest equivalents.

```
pages/                 the screen. Renders and delegates - no fetch calls here.
  features/x/useX.js   the use case: state + orchestration        (facade)
  features/x/xRules.js pure rules over data - no React, no fetch  (service)
    api/xApi.js        URLs only, one module per backend controller (repository)
      api/http.js      transport: fetch, auth header, envelope, token
```

| Backend | Frontend | Holds |
| --- | --- | --- |
| `controller/` | `pages/` | the screen |
| `facade/` | `features/x/useX.js` | orchestration + state |
| `service/` | `features/x/xRules.js` | business rules, pure functions |
| `repository/` | `api/xApi.js` | data access |
| — | `api/http.js` | the transport everything sits on |

### Naming

| Kind | Pattern | Example |
| --- | --- | --- |
| Screen | `*Dashboard.jsx` / `*Page.jsx` | `AdminDashboard.jsx` |
| Use case | `use*.js` | `usePlans.js`, `useAdminDashboard.js` |
| Rules | `*Rules.js` | `planRules.js`, `sessionRules.js` |
| Data access | `*Api.js` | `planApi.js`, `adminApi.js` |
| Feature UI | `features/x/components/` | `features/plans/components/PlanCard.jsx` |
| Shared UI | `components/` | `StatusBadge.jsx`, `StarRating.jsx` |

`api/` module names line up 1:1 with backend controllers, so `planApi.js` is
`PlanController` and `adminApi.js` is `AdminController` + `AdminPlanController`.
If you know the endpoint you know the file.

A component lives under `features/x/components/` when one feature owns it, and in
`components/` only when it is feature-agnostic. Three qualify: `StatusBadge`,
`StarRating`, `ThemeToggle`.

### One hook per feature, not one per screen

`StudentDashboard` calls `useSessions()`, `usePlans()` and `useMaterials()`
separately. That is deliberate: each loads and fails on its own, so a broken
plans call cannot blank out the interview list.

`useAdminDashboard` is the exception, and for the same reason `AdminFacade`
exists on the backend - one admin screen genuinely needs users, mentor profiles,
sessions, two payment queues, plans and material at once, and verifying a payment
changes half of them. Reloading only one list would leave the rest quietly wrong.

### Where the layers earn it

Before: `AdminDashboard.jsx` was 606 lines with 16 `useState` and ten fetch calls
inline. `api/client.js` was one 345-line file covering eleven feature areas.

After: the screens are 446 and 435 lines of mostly JSX, the fetch calls live in
nine focused `api/` modules, and the rules are pure functions you can read on
their own.

### What is NOT here

No separate "dto/vo" on the frontend - the JSON is the contract, and mirroring the
backend's records in JS would be two definitions to keep in step with no type
checker to catch the drift. If this ever moves to TypeScript, that is when types
for the envelope and the VOs earn their place.

## Session lifetime

The JWT lives in **`sessionStorage`**, so closing the tab logs you out. Refreshing
does not — sessionStorage survives a reload and is cleared when the tab closes.
A second tab is a second session, because nothing is shared between them.

All of that lives in `tokenStore` in `api/client.js`. Every call is wrapped in
try/catch: storage access itself throws in some private-browsing modes, and a
login screen that white-screens is worse than one that cannot remember you.

`purgePersistedTokens()` runs on load and clears any token left in `localStorage`,
where the token used to be kept. Without it, anyone already logged in would keep a
session that outlives the tab and the new rule would apply to nobody.

The theme preference is still in `localStorage` — that should outlive a session.
