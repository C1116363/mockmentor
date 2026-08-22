/**
 * The tab strip used by all three dashboards, so they navigate the same way.
 *
 * `count` shows a neutral pill; `alert` makes it red, for things that need
 * doing rather than things that merely exist.
 *
 * Built from real buttons with role="tab" so arrow keys and screen readers
 * behave, rather than styled divs.
 */
export default function TabBar({ tabs, active, onChange }) {
  return (
    <div className="tabbar" role="tablist">
      {tabs.map((t) => (
        <button
          key={t.key}
          role="tab"
          aria-selected={active === t.key}
          className={`tabbar__tab ${active === t.key ? "tabbar__tab--on" : ""}`}
          onClick={() => onChange(t.key)}
        >
          {t.icon && <span className="tabbar__icon">{t.icon}</span>}
          <span>{t.label}</span>
          {t.count > 0 && (
            <span className={`tabbar__count ${t.alert ? "tabbar__count--alert" : ""}`}>
              {t.count}
            </span>
          )}
        </button>
      ))}
    </div>
  );
}
