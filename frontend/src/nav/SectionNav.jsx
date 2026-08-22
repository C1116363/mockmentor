import { createContext, useCallback, useContext, useMemo, useRef, useState } from "react";

/**
 * Navigation that lives in the header rather than above the content.
 *
 * The header shows a toggle with the current section's name. Clicking it opens
 * a second line underneath with every section; picking one navigates and
 * closes the line again.
 *
 * The state lives up here because the toggle is in the header while the
 * sections belong to whichever dashboard is mounted. Each dashboard registers
 * its own sections on mount, so the header doesn't need to know anything about
 * roles.
 */
const SectionNavContext = createContext(null);

export function SectionNavProvider({ children }) {
  const [items, setItems] = useState([]);
  const [active, setActive] = useState(null);
  const [open, setOpen] = useState(false);
  // Compared against the incoming registration so re-registering identical
  // sections (which happens on every parent render) doesn't loop setState.
  const signature = useRef("");

  const register = useCallback((next, defaultKey) => {
    const sig = JSON.stringify(next);
    if (sig === signature.current) return;
    signature.current = sig;
    setItems(next);
    setActive((current) => {
      const stillValid = next.some((i) => i.key === current);
      return stillValid ? current : defaultKey ?? next[0]?.key ?? null;
    });
  }, []);

  const go = useCallback((key) => {
    setActive(key);
    setOpen(false);
  }, []);

  const value = useMemo(
    () => ({ items, active, register, go, open, setOpen }),
    [items, active, register, go, open]
  );

  return <SectionNavContext.Provider value={value}>{children}</SectionNavContext.Provider>;
}

export function useSectionNav() {
  const context = useContext(SectionNavContext);
  if (!context) throw new Error("useSectionNav must be used inside <SectionNavProvider>");
  return context;
}

/** The button in the header. Shows where you are, and opens the second line. */
export function SectionToggle() {
  const { items, active, open, setOpen } = useSectionNav();
  if (items.length === 0) return null;

  const current = items.find((i) => i.key === active);
  const outstanding = items.reduce((n, i) => n + (i.alert && i.count > 0 ? i.count : 0), 0);

  return (
    <button
      className={`navtoggle ${open ? "navtoggle--open" : ""}`}
      onClick={() => setOpen(!open)}
      aria-expanded={open}
      aria-label="Browse sections"
    >
      <span className="navtoggle__icon">{current?.icon ?? "☰"}</span>
      <span className="navtoggle__label">{current?.label ?? "Menu"}</span>
      {/* Anything needing attention in a section you're NOT on, so closing the
          line never hides work from you. */}
      {outstanding > 0 && !open && <span className="navtoggle__dot">{outstanding}</span>}
      <span className="navtoggle__chevron" aria-hidden="true">▾</span>
    </button>
  );
}

/** The second line, revealed under the header. */
export function SectionLine() {
  const { items, active, go, open } = useSectionNav();
  if (items.length === 0 || !open) return null;

  return (
    <nav className="navline" aria-label="Sections">
      {items.map((i) => (
        <button
          key={i.key}
          className={`navline__item ${active === i.key ? "navline__item--on" : ""}`}
          onClick={() => go(i.key)}
          aria-current={active === i.key ? "page" : undefined}
        >
          {i.icon && <span className="navline__icon">{i.icon}</span>}
          <span>{i.label}</span>
          {i.count > 0 && (
            <span className={`navline__count ${i.alert ? "navline__count--alert" : ""}`}>
              {i.count}
            </span>
          )}
        </button>
      ))}
    </nav>
  );
}
