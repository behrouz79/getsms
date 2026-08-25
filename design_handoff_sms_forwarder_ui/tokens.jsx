// tokens.jsx — Color + Type system specimens
// Exports the C and T tokens used everywhere else, plus the swatch/specimen artboards.

const C = {
  bg:        '#FAF9F7',  // warm off-white app background
  surface:   '#FFFFFF',
  surfaceAlt:'#F5F4F1',  // section bg / pressed
  primary:   '#D97706',  // amber 600
  primaryWk: '#FEF3C7',  // amber 100  — tint for badges, focus rings
  text:      '#1C1917',  // stone 900
  textSec:   '#78716C',  // stone 500
  textMuted: '#A8A29E',  // stone 400
  divider:   '#E7E5E4',  // stone 200
  dividerLt: '#F1EFEC',  // stone 100
  success:   '#059669',  // emerald 600
  successWk: '#D1FAE5',
  error:     '#DC2626',  // red 600
  errorWk:   '#FEE2E2',
  warning:   '#D97706',
  warningWk: '#FEF3C7',
  info:      '#1E40AF',  // indigo 800
  infoWk:    '#DBEAFE',
};

const T = {
  // size, weight, line-height, letter-spacing — paired to Vazirmatn
  pageTitle:   { size: 22, weight: 600, line: 30, ls: -0.2 },
  sectionHead: { size: 11, weight: 600, line: 16, ls:  0.8, upper: true },
  cardTitle:   { size: 15, weight: 600, line: 22, ls: -0.1 },
  body:        { size: 13, weight: 400, line: 20, ls:  0   },
  bodyStrong:  { size: 13, weight: 500, line: 20, ls:  0   },
  caption:     { size: 11, weight: 400, line: 16, ls:  0.1 },
  badge:       { size: 10, weight: 600, line: 12, ls:  0.6, upper: true },
  number:      { size: 64, weight: 600, line: 68, ls: -2   },
};

// Turn a T entry into a style object.
const ts = (k) => {
  const t = T[k] || k;
  return {
    fontSize: t.size, fontWeight: t.weight,
    lineHeight: t.line + 'px', letterSpacing: t.ls + 'px',
    textTransform: t.upper ? 'uppercase' : 'none',
    fontFamily: 'Vazirmatn, "Segoe UI", system-ui, sans-serif',
  };
};

// ─── Color palette artboard ───────────────────────────────────
function PaletteArtboard() {
  const Group = ({ title, items }) => (
    <div style={{ marginBottom: 22 }}>
      <div style={{ ...ts('sectionHead'), color: C.textSec, marginBottom: 10 }}>{title}</div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1, background: C.divider, border: `1px solid ${C.divider}` }}>
        {items.map((it) => (
          <div key={it.name} style={{ background: C.surface, padding: '10px 12px', display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{ width: 28, height: 28, background: it.value, border: `1px solid ${C.divider}` }} />
            <div style={{ minWidth: 0, flex: 1 }}>
              <div style={{ ...ts('bodyStrong'), color: C.text }}>{it.name}</div>
              <div style={{ ...ts('caption'), color: C.textSec, fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace' }}>{it.value}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
  return (
    <div style={{ padding: 28, background: C.bg, height: '100%', boxSizing: 'border-box', overflow: 'auto', ...ts('body'), color: C.text }}>
      <div style={{ ...ts('pageTitle'), color: C.text, marginBottom: 4 }}>Color tokens</div>
      <div style={{ ...ts('body'), color: C.textSec, marginBottom: 22 }}>Warm stone neutrals · amber accent · semantic status pairs.</div>
      <Group title="Surface" items={[
        { name: 'background',  value: C.bg },
        { name: 'surface',     value: C.surface },
        { name: 'surface_alt', value: C.surfaceAlt },
        { name: 'divider',     value: C.divider },
      ]}/>
      <Group title="Text" items={[
        { name: 'text_primary',   value: C.text },
        { name: 'text_secondary', value: C.textSec },
        { name: 'text_muted',     value: C.textMuted },
        { name: 'divider_light',  value: C.dividerLt },
      ]}/>
      <Group title="Accent · Primary" items={[
        { name: 'primary',      value: C.primary },
        { name: 'primary_weak', value: C.primaryWk },
      ]}/>
      <Group title="Semantic" items={[
        { name: 'success',      value: C.success },
        { name: 'success_weak', value: C.successWk },
        { name: 'error',        value: C.error },
        { name: 'error_weak',   value: C.errorWk },
        { name: 'info',         value: C.info },
        { name: 'info_weak',    value: C.infoWk },
      ]}/>
    </div>
  );
}

// ─── Typography artboard ──────────────────────────────────────
function TypeArtboard() {
  const Row = ({ k, label, sample }) => {
    const t = T[k];
    return (
      <div style={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 24, padding: '18px 0', borderTop: `1px solid ${C.dividerLt}` }}>
        <div>
          <div style={{ ...ts('bodyStrong'), color: C.text }}>{label}</div>
          <div style={{ ...ts('caption'), color: C.textSec, fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace' }}>
            {t.size}sp · {t.weight === 600 ? 'Bold' : t.weight === 500 ? 'Med' : 'Reg'}
          </div>
        </div>
        <div style={{ ...ts(k), color: C.text }}>{sample}</div>
      </div>
    );
  };
  return (
    <div style={{ padding: 28, background: C.bg, height: '100%', boxSizing: 'border-box', overflow: 'auto' }}>
      <div style={{ ...ts('pageTitle'), color: C.text, marginBottom: 4 }}>Type scale</div>
      <div style={{ ...ts('body'), color: C.textSec, marginBottom: 12 }}>Shabnam in production · Vazirmatn shown here. Latin + Persian glyphs share metrics.</div>
      <Row k="pageTitle"   label="Page title"      sample="SMS Forwarder" />
      <Row k="cardTitle"   label="Card title"      sample="Forward to Slack webhook" />
      <Row k="sectionHead" label="Section header"  sample="Recent activity" />
      <Row k="bodyStrong"  label="Body · strong"   sample="https://hooks.slack.com/services/T0…" />
      <Row k="body"        label="Body"            sample="Template: {sender} — {message}" />
      <Row k="caption"     label="Caption"         sample="142 ms · 200 OK · SIM 1" />
      <Row k="badge"       label="Badge"           sample="success" />
      <div style={{ marginTop: 24, padding: 14, background: C.surface, border: `1px solid ${C.divider}` }}>
        <div style={{ ...ts('caption'), color: C.textSec, marginBottom: 6 }}>RTL sample · Persian</div>
        <div dir="rtl" style={{ ...ts('body'), color: C.text }}>پیامک از طرف بانک به وب‌هوک ارسال شد · ۱۴۲ میلی‌ثانیه</div>
      </div>
    </div>
  );
}

Object.assign(window, { C, T, ts, PaletteArtboard, TypeArtboard });
