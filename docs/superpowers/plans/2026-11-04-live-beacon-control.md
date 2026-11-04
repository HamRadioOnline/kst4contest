# Live Beacon Control Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make both beacon enable switches effective immediately at runtime and make the aggregate beacon status chip an accurate shared toggle.

**Architecture:** Keep `ChatPreferences` as the profile-scoped source of truth, but expose the two activation flags as thread-safe observable values. Route settings and chip actions through `ChatController`, publish one aggregate `MyBeacon` status, and give only that thread-status chip an optional click action.

**Tech Stack:** Java 21, Kotlin 2.2, Compose Multiplatform, JUnit 5, Mockito, Gradle wrapper

**Spec:** `docs/superpowers/specs/2026-11-04-live-beacon-control-design.md`

## Global Constraints

- Work directly on `main`, as explicitly authorized.
- Do not stage, commit, push, merge, tag, release or deploy.
- Keep beacon templates, interval handling, TX framing and second-chat eligibility unchanged.
- Write source-code and configuration comments in English.
- Use focused tests first and finish with `./gradlew.bat build --console=plain`.

## Review Focus

- A disabled second chat must leave its dormant second-beacon flag unchanged when the chip is clicked.
- An external chip toggle must update an already open Beacon settings tab without reopening it.
- A runtime toggle must not restart the timer or enqueue an immediate message.
- An invalid beacon template must still be reported as a critical status rather than hidden by the aggregate state.
- Profile reload must replace the observable values without carrying the previous profile's runtime flags across.

---

### Task 1: Observable profile-scoped beacon activation

**Files:**
- Modify: `core/src/main/java/kst4contest/model/ChatPreferences.java`
- Test: `core/src/test/java/kst4contest/model/ChatPreferencesBeaconStateTest.java`

**Interfaces:**
- Produces: existing boolean getters/setters plus observable main/second beacon value accessors
- Consumes: existing `MutableValue` / `SimpleValue` infrastructure

- [x] Write tests that changes are observable, values remain independent, XML reload restores both values, and an unsaved in-memory change does not survive a fresh load.
- [x] Run `./gradlew.bat :core:test --tests kst4contest.model.ChatPreferencesBeaconStateTest --console=plain` and verify the new observable-accessor tests fail because the API is absent.
- [x] Back the two flags with `SimpleValue<Boolean>` while preserving the existing getters, setters and XML element names.
- [x] Re-run the focused test and the existing ChatPreferences tests.

### Task 2: Immediate settings effect and aggregate controller semantics

**Files:**
- Modify: `core/src/main/java/kst4contest/controller/ChatController.java`
- Modify: `core/src/main/java/kst4contest/controller/BeaconTask.java`
- Create: `core/src/main/java/kst4contest/controller/BeaconStatus.java`
- Test: `core/src/test/java/kst4contest/controller/BeaconControlTest.java`
- Test: `core/src/test/java/kst4contest/controller/BeaconTaskTest.java`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/tabs/BeaconTabState.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/tabs/BeaconTab.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/ObservedValue.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/SettingsTabs.kt`
- Modify: `app-desktop/src/test/kotlin/kst4contest/view/compose/tabs/BeaconTabStateTest.kt`
- Modify: `app-desktop/src/test/kotlin/kst4contest/view/compose/tabs/SettingsApplyScreenTest.kt`
- Modify: `app-desktop/src/test/kotlin/kst4contest/view/compose/tabs/DraftedSettingsTableTest.kt`

**Interfaces:**
- Consumes: observable preference values from Task 1
- Produces: controller setters, usable-beacon toggle, aggregate status publication, Compose observation bridge

- [x] Write failing controller tests for `off`, `1/2`, `on`, main-only toggling when second chat is disabled, and both-category toggling when it is enabled.
- [x] Write failing BeaconTask tests proving a flag changed between two runs changes queued output without restarting a timer, and that the final status is aggregate.
- [x] Change Beacon settings tests so activation is immediate while templates and interval still wait for Apply; verify RED against the current draft-backed flags.
- [x] Implement the aggregate status helper and controller actions without changing the outbound-message path.
- [x] Make only the enable flags bypass `SettingsDraft`, observe their preference values in Compose, and wire their setters through the controller.
- [x] Run the core and app-desktop focused tests and verify GREEN.

### Task 3: Clickable beacon status chip

**Files:**
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/StatusIndicatorState.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/StatusBar.kt`
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `app-desktop/src/test/kotlin/kst4contest/view/compose/StatusIndicatorStateTest.kt`
- Create or modify: focused Compose status-bar rendering test under `app-desktop/src/test/kotlin/kst4contest/view/compose/`

**Interfaces:**
- Consumes: controller usable-beacon toggle from Task 2
- Produces: optional per-source chip action registered only for `MyBeacon`

- [x] Write failing state tests for actions registered before and after the first status report and for non-action thread chips.
- [x] Write a failing Compose test proving the beacon chip invokes its action while an ordinary status chip remains non-clickable.
- [x] Add optional actions to thread-status buttons and apply button semantics only when an action exists.
- [x] Register the controller toggle for `MyBeacon` during main-window state construction.
- [x] Run the focused status tests and the affected main-window tests.

### Task 4: Documentation and verification

**Files:**
- Modify: relevant beacon sections in `github_docs/de-Konfiguration.md`
- Modify: corresponding sections in `github_docs/en-Configuration.md`
- Modify: `docs/PROJECT_CONTEXT.md`
- Review: relevant screenshots and changelog impact only

**Interfaces:**
- Consumes: completed runtime and UI behavior from Tasks 1-3
- Produces: aligned user documentation and durable state/persistence invariant

- [x] Update both manuals with immediate runtime effect, explicit-save persistence, aggregate chip behavior and second-chat handling.
- [x] Record the observable canonical state, timer-read rule and persistence boundary in `docs/PROJECT_CONTEXT.md`.
- [x] Run all focused tests, then `./gradlew.bat build --console=plain` and read test-result XML summaries.
- [x] Run `git diff --check`, inspect PMD/SpotBugs XML for findings in changed production files, and review the final diff for unrelated changes.
- [x] Leave all changes unstaged and uncommitted and report remaining manual UI checks.
