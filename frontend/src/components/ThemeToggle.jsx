import { useEffect, useState } from "react";

/**
 * Light / dark switch.
 *
 * Reads the OS preference the first time, then remembers the explicit choice.
 * localStorage is wrapped because it throws in some private-browsing modes, and
 * a theme toggle should never be the thing that white-screens the app.
 */
function initialDark() {
  try {
    const saved = localStorage.getItem("mm.theme");
    if (saved) return saved === "dark";
  } catch {
    /* storage blocked - fall through to the OS setting */
  }
  return window.matchMedia?.("(prefers-color-scheme: dark)").matches ?? false;
}

export default function ThemeToggle() {
  const [dark, setDark] = useState(initialDark);

  useEffect(() => {
    document.documentElement.dataset.theme = dark ? "dark" : "light";
    try {
      localStorage.setItem("mm.theme", dark ? "dark" : "light");
    } catch {
      /* not fatal */
    }
  }, [dark]);

  return (
    <button
      className="theme-toggle"
      onClick={() => setDark((v) => !v)}
      aria-label={dark ? "Switch to light mode" : "Switch to dark mode"}
      title={dark ? "Light mode" : "Dark mode"}
    >
      {dark ? "☀" : "☾"}
    </button>
  );
}
