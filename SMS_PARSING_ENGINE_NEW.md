# SMS Parsing Engine — Layered Architecture Implementation Plan

## 0. Premise

This plan does not target "100% accuracy" as a literal property of any single
regex or heuristic, because that target is not achievable for free-text SMS
that carriers can reword without notice. Instead it targets the property that
actually matters for this app: **the system never silently corrupts data, and
every message it cannot confidently parse becomes a visible, recoverable
signal instead of a silent drop.** Accuracy then compounds upward over time
via the retraining loop in Layer 4, rather than being frozen at whatever
formats existed on ship day.

Two concrete bugs in the current code motivate why a single-regex-per-field
design cannot get there alone, reconfirmed against the live code in this repo
just before writing this plan:

- `AutoDetectUtils.kt`'s `mtnRef` pattern (`Transaction\s+ID:\s*(\d{8,15})`)
  silently truncates the real 16-digit T-CASH reference
  `0000013085405512` down to `000001308540551` (15 digits, last digit
  dropped) whenever a message contains the literal text `"Transaction ID:"`
  — which happens in T-CASH's airtime-received format, not just MTN's. A
  truncated reference will never match what a customer enters during a claim,
  silently breaking verification for that transaction with no error anywhere.
- `tCashRef` (`^(\d{18,22})`) never matches _any_ real T-CASH reference,
  because every real T-CASH reference in the sample data is exactly 16
  digits — so the dedicated pattern is dead code, and extraction is quietly
  falling through to an unbounded fallback regex for every single T-CASH
  message today.

Neither of these failures is visible anywhere in the current system — no log,
no flag, no user-facing signal. That invisibility, not the regex bugs
themselves, is the structural problem this plan fixes.

---

## 1. The five-layer pipeline

```
Incoming SMS body
       │
       ▼
┌─────────────────────────┐
│ Layer 1: Classification   │  shape-based, not keyword-based
│ (SmsClassifier)           │  → RECEIVED / SENT / WITHDRAWN / PURCHASE /
└──────────┬───────────────┘    BILL_PAYMENT / LOAN / NON_TX
           │ RECEIVED only
           ▼
┌─────────────────────────┐
│ Layer 2: Candidate-scored │  generate ALL plausible candidates per field,
│ extraction                │  score by context proximity + shape,
│ (FieldExtractor, new)     │  take best-scoring candidate
└──────────┬───────────────┘
           ▼
┌─────────────────────────┐
│ Layer 3: Confidence gate  │  reference + amount present → PENDING
│ (ParsedTransaction +       │  reference or amount missing → FAILED,
│  confidence/parsedBy)      │  raw SMS retained, nothing silently dropped
└──────────┬───────────────┘
           │ FAILED only, AND no rule exists or rule is stale
           ▼
┌─────────────────────────┐
│ Layer 5: LLM fallback     │  structured-output call to Claude, only when
│ extraction (optional,     │  Layers 2–3 produced nothing usable —
│  network-gated)           │  genuinely carrier-agnostic, no regex needed
└──────────┬───────────────┘
           ▼
┌─────────────────────────┐
│ Layer 4: Adaptive          │  3 consecutive FAILEDs for a sender →
│ retraining loop            │  notify user → pre-fill failed SMS into
│ (per-sender failure count) │  SenderConfigScreen → human confirms once →
└─────────────────────────┘    new rule persists, loop resets
```

Layers 1–3 are mandatory and run on-device, synchronously, with no network
dependency — this matters because `SmsListenerService` is a foreground
service reacting to a `BroadcastReceiver` and must not block on network calls
that might never return (phone could be offline when an SMS arrives). Layer
5 is optional, async, and only invoked for the subset of messages Layers 1–3
could not resolve. Layer 4 is the feedback loop that ties failures back into
better future Layer 2 rules, closing the system.

---

## 2. Layer 1 — Classification by phrase shape

**Replaces:** the bare-keyword check inside `SmsClassifier.classify()` and
the equivalent `body.contains(rule.isCreditKeyword, ignoreCase = true)` gate
inside `SmsParser.parse()`.

**Design:** classification runs as an ordered set of _exclusionary_ checks,
most specific first, so that a message matching multiple categories'
surface keywords still resolves to the correct one:

1. `NON_TX` — promotional/OTP/system message markers (`dial *`, `DO NOT
SHARE`, `Mashup`, bundle-marketing language with no transaction reference
   number present at all).
2. `LOAN` — explicit loan-product markers (`FlexLoan`, `Ready Loan`, `SOS
Loan`, `Loan Processing Fee`, `Xtratime loan`) anywhere in the body. This
   must be checked _before_ the generic RECEIVED check, because loan
   disbursement messages legitimately contain the word "received" while
   describing debt, not a customer payment.
3. `PURCHASE` / `BILL_PAYMENT` — `bundle purchase request`, `purchase
request of`, `paid off`, `Service Charge`, `Principal` markers. This
   covers both "you bought airtime" and "your bundle purchase request...
   has been received" (a purchase _confirmation_ notice that happens to
   contain "received" as its own false-positive trap).
4. `WITHDRAWN` — `withdrawn` keyword.
5. `SENT` — `sent to` shape.
6. `RECEIVED` — only reached if nothing above matched. Require a positive
   phrase-shape match such as
   `(?:have|has)\s+received\s+(?:GH[₵S]|₵)\s*[\d,.]+\s+from`, not a bare
   substring check on the word "received" alone.

**Open product decision, explicitly called out rather than silently
resolved:** "airtime received" messages (a merchant buying airtime _for_
someone else's number) report no money entering the merchant's own spendable
wallet balance, so they should classify as `PURCHASE`, not `RECEIVED`. This
plan adopts that as the default, but it's worth a one-line confirmation from
whoever owns the verification logic before Layer 1 ships, since it's a
business-meaning decision, not a parsing-correctness one.

---

## 3. Layer 2 — Candidate-scored extraction (the core carrier-agnostic piece)

This is the layer that replaces "regex per carrier, tried in a fixed order"
with something that generalizes without needing a human to anticipate every
carrier's exact template ahead of time.

**For each of the four extractable fields, the algorithm is:**

1. **Generate all candidates.** Don't take the first regex match — find
   every span in the message body that _could_ plausibly be this field type,
   based on shape alone:
   - _Reference candidates:_ every digit run of 8–22 characters.
   - _Amount candidates:_ every number immediately preceded by a currency
     marker (`GH₵`, `GHS`, `GHc`, `₵`, or a bare decimal with exactly 2
     fraction digits as a weaker secondary signal).
   - _Phone candidates:_ every digit run of 9–15 characters (deliberately not
     constrained to Ghana-specific prefixes, since that hardcoding is exactly
     what makes the current code non-carrier-agnostic).
   - _Name candidates:_ every maximal run of 2+ consecutive alphabetic words,
     each 2+ characters, excluding a small denylist of carrier/system tokens
     (`MTN`, `MOBILE`, `MONEY`, `TELECEL`, `CASH`, `AIRTIME`, `GHANA`, `LOAN`,
     `BALANCE`, `PIN`, `OTP`, `REFERENCE`). Real samples store names in ALL
     CAPS (`KYEREMANTENG DERRICK OHENE`), so case-shape alone cannot be the
     signal — the denylist plus word-count plus position is what does the
     work.

2. **Score each candidate**, rather than picking the first or longest match:
   - Distance to the nearest credit-context anchor word (`from`, `Transfer
From:`, `sender`, `received`, `balance is`) — closer scores higher.
   - For phone and name candidates specifically: adjacency to _each other_
     scores both higher, since the dominant shape across every sample format
     seen so far is `PHONE-NAME` or `PHONE NAME` as a single adjacent pair.
   - For reference candidates: position near the start of the message scores
     higher (every observed format puts the reference first), and any
     candidate whose span fully overlaps the chosen amount or phone
     candidate's span is disqualified outright — this single rule is what
     fixes the "fragment of the reference number gets returned as the phone"
     failure mode found during the original code audit.
   - For amount candidates: presence of a currency prefix is a hard
     requirement, not just a scoring bonus, since a bare number with no
     currency marker is too ambiguous to trust (transaction dates, percentage
     fees, and loan amounts are all bare numbers that appear in real samples).

3. **Take the highest-scoring candidate per field**, independently. If no
   candidate for a given field clears a minimum score, leave that field
   `null` rather than guessing — guessing produces exactly the kind of
   confident-but-wrong data this whole plan exists to prevent.

**Why this generalizes better than today's fixed regex set:** it does not
need to know in advance whether a sender writes `Transfer From: PHONE-NAME`
or `from PHONE - NAME` or some third format no one has seen yet — it only
needs the _relative positioning_ of fields to hold (reference near the
start, amount near a currency symbol, name/phone adjacent to each other near
a "from"-type word), which is a far safer cross-carrier assumption than exact
template wording. This is the mechanism that satisfies the original
carrier-agnostic requirement without hardcoding a new branch per network.

**Persistence implication for `SmsSource`/`ParsingRule`:** `refPattern` and
`amountPattern` can remain simple persisted regex strings derived once at
setup time (they're reliably shape-based and a fast literal regex is
cheaper than re-running the full candidate scorer on every SMS). But
`senderNamePattern`/`senderPhonePattern` should support a sentinel value
(e.g. `"USE_HEURISTIC"`) so `SmsParser.parse()` can fall back to calling the
Layer 2 scorer live, directly on the raw body, instead of a brittle fixed
regex — meaning a newly configured sender doesn't need a perfect name/phone
regex guessed from one sample message to work correctly on the next ten
messages that vary slightly in formatting.

---

## 4. Layer 3 — Confidence gate and failure visibility

**Data model additions** (additive, no destructive migration needed for the
core fields):

- `ParsedTransaction.confidence: Double` and `ParsedTransaction.parsedBy:
String` (`"RULE"`, `"HEURISTIC"`, `"RULE+HEURISTIC_FALLBACK"`, or later
  `"LLM_FALLBACK"`).
- Scoring: `reference` found → +0.45, `amount` found → +0.45, `senderPhone`
  found → +0.05, `senderName` found → +0.05. Threshold to accept: 0.6 — set
  so that **reference + amount alone always clears it**, since those are the
  only two fields `ClaimHandler.handleClaim()` actually checks for
  verification; `senderName`/`senderPhone`/`balanceAfter` are display-only
  and must never be allowed to block a valid transaction from being recorded.

**Behavioral change in `SmsListenerService`/`SmsParser` call site:** today,
`SmsParser.parse()` returning `null` results in `stopSelf()` with zero trace.
Going forward:

- Confidence ≥ 0.6 → save as `SmsTransactionEntity` with `status = PENDING`,
  exactly as today.
- Confidence < 0.6 (reference or amount missing) → **still save**, with
  `status = FAILED` and `rawSms` populated (the entity already has this
  field). This single change is what makes Layer 4's retraining loop and any
  future debugging possible — there is currently no record anywhere of _what
  failed to parse and why_, which made even diagnosing the truncation bug
  above require manually tracing regex behavior outside the app entirely.

---

## 5. Layer 5 — LLM fallback for genuinely unrecognized formats

**When it fires:** only when Layer 3 produces a `FAILED` result for a sender
that either (a) has no `parsingRule` configured at all, or (b) has a rule
that has now failed 3 consecutive times (see Layer 4) — i.e., never on the
hot path for a sender that's working fine, to avoid unnecessary cost/latency
and to keep the deterministic layers as the default source of truth.

**How:** send the raw SMS body to Claude via the Anthropic API (this app
already has the scaffolding for "Claude in Claude" API calls available, per
the `anthropic_api_in_artifacts` capability — the same pattern applies here
conceptually, called from the Android app's own backend-less architecture
via a direct HTTPS call to `api.anthropic.com`, not the in-artifact JS
pattern, since this is a native Android service, not a web artifact). Use a
structured-output system prompt forcing JSON-only output:

```
You will be given the raw text of a mobile money SMS. Extract:
reference, amount, senderName, senderPhone, balanceAfter, isReceived (boolean).
Respond with ONLY a JSON object with these exact keys, using null for any
field not present in the message. Do not include any other text.
```

The response is parsed, validated (must be valid JSON, `reference` and
`amount` must be non-null to be usable), and saved with `parsedBy =
"LLM_FALLBACK"` and a confidence of 1.0 if both required fields are present.
This path is genuinely carrier-agnostic in a way no regex/heuristic
combination can be, because it doesn't require the relative-positioning
assumptions Layer 2 depends on — it understands "transaction reference" and
"amount received" as concepts, the same way a human reading the message for
the first time would, regardless of which country or carrier produced it.

**Constraints that keep this safe and bounded:**

- Network-gated: if the device has no connectivity, this layer is skipped
  and the message simply remains `FAILED` for Layer 4 to surface — never
  block the foreground service waiting on a network call.
- Cost/rate-gated: only invoked on genuine extraction failures, not on every
  message, and only after the 3-failure threshold for an _existing_ sender
  (so a sender mid-format-change doesn't trigger an LLM call on every
  single incoming SMS while waiting for a human to fix the rule — the first
  2 failures just count toward the Layer 4 threshold with no LLM call at
  all; only the would-otherwise-be-3rd-failure event, or any failure from a
  totally unconfigured sender, triggers a call).
- Auditable: every transaction's `parsedBy` field tells you exactly which
  layer produced it, so accuracy is measurable in production rather than
  assumed, and any future need to "explain why this transaction is in the
  database" has a clear, retrievable answer.

---

## 6. Layer 4 — Adaptive retraining loop

**Add `consecutiveParseFailures: Int` to `SmsSource`**, persisted alongside
the existing fields in `SmsSourceRepository`. Increment on every `FAILED`
result for that sender (post-Layer-3); reset to 0 on every successful parse.

**At 3 consecutive failures:** post a notification via the already-existing
but currently-unused `MomoBridgeApp.CHANNEL_ID_EVENTS` channel: "The SMS
format for [sender label] seems to have changed. Tap to review." Deep-link
into `SenderConfigScreen` for that sender, with the most recent failed
`rawSms` pre-selected as the sample message in `ConfigStep.PICK_MESSAGE`
(rather than requiring the user to scroll their inbox to find it again).

**Why this closes the loop rather than just patching one bug:** every time a
carrier changes its template, the system self-detects within 3 messages,
surfaces the exact failing text to a human with one tap, and the human's
one-time confirmation produces a new rule that Layer 2 then uses going
forward — meaning accuracy trends upward the longer the app runs in the
field, instead of being fixed at whatever the carriers' formats happened to
be when this code was written.

---

## 7. Build order

1. **Layer 1** (classification rewrite) — smallest change, highest
   immediate correctness payoff, no data-model changes required. Ship first.
2. **Layer 3's data model and gate logic** (confidence/parsedBy fields,
   FAILED-status saving instead of silent drop) — needed before Layer 2 can
   be meaningfully evaluated, because without it there's still no visibility
   into whether Layer 2's candidate scoring is actually working better than
   the old fixed regexes.
3. **Layer 2** (candidate-scored extraction, replacing the fixed-order regex
   chain in `AutoDetectUtils`/`SmsParser`) — the biggest single piece of
   work, and the one that most directly delivers on carrier-agnosticism.
4. **Layer 4** (failure counting + notification + pre-filled retraining
   flow) — depends on Layer 3 existing so there's something concrete to
   count and show.
5. **Layer 5** (LLM fallback) — last, since it's the most expensive to build
   correctly (network/auth/rate-limit handling, structured-output
   validation) and the least urgent — Layers 1–4 alone already convert every
   silent failure into a one-tap-to-fix human workflow, which is most of the
   practical value. Layer 5 is what pushes accuracy closer to "self-healing
   without a human in the loop at all," which matters most once you're
   supporting carriers you haven't manually configured yet.

Each layer should be validated against the full `sms_samples.txt` set (all
100 messages, all senders) as a standing regression check before moving to
the next layer, so a later layer's bugs can't be confused with an earlier
layer's regressions.
