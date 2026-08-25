// xml-blocks.jsx — Android resource XML samples as code-block artboards.

function CodeBlock({ title, file, code, w = 540 }) {
  return (
    <div style={{ padding: 24, background: C.bg, height: '100%', boxSizing: 'border-box', overflow: 'auto' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 4 }}>
        <div style={{ ...ts('pageTitle'), color: C.text }}>{title}</div>
        <div style={{ ...ts('caption'), color: C.textSec, fontFamily: 'ui-monospace, monospace' }}>{file}</div>
      </div>
      <pre style={{
        margin: 0, padding: 18,
        background: C.surface, border: `1px solid ${C.divider}`, borderRadius: 4,
        fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
        fontSize: 11, lineHeight: 1.6, color: C.text,
        whiteSpace: 'pre', overflow: 'auto',
      }}>{code}</pre>
    </div>
  );
}

const COLORS_XML = `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Surface -->
    <color name="background">#FAF9F7</color>
    <color name="surface">#FFFFFF</color>
    <color name="surface_alt">#F5F4F1</color>
    <color name="divider">#E7E5E4</color>
    <color name="divider_light">#F1EFEC</color>

    <!-- Text -->
    <color name="text_primary">#1C1917</color>
    <color name="text_secondary">#78716C</color>
    <color name="text_muted">#A8A29E</color>
    <color name="text_on_primary">#FFFFFF</color>

    <!-- Brand · amber primary -->
    <color name="primary">#D97706</color>
    <color name="primary_weak">#FEF3C7</color>

    <!-- Semantic -->
    <color name="success">#059669</color>
    <color name="success_weak">#D1FAE5</color>
    <color name="error">#DC2626</color>
    <color name="error_weak">#FEE2E2</color>
    <color name="warning">#D97706</color>
    <color name="warning_weak">#FEF3C7</color>
    <color name="info">#1E40AF</color>
    <color name="info_weak">#DBEAFE</color>

    <!-- Status strip (3dp left border on list rows) -->
    <color name="strip_success">@color/success</color>
    <color name="strip_error">@color/error</color>
    <color name="strip_neutral">@color/text_muted</color>
</resources>`;

const THEMES_XML = `<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.SmsForwarder" parent="Theme.Material3.Light.NoActionBar">
        <!-- Material 3 color roles -->
        <item name="colorPrimary">@color/primary</item>
        <item name="colorOnPrimary">@color/text_on_primary</item>
        <item name="colorPrimaryContainer">@color/primary_weak</item>
        <item name="colorOnPrimaryContainer">@color/primary</item>

        <item name="colorSurface">@color/surface</item>
        <item name="colorOnSurface">@color/text_primary</item>
        <item name="colorSurfaceVariant">@color/surface_alt</item>
        <item name="colorOnSurfaceVariant">@color/text_secondary</item>
        <item name="colorOutline">@color/divider</item>
        <item name="colorOutlineVariant">@color/divider_light</item>

        <item name="colorError">@color/error</item>
        <item name="colorOnError">#FFFFFF</item>
        <item name="colorErrorContainer">@color/error_weak</item>

        <item name="android:colorBackground">@color/background</item>
        <item name="android:windowBackground">@color/background</item>
        <item name="android:statusBarColor">@color/background</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:navigationBarColor">@color/background</item>
        <item name="android:windowLightNavigationBar">true</item>

        <!-- Type — Shabnam everywhere -->
        <item name="android:fontFamily">@font/shabnam</item>
        <item name="fontFamily">@font/shabnam</item>

        <item name="textAppearanceTitleLarge">@style/Text.PageTitle</item>
        <item name="textAppearanceTitleMedium">@style/Text.CardTitle</item>
        <item name="textAppearanceBodyMedium">@style/Text.Body</item>
        <item name="textAppearanceBodySmall">@style/Text.Caption</item>
        <item name="textAppearanceLabelSmall">@style/Text.Badge</item>

        <!-- Shape — keep it tight; cards are flat with hairline border -->
        <item name="shapeAppearanceSmallComponent">@style/Shape.Tight</item>
        <item name="shapeAppearanceMediumComponent">@style/Shape.Tight</item>
        <item name="shapeAppearanceLargeComponent">@style/Shape.Tight</item>
    </style>

    <style name="Shape.Tight" parent="ShapeAppearance.Material3.SmallComponent">
        <item name="cornerSize">4dp</item>
    </style>
</resources>`;

const STYLES_XML = `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Type scale — Shabnam · sp values -->
    <style name="Text.PageTitle">
        <item name="android:textSize">22sp</item>
        <item name="android:fontFamily">@font/shabnam_bold</item>
        <item name="android:textColor">@color/text_primary</item>
        <item name="android:letterSpacing">-0.01</item>
    </style>
    <style name="Text.SectionHeader">
        <item name="android:textSize">11sp</item>
        <item name="android:fontFamily">@font/shabnam_bold</item>
        <item name="android:textAllCaps">true</item>
        <item name="android:textColor">@color/text_secondary</item>
        <item name="android:letterSpacing">0.08</item>
    </style>
    <style name="Text.CardTitle">
        <item name="android:textSize">15sp</item>
        <item name="android:fontFamily">@font/shabnam_bold</item>
        <item name="android:textColor">@color/text_primary</item>
    </style>
    <style name="Text.Body">
        <item name="android:textSize">13sp</item>
        <item name="android:fontFamily">@font/shabnam</item>
        <item name="android:textColor">@color/text_primary</item>
    </style>
    <style name="Text.Caption">
        <item name="android:textSize">11sp</item>
        <item name="android:fontFamily">@font/shabnam</item>
        <item name="android:textColor">@color/text_secondary</item>
    </style>
    <style name="Text.Badge">
        <item name="android:textSize">10sp</item>
        <item name="android:fontFamily">@font/shabnam_bold</item>
        <item name="android:textAllCaps">true</item>
        <item name="android:letterSpacing">0.06</item>
    </style>

    <!-- Buttons -->
    <style name="Button.Primary" parent="Widget.Material3.Button">
        <item name="backgroundTint">@color/text_primary</item>
        <item name="android:textColor">@color/text_on_primary</item>
        <item name="cornerRadius">4dp</item>
        <item name="android:minHeight">40dp</item>
        <item name="android:textAppearance">@style/Text.Body</item>
    </style>

    <style name="Button.Outline" parent="Widget.Material3.Button.OutlinedButton">
        <item name="strokeColor">@color/divider</item>
        <item name="android:textColor">@color/text_primary</item>
        <item name="cornerRadius">4dp</item>
        <item name="android:minHeight">36dp</item>
    </style>

    <style name="Button.Text" parent="Widget.Material3.Button.TextButton">
        <item name="android:textColor">@color/text_primary</item>
        <item name="android:textAppearance">@style/Text.Body</item>
    </style>

    <style name="Button.Destructive" parent="Widget.Material3.Button.TextButton">
        <item name="android:textColor">@color/error</item>
    </style>

    <!-- Service hero (start / stop) -->
    <style name="Button.Service" parent="Widget.Material3.Button">
        <item name="cornerRadius">4dp</item>
        <item name="android:minHeight">56dp</item>
        <item name="android:textSize">15sp</item>
        <item name="android:textAllCaps">true</item>
        <item name="android:fontFamily">@font/shabnam_bold</item>
        <item name="android:letterSpacing">0.04</item>
    </style>
</resources>`;

const DRAWABLES_XML = `<!-- res/drawable/bg_card.xml — flat card with hairline border -->
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
       android:shape="rectangle">
    <solid android:color="@color/surface"/>
    <stroke android:width="1dp" android:color="@color/divider"/>
    <corners android:radius="6dp"/>
</shape>

<!-- res/drawable/bg_input.xml -->
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <shape android:shape="rectangle">
            <solid android:color="@color/surface"/>
            <stroke android:width="1dp" android:color="@color/text_primary"/>
            <corners android:radius="4dp"/>
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/surface"/>
            <stroke android:width="1dp" android:color="@color/divider"/>
            <corners android:radius="4dp"/>
        </shape>
    </item>
</selector>

<!-- res/drawable/bg_badge_success.xml — small pill -->
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
       android:shape="rectangle">
    <solid android:color="@color/success_weak"/>
    <corners android:radius="3dp"/>
</shape>

<!-- res/drawable/strip_success.xml — left edge on log items
     Apply as the item's background; the 3dp left inset becomes the strip. -->
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/strip_success"/>
            <corners android:radius="4dp"/>
        </shape>
    </item>
    <item android:left="3dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/surface"/>
            <stroke android:width="1dp" android:color="@color/divider"/>
            <corners
                android:topRightRadius="4dp"
                android:bottomRightRadius="4dp"/>
        </shape>
    </item>
</layer-list>

<!-- res/drawable/divider.xml -->
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
       android:shape="rectangle">
    <size android:height="1dp"/>
    <solid android:color="@color/divider"/>
</shape>

<!-- res/drawable/ripple_card.xml -->
<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
        android:color="?attr/colorControlHighlight">
    <item android:id="@android:id/mask">
        <shape android:shape="rectangle">
            <solid android:color="#FFFFFFFF"/>
            <corners android:radius="6dp"/>
        </shape>
    </item>
    <item android:drawable="@drawable/bg_card"/>
</ripple>`;

function ColorsXmlArtboard()    { return <CodeBlock title="Color tokens"      file="res/values/colors.xml"     code={COLORS_XML} />; }
function ThemesXmlArtboard()    { return <CodeBlock title="Material 3 theme"  file="res/values/themes.xml"     code={THEMES_XML} />; }
function StylesXmlArtboard()    { return <CodeBlock title="Type & buttons"    file="res/values/styles.xml"     code={STYLES_XML} />; }
function DrawablesXmlArtboard() { return <CodeBlock title="Drawables"         file="res/drawable/*.xml"        code={DRAWABLES_XML} />; }

Object.assign(window, { ColorsXmlArtboard, ThemesXmlArtboard, StylesXmlArtboard, DrawablesXmlArtboard });
