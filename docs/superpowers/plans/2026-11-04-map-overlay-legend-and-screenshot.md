# Map Overlay Legend and Screenshot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a responsive, localized map-overlay legend and document the spatial QRB overlay with the supplied screenshot.

**Architecture:** A focused Compose legend consumes the same red antenna and theme-aware blue filter colours as the canvas. `StationMapWindow` derives visibility from the effective antenna and spatial-filter state and places the legend in the existing header `FlowRow`. Documentation receives a new immutable screenshot asset without repurposing the path-analysis image.

**Tech Stack:** Kotlin, Compose Multiplatform, generated properties-based i18n, JUnit 5 Compose UI tests, Markdown, Eleventy

**Spec:** `docs/superpowers/specs/2026-11-04-map-overlay-legend-and-screenshot-design.md`

## Global Constraints

- Preserve map filtering, station selection, antenna heading, rotor control and overlay geometry.
- Reuse existing design tokens and exact overlay colours; add no dependency.
- Keep German and English strings and documentation semantically aligned.
- Keep all changes ungestaged and uncommitted.

## Review Focus

- A map with no drawable overlay must not reserve an empty legend area; Task 1 tests both items absent.
- A QRB-only filter must show the blue item without implying QTF is active; Task 1 tests the generic **Active filters** wording.
- Invalid spatial values must not show the blue item; Task 1 uses `hasRenderableValues` as the gate.
- Narrow windows must wrap rather than clip controls; Task 1 retains the existing `FlowRow` and adds a narrow rendering assertion.
- The screenshot must not replace an image whose caption promises visible path-analysis panels; Task 2 checks all asset references.

---

### Task 1: Localized responsive overlay legend

**Files:**
- Create: `app-desktop/src/main/kotlin/kst4contest/view/compose/map/MapOverlayLegend.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/map/ComposeStationMap.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/map/SpatialFilterOverlay.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/map/StationMapWindow.kt`
- Modify: `app-desktop/src/main/i18n/strings_en.properties`
- Modify: `app-desktop/src/main/i18n/strings_de.properties`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/map/StationMapWindowScreenTest.kt`
- Test: existing i18n-generator and translation tests

**Interfaces:**
- Consumes: antenna-sector visibility, `SpatialFilterOverlayState.isActive`, `SpatialFilterOverlayState.hasRenderableValues`, current theme.
- Produces: `MapOverlayLegend(showAntenna: Boolean, showSpatialFilters: Boolean, darkMode: Boolean)` and shared overlay-colour accessors.

- [ ] **Step 1: Add failing Compose tests**

Assert that the red item appears for a drawable antenna sector, the blue item appears only for an active valid spatial filter, both disappear with neither overlay, and the header remains laid out at a narrow test width.

- [ ] **Step 2: Run the focused screen test and confirm the missing legend fails**

Run: `./gradlew.bat :app-desktop:test --tests kst4contest.view.compose.map.StationMapWindowScreenTest --console=plain`

- [ ] **Step 3: Add localized labels and tooltip text**

Add English `Antenna`, `Active filters` and the two-colour explanation, with equivalent German strings `Antenne`, `Aktive Filter` and its explanation.

- [ ] **Step 4: Implement the legend with shared colours**

Expose the existing antenna colour and theme-aware spatial-filter line colour within the map package. Implement compact colour swatches and labels in `MapOverlayLegend.kt`, and add the component to the existing header `FlowRow` with dynamic visibility.

- [ ] **Step 5: Run focused Compose and i18n tests**

Expected: all selected tests pass in both languages; existing map interactions remain unchanged.

### Task 2: Replace the outdated general map screenshot

**Files:**
- Create: `github_docs/station_map_spatial_filters.png`
- Modify: `github_docs/de-Benutzeroberflaeche.md`
- Modify: `github_docs/en-User-Interface.md`
- Modify: `website/src/features/station-map.md`
- Modify when the durable UI invariant changes: `docs/PROJECT_CONTEXT.md`

**Interfaces:**
- Consumes: supplied 1257×1016 PNG showing `QRB > 500 km` and `QRB ≤ 666 km`.
- Produces: one shared manual/website asset copied by Eleventy to `/manual/assets/`.

- [ ] **Step 1: Copy the supplied PNG without resampling**

Verify its dimensions and file hash after copying.

- [ ] **Step 2: Update targeted DE/EN manual references**

Use captions that describe the selected path, red antenna sector and blue QRB ring. Remove only the obsolete screenshot note attached to the replaced general map image.

- [ ] **Step 3: Update the Station Map website feature page**

Reference `/manual/assets/station_map_spatial_filters.png` and retain the older path-analysis image for manual sections where the analysis panels are explicitly described.

- [ ] **Step 4: Verify asset references and website output**

Run: `npm.cmd test` and `npm.cmd run build` in `website`; verify the generated image path exists.

### Task 3: Final verification

**Files:**
- Review all files from Tasks 1 and 2.

**Interfaces:**
- Consumes: completed legend, translations and screenshot references.
- Produces: verified uncommitted working tree.

- [ ] **Step 1: Run focused map and translation tests**

- [ ] **Step 2: Run `./gradlew.bat build --console=plain` and inspect test XML plus PMD/SpotBugs reports**

- [ ] **Step 3: Run `git diff --check` and inspect `git status --short --branch`**

- [ ] **Step 4: Report native visual checks separately from automated rendering evidence**
