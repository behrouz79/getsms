// ui.jsx — minimal Android UI primitives.
// All sizes follow Android dp/sp values; hairlines are 1dp.

// ─── Card ─────────────────────────────────────────────────────
function Card({ children, style, onClick, noPad }) {
  return (
    <div onClick={onClick} style={{
      background: C.surface,
      border: `1px solid ${C.divider}`,
      borderRadius: 6,
      padding: noPad ? 0 : 14,
      ...style,
    }}>{children}</div>
  );
}

// ─── Buttons ──────────────────────────────────────────────────
function PrimaryButton({ children, full, color = C.text, style }) {
  return (
    <button style={{
      background: color, color: '#fff',
      border: 'none', borderRadius: 4,
      padding: '0 18px', height: 40,
      width: full ? '100%' : 'auto',
      ...ts('bodyStrong'),
      letterSpacing: 0.2,
      cursor: 'pointer',
      ...style,
    }}>{children}</button>
  );
}

function OutlineButton({ children, full, color = C.text, style }) {
  return (
    <button style={{
      background: 'transparent', color,
      border: `1px solid ${C.divider}`, borderRadius: 4,
      padding: '0 14px', height: 36,
      width: full ? '100%' : 'auto',
      ...ts('bodyStrong'),
      cursor: 'pointer',
      ...style,
    }}>{children}</button>
  );
}

function TextButton({ children, color = C.text, style }) {
  return (
    <button style={{
      background: 'transparent', color,
      border: 'none', padding: '6px 8px', margin: '-6px -8px',
      ...ts('bodyStrong'),
      cursor: 'pointer',
      ...style,
    }}>{children}</button>
  );
}

// Service hero button — full-width, big, the most prominent control.
function ServiceButton({ running, style }) {
  const c = running ? C.error : C.success;
  return (
    <button style={{
      width: '100%', height: 56,
      background: c, color: '#fff',
      border: 'none', borderRadius: 4,
      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
      fontSize: 15, fontWeight: 600, letterSpacing: 0.6, textTransform: 'uppercase',
      fontFamily: 'Vazirmatn, "Segoe UI", system-ui, sans-serif',
      cursor: 'pointer',
      ...style,
    }}>
      <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#fff', opacity: running ? 1 : 0.95,
        boxShadow: running ? '0 0 0 4px rgba(255,255,255,0.25)' : 'none' }} />
      {running ? 'Stop service' : 'Start service'}
    </button>
  );
}

// ─── Badge / pill ─────────────────────────────────────────────
function Badge({ children, tone = 'neutral', solid = false }) {
  const map = {
    neutral: { fg: C.textSec, bg: C.surfaceAlt, bd: C.divider },
    primary: { fg: C.primary, bg: C.primaryWk, bd: 'transparent' },
    success: { fg: C.success, bg: C.successWk, bd: 'transparent' },
    error:   { fg: C.error,   bg: C.errorWk,   bd: 'transparent' },
    info:    { fg: C.info,    bg: C.infoWk,    bd: 'transparent' },
  };
  const m = map[tone];
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center',
      padding: '2px 7px', borderRadius: 3,
      background: solid ? m.fg : m.bg,
      color: solid ? '#fff' : m.fg,
      border: solid ? 'none' : `1px solid ${m.bd}`,
      ...ts('badge'),
    }}>{children}</span>
  );
}

// ─── Status strip (thin colored left edge) ────────────────────
function StatusStrip({ tone = 'success', children, style }) {
  const color = tone === 'success' ? C.success : tone === 'error' ? C.error : tone === 'warning' ? C.warning : C.textMuted;
  return (
    <div style={{
      position: 'relative',
      background: C.surface,
      border: `1px solid ${C.divider}`,
      borderLeft: `3px solid ${color}`,
      borderRadius: 4,
      padding: '10px 12px 10px 13px',
      ...style,
    }}>{children}</div>
  );
}

// ─── Toggle (Material switch, minimal) ────────────────────────
function Toggle({ on }) {
  return (
    <span style={{
      display: 'inline-block', width: 36, height: 20,
      borderRadius: 10, padding: 2, boxSizing: 'border-box',
      background: on ? C.text : C.divider,
      border: `1px solid ${on ? C.text : C.divider}`,
      transition: 'background .15s',
      verticalAlign: 'middle',
    }}>
      <span style={{
        display: 'block', width: 14, height: 14, borderRadius: 7,
        background: '#fff',
        transform: on ? 'translateX(16px)' : 'translateX(0)',
        transition: 'transform .15s',
      }} />
    </span>
  );
}

// ─── Input field ──────────────────────────────────────────────
function Input({ label, value, placeholder, helper, suffix, focused, style }) {
  return (
    <div style={{ marginBottom: 14, ...style }}>
      {label && (
        <div style={{ ...ts('caption'), color: C.textSec, marginBottom: 6 }}>{label}</div>
      )}
      <div style={{
        display: 'flex', alignItems: 'center',
        height: 40, padding: '0 12px',
        background: C.surface,
        border: `1px solid ${focused ? C.text : C.divider}`,
        borderRadius: 4,
      }}>
        <div style={{
          flex: 1, ...ts('body'),
          color: value ? C.text : C.textMuted,
          fontFamily: value && value.includes('://') ? 'ui-monospace, SFMono-Regular, Menlo, monospace' : ts('body').fontFamily,
        }}>{value || placeholder}</div>
        {suffix && <div style={{ ...ts('caption'), color: C.textSec, marginLeft: 8 }}>{suffix}</div>}
      </div>
      {helper && <div style={{ ...ts('caption'), color: C.textSec, marginTop: 6 }}>{helper}</div>}
    </div>
  );
}

// ─── Select (read-only display version) ───────────────────────
function Select({ label, value }) {
  return (
    <div style={{ marginBottom: 14 }}>
      {label && <div style={{ ...ts('caption'), color: C.textSec, marginBottom: 6 }}>{label}</div>}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        height: 40, padding: '0 12px',
        background: C.surface, border: `1px solid ${C.divider}`, borderRadius: 4,
        ...ts('body'), color: C.text,
      }}>
        <span>{value}</span>
        <span style={{ color: C.textSec, fontSize: 10 }}>▾</span>
      </div>
    </div>
  );
}

// ─── Segmented tabs ───────────────────────────────────────────
function Tabs({ items, active }) {
  return (
    <div style={{ display: 'flex', borderBottom: `1px solid ${C.divider}` }}>
      {items.map((it, i) => (
        <div key={i} style={{
          padding: '10px 14px',
          ...ts('bodyStrong'),
          color: i === active ? C.text : C.textSec,
          borderBottom: i === active ? `2px solid ${C.primary}` : '2px solid transparent',
          marginBottom: -1,
        }}>{it}</div>
      ))}
    </div>
  );
}

// ─── Section header (in-screen) ───────────────────────────────
function SectionHeader({ children, right, style }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '14px 16px 8px', ...style,
    }}>
      <div style={{ ...ts('sectionHead'), color: C.textSec }}>{children}</div>
      {right}
    </div>
  );
}

// ─── Stats bar atom ───────────────────────────────────────────
function Stat({ label, value, tone }) {
  const color = tone === 'success' ? C.success : tone === 'error' ? C.error : C.text;
  return (
    <div style={{ flex: 1 }}>
      <div style={{ ...ts('caption'), color: C.textSec, marginBottom: 2 }}>{label}</div>
      <div style={{ fontSize: 20, fontWeight: 600, color, letterSpacing: -0.3,
        fontFamily: 'Vazirmatn, "Segoe UI", system-ui, sans-serif', fontVariantNumeric: 'tabular-nums' }}>{value}</div>
    </div>
  );
}

// ─── Divider ──────────────────────────────────────────────────
function Divider({ inset = 0 }) {
  return <div style={{ height: 1, background: C.divider, marginLeft: inset }} />;
}

// ─── Icon button placeholder (unicode glyph in 40dp tap area) ─
function IconBtn({ glyph, color = C.text }) {
  return (
    <div style={{
      width: 40, height: 40, display: 'flex', alignItems: 'center', justifyContent: 'center',
      color, fontSize: 16, fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
    }}>{glyph}</div>
  );
}

Object.assign(window, {
  Card, PrimaryButton, OutlineButton, TextButton, ServiceButton,
  Badge, StatusStrip, Toggle, Input, Select, Tabs, SectionHeader, Stat, Divider, IconBtn,
});
