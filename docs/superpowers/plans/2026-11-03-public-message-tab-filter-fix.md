# Public Message Tab Filter Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the main-window **Public messages** tab show only ON4KST messages whose protocol receiver is public (`0`, represented as `ALL`).

**Architecture:** Keep the global message roster as the complete event store and reuse `ChatController.toAllMessages()` as the single existing public-message view. A small package-visible refresh method updates all three Compose message tables so initial population and live updates use the same filtered sources.

**Tech Stack:** Java 21, Kotlin 2.2, Compose Multiplatform, JUnit 5, Mockito, Gradle

**Spec:** `github_docs/en-User-Interface.md` and `github_docs/de-Benutzeroberflaeche.md` document the intended global-tab semantics.

## Global Constraints

- Preserve ON4KST parsing, the complete global message history, message order and chat-category identity.
- Preserve the selected-station message modes and the existing `pm to me`/`pm to other` semantics.
- Do not stage, commit, push or publish without separate authorization.
- Source comments remain English.

## Review Focus

- A genuine public frame remains in the public tab after initial population and a later refresh.
- A PM to the local callsign never enters the public tab.
- A directed message between other stations never enters the public tab.
- Directed and QSO-of-the-other tables continue to receive their existing filtered views.
- Repeated table refreshes replace rows without changing their newest-first order.

---

### Task 1: Route the Compose public table through the existing filtered view

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Create: `app-desktop/src/test/kotlin/kst4contest/view/ComposeMessageTableRefreshTest.kt`

**Interfaces:**
- Consumes: `ChatController.toMeMessages()`, `ChatController.toAllMessages()`, and `ChatController.toOtherMessages()`
- Produces: package-visible `Kst4ContestApplication.refreshComposeMessageTables(...)`

- [x] **Step 1: Write the failing regression test**

Create a mixed-message test whose mocked controller exposes separate directed, public and other-station views. Assert that the public table receives only `toAllMessages()`, then change the mocked public result, refresh again and assert the replacement remains filtered.

- [x] **Step 2: Run the focused test and verify it fails**

Run: `./gradlew.bat :app-desktop:test --tests kst4contest.view.ComposeMessageTableRefreshTest --console=plain`

Expected: compilation failure because `refreshComposeMessageTables(...)` does not yet exist.

- [x] **Step 3: Implement the shared refresh method**

Add package-visible static `refreshComposeMessageTables(ChatController, DataTableState<ChatMessage>, DataTableState<ChatMessage>, DataTableState<ChatMessage>)`. It must populate the tables from `toMeMessages()`, `toAllMessages()` and `toOtherMessages()` respectively.

- [x] **Step 4: Use the method for both update paths**

Call the shared method from the global-message listener on the UI dispatcher and from the initial Compose table population. Remove both raw writes to the public table.

- [x] **Step 5: Run focused and related tests**

Run the new test plus `MessageTabsScreenTest`, `MessageBusManagementThreadChatFrameTest`, `SelectedStationMessageFlowTest`, and `SelectedStationMessagePredicateTest`.

- [x] **Step 6: Run full verification**

Run `./gradlew.bat build --console=plain`, inspect test-result XML and changed-file static-analysis reports, then run `git diff --check` and `git status --short --branch`.

- [x] **Step 7: Assess documentation impact**

Confirm the German and English manuals already state that the global public tab contains public messages only. Update no documentation unless implementation reveals a conflicting invariant.
