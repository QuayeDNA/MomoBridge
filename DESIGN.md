# MoMo Bridge — Design Direction

## 1. Design Philosophy

### Brand Identity & Positioning
MoMo Bridge is a financial utility — invisible when it works, delightful when you look. It lives in the status bar and surfaces only when needed. The design is trustworthy, precise, and ambient.

**Tone:** Restrained, technical, confidence-inspiring. Banking terminal meets modern tool.

### Emotional Register
Cold & clinical meets warm gold accent. The app should feel like a piece of financial infrastructure — not a consumer app. Every pixel communicates reliability.

### Signature Moments
- **Transaction card entrance:** When an SMS arrives, the card materializes with a slide + fade, the amount snaps into monospace alignment, and the status badge animates from Pending → Confirmed with a green sweep.
- **Claim badge transition:** When the relay confirms a claim, a "CONFIRMED" badge appears with a scale bounce (1.0 → 1.1 → 1.0) + green border fade-in.
- **Setup completion:** After generating API key + connecting to relay, the CTA morphs into a gold spinner → green checkmark on success.

### Anti-Cliché Commitments
- No purple gradients
- No Inter/Roboto/SF Pro — system sans-serif is the brand
- No generic SaaS card patterns
- Status indicators feel mechanical, not decorative
- Gold appears on exactly ONE element per screen (restraint is key)

---

## 2. Theme Architecture

### MomoColors

```kotlin
// Core palette — Deep Navy / Gold / Charcoal
object MomoColors {
    // Ground — the dark foundation
    val GroundDark = Color(0xFF0A0E1A)
    val GroundMedium = Color(0xFF0F1424)
    val GroundLight = Color(0xFF1A2140)

    // Surface layers (Material3 tonal elevation)
    // Level 0 = GroundDark, Level 1 = GroundMedium, Level 2 = GroundLight, Level 3 = Color(0xFF222B50)

    // Accent — the gold signature
    val Gold = Color(0xFFD4A843)
    val GoldDim = Color(0xFF8B7332)
    val GoldVivid = Color(0xFFEBC875)
    val OnGold = Color(0xFF0A0E1A)

    // Semantic status
    val StatusPending = Color(0xFFF0B429)
    val StatusConfirmed = Color(0xFF00C853)
    val StatusFailed = Color(0xFFEF5350)
    val StatusExpired = Color(0xFF9E9E9E)

    // Text
    val TextPrimary = Color(0xFFE8EDF5)
    val TextSecondary = Color(0xFF8B95B0)
    val TextTertiary = Color(0xFF5A6480)
    val TextOnGold = Color(0xFF0A0E1A)

    // Borders & dividers
    val BorderSubtle = Color(0xFF1E2748)
    val BorderAccent = Color(0xFFD4A843)

    // Overlays
    val Scrim = Color(0x80000000)
    val Highlight = Color(0x14D4A843)
}
```

**Color usage rules:**
- Gold appears on exactly ONE element per screen
- Status colors are semantic, not decorative
- Never use pure white (#FFF) on dark — TextPrimary is #E8EDF5
- Higher surface levels are slightly lighter (tonal elevation)

### MomoTypography

```kotlin
object MomoTypography {
    // Display — large numbers (amounts, balances)
    val DisplayLarge = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 34.sp,
        lineHeight = 40.sp, letterSpacing = (-0.5).sp
    )
    val DisplayMedium = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 28.sp,
        lineHeight = 34.sp, letterSpacing = (-0.25).sp
    )

    // Screen titles
    val TitleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
        lineHeight = 28.sp, letterSpacing = 0.sp
    )
    val TitleMedium = TextStyle(
        fontWeight = FontWeight.Medium, fontSize = 16.sp,
        lineHeight = 22.sp, letterSpacing = 0.15.sp
    )
    val TitleSmall = TextStyle(
        fontWeight = FontWeight.Medium, fontSize = 14.sp,
        lineHeight = 18.sp, letterSpacing = 0.1.sp
    )

    // Body
    val BodyLarge = TextStyle(
        fontWeight = FontWeight.Normal, fontSize = 16.sp,
        lineHeight = 24.sp, letterSpacing = 0.5.sp
    )
    val BodyMedium = TextStyle(
        fontWeight = FontWeight.Normal, fontSize = 14.sp,
        lineHeight = 20.sp, letterSpacing = 0.25.sp
    )
    val BodySmall = TextStyle(
        fontWeight = FontWeight.Normal, fontSize = 12.sp,
        lineHeight = 16.sp, letterSpacing = 0.4.sp
    )

    // Labels & meta
    val LabelLarge = TextStyle(
        fontWeight = FontWeight.Medium, fontSize = 14.sp,
        lineHeight = 18.sp, letterSpacing = 0.1.sp
    )
    val LabelSmall = TextStyle(
        fontWeight = FontWeight.Medium, fontSize = 11.sp,
        lineHeight = 14.sp, letterSpacing = 0.5.sp
    )

    // Amount mono (for transaction amounts)
    val AmountMono = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 18.sp,
        lineHeight = 24.sp, fontFamily = FontFamily.Monospace,
        letterSpacing = 0.sp
    )
}
```

**Typography rules:**
- Amounts always use `AmountMono` for financial alignment
- Timestamps use `LabelSmall` with `TextTertiary` color
- Transaction references use `BodySmall` secondary
- Status labels use `LabelSmall` ALL CAPS bold

### MomoSpacing

```kotlin
object MomoSpacing {
    // Base unit: 4dp
    val Xxs = 2.dp
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Xxxl = 48.dp

    // Component-specific
    val CardPadding = 12.dp
    val ScreenPadding = 16.dp
    val SectionSpacing = 24.dp
    val BadgeSize = 48.dp
    val ChipHeight = 32.dp
}
```

### MomoShapes

```kotlin
object MomoShapes {
    val CardShape = RoundedCornerShape(12.dp)
    val BadgeShape = RoundedCornerShape(8.dp)
    val ChipShape = RoundedCornerShape(20.dp)
    val ButtonShape = RoundedCornerShape(20.dp)
    val InputShape = RoundedCornerShape(10.dp)
    val BottomSheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val IconBgShape = RoundedCornerShape(50)
}
```

### MomoMotion

```kotlin
object MomoMotion {
    val Instant = 100.ms
    val Fast = 200.ms
    val Normal = 350.ms
    val Slow = 500.ms

    val EaseOutExpo = FastOutLinearInEasing
    val EaseInOutQuart = FastOutSlowInEasing
    val LinearEasing = LinearEasing

    val CardEntrance = tween<Offset>(
        durationMillis = 350, easing = FastOutLinearInEasing
    )
    val StatusTransition = tween<Float>(
        durationMillis = 500, easing = FastOutSlowInEasing
    )
    val FadeIn = tween<Float>(
        durationMillis = 200, easing = LinearEasing
    )
}
```

**Motion rules:**
- One choreographed entrance > many scattered micro-animations
- Cards enter staggered with 50ms delay each (translateY: 24dp → 0, fade 0 → 1)
- Status changes animate with color crossfade + badge scale bounce
- Haptic feedback (`LongPress`) on claim confirmation
- Respect `prefers-reduced-motion`

### Material3 Color Scheme Mapping

```kotlin
private val MomoDarkScheme = darkColorScheme(
    primary = MomoColors.Gold,
    onPrimary = MomoColors.OnGold,
    primaryContainer = MomoColors.GoldDim,
    secondary = MomoColors.GoldVivid,
    tertiary = MomoColors.StatusConfirmed,
    background = MomoColors.GroundDark,
    surface = MomoColors.GroundMedium,
    surfaceVariant = MomoColors.GroundLight,
    onBackground = MomoColors.TextPrimary,
    onSurface = MomoColors.TextPrimary,
    onSurfaceVariant = MomoColors.TextSecondary,
    error = MomoColors.StatusFailed,
    onError = Color.White,
    outline = MomoColors.BorderSubtle,
    outlineVariant = MomoColors.BorderSubtle.copy(alpha = 0.5f)
)
```

---

## 3. Component Library

### GoldButton (Primary CTA)
- Filled gold pill shape (`ButtonShape`)
- Background: `MomoColors.Gold`, text: `MomoColors.OnGold`
- Hover/pressed: `MomoColors.GoldVivid`
- Modifier: `height = 48.dp`, `fillMaxWidth()` or content width
- Internal padding: `horizontal = 24.dp`
- Loading state: gold `CircularProgressIndicator` replaces text
- Disabled: opacity 0.5

### OutlineButton (Secondary)
- Ghost button with gold 1dp border
- Shape: `ButtonShape` (pill)
- Border: `MomoColors.BorderAccent`, text: `MomoColors.Gold`
- On interaction: background fill with accent color
- Internal padding: `horizontal = 20.dp`, height 44dp

### MomoTextField
- Dark background: `MomoColors.GroundLight`
- Shape: `InputShape` (10dp)
- Unfocused border: `MomoColors.BorderSubtle` (1dp)
- Focused border: `MomoColors.BorderAccent` (2dp)
- Label: `BodySmall` in `TextSecondary` above field (not floating animation — utility style)
- Placeholder: `BodyMedium` in `TextTertiary`
- Error state: red border (`StatusFailed`) + red helper text below
- No label animation — the input is utilitarian, not consumer

### StatusBadge
```
┌─────────────────┐
│   ↑ 12 Pending │
└─────────────────┘
```
- Container: `GroundLight` background, `BadgeShape` (8dp radius)
- Icon + count are dominant visual, label is secondary
- Color is semantic (amber/green/red/gray)
- Size: `BadgeSize` (48dp) icon circle
- Tappable — triggers filtered view or action
- Min width: 100dp

### TransactionCard
```
┌──────────────────────────────────┐
│  T-CASH · GH₵18.00    CONFIRMED  │
│  from Bridget Baidoo             │
│  0000013331054115                │
│  Confirmed · 17 Jun 00:33       │
└──────────────────────────────────┘
```
- Background: `GroundMedium`, shape: `CardShape` (12dp)
- Border: `BorderSubtle` (1dp) normally, 1dp green (`StatusConfirmed`) when CONFIRMED
- Left column: vertical stack (network + amount | senderName | reference | timestamp)
- Right column: status icon + label (centered)
- Amount uses `AmountMono` (monospace bold)
- Network uses `TitleSmall`, amount sits beside it
- Sender name in `BodySmall` secondary
- Reference in `BodySmall` secondary
- Timestamp in `LabelSmall` tertiary
- Confirmed cards get inline "CONFIRMED" badge (green `LabelSmall` bold, all caps)
- Padding: `CardPadding` (12dp)

### SmsSourceCard
```
┌──────────────────────────────────┐
│  MobileMoney        ● Configured │
│  Sender: +447771234567          │
└──────────────────────────────────┘
```
- Compact card, `GroundMedium` background, `CardShape`
- Green dot = configured with parsing rules
- Gray dot = not configured
- Label as title in `TitleSmall`
- "Configured" badge (green `LabelSmall`) or "Not configured" (gray) as subtitle
- Toggle, Configure (edit icon), and Delete (trash icon) buttons
- Tap to edit, long-press to delete

### SetupWizardStep (Onboarding Section)
```
┌──────────────────────────────────┐
│ ● Step 1 of 2                    │
│   Your API Key                   │
│ ┌──────────────────────────────┐ │
│ │ mb_a1b2c3d4e5f6...          │ │
│ └──────────────────────────────┘ │
│ [        Copy & Continue     ]   │
└──────────────────────────────────┘
```
- Step indicator bar: gold dot (current), gray dot (future), green dot (completed)
- Section title in `TitleMedium`
- Input fields with gold focus ring (no label animation)
- Error state with red border + red helper
- CTA: GoldButton ("Connect & Continue")

### AmountText
```kotlin
@Composable
fun AmountText(amount: Double, style: TextStyle = MomoTypography.AmountMono) {
    Text(
        text = "GH₵${"%.2f".format(amount)}",
        style = style,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
    )
}
```

### TimestampText
```kotlin
@Composable
fun TimestampText(timestamp: Long, verbose: Boolean = false) {
    val format = when {
        // Today: HH:mm
        // This week: EEE HH:mm
        // Older: dd MMM
        // Full (verbose): dd MMM yyyy HH:mm
    }
    Text(text = format, style = MomoTypography.LabelSmall, color = MomoColors.TextTertiary)
}
```

### EmptyState
- Crafted illustration composable (geometric shape with gold accent)
- Title in `BodyLarge` secondary
- Action button (optional) for context-appropriate action
- No generic "No transactions yet" text

### SkeletonShimmer
- Shimmer animation for loading states
- Gray rectangles with animated gradient overlay
- Matches card shapes (12dp radius for cards, 10dp for inputs)
- Replaces CircularProgressIndicator in most contexts

---

## 4. Screen-by-Screen Redesign

### 4a. Dashboard Screen

**Layout (top to bottom):**
1. TopAppBar: "MoMo Bridge" title (TitleLarge), relay connection dot (green=connected, red=disconnected) on left, "Settings" text (right)
2. Status row: 2 StatusBadge — Pending (amber/HourglassEmpty), Confirmed (green/CheckCircle)
3. "Recent Transactions" section header (TitleMedium)
4. LazyColumn of TransactionCard with staggered entrance animation
5. "View all transactions" — OutlineButton with gold border

**States:**
- **Loading:** SkeletonShimmer cards in list
- **Empty:** EmptyState illustration + "No incoming transactions yet"
- **Error:** Snackbar with error message
- **Relay disconnected:** Banner below TopAppBar: "Relay disconnected — reconnecting..." with amber background

**Interactions:**
- Tap card: open TransactionDetailDialog (bottom sheet)
- Tap relay dot: navigate to Settings to view relay status
- Confirmation notification: local Android notification when relay claim comes in

### 4b. Setup Screen

**Layout (2-step wizard):**

**Step 1 — Generate API Key:**
```
┌──────────────────────────────┐
│  ● ○  Step 1 of 2            │
│                              │
│  Your API Key                │
│                              │
│  ┌─────────────────────────┐ │
│  │ mb_a1b2c3d4e...        │ │
│  └─────────────────────────┘ │
│                              │
│  This key is used by your    │
│  website to send claim       │
│  requests to this app.       │
│                              │
│  [   Copy & Continue     ]   │
└──────────────────────────────┘
```

**Step 2 — Connect to Relay:**
```
┌──────────────────────────────┐
│  ✓ ●  Step 2 of 2            │
│                              │
│  Relay Server                │
│                              │
│  Enter the URL of your relay │
│  server (e.g., Fly.io/Render)│
│                              │
│  ┌─────────────────────────┐ │
│  │ https://momo-relay...   │ │
│  └─────────────────────────┘ │
│                              │
│  [   Connect & Finish    ]   │
└──────────────────────────────┘
```

**States:**
- **Step 1:** API key generated and displayed; copy button
- **Step 2:** Relay URL input; "Connect" button validates by trying WebSocket handshake
- **Error:** Red border on relay field + helper text
- **Success:** CTA morphs to green checkmark, auto-navigates to Dashboard

### 4c. Settings Screen

**Layout (scrollable):**
1. TopAppBar: "Settings" (TitleLarge), back arrow
2. "Relay Connection" section (TitleMedium)
   - Status card: green dot + "Connected" or red dot + "Disconnected"
   - URL shown in `BodySmall` secondary
   - Reconnect button (OutlineButton)
3. "API Key" section (TitleMedium)
   - Key displayed in code-style card
   - Copy button
   - QR code for easy transfer
4. "Monitored Senders" section (TitleMedium)
   - Helper text explaining purpose
   - Empty state card if no senders
   - List of SmsSourceCard components
   - Row: "Scan SMS Inbox" (OutlineButton) + "Add Manually" (OutlineButton)
5. "Past Transactions" section (TitleMedium)
   - Helper text
   - OutlineButton: "Scan Inbox for Past Transactions"
   - Scan result card (tonal surface, green/amber border)
6. "Help & Support" — OutlineButton
7. "Reset All" — red outlined button

**Dialogs:**
- Add Sender: sender address + label fields
- Scan Results: list of found senders with add button each
- Reset: confirmation with warning text ("This will delete all data and require re-setup")

### 4d. Sender Config Screen

**Layout (3-step wizard):**

**Step 1 — Pick Message:**
- Step indicator: 3 dots, first gold
- Helper text: "Pick a sample SMS to use as template"
- LazyColumn of messages from inbox for this sender
- Each message is a tappable card showing:
  - Body text (3 lines max) in BodySmall
  - Timestamp in LabelSmall tertiary

**Step 2 — Confirm Fields:**
- Step indicator: second dot gold, first green
- FieldConfirmCard for each parsed field:
  - Reference (required)
  - Amount (required)
  - Sender Name (optional)
  - Sender Phone (optional)
  - Balance After (optional)
  - Credit Keyword (required)
- Each card shows auto-detected value + Edit icon tap to override
- Required fields highlighted with red container if empty
- GoldButton: "Save Configuration"

**Step 3 — Done:**
- Step indicator: all three green
- Checkmark icon + "Configuration saved!"
- Description text
- GoldButton: "Done"

### 4e. Transactions Screen

**Layout:**
1. TopAppBar: "Transaction Log" (TitleLarge), back arrow
2. Filter chip row (ChipShape): All | Pending | Confirmed | Failed | Expired
3. Active filter count label
4. LazyColumn of TransactionDetailCard

**TransactionDetailCard vs TransactionCard:**
- Same visual as TransactionCard but:
  - Full timestamp format (dd MMM yyyy HH:mm)
  - Slightly more padding
  - Tappable for detail view

**States:**
- **Loading:** SkeletonShimmer
- **Empty:** EmptyState illustration with filter context
- **Filtered empty:** "No {status} transactions" message
- **Populated:** Staggered card list

---

## 5. Animation & Interaction Spec

| Interaction | Trigger | Animation | Duration | Easing |
|------------|---------|-----------|----------|--------|
| Card entrance | List appears | Slide up (24dp → 0) + fade (0 → 1) | 350ms | FastOutLinearIn |
| Card stagger | Per card | delay = index × 50ms | — | — |
| Status change | Claim confirmation | Color crossfade + scale (1.0 → 1.1 → 1.0) | 500ms | FastOutSlowIn |
| Button press | Tap | Scale 1.0 → 0.97 | 100ms | Linear |
| Navigation | Route change | Fade + slide (contextual direction) | 350ms | FastOutSlowIn |
| Haptic feedback | Claim confirmation | LongPress | — | — |
| Skeleton shimmer | Data loading | Gradient sweep left → right loop | 1500ms | Linear |

**Reduced motion:** Check `isAnimationEnabled` from Compose before running animations. Fall back to instant opacity changes.

---

## 6. Data Display Patterns

### Status → Visual Mapping

| Status | Icon Material Name | Hex Color | Label |
|--------|-------------------|-----------|-------|
| PENDING | HourglassEmpty | #F0B429 | Pending |
| CONFIRMED | CheckCircle | #00C853 | Confirmed |
| FAILED | Error | #EF5350 | Failed |
| EXPIRED | Timelapse | #9E9E9E | Expired |

### Amount Display
- Always `GH₵` prefix
- Two decimal places: `GH₵${"%.2f".format(amount)}`
- Monospace for list alignment
- Bold weight, `AmountMono` style by default

### Timestamp Display
- Today: `HH:mm`
- This week: `EEE HH:mm` (e.g., "Mon 14:30")
- Older: `dd MMM` (e.g., "17 Jun")
- Full (detail): `dd MMM yyyy HH:mm`

---

## 7. Accessibility & UX Standards

- All interactive elements: 44×44dp minimum touch target
- Status conveyed by icon + color + text label (not color alone)
- Focus indicators: gold outline, 2dp width, 4dp offset
- Empty states: crafted illustration (not bare text)
- Loading states: skeleton shimmer (not spinning spinners)
- Error states: clear action button (Retry, Configure, etc.)
- `prefers-reduced-motion`: respected via `animate*` cancellation checks

---

## 8. Implementation Order

```
Phase 1 — Foundation (done)
├── MomoColors.kt (color palette object)
├── MomoTypography.kt (text styles)
├── MomoSpacing.kt (spacing constants)
├── MomoShapes.kt (shape definitions)
├── MomoMotion.kt (animation specs)
└── Update Theme.kt (map to Material3 darkColorScheme)

Phase 2 — Components (done)
├── GoldButton.kt / OutlineButton.kt
├── MomoTextField.kt
├── StatusBadge.kt
├── TransactionCard.kt
├── SmsSourceCard.kt
├── StepIndicator.kt
├── AmountText.kt / TimestampText.kt
├── EmptyState.kt
└── SkeletonShimmer.kt

Phase 3 — Backend Strip + Relay Add
├── Delete: BridgeApiService, TransactionPayload, NetworkModule
├── Delete: DynamicBaseUrlInterceptor, StoreConfig, StoreRepository
├── Delete: SmsUploadWorker, StoreCard
├── Add: domain/ApiKeyGenerator.kt
├── Add: service/RelayClient.kt (WebSocket)
├── Add: service/ClaimHandler.kt
├── Add: relay/index.ts (Bun server)
├── Add: widget/widget.html
└── Update: AppModule.kt (API key prefs + relay URL)

Phase 4 — Database Refactor
├── SmsTransactionEntity: remove vendorId, remove UPLOADED/DUPLICATE statuses
├── SmsTransactionEntity: add confirmedAt, rename CLAIMED→CONFIRMED
├── SmsTransactionDao: remove upload queries, add findByReference(ref)
├── MomoBridgeDatabase: bump version, migration
└── TransactionRepository: gut upload methods, add confirmTransaction()

Phase 5 — UI Updates
├── SetupScreen/ViewModel: API key generation + relay connection
├── DashboardScreen/ViewModel: connection dot, remove sync, update statuses
├── SettingsScreen/ViewModel: relay status, API key display, no stores
├── TransactionsScreen/ViewModel: update filter chips
├── AppNavigation: update if needed
└── SmsListenerService: remove store iteration, simplify

Phase 6 — Relay + Widget
├── relay/index.ts: WebSocket + POST /claim + in-memory map
├── widget/widget.html: embeddable form, postMessage callback
└── end-to-end testing

Phase 7 — Polish
├── QR code on API key display
├── Reconnection logic with exponential backoff
├── Heartbeat (30s ping/pong)
├── Offline banner
└── Animations review
```

---

## 9. Current Implementation Gaps vs Design System

| Area | Current State | Target |
|------|--------------|--------|
| Theme colors | 8 hardcoded Material3 colors | 16+ MomoColors tokens mapped to Material3 scheme |
| Typography | Default Material3 type scale | 11 custom text styles with system sans-serif + monospace |
| Spacing | Inconsistent dp values | 8-step 4dp grid with named constants |
| Shapes | Default M3 shapes | 7 named shape constants |
| Motion | None | 3 durations, 3 easings, composable specs |
| StatusBadge | Basic Card with inline color | Tonal surface + semantic icon + count + label |
| TransactionCard | Functional with status colors | Full spec: monospace amount, CONFIRMED border, status column |
| SmsSourceCard | Basic Card with toggle | Health dot + config status + styled layout |
| Buttons | Default Material Button/OutlinedButton | Gold pill / ghost with gold border |
| TextFields | Default OutlinedTextField | Dark bg, subtle border, gold focus ring |
| Setup wizard | Single form screen | 2-step wizard: API key → relay URL |
| Empty states | Plain text | Crafted illustration + action button |
| Loading states | CircularProgressIndicator | Skeleton shimmer |
| Card entrance | Instant | Staggered slide-up + fade (50ms per card) |
| Status transitions | Instant color change | Color crossfade + scale bounce (500ms) |

---

## 10. User Flow & Navigation Architecture

### 10a. App Launch Flow

```
SHOW SPLASH SCREEN
  │
  ├── IF not configured → SETUP STEP 1 (Generate API Key)
  │                           │
  │                           └── SETUP STEP 2 (Enter Relay URL)
  │                                   │
  │                                   └── DASHBOARD (Main app)
  │
  └── IF configured → Relay auto-connects
                        │
                        └── DASHBOARD (Main app)
```

**Splash Screen (2 seconds):**
- Full-screen centered logo: geometric "MB" monogram in gold on navy
- Tagline: "MoMo Bridge" in TitleLarge
- Subtitle: "Mobile Money Gateway" in BodySmall secondary
- Bottom loading indicator (gold shimmer or pulsing dot)
- No user interaction — auto-transitions after ~2s or when init check completes

### 10b. Tabbed Bottom Navigation

After splash/setup, the main app uses a 3-tab bottom navigation bar:

```
┌──────────────────────────────────────────┐
│                                          │
│              [Screen Content]             │
│                                          │
│                                          │
│                                          │
├──────────────────────────────────────────┤
│   📊 Dashboard  │  📋 Txns  │  ⚙ Settings │
└──────────────────────────────────────────┘
```

**Tab bar properties:**
- Background: `MomoColors.GroundMedium` with top border `BorderSubtle`
- Active tab: gold icon + gold text, `LabelSmall` bold
- Inactive tab: `TextTertiary` icon + text
- Height: 64dp (56dp content + 8dp safe area)
- Icons: Dashboard (Home), Transactions (Receipt), Settings (Settings)

**Tab behaviors:**
- Initial tab: Dashboard
- Tapping active tab: scroll to top
- Navigation bar hidden on sub-screens (Sender Config, Help, etc.)
- Routes: `tab_dashboard`, `tab_transactions`, `tab_settings`

### 10c. Updated Route Map

```
SPLASH (/)
  │
  ├── SETUP (/setup) [no bottom nav]
  │     ├── Step 1: Generate API Key
  │     └── Step 2: Enter Relay URL
  │
  └── MAIN APP [bottom nav shown]
        ├── TAB_DASHBOARD (/main/dashboard)
        │     └── [sub] Transaction Detail (bottom sheet)
        │
        ├── TAB_TRANSACTIONS (/main/transactions)
        │     └── [sub] Transaction Detail (bottom sheet)
        │
        └── TAB_SETTINGS (/main/settings) [scrollable]
              ├── Sender Config (/sender_config/{addr}/{label}) — no bottom nav
              └── Help & Support (/help) — no bottom nav
```

### 10d. Screen Details

#### SplashScreen
```
┌──────────────────────────────┐
│                              │
│                              │
│         ┌───┐                │
│         │ M │  B             │  ← Gold monogram
│         └───┘                │
│                              │
│      MoMo Bridge             │  ← TitleLarge, TextPrimary
│   Mobile Money Gateway       │  ← BodySmall, TextSecondary
│                              │
│          ● ● ●               │  ← Pulsing dots (gold)
│                              │
│   v1.0.0                     │  ← LabelSmall, TextTertiary
└──────────────────────────────┘
```
- Dark background (GroundDark), no TopAppBar
- Centered layout with vertical spacing
- Version number at bottom
- 2s minimum display, exits when init check passes
- Auto-navigates to Setup (if not configured) or Tabbed Main

#### Expanded Setup — Step 1: API Key Generation
```
┌──────────────────────────────┐
│  ● ○  Step 1 of 2            │  ← StepIndicator
│                              │
│  Your API Key                │  ← TitleLarge
│                              │
│  This key allows your        │  ← BodySmall, TextSecondary
│  website to send payment     │
│  verification requests to    │
│  this app via the relay.     │
│                              │
│  ┌─────────────────────────┐ │
│  │ mb_a1b2c3d4e5f6...     │ │  ← Code-style card
│  └─────────────────────────┘ │
│                              │
│  [  📋  Copy Key  ]         │  ← GoldButton
│                              │
│  Keep this key private.      │  ← LabelSmall, warning text
│  You'll need it to configure │
│  your website widget.        │
└──────────────────────────────┘
```

#### Expanded Setup — Step 2: Relay Connection
```
┌──────────────────────────────┐
│  ✓ ●  Step 2 of 2            │  ← StepIndicator (step 1 green)
│                              │
│  Connect to Relay            │  ← TitleLarge
│                              │
│  Enter the public URL of     │  ← BodySmall, TextSecondary
│  your relay server.          │
│  Deploy it to Render or      │
│  your own server.            │
│                              │
│  Relay URL                   │  ← MomoTextField label
│  ┌─────────────────────────┐ │
│  │ https://momo-relay...   │ │
│  └─────────────────────────┘ │
│                              │
│  [   Connect & Finish     ]  │  ← GoldButton
│                              │
│  Need a relay? Deploy one →  │  ← TextButton link (Help)
└──────────────────────────────┘
```

#### Redesigned Dashboard
```
┌──────────────────────────────┐
│  MoMo Bridge          ● ●   │  ← TopAppBar + relay status
│                              │
│  ┌──────┐ ┌──────┐          │
│  │  ↑ 3 │ │  ✓ 2 │          │  ← StatusBadge row
│  │Pending│ │Confirm│        │
│  └──────┘ └──────┘          │
│                              │
│  Recent Transactions  → All  │  ← Section header + link
│                              │
│  ┌─────────────────────────┐ │
│  │ T-CASH · GH₵18.00 CONFIRM│ │  ← TransactionCard
│  │ from Bridget Baidoo     │ │     (staggered entrance)
│  │ 0000013331054115        │ │
│  │ Confirmed · 17 Jun 00:33│ │
│  └─────────────────────────┘ │
│                              │
│  ┌─────────────────────────┐ │
│  │ MobileMoney · GH₵50.00  │ │
│  │ from John Doe           │ │  ← TransactionCard
│  │ 0000019876543210        │ │
│  │ Pending · 17 Jun 01:15 │ │
│  └─────────────────────────┘ │
│                              │
│  [   View All Transactions ]│  ← GoldOutlineButton
│                              │
└──────────────────────────────┘
```

**Dashboard improvements:**
- Status Badge row shows confirmed count instead of backend sync count
- Connection status dot in the TopAppBar: green = connected, red = disconnected, amber = reconnecting
- Tapping a StatusBadge filters the list
- Transaction cards use staggered entrance animation
- Empty state shows illustration + setup CTA instead of bare text

#### Help & Support Screen
```
┌──────────────────────────────┐
│ ←  Help & Support            │  ← TopAppBar with back
│                              │
│  ┌─────────────────────────┐ │
│  │ ❓ About MoMo Bridge    │ │  ← HelpCard with icon
│  │ What this app does      │ │
│  └─────────────────────────┘ │
│                              │
│  ┌─────────────────────────┐ │
│  │ 🔑 API Key & Relay     │ │  ← HelpCard
│  │ How keys work, relay    │ │
│  │ connection, security    │ │
│  └─────────────────────────┘ │
│                              │
│  ┌─────────────────────────┐ │
│  │ 📨 Monitoring Senders   │ │  ← HelpCard
│  │ How SMS interception    │ │
│  │ and parsing works       │ │
│  └─────────────────────────┘ │
│                              │
│  ┌─────────────────────────┐ │
│  │ ✅ Confirming Payments  │ │  ← HelpCard
│  │ How customers claim     │ │
│  │ via the web widget      │ │
│  └─────────────────────────┘ │
│                              │
│  ┌─────────────────────────┐ │
│  │ 🔧 Troubleshooting      │ │  ← HelpCard
│  │ Common issues & fixes   │ │
│  └─────────────────────────┘ │
│                              │
│  ┌─────────────────────────┐ │
│  │ 💬 Contact Support      │ │  ← HelpCard
│  │ Send us a message       │ │
│  └─────────────────────────┘ │
│                              │
│  App Version 1.0.0           │  ← LabelSmall, centered bottom
└──────────────────────────────┘
```
- Each HelpCard is tappable → expands to show detailed content inline
- Content explains the feature in plain language
- "Contact Support" → opens email or web form
- Accessible from Settings and Setup

### 10e. Navigation Implementation

```kotlin
// Navigation structure
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Setup : Screen("setup")
    object Main : Screen("main")           // Bottom nav host
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object Settings : Screen("settings")
    object SenderConfig : Screen("sender_config/{senderAddress}/{label}")
    object Help : Screen("help")
}
```

**Main app uses a nested NavHost:**
- Outer NavHost: Splash, Setup, Main, SenderConfig, Help
- Inner NavHost (inside Main): Dashboard, Transactions, Settings
- Bottom navigation bar visible only on inner routes
- SenderConfig and Help hide the bottom nav (full-screen)

### 10f. Toast Notification Strategy

- **Claim confirmed:** "Payment confirmed: GH₵18.00" (long, green)
- **Relay connected:** "Relay connected" (short, green)
- **Relay disconnected:** "Relay disconnected — reconnecting" (short, red)
- **SMS intercepted:** "New transaction: GH₵50.00" (short, default)
- **Setup complete:** "Setup complete! Relay connected" (short, green)
- **Setup error:** "Could not connect to relay — check URL" (short, red)
- **Sender configured:** "Sender configuration saved" (short, green)

Toast implementation uses Compose `SnackbarHost` with custom gold/red/green color schemes anchored to the bottom of the Scaffold (above the bottom nav bar when visible).

### 10g. Screen Relationship Diagram

```
SPLASH
  │
  ├──(first launch)──→ SETUP (step 1 — API Key)
  │                       │
  │                       └──→ SETUP (step 2 — Relay URL)
  │                              │
  │                              └──→ MAIN
  │
  └──(returning)──→ MAIN
                      │
                      ├── TAB: DASHBOARD
                      │     └──→ TRANSACTIONS (via "View All" link)
                      │
                      ├── TAB: TRANSACTIONS
                      │
                      └── TAB: SETTINGS
                            ├──→ SENDER CONFIG (no nav bar)
                            ├──→ HELP & SUPPORT (no nav bar)
                            └──→ SETUP (via Reset)
```

### 10h. Files to Create/Modify

| File | Action | Purpose |
|------|--------|---------|
| `domain/ApiKeyGenerator.kt` | Create | Generate mb_ prefixed API key |
| `service/RelayClient.kt` | Create | WebSocket connection to relay |
| `service/ClaimHandler.kt` | Create | Verify claims against local Room DB |
| `relay/index.ts` | Create | Bun relay server |
| `widget/widget.html` | Create | Embeddable claim widget |
| `data/local/SmsTransactionEntity.kt` | Modify | Remove vendorId, UPLOADED/DUPLICATE; add confirmedAt; rename CLAIMED→CONFIRMED |
| `data/local/SmsTransactionDao.kt` | Modify | Remove upload queries, add findByReference(ref) |
| `data/local/MomoBridgeDatabase.kt` | Modify | Bump version, migration |
| `data/repository/TransactionRepository.kt` | Modify | Remove upload methods, add confirmTransaction() |
| `di/AppModule.kt` | Modify | Add API key + relay URL prefs |
| `service/SmsListenerService.kt` | Modify | Remove store iteration |
| `ui/setup/SetupScreen.kt` | Modify | API key gen + relay URL wizard |
| `ui/setup/SetupViewModel.kt` | Modify | Key generation + WebSocket handshake |
| `ui/dashboard/DashboardScreen.kt` | Modify | Connection dot, remove sync |
| `ui/dashboard/DashboardViewModel.kt` | Modify | Remove upload/sync, add WS status |
| `ui/settings/SettingsScreen.kt` | Modify | Relay status + API key display |
| `ui/settings/SettingsViewModel.kt` | Modify | Remove store methods |
| `ui/transactions/TransactionsScreen.kt` | Modify | Update filter chips |
| `ui/navigation/AppNavigation.kt` | Modify | Minor route updates |
| `receiver/BootReceiver.kt` | Modify | Start RelayClient on boot |
| `MomoBridgeApp.kt` | Modify | Start RelayClient on app start |
| **Delete** | `BridgeApiService.kt` | Remove |
| **Delete** | `TransactionPayload.kt` | Remove |
| **Delete** | `DynamicBaseUrlInterceptor.kt` | Remove |
| **Delete** | `NetworkModule.kt` | Remove |
| **Delete** | `StoreConfig.kt` | Remove |
| **Delete** | `StoreRepository.kt` | Remove |
| **Delete** | `SmsUploadWorker.kt` | Remove |
| **Delete** | `StoreCard.kt` | Remove |
