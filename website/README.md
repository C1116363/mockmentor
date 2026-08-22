# 1. website — the public marketing site

The page anyone lands on first. Plain HTML, CSS and JavaScript in **one single
file**. No build step, no `npm install`, no dependencies.

## Run it

```bash
./serve.sh
```

Then open <http://localhost:3000>.

## Where to change things

Everything lives in `index.html`. It has three parts, in this order:

| Part | Where | What's in it |
| --- | --- | --- |
| **Styles** | inside `<style>` at the top | colours, spacing, dark mode |
| **Content** | inside `<body>` | the actual words and sections |
| **Behaviour** | inside `<script>` at the bottom | filters, carousel, animations |

### Common edits

| I want to change... | Look for |
| --- | --- |
| The big headline | `<h1>` in the `<section class="hero">` |
| The words that rotate in the headline | `const WORDS = [...]` in the script |
| Colours / dark mode | `:root { }` and `[data-theme="dark"] { }` at the top |
| Where the buttons go | `const APP_URL` at the top of the script |
| The topic filter chips | `<div class="chips">` and the `topicsOf()` function |
| The three steps | `<section id="how">` |
| Reviews | `<section id="reviews">` — each one is a `<figure class="quote">` |
| FAQ questions | `<section id="faq">` — each one is a `<div class="qa">` |
| Footer links | `<footer class="foot">` |

### Mentor cards

They are **not** hardcoded — they load from the API at
`GET /api/public/mentors?limit=3`, so adding a mentor through the app makes them
appear here automatically.

If the API is unreachable the page falls back to the `FALLBACK` array in the
script, so it never looks broken. To change the card layout, edit the template
string inside `render()`.
