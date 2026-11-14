# Beacon Session Safety Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make beacon activation session-only, enforce a five-minute minimum, disable the correct category after an external QRG move beyond 5 kHz, and render a reason-aware traffic-colour `MyBeacon` chip.

**Architecture:** A controller-owned `BeaconSessionState` coordinates the existing observable runtime flags, category references, inactive reasons and send failures. Manual QRG commits and UCXLog/Win-Test reports enter through distinct controller methods; a structured model snapshot crosses the existing thread-status boundary and `app-desktop` localises and styles it.

**Tech Stack:** Java 21, Kotlin 2.2, Compose Multiplatform 1.8.x, JUnit 5, Mockito, Gradle wrapper

**Spec:** `docs/superpowers/specs/2026-11-11-beacon-session-safety-design.md`

## Global Constraints

- Work directly on `main`, as explicitly authorised.
- Do not stage, commit, push, merge, tag, release or deploy; commit steps are deliberately omitted because Marc authorised none of them.
- Keep source comments and Javadoc in English; user-facing DE/EN text follows the project writing style.
- Use exactly 5 minutes and 5,000 Hz; exactly 5,000 Hz must not disable a beacon.
- Do not add a production dependency or change ON4KST/UCXLog/Win-Test framing.
- Keep the shared timer, initial ten-second check, outbound queue and second-chat eligibility unchanged.
- Do not execute UI callbacks while holding an internal beacon-state lock.
- Preserve unrelated local changes and inspect the diff before and after every task.

## Review Focus

- A secondary beacon using `MYQRG` must be refused or disabled when the primary QRG is invalid, even though its own reference remains the secondary QRG; Task 1 pins this cross-template case.
- An external report with no reference must establish the reference without disabling; Task 1 pins this first-report case.
- Without SO2R a Win-Test/UCXLog report must affect only the main beacon; Task 3 pins both integrations.
- An old XML file with enabled flags and a one-minute interval must load as disabled with a five-minute interval and later write `false`; Task 2 pins the migration.
- `FAILED` must stay red without an orange flash while ordinary worker chips retain current theme colours; Task 4 pins both sides.

---

### Task 1: Pure beacon session policy and structured status

**Files:**
- Create: `core/src/main/java/kst4contest/controller/BeaconFrequencyParser.java`
- Create: `core/src/main/java/kst4contest/controller/BeaconSessionState.java`
- Create: `core/src/main/java/kst4contest/model/BeaconStatusSnapshot.java`
- Create: `core/src/test/java/kst4contest/controller/BeaconFrequencyParserTest.java`
- Create: `core/src/test/java/kst4contest/controller/BeaconSessionStateTest.java`

**Interfaces:**
- Produces: `BeaconFrequencyParser.parseHz(String): OptionalLong`.
- Produces: `BeaconSessionState.Channel { MAIN, SECOND }` and synchronized policy methods `resetSession(ChatPreferences)`, `setEnabled(Channel, boolean, ChatPreferences)`, `onManualQrgChanged(Channel, String, ChatPreferences)`, `onExternalQrgChanged(Channel, long, ChatPreferences)`, `onTemplateChanged(Channel, ChatPreferences)`, `recordSendFailure(Channel, boolean)` and `snapshot(ChatPreferences): BeaconStatusSnapshot`.
- Produces: explicit `BeaconStatusSnapshot` DTO with aggregate state `OFF`, `PARTIAL`, `ON`, `FAILED`, plus main/second category DTOs carrying availability, enablement, failure and `InactiveReason`.

- [ ] **Step 1: Write parser tests** for `144.300`, `144,300`, `144.300.03`, `1296.123.000`, compact `144300`, compact `10368100`, blanks, malformed text, non-positive values and overflow.
- [ ] **Step 2: Run the parser test and verify RED.**

  Run: `.\gradlew.bat :core:test --tests kst4contest.controller.BeaconFrequencyParserTest --console=plain`

  Expected: compilation failure because `BeaconFrequencyParser` does not exist.

- [ ] **Step 3: Implement `BeaconFrequencyParser`** with integer arithmetic and the QRG forms already used by the client; accept only a complete positive frequency that maps to a supported `Band`.
- [ ] **Step 4: Re-run the parser test and verify GREEN.**
- [ ] **Step 5: Write session-policy tests** for initial reasons, independent enablement, placeholder requirements in either template, partial aggregate activation, manual reference creation/rebase, invalid manual clearing, cross-template invalidation, first external reference, 5,000/5,001-Hz boundaries, per-category external changes, reason retention and send-failure clearing.
- [ ] **Step 6: Run `BeaconSessionStateTest` and verify RED** because the policy and DTO are absent.
- [ ] **Step 7: Implement the DTO and policy** so the existing observable preference booleans are projection only and no listener/callback leaves a synchronized method.
- [ ] **Step 8: Re-run both focused tests and verify GREEN.**
- [ ] **Step 9: Inspect the task diff** for duplicated state, floating-point frequency comparisons or display strings in `core`.

### Task 2: Session-only persistence and five-minute migration

**Files:**
- Modify: `core/src/main/java/kst4contest/model/ChatPreferences.java`
- Modify: `core/src/main/java/kst4contest/controller/ChatController.java`
- Modify: `core/src/main/resources/praktiKSTpreferences.xml`
- Modify: `core/src/test/java/kst4contest/model/ChatPreferencesBeaconStateTest.java`
- Modify: `core/src/test/java/kst4contest/model/ChatPreferencesRoundTripTest.java`
- Modify: `app-desktop/src/test/kotlin/kst4contest/view/compose/tabs/BeaconTabStateTest.kt`
- Modify: `app-desktop/src/test/kotlin/kst4contest/view/i18n/RemainingTextsFollowTheLanguageTest.kt`

**Interfaces:**
- Consumes: Task 1 `BeaconSessionState`.
- Produces: `ChatController.MIN_BEACON_INTERVAL_MINUTES == 5` and session reset during controller/profile construction without reset on reconnect.

- [ ] **Step 1: Replace persistence expectations with failing tests** proving defaults are off, stored `true` is ignored, every full write stores both legacy/current enable tags as `false`, layout-only writes do not revive them, and intervals below five normalise in memory.
- [ ] **Step 2: Add interval tests** proving four is refused, five accepted, and an old stored one becomes five without changing a larger value.
- [ ] **Step 3: Run the focused core and desktop tests and verify RED.**

  Run: `.\gradlew.bat :core:test --tests kst4contest.model.ChatPreferencesBeaconStateTest --tests kst4contest.model.ChatPreferencesRoundTripTest :app-desktop:test --tests kst4contest.view.compose.tabs.BeaconTabStateTest --tests kst4contest.view.i18n.RemainingTextsFollowTheLanguageTest --console=plain`

- [ ] **Step 4: Make enable flags session-only**: initialise both false, ignore XML enable values, always serialise `false`, retain existing element names, and reset the policy only when a new controller/profile preferences object is installed.
- [ ] **Step 5: Set and enforce the five-minute minimum** in controller, XML defaults and preference-load normalisation; retain timer start/restart semantics.
- [ ] **Step 6: Re-run the focused tests and inspect their XML summaries.**
- [ ] **Step 7: Inspect the task diff** to confirm no reconnect path resets the session and no default was silently raised to ten minutes.

### Task 3: Explicit manual and external QRG paths

**Files:**
- Modify: `core/src/main/java/kst4contest/controller/ChatController.java`
- Modify: `core/src/main/java/kst4contest/controller/ReadUDPbyUCXMessageThread.java`
- Modify: `core/src/main/java/kst4contest/controller/ReadUDPByWintestThread.java`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/ChatInputState.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/ChatInputRow.kt`
- Modify: `app-desktop/src/main/java/kst4contest/view/ComposeChatInputActions.java`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/tabs/BeaconTabState.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/SettingsTabs.kt`
- Modify: focused fake `ChatInputActions` implementations in affected tests
- Modify: `core/src/test/java/kst4contest/controller/BeaconControlTest.java`
- Modify: `core/src/test/java/kst4contest/controller/ReadUDPbyUCXMessageThreadLoopbackTest.java`
- Modify: `core/src/test/java/kst4contest/controller/WinTestStatusSlotTest.java`
- Modify: `app-desktop/src/test/kotlin/kst4contest/view/compose/ChatInputStateTest.kt`
- Modify: `app-desktop/src/test/kotlin/kst4contest/view/compose/tabs/BeaconTabStateTest.kt`

**Interfaces:**
- Produces: controller methods `applyManualQrg(RadioSlot, String)`, `applyExternalQrg(RadioSlot, String, long)`, `onMainBeaconTemplateApplied()` and `onSecondBeaconTemplateApplied()`.
- Produces: a `ChatInputActions`/`ChatInputState` manual-QRG commit method used by `ChatInputRow` instead of writing preferences directly.
- Consumes: Task 1 policy and Task 2 session lifecycle.

- [ ] **Step 1: Add failing controller tests** for individual activation refusal, valid partial chip activation, user disable reason, manual rebase, invalid manual cross-template disabling and template-apply revalidation.
- [ ] **Step 2: Add failing chat-input tests** proving both manual fields call the explicit action with the correct `RadioSlot`/category and never write preferences directly.
- [ ] **Step 3: Add failing UCXLog tests** for exact Hz conversion, first-reference behaviour, >5-kHz main-only shutdown without SO2R and second-only shutdown for RadioNr 2 with SO2R.
- [ ] **Step 4: Add failing Win-Test tests** for main/pass frequency selection, 100-Hz units, non-SO2R main-only and SO2R slot routing.
- [ ] **Step 5: Run the four focused test groups and verify RED.**
- [ ] **Step 6: Implement the controller entry points** so the preference value and policy update occur together on the existing UI-dispatch boundary and publish a fresh beacon status only after state is consistent.
- [ ] **Step 7: Route manual Compose commits through `ComposeChatInputActions`** while preserving focus/commit behaviour and existing displayed `ObservedValue` mirrors.
- [ ] **Step 8: Give both template draft properties a shared post-apply effect key** that revalidates active beacons after all writes, without changing their validation-before-stage behaviour.
- [ ] **Step 9: Route UCXLog and Win-Test through `applyExternalQrg`** only when their current QRG-sync setting already permits the update; preserve packet parsing and diagnostics.
- [ ] **Step 10: Re-run focused tests and inspect XML summaries.**
- [ ] **Step 11: Run related integration tests** for SO2R routing, settings apply and chat-input rendering.
- [ ] **Step 12: Inspect the task diff** for protocol/framing changes, toolkit calls in `core`, or source inference through listeners.

### Task 4: Beacon task status, translated tooltips and traffic colours

**Files:**
- Modify: `core/src/main/java/kst4contest/controller/BeaconTask.java`
- Modify: `core/src/main/java/kst4contest/controller/BeaconStatus.java`
- Modify: `core/src/main/java/kst4contest/model/ThreadStateMessage.java`
- Create: `app-desktop/src/main/kotlin/kst4contest/view/compose/BeaconStatusPresentation.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/StatusIndicatorState.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/StatusBar.kt`
- Modify: `app-desktop/src/main/i18n/strings_de.properties`
- Modify: `app-desktop/src/main/i18n/strings_en.properties`
- Modify: `core/src/test/java/kst4contest/controller/BeaconTaskTest.java`
- Modify: `core/src/test/java/kst4contest/controller/BeaconControlTest.java`
- Create: `app-desktop/src/test/kotlin/kst4contest/view/compose/BeaconStatusPresentationTest.kt`
- Modify: `app-desktop/src/test/kotlin/kst4contest/view/compose/StatusIndicatorStateTest.kt`
- Modify: `app-desktop/src/test/kotlin/kst4contest/view/compose/StatusBarScreenTest.kt`
- Modify: relevant i18n boundary tests

**Interfaces:**
- Consumes: Task 1 `BeaconStatusSnapshot` and policy send-failure methods.
- Produces: optional structured beacon snapshot on `ThreadStateMessage`; other thread messages remain unchanged.
- Produces: `BeaconStatusPresentation` mapping snapshot plus current strings to label, tooltip and visual style.

- [ ] **Step 1: Add failing core tests** proving a failed active category yields `FAILED`, a later successful run clears only its failure, disabled categories are not failed, and ordinary timer runs preserve inactive reasons.
- [ ] **Step 2: Add failing presentation tests** for bilingual per-category tooltips, initial/user/QRG/QRG-invalid reasons, `1/2`, and no concrete frequency in the QRG reason.
- [ ] **Step 3: Add failing colour-model tests** for the exact light/dark OFF colours, ON/PARTIAL green, 200-ms orange highlight, FAILED red/white with highlight suppression and unchanged default-worker colours.
- [ ] **Step 4: Run focused core and desktop tests and verify RED.**
- [ ] **Step 5: Attach the structured snapshot to the existing status message** without adding translated display prose to `core`.
- [ ] **Step 6: Make `BeaconTask` record per-category outcomes** while retaining text resolution, category validation and regular TX queue insertion.
- [ ] **Step 7: Implement translated presentation and visual style** only for messages carrying a beacon snapshot; keep action/provider ordering and stale-update protection intact.
- [ ] **Step 8: Implement the selected colours in `ThreadStatusButtonView`** and suppress the highlight when style is FAILED.
- [ ] **Step 9: Add aligned DE/EN i18n keys** and re-run translation-boundary tests.
- [ ] **Step 10: Re-run focused tests and inspect XML summaries.**
- [ ] **Step 11: Run the affected Compose screen tests** and inspect a rendered light/dark result if the test harness emits one.
- [ ] **Step 12: Inspect the task diff** to ensure no non-beacon chip acquired special colours or click behaviour.

### Task 5: Targeted documentation, context and full verification

**Files:**
- Modify: `github_docs/de-Konfiguration.md`
- Modify: `github_docs/en-Configuration.md`
- Modify: `github_docs/de-Contest-Workflow.md`
- Modify: `github_docs/en-Contest-Workflow.md`
- Modify: `github_docs/de-Makros-und-Variablen.md`
- Modify: `github_docs/en-Macros-and-Variables.md`
- Review/modify: `github_docs/de-Funktionen.md`, `github_docs/en-Features.md`
- Review/modify: `github_docs/de-Benutzeroberflaeche.md`, `github_docs/en-User-Interface.md`
- Modify: `github_docs/de-Changelog.md`
- Modify: `github_docs/en-Changelog.md`
- Modify: `website/src/features/macros.md`
- Review/modify: `website/src/features/trx-qrg-synchronisation.md`
- Modify: `docs/PROJECT_CONTEXT.md`
- Modify: `docs/superpowers/specs/2026-11-04-live-beacon-control-design.md`
- Review: `github_docs/client_settings_window_beacon.png`

**Interfaces:**
- Consumes: completed behaviour from Tasks 1-4.
- Produces: semantically aligned DE/EN user documentation and current durable project context.

- [ ] **Step 1: Update the targeted manual sections** with session-only activation, five-minute minimum, activation prerequisites, source-specific reference/QSY rules, per-category tooltip and traffic colours.
- [ ] **Step 2: Update both changelogs compactly** without inventing a version or release state.
- [ ] **Step 3: Update only website sections that already describe interval or TRX-QRG behaviour**; do not duplicate the manual.
- [ ] **Step 4: Reconcile the older live-beacon design and `PROJECT_CONTEXT`** so they no longer claim activation persists or the one-minute minimum remains unchanged.
- [ ] **Step 5: Inspect the existing Beacon settings screenshot** and record replacement only if a visible value is now false; do not fabricate an image.
- [ ] **Step 6: Run all focused test classes from Tasks 1-4.**
- [ ] **Step 7: Run the full build.**

  Run: `.\gradlew.bat build --console=plain`

  Expected: `BUILD SUCCESSFUL` with no failed tests.

- [ ] **Step 8: Read every generated `*/build/test-results/**/TEST-*.xml` summary** and report test/failure/error/skipped totals rather than relying only on exit code.
- [ ] **Step 9: Count PMD and SpotBugs XML findings per module** from `<violation` and `<BugInstance>` and identify findings touching changed production files.
- [ ] **Step 10: Run `git diff --check` and inspect `git status --short`.**
- [ ] **Step 11: Review the complete diff against the approved spec** for unrelated cleanup, stale documentation, accidental protocol changes and user-authored changes.
- [ ] **Step 12: Perform a manual runtime check if safely possible**: initial off state, valid/invalid category activation, manual rebase, external >5-kHz shutdown, `1/2`, OFF tooltip, orange flash and persistent FAILED red.

## Self-Review Result

- Every approved behaviour maps to Tasks 1-4; documentation and durable context map to Task 5.
- Exact types and controller entry points are defined once in the Interfaces blocks.
- Cross-template QRG dependencies, first external reference, non-SO2R routing, XML migration and FAILED colour priority are explicit tests.
- Plan size reflects five independently reviewable deliverables; no implementation bodies or unauthorised Git actions are included.
