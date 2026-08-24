import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
} from "react";

/**
 * Which section of a dashboard is showing.
 *
 * The state lives up here because the tabs are in the app shell while the
 * sections belong to whichever dashboard is mounted. Each dashboard registers
 * its own sections on mount, so the shell knows nothing about roles.
 */
const SectionNavContext = createContext(null);

export function SectionNavProvider({ children }) {
  const [items, setItems] = useState([]);
  const [active, setActive] = useState(null);
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

  const go = useCallback((key) => setActive(key), []);

  const value = useMemo(() => ({ items, active, register, go }), [items, active, register, go]);

  return <SectionNavContext.Provider value={value}>{children}</SectionNavContext.Provider>;
}

export function useSectionNav() {
  const context = useContext(SectionNavContext);
  if (!context) throw new Error("useSectionNav must be used inside <SectionNavProvider>");
  return context;
}

/**
 * The section tabs.
 *
 * <h2>Why these are tabs and not a menu</h2>
 * This was a hamburger button that opened a dropdown. Everything was one click
 * away and nothing was visible, so at any moment the screen was a single panel
 * and a button - which is exactly why the app read as "one box". Worse, the
 * counts that tell an admin there are four payments waiting were inside the
 * closed menu, so the one thing that should pull you somewhere was the one thing
 * you could not see.
 *
 * Tabs put the whole shape of the dashboard on screen: where you are, what else
 * exists, and what is waiting. The dot on a tab with outstanding work is the
 * point - it is a reason to click, visible without clicking anything.
 */
export function SectionTabs() {
  const { items, active, go } = useSectionNav();

  if (items.length === 0) return null;

  return (
    <nav className="tabs-bar" aria-label="Sections">
      <div className="tabs-bar__scroll" role="tablist">
        {items.map((item) => {
          const on = active === item.key;
          // "alert" marks a section as urgent; the dot only shows when there is
          // actually something in it. An empty urgent section is just a section.
          const urgent = item.alert && item.count > 0;

          return (
            <button
              key={item.key}
              role="tab"
              aria-selected={on}
              className={`tab-btn ${on ? "tab-btn--on" : ""} ${urgent ? "tab-btn--urgent" : ""}`}
              onClick={() => go(item.key)}
            >
              <span className="tab-btn__icon" aria-hidden="true">{item.icon}</span>
              <span className="tab-btn__label">{item.label}</span>

              {item.count > 0 && (
                <span className="tab-btn__count">{item.count}</span>
              )}

              {urgent && !on && <span className="tab-btn__dot" aria-label="needs attention" />}
            </button>
          );
        })}
      </div>
    </nav>
  );
}
