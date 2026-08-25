// components-showcase.jsx — Two artboards showing the component library.

function ButtonsArtboard() {
  return (
    <div style={{ padding: 28, background: C.bg, height: '100%', boxSizing: 'border-box', overflow: 'auto',
      fontFamily: 'Vazirmatn, "Segoe UI", system-ui, sans-serif' }}>
      <div style={{ ...ts('pageTitle'), color: C.text, marginBottom: 4 }}>Buttons</div>
      <div style={{ ...ts('body'), color: C.textSec, marginBottom: 22 }}>Hierarchy: hero · primary · outline · text · destructive.</div>

      <div style={{ ...ts('sectionHead'), color: C.textSec, marginBottom: 10 }}>Service hero</div>
      <div style={{ display: 'grid', gap: 10, marginBottom: 26, maxWidth: 320 }}>
        <ServiceButton running={false} />
        <ServiceButton running={true} />
      </div>

      <div style={{ ...ts('sectionHead'), color: C.textSec, marginBottom: 10 }}>Primary · outline · text</div>
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 14 }}>
        <PrimaryButton>Save rule</PrimaryButton>
        <PrimaryButton color={C.primary}>Add action</PrimaryButton>
        <OutlineButton>Cancel</OutlineButton>
        <OutlineButton>Backup</OutlineButton>
      </div>
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 26 }}>
        <TextButton>Edit</TextButton>
        <TextButton>Duplicate</TextButton>
        <TextButton color={C.error}>Delete</TextButton>
        <TextButton color={C.primary}>Watch ad</TextButton>
      </div>

      <div style={{ ...ts('sectionHead'), color: C.textSec, marginBottom: 10 }}>Badges</div>
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 14 }}>
        <Badge tone="success">success</Badge>
        <Badge tone="error">failed</Badge>
        <Badge tone="primary">webhook</Badge>
        <Badge tone="info">SIM 1</Badge>
        <Badge tone="neutral">retry 2/3</Badge>
        <Badge tone="error" solid>503</Badge>
      </div>

      <div style={{ ...ts('sectionHead'), color: C.textSec, margin: '20px 0 10px' }}>Toggle</div>
      <div style={{ display: 'flex', gap: 16, alignItems: 'center', marginBottom: 22 }}>
        <Toggle on={false} /><Toggle on={true} />
        <span style={{ ...ts('caption'), color: C.textSec }}>20dp track · 14dp thumb</span>
      </div>

      <div style={{ ...ts('sectionHead'), color: C.textSec, marginBottom: 10 }}>Status strips</div>
      <div style={{ display: 'grid', gap: 8 }}>
        <StatusStrip tone="success">
          <div style={{ ...ts('bodyStrong') }}>WEBHOOK <span style={{ color: C.textSec, fontWeight: 400 }}>hooks.slack.com/T0…</span></div>
          <div style={{ ...ts('caption'), color: C.textSec, marginTop: 2 }}>142 ms · 200 OK</div>
        </StatusStrip>
        <StatusStrip tone="error">
          <div style={{ ...ts('bodyStrong') }}>TELEGRAM <span style={{ color: C.textSec, fontWeight: 400 }}>@ops_alerts</span></div>
          <div style={{ ...ts('caption'), color: C.error, marginTop: 2 }}>Network unreachable · retry 2/3</div>
        </StatusStrip>
      </div>
    </div>
  );
}

function FormsArtboard() {
  return (
    <div style={{ padding: 28, background: C.bg, height: '100%', boxSizing: 'border-box', overflow: 'auto',
      fontFamily: 'Vazirmatn, "Segoe UI", system-ui, sans-serif' }}>
      <div style={{ ...ts('pageTitle'), color: C.text, marginBottom: 4 }}>Forms &amp; cards</div>
      <div style={{ ...ts('body'), color: C.textSec, marginBottom: 22 }}>Inputs · selects · cards · tabs · dividers.</div>

      <div style={{ ...ts('sectionHead'), color: C.textSec, marginBottom: 10 }}>Inputs</div>
      <div style={{ maxWidth: 320 }}>
        <Input label="RULE NAME" value="Bank SMS → Slack" />
        <Input label="WEBHOOK URL" value="https://hooks.slack.com/services/T0/B0/x" focused helper="POST · application/json" />
        <Input label="SENDER" placeholder="Contains, equals, regex…" />
      </div>

      <div style={{ ...ts('sectionHead'), color: C.textSec, margin: '20px 0 10px' }}>Selects</div>
      <div style={{ maxWidth: 320 }}>
        <Select label="SIM CARD" value="Any SIM" />
        <Select label="STRATEGY" value="Exponential backoff" />
      </div>

      <div style={{ ...ts('sectionHead'), color: C.textSec, margin: '20px 0 10px' }}>Card</div>
      <Card style={{ maxWidth: 320 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
          <div style={{ ...ts('cardTitle') }}>Bank SMS → Slack</div>
          <Toggle on={true} />
        </div>
        <div style={{ ...ts('caption'), color: C.textSec, marginTop: 4 }}>Sender contains "BANK" · Any SIM · 2 actions</div>
      </Card>

      <div style={{ ...ts('sectionHead'), color: C.textSec, margin: '20px 0 10px' }}>Tabs</div>
      <div style={{ maxWidth: 320, background: C.surface, border: `1px solid ${C.divider}` }}>
        <Tabs items={['All', 'Success', 'Failed']} active={0} />
      </div>

      <div style={{ ...ts('sectionHead'), color: C.textSec, margin: '20px 0 10px' }}>Divider</div>
      <div style={{ maxWidth: 320, background: C.surface, border: `1px solid ${C.divider}`, padding: '4px 14px' }}>
        <div style={{ padding: '10px 0', ...ts('body') }}>Row one</div>
        <Divider />
        <div style={{ padding: '10px 0', ...ts('body') }}>Row two</div>
        <Divider inset={0} />
        <div style={{ padding: '10px 0', ...ts('body'), color: C.textSec }}>Row three (muted)</div>
      </div>
    </div>
  );
}

Object.assign(window, { ButtonsArtboard, FormsArtboard });
