// screens.jsx — The 6 screen mockups.

// ─────────────────────────────────────────────────────────────
// 1. Dashboard
// ─────────────────────────────────────────────────────────────
function DashboardScreen() {
  const logs = [
    { tone: 'success', sender: 'BANK',        action: 'WEBHOOK',  dest: 'hooks.slack.com/T0/B0…', meta: '142 ms · 200 OK',         time: '09:28', sim: '1' },
    { tone: 'success', sender: '+1 415 555…', action: 'TELEGRAM', dest: '@ops_alerts',            meta: '210 ms · OK',             time: '09:24', sim: '2' },
    { tone: 'error',   sender: 'OTP',         action: 'WEBHOOK',  dest: 'api.zapier.com/hooks/…', meta: '5.0 s · timeout',         time: '09:21', sim: '1', badge: 'retry 2/3' },
    { tone: 'success', sender: 'AMAZON',      action: 'SMS',      dest: '+1 415 867 5309',        meta: 'delivered',               time: '09:14', sim: '1' },
    { tone: 'error',   sender: 'UBER',        action: 'WEBHOOK',  dest: 'webhook.site/abc…',      meta: '503 · service unavailable', time: '08:52', sim: '2', badge: 'backup ✓' },
    { tone: 'success', sender: 'DOORDASH',    action: 'TELEGRAM', dest: '@delivery_log',          meta: '180 ms · OK',             time: '08:47', sim: '1' },
  ];
  return (
    <Phone title="SMS Forwarder" subtitle="Service running · since 07:14" actions={['⋮']}>
      {/* Service hero */}
      <div style={{ padding: '16px 16px 12px' }}>
        <ServiceButton running={true} />
      </div>

      {/* Stats bar */}
      <div style={{ display: 'flex', gap: 0, padding: '8px 16px 14px', borderBottom: `1px solid ${C.divider}` }}>
        <Stat label="FORWARDED"  value="1,284" />
        <Stat label="FAILED"     value="12" tone="error" />
        <Stat label="CREDITS"    value="247" />
        <div style={{ alignSelf: 'center' }}>
          <Badge tone="primary">+50</Badge>
        </div>
      </div>

      {/* Tabs */}
      <div style={{ background: C.bg }}>
        <Tabs items={['All', 'Success', 'Failed']} active={0} />
      </div>

      <SectionHeader right={<TextButton color={C.textSec} style={{ ...ts('caption') }}>CLEAR</TextButton>}>Recent activity</SectionHeader>

      {/* Log items */}
      <div style={{ padding: '0 16px 24px', display: 'grid', gap: 8 }}>
        {logs.map((l, i) => (
          <StatusStrip key={i} tone={l.tone}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
              <div style={{ minWidth: 0, flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
                  <span style={{ ...ts('badge'), color: C.primary, fontWeight: 600 }}>{l.action}</span>
                  <span style={{ ...ts('bodyStrong'), color: C.text, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace', fontSize: 12 }}>{l.dest}</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 4 }}>
                  <span style={{ ...ts('caption'), color: l.tone === 'error' ? C.error : C.textSec }}>{l.meta}</span>
                  {l.badge && <Badge tone={l.tone === 'error' ? 'neutral' : 'success'}>{l.badge}</Badge>}
                </div>
                <div style={{ ...ts('caption'), color: C.textMuted, marginTop: 2 }}>from {l.sender} · SIM {l.sim}</div>
              </div>
              <div style={{ ...ts('caption'), color: C.textMuted, fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>{l.time}</div>
            </div>
          </StatusStrip>
        ))}
      </div>
    </Phone>
  );
}

// ─────────────────────────────────────────────────────────────
// 2. Rules List
// ─────────────────────────────────────────────────────────────
function RulesListScreen() {
  const rules = [
    { name: 'Bank SMS → Slack',          summary: 'Sender contains "BANK" · Any SIM',   actions: 2, on: true },
    { name: 'OTP codes → Telegram',      summary: 'Body regex "\\d{6}" · SIM 1',         actions: 1, on: true },
    { name: 'Delivery alerts',           summary: 'Sender ∈ AMAZON, UBER, DOORDASH',     actions: 2, on: true },
    { name: 'Spam blocklist',            summary: 'Sender contains "PROMO" · drop',     actions: 0, on: false },
    { name: 'Forward all (SIM 2)',       summary: 'Any sender · SIM 2',                  actions: 1, on: false },
  ];
  return (
    <Phone title="Rules" subtitle="5 rules · 3 enabled" back actions={['↻', '⋮']} fab="+">
      {/* Toolbar */}
      <div style={{ display: 'flex', gap: 8, padding: '12px 16px', borderBottom: `1px solid ${C.divider}` }}>
        <OutlineButton style={{ flex: 1, height: 34, fontSize: 12 }}>↑  BACKUP</OutlineButton>
        <OutlineButton style={{ flex: 1, height: 34, fontSize: 12 }}>↓  RESTORE</OutlineButton>
      </div>

      {/* Rule cards */}
      <div style={{ padding: '14px 16px 24px', display: 'grid', gap: 10 }}>
        {rules.map((r, i) => (
          <Card key={i} style={{ opacity: r.on ? 1 : 0.65 }}>
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
              <div style={{ color: C.textMuted, fontFamily: 'ui-monospace, monospace', paddingTop: 2, cursor: 'grab' }}>⋮⋮</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
                  <div style={{ ...ts('cardTitle'), color: C.text }}>{r.name}</div>
                  <Toggle on={r.on} />
                </div>
                <div style={{ ...ts('caption'), color: C.textSec, marginTop: 4 }}>{r.summary}</div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 10 }}>
                  <Badge tone={r.actions ? 'primary' : 'neutral'}>{r.actions} action{r.actions === 1 ? '' : 's'}</Badge>
                  <div style={{ display: 'flex', gap: 4, margin: -6 }}>
                    <TextButton color={C.textSec} style={{ ...ts('caption'), fontWeight: 600 }}>EDIT</TextButton>
                    <TextButton color={C.textSec} style={{ ...ts('caption'), fontWeight: 600 }}>COPY</TextButton>
                    <TextButton color={C.error} style={{ ...ts('caption'), fontWeight: 600 }}>DELETE</TextButton>
                  </div>
                </div>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </Phone>
  );
}

// ─────────────────────────────────────────────────────────────
// 3. Rule Editor
// ─────────────────────────────────────────────────────────────
function RuleEditorScreen() {
  return (
    <Phone title="Edit rule" back actions={['✓']}>
      <div style={{ padding: '16px 16px 0' }}>
        <Input label="RULE NAME" value="Bank SMS → Slack" />
        <Select label="SIM CARD" value="Any SIM" />
      </div>

      <SectionHeader>Sender filter</SectionHeader>
      <div style={{ padding: '0 16px' }}>
        <Select label="MATCH TYPE" value="Contains" />
        <Input label="VALUE" value="BANK" />
      </div>

      <SectionHeader>Message filter</SectionHeader>
      <div style={{ padding: '0 16px' }}>
        <Select label="MATCH TYPE" value="Regex" />
        <Input label="PATTERN" value="^Transfer of \\$\\d+" />
      </div>

      <SectionHeader right={<TextButton color={C.primary} style={{ ...ts('caption'), fontWeight: 700 }}>+ ADD</TextButton>}>
        Actions · 2
      </SectionHeader>
      <div style={{ padding: '0 16px 24px', display: 'grid', gap: 8 }}>
        <Card noPad>
          <div style={{ padding: '12px 14px', display: 'flex', alignItems: 'flex-start', gap: 10 }}>
            <div style={{ color: C.textMuted, paddingTop: 2 }}>⋮⋮</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ ...ts('badge'), color: C.primary, fontWeight: 700, marginBottom: 4 }}>WEBHOOK</div>
              <div style={{ ...ts('bodyStrong'), fontFamily: 'ui-monospace, monospace', fontSize: 12,
                color: C.text, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                hooks.slack.com/services/T0/B0/x
              </div>
              <div style={{ ...ts('caption'), color: C.textSec, marginTop: 4, fontFamily: 'ui-monospace, monospace' }}>
                {'{sender} — {message}'}
              </div>
            </div>
            <div style={{ color: C.textMuted, fontSize: 16 }}>›</div>
          </div>
        </Card>
        <Card noPad>
          <div style={{ padding: '12px 14px', display: 'flex', alignItems: 'flex-start', gap: 10 }}>
            <div style={{ color: C.textMuted, paddingTop: 2 }}>⋮⋮</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ ...ts('badge'), color: C.primary, fontWeight: 700, marginBottom: 4 }}>TELEGRAM</div>
              <div style={{ ...ts('bodyStrong'), fontFamily: 'ui-monospace, monospace', fontSize: 12, color: C.text }}>
                @bank_alerts · chat 123…
              </div>
              <div style={{ ...ts('caption'), color: C.textSec, marginTop: 4, fontFamily: 'ui-monospace, monospace' }}>
                🏦 {'{message}'}
              </div>
            </div>
            <div style={{ color: C.textMuted, fontSize: 16 }}>›</div>
          </div>
        </Card>
      </div>

      {/* Save bar */}
      <div style={{ position: 'sticky', bottom: 0, padding: '12px 16px', background: C.bg, borderTop: `1px solid ${C.divider}`, display: 'flex', gap: 10 }}>
        <OutlineButton style={{ flex: 1, height: 44 }}>Cancel</OutlineButton>
        <PrimaryButton full style={{ flex: 1, height: 44 }}>Save rule</PrimaryButton>
      </div>
    </Phone>
  );
}

// ─────────────────────────────────────────────────────────────
// 4. Action Editor
// ─────────────────────────────────────────────────────────────
function ActionEditorScreen() {
  return (
    <Phone title="Edit action" back actions={['✓']}>
      <SectionHeader>Type</SectionHeader>
      <div style={{ padding: '0 16px' }}>
        <div style={{ display: 'flex', gap: 6 }}>
          {['Webhook', 'SMS', 'Telegram'].map((t, i) => (
            <div key={t} style={{
              flex: 1, height: 36, display: 'flex', alignItems: 'center', justifyContent: 'center',
              border: `1px solid ${i === 0 ? C.text : C.divider}`,
              background: i === 0 ? C.text : C.surface,
              color: i === 0 ? '#fff' : C.text,
              ...ts('bodyStrong'), borderRadius: 4,
            }}>{t}</div>
          ))}
        </div>
      </div>

      <SectionHeader>Destination</SectionHeader>
      <div style={{ padding: '0 16px' }}>
        <Input label="WEBHOOK URL" value="https://hooks.slack.com/services/T0/B0/x" focused helper="POST · application/json" />
        <Select label="METHOD" value="POST" />
      </div>

      <SectionHeader>Message template</SectionHeader>
      <div style={{ padding: '0 16px' }}>
        <div style={{
          background: C.surface, border: `1px solid ${C.divider}`, borderRadius: 4,
          padding: 12, minHeight: 88,
          fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
          fontSize: 12, color: C.text, lineHeight: 1.5,
        }}>
          {'{ "text": "*'}<span style={{ color: C.primary }}>{'{sender}'}</span>{'*\\n'}<span style={{ color: C.primary }}>{'{message}'}</span>{'", "ts": "'}<span style={{ color: C.primary }}>{'{timestamp}'}</span>{'" }'}
        </div>
        <div style={{ ...ts('caption'), color: C.textSec, marginTop: 6, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <Badge>{'{sender}'}</Badge>
          <Badge>{'{message}'}</Badge>
          <Badge>{'{timestamp}'}</Badge>
          <Badge>{'{sim}'}</Badge>
        </div>
      </div>

      <SectionHeader right={<Toggle on={true} />}>Transform</SectionHeader>
      <div style={{ padding: '0 16px' }}>
        <Input label="EXTRACT (REGEX)" value="OTP: (\\d{6})" />
        <Input label="REPLACE WITH" value="Your code is $1" />
      </div>

      <SectionHeader right={<Toggle on={true} />}>Retry</SectionHeader>
      <div style={{ padding: '0 16px' }}>
        <div style={{ display: 'flex', gap: 10 }}>
          <Input label="MAX RETRIES" value="3" style={{ flex: 1 }} />
          <Input label="DELAY (S)" value="5" style={{ flex: 1 }} />
        </div>
        <Select label="STRATEGY" value="Exponential backoff" />
      </div>

      <SectionHeader right={<Toggle on={false} />}>Backup action</SectionHeader>
      <div style={{ padding: '0 16px 4px', ...ts('caption'), color: C.textSec }}>
        Run a fallback when this action fails after all retries.
      </div>

      <SectionHeader>Error handling</SectionHeader>
      <div style={{ padding: '0 16px 24px', display: 'grid', gap: 10 }}>
        {[
          { label: 'Notify on failure', on: true },
          { label: 'Log to file',       on: true },
          { label: 'Stop rule on fail', on: false },
        ].map((o, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '10px 12px', background: C.surface, border: `1px solid ${C.divider}`, borderRadius: 4 }}>
            <div style={{ ...ts('body'), color: C.text }}>{o.label}</div>
            <div style={{
              width: 18, height: 18, borderRadius: 3,
              border: `1.5px solid ${o.on ? C.text : C.divider}`,
              background: o.on ? C.text : C.surface,
              color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12,
            }}>{o.on ? '✓' : ''}</div>
          </div>
        ))}
      </div>

      <div style={{ position: 'sticky', bottom: 0, padding: '12px 16px', background: C.bg, borderTop: `1px solid ${C.divider}`, display: 'flex', gap: 10 }}>
        <OutlineButton style={{ flex: 1, height: 44 }}>Test</OutlineButton>
        <PrimaryButton full style={{ flex: 1.4, height: 44 }}>Save action</PrimaryButton>
      </div>
    </Phone>
  );
}

// ─────────────────────────────────────────────────────────────
// 5. Credits
// ─────────────────────────────────────────────────────────────
function CreditsScreen() {
  return (
    <Phone title="Credits" back actions={['?']}>
      {/* Big balance */}
      <div style={{ padding: '40px 24px 32px', textAlign: 'center', borderBottom: `1px solid ${C.divider}` }}>
        <div style={{ ...ts('sectionHead'), color: C.textSec, marginBottom: 8 }}>Balance</div>
        <div style={{ ...ts('number'), color: C.text, fontVariantNumeric: 'tabular-nums' }}>247</div>
        <div style={{ ...ts('body'), color: C.textSec, marginTop: 4 }}>credits remaining</div>
        <div style={{ marginTop: 12, display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <Badge tone="success">+50 this week</Badge>
          <Badge>~12 days left</Badge>
        </div>
      </div>

      {/* Earn */}
      <SectionHeader>Earn credits</SectionHeader>
      <div style={{ padding: '0 16px', display: 'grid', gap: 10 }}>
        <Card>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <div style={{ ...ts('cardTitle') }}>Watch an ad</div>
              <div style={{ ...ts('caption'), color: C.textSec, marginTop: 2 }}>+10 credits · ~30 sec</div>
            </div>
            <PrimaryButton color={C.primary} style={{ height: 36, padding: '0 14px' }}>Watch</PrimaryButton>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 10 }}>
            <div style={{ flex: 1, height: 3, background: C.divider, borderRadius: 2, overflow: 'hidden' }}>
              <div style={{ width: '32%', height: '100%', background: C.primary }} />
            </div>
            <div style={{ ...ts('caption'), color: C.textSec, fontVariantNumeric: 'tabular-nums' }}>1 / 3 today</div>
          </div>
        </Card>
        <Card>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <div style={{ ...ts('cardTitle') }}>Redeem token</div>
              <div style={{ ...ts('caption'), color: C.textSec, marginTop: 2 }}>Paste a referral or promo code.</div>
            </div>
            <OutlineButton style={{ height: 36 }}>Redeem</OutlineButton>
          </div>
        </Card>
      </div>

      <SectionHeader>Reset</SectionHeader>
      <div style={{ padding: '0 16px' }}>
        <Card>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <div style={{ ...ts('cardTitle') }}>Free tier resets in</div>
              <div style={{ ...ts('caption'), color: C.textSec, marginTop: 2 }}>+100 credits every Monday at 00:00</div>
            </div>
            <div style={{ ...ts('number'), fontSize: 28, lineHeight: '32px', color: C.text, fontVariantNumeric: 'tabular-nums' }}>2d 14h</div>
          </div>
        </Card>
      </div>

      <SectionHeader>Usage</SectionHeader>
      <div style={{ padding: '0 16px 24px' }}>
        <Card noPad>
          {[
            { l: 'Webhook forward',  v: '1 credit' },
            { l: 'Telegram forward', v: '1 credit' },
            { l: 'SMS forward',      v: '3 credits' },
            { l: 'Retry attempt',    v: 'free' },
          ].map((r, i, a) => (
            <React.Fragment key={r.l}>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '12px 14px' }}>
                <div style={{ ...ts('body'), color: C.text }}>{r.l}</div>
                <div style={{ ...ts('bodyStrong'), color: C.textSec, fontVariantNumeric: 'tabular-nums' }}>{r.v}</div>
              </div>
              {i < a.length - 1 && <Divider />}
            </React.Fragment>
          ))}
        </Card>
      </div>
    </Phone>
  );
}

// ─────────────────────────────────────────────────────────────
// 6. Action Logs
// ─────────────────────────────────────────────────────────────
function ActionLogsScreen() {
  const logs = [
    { tone: 'success', action: 'WEBHOOK',  dest: 'hooks.slack.com/T0/B0/x',     meta: '142 ms · 200 OK', time: '09:28', body: 'Transfer of $420 to acct ****1234',                 sim: '1' },
    { tone: 'error',   action: 'WEBHOOK',  dest: 'api.zapier.com/hooks/abc',    meta: '5.0 s · timeout',  time: '09:21', body: 'OTP: 829304 — do not share',                          sim: '1', badge: 'retry 2/3' },
    { tone: 'success', action: 'TELEGRAM', dest: '@ops_alerts · chat 14523',    meta: '210 ms · OK',     time: '09:24', body: 'Server alert from monitoring',                         sim: '2' },
    { tone: 'error',   action: 'WEBHOOK',  dest: 'webhook.site/abc',            meta: '503 · service unavailable', time: '08:52', body: 'Your Uber driver Ahmed is 2 min away',  sim: '2', backup: true },
    { tone: 'success', action: 'SMS',      dest: '+1 415 867 5309',             meta: 'delivered',       time: '09:14', body: 'Your AMAZON package has been delivered',               sim: '1' },
    { tone: 'success', action: 'TELEGRAM', dest: '@delivery_log',               meta: '180 ms · OK',     time: '08:47', body: 'DoorDash: order on the way',                            sim: '1' },
    { tone: 'error',   action: 'WEBHOOK',  dest: 'self-hosted.example.com/in',  meta: '0 ms · DNS',      time: '08:14', body: 'PROMO 25% off only this weekend',                       sim: '1', badge: 'retry 3/3' },
  ];
  return (
    <Phone title="Action logs" subtitle="Last 24 hours · 247 entries" back actions={['↓', '⋮']}>
      {/* Stats summary */}
      <div style={{ display: 'flex', padding: '14px 16px', borderBottom: `1px solid ${C.divider}` }}>
        <Stat label="TOTAL"   value="247" />
        <Stat label="SUCCESS" value="231" tone="success" />
        <Stat label="FAILED"  value="16"  tone="error" />
        <Stat label="AVG"     value="178ms" />
      </div>

      <div style={{ background: C.bg }}>
        <Tabs items={['All', 'Failed']} active={0} />
      </div>

      <div style={{ padding: '12px 16px 24px', display: 'grid', gap: 8 }}>
        {logs.map((l, i) => (
          <StatusStrip key={i} tone={l.tone}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
              <div style={{ ...ts('badge'), color: C.primary, fontWeight: 700 }}>{l.action}</div>
              <div style={{ ...ts('caption'), color: C.textMuted, fontVariantNumeric: 'tabular-nums' }}>{l.time}</div>
            </div>
            <div style={{ ...ts('bodyStrong'), color: C.text, fontFamily: 'ui-monospace, monospace', fontSize: 12,
              overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginTop: 2 }}>
              {l.dest}
            </div>
            <div style={{ ...ts('body'), color: C.textSec, marginTop: 4,
              overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {l.body}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 6, flexWrap: 'wrap' }}>
              <span style={{ ...ts('caption'), color: l.tone === 'error' ? C.error : C.textSec, fontFamily: 'ui-monospace, monospace' }}>{l.meta}</span>
              <span style={{ ...ts('caption'), color: C.textMuted }}>· SIM {l.sim}</span>
              {l.badge && <Badge tone="neutral">{l.badge}</Badge>}
              {l.backup && <Badge tone="info">backup ✓</Badge>}
            </div>
          </StatusStrip>
        ))}
      </div>

      <div style={{ position: 'sticky', bottom: 0, padding: '12px 16px', background: C.bg, borderTop: `1px solid ${C.divider}`, display: 'flex', gap: 10 }}>
        <OutlineButton style={{ flex: 1, height: 40 }}>Export</OutlineButton>
        <OutlineButton color={C.error} style={{ flex: 1, height: 40, color: C.error, borderColor: C.errorWk }}>Clear logs</OutlineButton>
      </div>
    </Phone>
  );
}

Object.assign(window, {
  DashboardScreen, RulesListScreen, RuleEditorScreen, ActionEditorScreen, CreditsScreen, ActionLogsScreen,
});
