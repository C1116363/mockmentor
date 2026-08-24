import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

/**
 * Navigation lives behind a menu button in the header.
 *
 * Nothing is shown until you click it; then the sections drop down one under
 * the other, and picking one shows that screen and closes the menu.
 *
 * The state lives up here because the button is in the header while the
 * sections belong to whichever dashboard is mounted. Each dashboard registers
 * its own sections on mount, so the header knows nothing about roles.
 */
const SectionNavContext = createContext(null);

export function SectionNavProvider({ children }) {
  const [items, setItems] = useState([]);
  const [active, setActive] = useState(null);
  const [open, setOpen] = useState(false);
  // Compared against each incoming registration, so re-registering identical
  // sections (which happens on every parent render) can't loop setState.
  const signature = useRef("");

  const register = useCallback((next, defaultKey) => {
    const sig = JSON.stringify(next);
    if (sig === signature.current) return;
    signature.current = sig;
    setItems(next);
    setActive((current) =>
      next.some((i) => i.key === current) ? current : defaultKey ?? next[0]?.key ?? null
    );
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

/**
 * The menu button plus the panel it opens.
 *
 * Both live in one component so the panel can be positioned against the
 * button, and so the outside-click handler has a single element to test.
 */
export function SectionMenu() {
  const { items, active, go, open, setOpen } = useSectionNav();
  const wrapRef = useRef(null);

  // Clicking anywhere else, or pressing Escape, closes the menu - a dropdown
  // you can only close by clicking the button again feels stuck.
  useEffect(() => {
    if (!open) return;

    const onPointer = (e) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) setOpen(false);
    };
    const onKey = (e) => e.key === "Escape" && setOpen(false);

    document.addEventListener("mousedown", onPointer);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onPointer);
      document.removeEventListener("keydown", onKey);
    };
  }, [open, setOpen]);

  if (items.length === 0) return null;

  const current = items.find((i) => i.key === active);
  // Work waiting in sections you are not on, so a closed menu never hides it.
  const outstanding = items.reduce(
    (n, i) => n + (i.alert && i.count > 0 && i.key !== active ? i.count : 0),
    0
  );

  return (
    <div className="navmenu" ref={wrapRef}>
      <button
        className={`navmenu__btn ${open ? "navmenu__btn--open" : ""}`}
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label={open ? "Close menu" : "Open menu"}
      >
        <span className="burger" aria-hidden="true">
          <span />
          <span />
          <span />
        </span>
        {outstanding > 0 && !open && <span className="navmenu__dot">{outstanding}</span>}
      </button>

      {open && (
        <div className="navmenu__panel" role="menu">
          <p className="navmenu__heading">Go to</p>

          {items.map((i) => (
            <button
              key={i.key}
              role="menuitem"
              className={`navmenu__item ${active === i.key ? "navmenu__item--on" : ""}`}
              onClick={() => go(i.key)}
            >
              <span className="navmenu__icon">{i.icon}</span>
              <span className="navmenu__label">{i.label}</span>
              {i.count > 0 && (
                <span className={`navmenu__count ${i.alert ? "navmenu__count--alert" : ""}`}>
                  {i.count}
                </span>
              )}
              {active === i.key && (
                <span className="navmenu__tick" aria-hidden="true">
                  ✓
                </span>
              )}
            </button>
          ))}
        </div>
      )}

      {/* what you're currently looking at, next to the button */}
      <span className="navmenu__current">{current?.label}</span>
    </div>
  );
}
