// phone.jsx — Minimal Android phone frame for this app.
// Custom-built (not the starter) so it matches the warm amber-on-stone palette.

const PHONE_W = 360;
const PHONE_H = 760;

// Status bar — slim, monochrome, minimal.
function PhoneStatus() {
  return (
    <div style={{
      height: 28, padding: '0 16px',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      background: C.bg, color: C.text,
      ...ts('caption'), fontWeight: 500,
      fontVariantNumeric: 'tabular-nums',
    }}>
      <span>9:30</span>
      <span style={{ fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace', fontSize: 10, letterSpacing: 1, color: C.textSec }}>
        ▲▲▲  ◯  ▮▮▮
      </span>
    </div>
  );
}

// App bar — title left, optional back/menu glyph, action glyphs right.
function PhoneAppBar({ title, back, actions = [], subtitle }) {
  return (
    <div style={{
      padding: '0 4px 0 4px',
      background: C.bg,
      borderBottom: `1px solid ${C.divider}`,
    }}>
      <div style={{ height: 52, display: 'flex', alignItems: 'center' }}>
        <div style={{
          width: 44, height: 44, display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: C.text, fontSize: 18, fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
        }}>{back ? '←' : '☰'}</div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ ...ts('cardTitle'), color: C.text, fontWeight: 600 }}>{title}</div>
          {subtitle && <div style={{ ...ts('caption'), color: C.textSec, marginTop: -1 }}>{subtitle}</div>}
        </div>
        {actions.map((a, i) => (
          <div key={i} style={{
            width: 44, height: 44, display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: C.text, fontSize: 16, fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
          }}>{a}</div>
        ))}
      </div>
    </div>
  );
}

// Gesture nav pill at bottom.
function PhoneNav() {
  return (
    <div style={{ height: 22, display: 'flex', alignItems: 'center', justifyContent: 'center', background: C.bg }}>
      <div style={{ width: 100, height: 3, borderRadius: 2, background: C.textMuted, opacity: 0.6 }} />
    </div>
  );
}

// Phone — wraps a screen.
function Phone({ title, subtitle, back, actions, fab, children }) {
  return (
    <div style={{
      width: PHONE_W, height: PHONE_H,
      background: C.bg,
      display: 'flex', flexDirection: 'column',
      fontFamily: 'Vazirmatn, "Segoe UI", system-ui, sans-serif',
      color: C.text,
      position: 'relative',
      overflow: 'hidden',
    }}>
      <PhoneStatus />
      {title !== undefined && <PhoneAppBar title={title} subtitle={subtitle} back={back} actions={actions} />}
      <div style={{ flex: 1, overflow: 'auto', position: 'relative', background: C.bg }}>
        {children}
      </div>
      {fab && (
        <div style={{
          position: 'absolute', right: 16, bottom: 36,
          width: 52, height: 52, borderRadius: 4,
          background: C.text, color: '#fff',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 22, fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
          boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
        }}>{fab}</div>
      )}
      <PhoneNav />
    </div>
  );
}

Object.assign(window, { Phone, PHONE_W, PHONE_H });
