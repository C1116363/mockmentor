# Contribute website page

The public page lives at `/contribute/`. GitHub Pages redirects `/contribute` to
the directory's index page. The CNAME keeps the domain `confirmplacement.com`.

## Structure

- `index.html`: homepage markup with Contribute links.
- `assets/css/home.css`, `assets/js/home.js`: existing homepage styles and behavior.
- `contribute/index.html`: Contribute page content and semantic HTML.
- `assets/css/base.css`: layout, navigation, buttons, and typography for new pages.
- `assets/css/contribute.css`: Contribute sections and responsive workspace preview.
- `assets/js/shared/navigation.js`: mobile menu.
- `assets/js/contribute/index.js`: page initialization.
- `assets/js/contribute/config.js`: deployed Contribute app URL.
- `assets/js/contribute/app-links.js`: enrollment navigation and unavailable dialog.
- `assets/js/contribute/demo-data.js`: fictional sample tickets and people.
- `assets/js/contribute/workspace-demo.js`: preview interactions; no server writes.

## Preview

Run `python -m http.server 3001 --bind 127.0.0.1` in the website directory.
Open `http://localhost:3001/contribute/`. No build dependencies are required.

## Connect the application

Set `CONTRIBUTE_APP_URL` in `assets/js/contribute/config.js` to the deployed
Contribute application's HTTPS URL. It starts empty because the existing
`app.confirmplacement.com` serves a different product. Until configured, enrollment
buttons open an explanatory dialog. This page collects no payments or signup data.
Team and admin logins remain in the separate application.

## Publish

The existing GitHub Pages workflow publishes the repository root on pushes to
`gh-pages`, including `contribute/` and `assets/`. Review and merge these changes
into that branch to publish `https://confirmplacement.com/contribute/`.
Local changes do not update the live domain automatically.

## Content

The workspace is a demo, not a live catalog. Approval simulation does not grant
engineers real review privileges. Membership prices and capacity belong in the
application. Repository invitations are arranged after joining. Optional Team Room
access is INR 199 per Software Engineer per project.
