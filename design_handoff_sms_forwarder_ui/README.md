# Handoff: SMS Forwarder — Android UI System

## Overview

A complete minimal Material 3 UI system for **SMS Forwarder**, an Android utility
that listens for incoming SMS, matches them against rules, and forwards them to
webhooks / Telegram / SMS. The audience is technical (developers, sysadmins),
so the visual language is calm, dense, monospaced where appropriate, and amber
is used as an accent — not as the dominant brand color.

This bundle contains:

- **6 screen designs** (Dashboard, Rules list, Rule editor, Action editor,
  Credits, Action logs)
- **A component library** (buttons, cards, badges, status strips, toggles,
  inputs, tabs, dividers)
- **Drop-in Android XML resources** under `android/res/` — `colors.xml`,
  `themes.xml`, `styles.xml`, and a set of `drawable/*.xml`

## About the Design Files

The HTML/JSX files in this bundle (`SMS Forwarder UI System.html` plus its
`*.jsx` modules) are **design references** — a React prototype shown on an
infinite design canvas that demonstrates intended look, hierarchy, copy, and
spacing. They are **not production code to copy directly**.

The task is to **recreate these designs as native Android views** using the
target codebase's existing layout patterns (XML layouts with
`LinearLayout`/`ConstraintLayout`/`RecyclerView`, or Jetpack Compose if the
project has migrated). The Android XML files under `android/res/` in this
bundle **are** intended to be dropped into the project as-is — they are the
real, shippable token / theme / drawable files this design is built on.

To preview the designs visually: open `SMS Forwarder UI System.html` in any
modern browser. Drag to pan, scroll to zoom, click an artboard label to open it
fullscreen, ←/→ to navigate.

## Fidelity

**High-fidelity.** Colors, spacing, type sizes, line heights, and copy are all
final. Reproduce them pixel-for-pixel. The only intentional substitution is
that the prototype renders in **Vazirmatn** (a free Google Font with the same
humanist Latin/Persian proportions) because Shabnam is not available as a web
font. In the real app, swap to `@font/shabnam` / `@font/shabnam_bold` via the
already-configured theme.

## Design Tokens

### Colors

All tokens are in `android/res/values/colors.xml`. Hex reference:

| Token | Hex | Role |
|---|---|---|
| `background` | `#FAF9F7` | App background (warm off-white) |
| `surface` | `#FFFFFF` | Cards, inputs, sheets |
| `surface_alt` | `#F5F4F1` | Neutral badge fill, pressed states |
| `divider` | `#E7E5E4` | 1dp hairlines, card borders |
| `divider_light` | `#F1EFEC` | In-card row separators |
| `text_primary` | `#1C1917` | Primary text, **filled-button bg** |
| `text_secondary` | `#78716C` | Captions, helper text |
| `text_muted` | `#A8A29E` | Timestamps, placeholders |
| `primary` | `#D97706` | Amber accent (action labels, focus) |
| `primary_weak` | `#FEF3C7` | Amber badge fill |
| `success` | `#059669` | Start service, success strips/badges |
| `success_weak` | `#D1FAE5` | Success badge fill |
| `error` | `#DC2626` | Stop service, error strips/badges, destructive text |
| `error_weak` | `#FEE2E2` | Error badge fill |
| `info` | `#1E40AF` | Info badges (e.g. SIM number) |
| `info_weak` | `#DBEAFE` | Info badge fill |

**Critical usage rule:** the primary filled button background is
`text_primary`, **not** `primary`. Amber is reserved for: action-type labels
(WEBHOOK / TELEGRAM in uppercase), tab underlines, the "Watch ad" CTA,
and primary-tone badges. Service start/stop owns the green/red semantics.

### Spacing scale

| Token | dp | Use |
|---|---|---|
| `space_xs` | 4 | Inline gaps in chip rows |
| `space_sm` | 8 | Stat groups, badge spacing |
| `space_md` | 12 | Inside cards, between rows |
| `space_lg` | 16 | Screen edge padding |
| `space_xl` | 24 | Section breaks |

Screen horizontal padding is **16dp**. Card internal padding is **12–14dp**.
The list item left strip is **3dp wide**.

### Typography

Font: **Shabnam** (regular + bold). Configured in the theme so all `TextView`
instances inherit it.

| Style | sp | Weight | Letter-spacing | Use |
|---|---|---|---|---|
| `Text.PageTitle` | 22 | Bold | -0.01 | Top app bar title |
| `Text.SectionHeader` | 11 | Bold + ALLCAPS | 0.08 | Section labels |
| `Text.CardTitle` | 15 | Bold | 0 | Rule names, card headers |
| `Text.Body` | 13 | Regular | 0 | Default body text |
| `Text.Caption` | 11 | Regular | 0 | Captions, helper text |
| `Text.Badge` | 10 | Bold + ALLCAPS | 0.06 | Pill labels |
| `Text.Mono` | 12 | Regular | 0 | URLs, regex, templates (monospace) |

Use `Text.Mono` for any technical string the user typed: URLs, regex patterns,
JSON message templates, chat IDs. Phone numbers also use tabular figures.

### Border radius

- Cards: **6dp**
- Inputs, buttons, badges (large): **4dp**
- Small badges: **3dp**
- Status strips: **4dp** (only on the outer container; the 3dp left rail is
  flush)

### Shadows

**None.** Cards are flat — `cardElevation=0dp`, 1dp `@color/divider` stroke.
The FAB is the only exception (subtle 2dp/8dp shadow).

---

## Screens

### 1. Dashboard

**Purpose:** Service control + live activity feed.

**Layout (top → bottom):**

1. **Top app bar** (52dp) — title "SMS Forwarder", subtitle "Service running ·
   since 07:14", overflow menu on the right.
2. **Service hero button** — `Button.Service`, full width (with 16dp side
   padding), 56dp tall. **Green when stopped (start)**, **red when running
   (stop)**. Centered icon dot + uppercase label. Most prominent element on
   screen.
3. **Stats bar** — 4 columns: FORWARDED, FAILED (red), CREDITS, plus a
   `+50` primary-tone badge for credits gained this period. 8/14dp vertical
   padding, divider below.
4. **Tabs** — `All / Success / Failed`. Active tab has 2dp amber underline.
5. **Section header** — "RECENT ACTIVITY" with a right-aligned "CLEAR" text
   button.
6. **Log list** — vertical stack of status-strip rows (see Components →
   Status strip). Each row shows:
   - Action type badge (WEBHOOK/TELEGRAM/SMS, amber bold uppercase)
   - Destination (monospace, ellipsized)
   - Meta line: "142 ms · 200 OK" (caption, red if failed)
   - From-line: "from BANK · SIM 1" (text_muted)
   - Right column: timestamp (text_muted, tabular figures)
   - Optional inline badge: `retry 2/3`, `backup ✓`

**State:** service running/stopped is the only required local state.
Tabs filter the same source list. The stats bar reads from session counters.

---

### 2. Rules list

**Purpose:** Manage forwarding rules.

**Layout:**

1. Top app bar — title "Rules", subtitle "5 rules · 3 enabled", back arrow,
   refresh + overflow on the right.
2. **Toolbar row** — two equal-width outline buttons: `↑ BACKUP` / `↓ RESTORE`,
   34dp tall.
3. **Rule cards** — vertical list, 10dp gap, 14dp screen padding. Each card:
   - Drag grip on the left (⋮⋮ glyph, `text_muted`)
   - Rule name (CardTitle)
   - Toggle on the right
   - Conditions summary (Caption, text_secondary)
   - Bottom row: action count badge on left, `EDIT / COPY / DELETE` text
     buttons on the right (DELETE in `error`)
   - Disabled rules render at 65% opacity
4. **FAB** — bottom-right, 52dp, dark fill, `+` glyph, 4dp radius (a true
   square FAB matching the system's flat aesthetic, not the default circular
   M3 FAB)

**Interactions:** long-press + drag the grip to reorder.

---

### 3. Rule editor

**Purpose:** Create or edit a single forwarding rule.

**Layout:**

1. Top app bar — title "Edit rule", back arrow, ✓ confirm action.
2. **Top fields** (16dp padding) — Rule name input, SIM card select ("Any
   SIM" / "SIM 1" / "SIM 2").
3. **Section: Sender filter** — match-type select (Contains / Equals / Regex /
   Starts with / Ends with), value input.
4. **Section: Message filter** — same shape as sender filter.
5. **Section: Actions · N** — header has right-aligned amber `+ ADD` button.
   Below: a stack of attached actions, each as a card with:
   - Drag grip
   - Action type uppercase amber badge (WEBHOOK / TELEGRAM / SMS)
   - Destination (monospace, ellipsized)
   - Template preview (monospace caption)
   - Right-side `›` chevron (tap to open Action editor)
6. **Bottom save bar** — sticky, 12dp padding, top divider. Left: outline
   `Cancel`. Right: filled `Save rule` (1.4× wider).

All inputs use the outlined `TextInputLayout` style with the focused state
swapping the stroke to `text_primary` (not amber).

---

### 4. Action editor

**Purpose:** Configure a single forwarding action.

**Layout:**

1. Top app bar — "Edit action", back, ✓.
2. **Section: Type** — segmented control, 3 equal buttons: Webhook / SMS /
   Telegram. Selected = `text_primary` fill, white label. Unselected = surface,
   text_primary label, divider stroke.
3. **Section: Destination** — fields shown depend on type:
   - Webhook: URL input (monospace) + method select (POST/GET/PUT)
   - SMS: phone number input + SIM card select
   - Telegram: bot token input + chat ID input
4. **Section: Message template** — multiline monospace textarea with token
   chips below it: `{sender}`, `{message}`, `{timestamp}`, `{sim}`. Tapping a
   chip inserts the token at the cursor.
5. **Section: Transform** *(optional, toggle in header)* — extract regex input
   + replace input.
6. **Section: Retry** *(optional, toggle in header)* — max retries (number),
   delay (number, seconds), strategy select (Fixed / Linear / Exponential
   backoff).
7. **Section: Backup action** *(optional, toggle in header)* — when on,
   reveals a nested action selector.
8. **Section: Error handling** — three checkbox rows:
   - Notify on failure (default on)
   - Log to file (default on)
   - Stop rule on fail (default off)
9. **Bottom save bar** — `Test` outline + `Save action` filled (1.4× wider).

Token chips inside the template editor use `bg_badge_neutral` with monospace
text.

---

### 5. Credits

**Purpose:** Show balance, ways to earn more, free-tier reset, and unit costs.

**Layout:**

1. Top app bar — "Credits", back, help (?) action.
2. **Balance hero** (40/24dp padding, centered) —
   - "BALANCE" section header
   - Huge number: **64sp Bold, tabular figures**
   - "credits remaining" caption
   - Two inline badges: `+50 this week` (success), `~12 days left` (neutral)
   - Bottom divider
3. **Section: Earn credits** — two cards:
   - **Watch an ad** — title + caption ("+10 credits · ~30 sec") + amber
     primary `Watch` button. Below: a 3dp progress bar showing daily ad cap
     ("1 / 3 today").
   - **Redeem token** — title + caption + outline `Redeem` button.
4. **Section: Reset** — card showing "Free tier resets in" + a 28sp tabular
   countdown like `2d 14h`.
5. **Section: Usage** — flat-stack card with 4 rows: Webhook forward / Telegram
   forward / SMS forward / Retry attempt. Each row: label on left, cost on
   right ("1 credit", "3 credits", "free").

---

### 6. Action logs

**Purpose:** Detailed per-action execution history for debugging.

**Layout:**

1. Top app bar — "Action logs", subtitle "Last 24 hours · 247 entries", back,
   download (↓), overflow.
2. **Stats row** — 4 stats: TOTAL, SUCCESS (green), FAILED (red), AVG (e.g.
   "178ms").
3. **Tabs** — `All / Failed`.
4. **Log list** — denser status-strip rows than the dashboard. Each row:
   - Header line: action-type badge (amber) on the left, timestamp on the
     right.
   - Destination (monospace, ellipsized).
   - Message body preview (one line, ellipsized).
   - Meta footer (caption row): "142 ms · 200 OK" + "SIM 1" + optional
     `retry 2/3` and `backup ✓` badges.
5. **Sticky bottom bar** — outline `Export` + outline `Clear logs` (text and
   border in `error`).

---

## Components

Build these as reusable XML includes or Compose composables. Reproduce the
exact dimensions below.

### Card

- Background: `@drawable/bg_card`
- Padding: 14dp
- Internal vertical gap between header row and body: 4dp
- For tappable cards, swap to `@drawable/ripple_card`.

### Primary button (`Button.Primary`)

- Height: 40dp · radius 4dp · padding 0/18dp · `Text.Body` weight 500
- Background: `@color/text_primary` · text: white

### Outline button (`Button.Outline`)

- Height: 36dp · radius 4dp · padding 0/14dp
- Stroke: 1dp `@color/divider` · text: `@color/text_primary`

### Text button (`Button.Text` / `Button.Destructive`)

- Inline. No background, no border. 6dp padding.
- Destructive variant uses `@color/error`.

### Service button (`Button.Service`)

- Full width (after 16dp screen padding) · 56dp height · radius 4dp
- Uppercase, letter-spacing 0.04
- Backgrounds: `@color/success` when stopped → "Start service"; `@color/error`
  when running → "Stop service".
- Leading 8dp dot indicator (white). When running, the dot has a 4dp
  semi-transparent white halo.

### Badge

A small pill: 2dp/7dp padding · radius 3dp · `Text.Badge`. Six tones via
drawables (`bg_badge_*.xml`): success / error / primary / info / neutral, plus
a solid `error` variant for HTTP status codes (503, 504…).

### Status strip

A list-row container with a 3dp colored left rail. Implement as a `LinearLayout`
with `@drawable/strip_success` (or `_error`) as the background. Internal
padding: 10dp top/bottom, 12dp right, 13dp left (12dp + 1dp to clear the rail).

### Toggle

Material switch with `colorPrimary` overridden locally to `text_primary` so it
reads neutral, not amber. Track 36×20dp, thumb 14dp.

### Input

`TextInputLayout` with the `Input.Outlined` style. 40dp content height. Caption
above ("RULE NAME", `Text.SectionHeader` style at 11sp).

### Tabs

`TabLayout` with `tabIndicatorColor=@color/primary`, indicator height 2dp,
selected text color `text_primary`, unselected `text_secondary`,
`Text.Body` weight 500.

### Section header (in-screen)

Padding 14/16/8/16 dp, `Text.SectionHeader` style, optional right-aligned
slot for a text button or toggle.

---

## Interactions & Behavior

- **Service start/stop**: foreground service toggle. The button swaps color +
  label immediately on tap; show a small spinner inside the button until the
  service confirms its state.
- **Rule toggle**: persists immediately to DataStore/SharedPreferences. Disabled
  rules render at 65% opacity but stay tappable.
- **Drag-reorder**: standard `ItemTouchHelper` on the RecyclerView. Drag handle
  is the `⋮⋮` grip; long-press anywhere on the card also starts the drag.
- **EDIT / COPY / DELETE**: tap targets must be ≥44dp despite the small visual
  size — wrap the text in 8dp invisible padding.
- **Tabs**: `ViewPager2` if the lists are heavy, otherwise filter in-place.
- **Save bars**: bottom-anchored. They overlap the content; the scroll view
  must add 72dp bottom padding so the last row isn't hidden behind the bar.
- **RTL**: every screen must mirror correctly. Use `start`/`end` instead of
  `left`/`right` everywhere. Status-strip rail flips to the right edge in RTL.
- **Empty states**: rules list and action logs need empty illustrations — not
  designed yet; ask before shipping.

---

## State Management

Per screen, the minimum view-model state:

- **Dashboard**: `serviceRunning: Boolean`, `stats: Counters`,
  `logs: Flow<List<LogEntry>>`, `tab: All | Success | Failed`.
- **Rules list**: `rules: Flow<List<Rule>>`, `editing: Rule?`.
- **Rule editor**: `rule: Rule`, mutated locally, persisted only on Save.
- **Action editor**: `action: Action` + the four optional-section toggles
  (`transformEnabled`, `retryEnabled`, `backupEnabled`).
- **Credits**: `balance: Int`, `adsWatchedToday: Int`, `resetCountdown: Duration`.
- **Action logs**: `logs: PagingData<ActionLog>`, `filter: All | Failed`.

Persist rules and credits to Room. The service publishes log entries via a
broadcast/Flow that both dashboard and logs subscribe to.

---

## Assets

- **No SVG icons.** Use Unicode glyphs or text labels for everything (`☰`, `↑`,
  `↓`, `›`, `+`, `✓`, `←`, `⋮`). If you want richer icons later, switch to the
  Material Symbols font.
- **No images** in the design. Empty states will eventually need placeholders
  — flag this with the designer (me) before shipping.
- **Fonts**: Shabnam regular + bold must be added under `res/font/`. The theme
  already references `@font/shabnam` and `@font/shabnam_bold`.

---

## Files in this bundle

### Design references (preview only)

- `SMS Forwarder UI System.html` — open in a browser to see the canvas.
- `design-canvas.jsx` — the pan/zoom canvas component.
- `tokens.jsx` — color and type tokens, palette + type artboards.
- `ui.jsx` — primitive components (Card, Button, Badge, Strip, Input…).
- `phone.jsx` — phone-frame wrapper (status bar + app bar + nav pill).
- `components-showcase.jsx` — the two component artboards.
- `screens.jsx` — the six screen mockups.
- `xml-blocks.jsx` — the code-display artboards (sourced from the files
  below).

### Drop-in Android resources

```
android/res/
├── values/
│   ├── colors.xml      ← full token set
│   ├── themes.xml      ← Theme.SmsForwarder · Material3 light
│   └── styles.xml      ← Text.* + Button.* + Card.Flat + Input.Outlined
└── drawable/
    ├── bg_card.xml             ← 1dp hairline, 6dp radius card
    ├── bg_input.xml            ← selector: focused / default input
    ├── bg_badge_success.xml
    ├── bg_badge_error.xml
    ├── bg_badge_primary.xml
    ├── bg_badge_neutral.xml
    ├── strip_success.xml       ← 3dp green left rail layer-list
    ├── strip_error.xml         ← 3dp red left rail layer-list
    ├── divider.xml             ← 1dp hairline
    └── ripple_card.xml         ← tappable card variant
```

---

## Suggested implementation order

1. Drop `android/res/values/colors.xml`, `themes.xml`, `styles.xml` into the
   project. Apply `Theme.SmsForwarder` in the manifest.
2. Add Shabnam fonts under `res/font/` (or fall back to the system serif —
   Vazirmatn from Google Fonts is a fine open-source substitute).
3. Implement the **status strip list row** first — it's used on both the
   Dashboard and the Action logs and is the densest piece of layout.
4. Then the **rule card**, then the **action card** (used inside the rule
   editor). After those three list items, the screens fall together quickly.
5. Wire the service start/stop button last, after the data layer is in place.

---

If anything is ambiguous, the source of truth is the HTML mockup. Open
`SMS Forwarder UI System.html`, focus the relevant artboard with the
expand button, and inspect with browser devtools — every spacing and color
value is in the inline styles.
