# SMS Parsing Engine — Problem Brief

## What MoMo Bridge Does

The app intercepts incoming SMS from mobile money/carrier senders (MTN, T-CASH/Telecel, AT Money, etc.), parses them to extract transaction details, and stores them locally. When a customer enters a transaction reference on a website, the app looks up the local DB and confirms/rejects the payment.

**The app must be carrier-agnostic** — it should work with ANY mobile money provider in ANY country, not just Ghanaian ones. The parsing rules must adapt when carriers change their SMS format.

## Current Architecture (4 files)

### 1. `domain/parser/SmsClassifier.kt`
Classifies a raw SMS body into an action type:
- `RECEIVED` — money came in (keep these)
- `SENT` — money went out (discard)
- `WITHDRAWN` — cash-out (discard)
- `PURCHASE` — bought airtime/data/bundle (discard)
- `BILL_PAYMENT` — paid a bill (discard)
- `LOAN` — loan received (discard)
- `NON_TX` — promotional, OTP, system message (discard)

Uses keyword matching against word lists. Works but has edge cases:
- "You have received GHS50.00 FlexLoan" — contains "received" but is a LOAN, not RECEIVED
- "Your bundle purchase request has been received" — contains "received" but is PURCHASE
- "Transaction ID: xxx confirmed from 555. You have received airtime..." — contains both "received" and purchase context

### 2. `domain/parser/FieldExtractor.kt` — THE PROBLEMATIC FILE
Generic heuristic-based field extraction with no carrier-specific regex. Extracts:
- `reference` — transaction reference number
- `amount` — money amount
- `senderName` — who sent the money
- `senderPhone` — their phone number
- `balanceAfter` — account balance after transaction
- `confidence` — how confident the extraction is (0.0–1.0)

**Known Bugs:**

**Bug A — Phone number matches transaction reference IDs**
The `ghanaPhonePattern` regex `\b(0(?:2[0-9]|3[0-9]|5[0-9]|4[0-9]|5[0-9]|7[0-9]|9[0-9])\d{7})\b` requires valid Ghanaian prefixes (02X, 05X, 024, 027, 054, etc.). But T-CASH reference IDs like `0000013331054115` could still partially match if the pattern is too loose, or specific numbers like "0201462617" (which is a phone in SMS #81: "airtime for 0201462617") match as phones when they shouldn't — that number is the recipient of airtime, not the sender.

**Bug B — Sender name not extracted for many formats**
The `extractSenderName()` function tries patterns in order:
1. `Transfer From: PHONE-NAME on` — works for T-CASH cross-network transfers
2. `from PHONE - NAME on` — works for some formats
3. `received X from NAME. Your/Current/Telecel/New` — regex may be too strict
4. Generic `from NAME before PHONE` — misses formats where name isn't near a phone

For MTN payment received format: _"Payment received for GHS 18.00 from JOHN DOE. Current Balance..."_ — pattern 3 should catch "JOHN DOE" but the lookahead `(?:\.\s*(?:Your|Current|Telecel|New))` might fail if the punctuation or spacing differs.

**Bug C — Confidence threshold too high or wrong scoring**
Current confidence scoring:
- reference found: +0.25
- amount found: +0.35
- phone found: +0.15
- name found: +0.10
- has "received"/"credited"/"deposited": +0.15
- Total max: 1.0

Threshold: 0.6. If any field is missing (e.g., no sender phone because it's an internal transfer), the score drops below 0.6 and the extraction is discarded.

### 3. `domain/parser/SmsParser.kt`
Orchestrates: classify → try regex rule → fall back to FieldExtractor heuristic.
- First calls `SmsClassifier.classify()` — if not RECEIVED, returns null
- Then tries `tryParseWithRule()` — uses the sender's configured regex patterns
- If rule fails, falls back to `tryParseHeuristic()` — uses FieldExtractor
- Returns null if both fail

### 4. `domain/parser/AutoDetectUtils.kt`
Used ONLY during sender configuration (not during live parsing). When a user adds a new sender, this auto-detects fields from a sample message and builds regex rules. **Heavily hardcoded to Ghanaian carriers** (MTN, T-CASH/Telecel). Contains carrier-specific format detection and regex generation.

## The SMS Formats (from `sms_samples.txt`)

### RECEIVED (should keep):
**SMS #10** — T-CASH received from MTN MoMo:
`0000013374213220 Confirmed. You have received GHS700.00 from MTN MOBILE MONEY with transaction reference: Transfer From: 233542405901-KYEREMANTENG DERRICK OHENE on 2026-06-15 at 07:24:51. Your Telecel Cash balance is GHS702.46.`
→ Ref: `0000013374213220`, Amount: 700.00, Sender: KYEREMANTENG DERRICK OHENE, Phone: 233542405901, Balance: 702.46

**SMS #21** — T-CASH received from MTN MoMo:
`0000013331054115 Confirmed. You have received GHS18.00 from MTN MOBILE MONEY with transaction reference: Transfer From: 233540815825-BRIDGET BAIDOO on 2026-06-11 at 12:05:54. Your Telecel Cash balance is GHS39.07.`
→ Ref: `0000013331054115`, Amount: 18.00, Sender: BRIDGET BAIDOO, Phone: 233540815825, Balance: 39.07

**SMS #60** — T-CASH received from MTN MoMo:
`0000013194814933 Confirmed. You have received GHS600.00 from MTN MOBILE MONEY with transaction reference: Transfer From: 233548983019-BRIGHT ATTA BAIDEN on 2026-05-30 at 08:35:03. Your Telecel Cash balance is GHS600.57.`
→ Ref: `0000013194814933`, Amount: 600.00, Sender: BRIGHT ATTA BAIDEN, Phone: 233548983019, Balance: 600.57

**SMS #73** — T-CASH received direct:
`0000013163163353 Confirmed. You have received GHS300.00 from 233504982678 - JOEL NII ARMAH MENSAH on 2026-05-27 at 11:08:05. Your Telecel Cash balance is GHS349.77. Reference: TRAVEL.`
→ Ref: `0000013163163353`, Amount: 300.00, Sender: JOEL NII ARMAH MENSAH, Phone: 233504982678, Balance: 349.77

**SMS #96** — T-CASH airtime received (debatable if payment):
`Transaction ID: 0000013085405512 confirmed from 555. You have received airtime of GHS31.00 from 233205516734 - AYITEY DAVID QUAYE on 2026-05-20 at 10:28:48.`
→ Ref: `0000013085405512`, Amount: 31.00, Sender: AYITEY DAVID QUAYE, Phone: 233205516734, No balance

**MTN format** (not in samples but expected):
`Payment received for GHS 18.00 from JOHN DOE. Current Balance GHS 150.00. Transaction ID: 12345678.`
→ Ref: `12345678`, Amount: 18.00, Sender: JOHN DOE, Phone: none, Balance: 150.00

### SENT / WITHDRAWN / PURCHASE (must filter out):
**SMS #9** — sent: `GHS125.00 sent to 0543273403 WINIFRED MAMA NYEKOSI on MTN MOBILE MONEY`
**SMS #49** — sent: `GHS30.50 sent to 0597489309 MARTHA KYEREWAA GYASI on MTN MOBILE MONEY`
**SMS #8** — withdrawn: `You have withdrawn GHS500.00 from G25805`
**SMS #2** — purchase: `You bought GHS10.00 of airtime for 0592078372`
**SMS #40** — purchase: `Your bundle purchase request of 1.45 GB @ GHs4.50`

### NON_TX (must filter out):
**SMS #1** — promo: `Congrats! you have purchased a Mashup of Ghc5`
**SMS #6** — system: `Celebrate 30 years of success with us`
**SMS #18** — OTP: `Enter code 114001 to pay GHS 5.61 to BRYTELINK. DO NOT SHARE`

## What Needs To Be Fixed / Rebuilt

### Priority 1: FieldExtractor Accuracy
- **Phone extraction** must never match reference IDs. Must only extract phones found near context keywords (`from`, `Transfer From:`, `by`, `sender`) OR that match country-specific patterns with word boundaries. Reference IDs like `0000013331054115` start with 4+ zeros and are 16+ digits — phones are 10 digits and start with valid prefixes.
- **Sender name extraction** needs to handle all formats in the samples. The current regex approach (sequential pattern matching) is fragile. Consider NLP heuristics:
  - Find all proper noun sequences
  - Cross-reference with position near keywords
  - Use capitalization patterns (names are Title Case, not ALL CAPS like carriers)
- **Confidence scoring** should be weighted differently. Amount + reference should be enough (>0.6) even without phone or name. Phone is a bonus, not a requirement.

### Priority 2: AutoDetectUtils Rewrite
Currently carrier-specific (MTN/T-CASH hardcoded). Should be replaced with the same generic approach as FieldExtractor. The "build rule" step should:
1. Let the user select the reference, amount, name, phone from highlighted candidates
2. Generate a regex that correctly identifies ONLY those fields
3. Validate against a second message to ensure the regex isn't too broad

### Priority 3: SmsClassifier Improvements
- "FlexLoan", "Ready Loan" contexts should be LOAN even though they contain "received"
- "Your bundle purchase request... has been received" should be PURCHASE, not RECEIVED
- "You have received airtime of GHSxx" — this is ambiguous (someone bought you airtime = money received in value)

### Priority 4: Adaptive Retraining
When `SmsParser.parse()` fails for a known sender (has a rule but parsing returns null):
- Track failure count per sender
- After 3 consecutive failures, notify user: "The SMS format for [sender] seems to have changed. Update your parsing rules."
- Show the failed message as a template for retraining

## Files Summary

| File | Path | Purpose | Issues |
|------|------|---------|--------|
| SmsClassifier | `domain/parser/SmsClassifier.kt` | Classify SMS action type | Edge cases with loan/purchase containing "received" |
| FieldExtractor | `domain/parser/FieldExtractor.kt` | Generic heuristic extraction | Phone matching ref IDs, sender name fails, confidence too strict |
| SmsParser | `domain/parser/SmsParser.kt` | Orchestrate parse pipeline | Wires classifier → rule → heuristic |
| AutoDetectUtils | `domain/parser/AutoDetectUtils.kt` | Manual rule builder | Ghana-carrier-hardcoded |
| ParsedTransaction | `domain/model/ParsedTransaction.kt` | Parsing output model | Has confidence + parsedBy fields |
| SmsSource | `domain/model/SmsSource.kt` | Sender config model | Has SenderType, trainingMessages |
| sms_samples.txt | `sms_samples.txt` | 100 labeled SMS samples | Ground truth for testing |
