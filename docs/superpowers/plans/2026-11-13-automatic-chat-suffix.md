# Automatic Chat Suffix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Append the fixed suffix ` *a*` to every active KST4Contest autoanswer and beacon while leaving manual and protocol traffic unchanged.

**Architecture:** A package-private `AutomaticChatMessageFormatter` owns the suffix and performs unconditional appending. The active autoanswer and beacon producers call it explicitly; beacon validation keeps a 120-character payload allowance and validates the resulting 124-character maximum.

**Tech Stack:** Java 21, Kotlin 2.2, Gradle wrapper, JUnit 5, Mockito

**Spec:** `docs/superpowers/specs/2026-11-13-automatic-chat-suffix-design.md`

## Global Constraints

- The exact suffix is ` *a*`, including its leading space.
- `[KST4C Automsg]` remains on autoanswers and remains the loop-prevention marker.
- Suffix insertion is unconditional; duplicate `*a*` markers are allowed.
- Beacon payloads remain limited to 120 characters; complete suffixed beacons are limited to 124 characters.
- Manual messages and internal ON4KST frames must remain byte-for-byte unaffected by this feature.
- No new dependency, setting, persistence field, version bump, staging, commit or publication action.

## Review Focus

- A 120-character beacon payload must be sent as exactly 124 characters after suffix insertion.
- A 121-character beacon payload must still be rejected rather than producing a 125-character frame.
- Text already ending in `*a*` must receive a second suffix without deduplication.
- Autoanswer validation and cooldown accounting must still reject invalid text before consuming cooldown.
- The existing prefix-based ping-pong guard and all manual send paths must remain unchanged.

---

### Task 1: Shared automatic-message formatter and autoanswers

**Files:**
- Create: `core/src/main/java/kst4contest/controller/AutomaticChatMessageFormatter.java`
- Create: `core/src/test/java/kst4contest/controller/AutomaticChatMessageFormatterTest.java`
- Modify: `core/src/main/java/kst4contest/controller/MessageBusManagementThread.java`
- Modify: `core/src/test/java/kst4contest/controller/MessageBusManagementThreadAutoAnswerDecisionTest.java`
- Modify: `core/src/test/java/kst4contest/controller/MessageBusManagementThreadAutoAnswerTest.java`
- Modify: `core/src/test/java/kst4contest/controller/MessageBusManagementThreadChatFrameTest.java`

**Interfaces:**
- Produces: `AutomaticChatMessageFormatter.appendSuffix(String messageText): String`
- Consumes: existing `ApplicationConstants.AUTOANSWER_PREFIX` and `On4KstProtocol.messageText(String)`

- [x] Add failing formatter tests for exact suffix insertion and intentional duplicate suffixes.
- [x] Run `\.\gradlew.bat :core:test --tests "kst4contest.controller.AutomaticChatMessageFormatterTest"` and confirm the tests fail because the formatter is absent.
- [x] Implement the package-private formatter with `SUFFIX = " *a*"` and unconditional appending.
- [x] Update failing autoanswer and version-reply expectations so generic, QRG and `//VER` replies end in ` *a*` while retaining `[KST4C Automsg]`.
- [x] Append before final autoanswer protocol validation and in the `//VER` construction path.
- [x] Run the formatter and three focused autoanswer test classes and confirm zero failures.

### Task 2: Beacon suffix and length boundaries

**Files:**
- Modify: `core/src/main/java/kst4contest/controller/ChatController.java`
- Modify: `core/src/main/java/kst4contest/controller/BeaconTask.java`
- Modify: `core/src/test/java/kst4contest/controller/On4KstProtocolTest.java`
- Modify: `core/src/test/java/kst4contest/controller/BeaconTaskTest.java`

**Interfaces:**
- Consumes: `AutomaticChatMessageFormatter.appendSuffix(String)` from Task 1
- Produces: beacon payload validation at 120 characters and final-message validation at 124 characters

- [x] Add failing tests asserting that each queued beacon ends in ` *a*`, a 120-character payload produces a 124-character message, and a 121-character payload is rejected.
- [x] Run `\.\gradlew.bat :core:test --tests "kst4contest.controller.On4KstProtocolTest" --tests "kst4contest.controller.BeaconTaskTest"` and confirm the new assertions fail.
- [x] Separate the 120-character payload limit from the 124-character final beacon limit in `ChatController`.
- [x] Append the suffix only after macro resolution and payload validation, then validate the final message before queueing.
- [x] Run the focused beacon tests and confirm zero failures.

### Task 3: Targeted documentation and verification

**Files:**
- Modify: `github_docs/de-Konfiguration.md`
- Modify: `github_docs/en-Configuration.md`
- Modify: `github_docs/de-Makros-und-Variablen.md`
- Modify: `github_docs/en-Macros-and-Variables.md`
- Modify: `github_docs/de-Funktionen.md`
- Modify: `github_docs/en-Features.md`
- Modify: `github_docs/de-Changelog.md`
- Modify: `github_docs/en-Changelog.md`
- Modify: `website/src/features/automatic-replies.md`
- Modify: `website/src/features/macros.md`
- Modify: `docs/PROJECT_CONTEXT.md`

**Interfaces:**
- Consumes: implemented suffix and length behaviour from Tasks 1 and 2
- Produces: aligned German/English user documentation and durable current-state context

- [x] Update examples and behavioural text so autoanswers retain `[KST4C Automsg]` and all automated chat messages end in ` *a*`.
- [x] Document the 120-character beacon payload and 124-character transmitted-message limits in German and English.
- [x] Record the fixed suffix rule in the changelogs and `docs/PROJECT_CONTEXT.md` without changing unrelated documentation.
- [x] Run the focused core tests from Tasks 1 and 2, then `\.\gradlew.bat build`.
- [x] Read every Gradle test-result XML summary and report failures, errors and skipped tests; inspect PMD and SpotBugs XML counts produced by the build.
- [x] Run the website test/build commands defined by the current `website/package.json` if the changed website pages participate in that build.
- [x] Run `git diff --check` and review the complete scoped diff for accidental changes; perform no Git publication action.
