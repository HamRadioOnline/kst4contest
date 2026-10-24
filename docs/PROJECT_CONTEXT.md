# KST4Contest Project Context

Last reviewed: 2026-10-23

This file is the durable technical project context for KST4Contest. It is not a user manual and not a replacement for the changelog. Current code, tests and authoritative external specifications remain the source of truth when this document is stale or ambiguous.

## Purpose

KST4Contest is a Java/Kotlin desktop client for ON4KST chat, with a Compose Multiplatform user interface, focused on VHF/UHF/microwave contest workflows. It combines chat handling with contest-oriented station prioritisation, sked/timeline workflows and integrations with logging, aircraft-scatter, rotor and DX-cluster tooling.

## Current Architecture

- Java 21 desktop application built with Gradle in the modules `core` (domain, free of user-interface technology) and `app-desktop` (the Compose Multiplatform user interface).
- Domain, controller and network code is under `core/src/main/java/kst4contest/`, the user interface under `app-desktop/src/main/java/kst4contest/`. Package names are shared across both modules.
- Responsibilities are separated across controller, service, logic, model, utility and view areas.
- Network/parser/service/controller/UI boundaries should remain explicit.
- Long-running network/message processing must tolerate malformed or incomplete external input without terminating processing threads.
- `ObservableRoster` state in `core` is the canonical store; the `ObservableList` a `TableView` is bound to is a mirror of it, not the store.

## Important Invariants

### Chat identity

- For remote chat participants, the complete visible callsign plus category forms the chat-member identity.
- Full callsign variants can therefore be distinct chat-member identities.
- Base-call normalization is permitted only for explicitly base-call-wide functions.
- Worked status is shared across suffix variants of the same base call.
- Monitoring a variant such as `DN9APW-2` or `DN9APW-70` intentionally monitors the base call `DN9APW`.
- Suffixes must not be globally interpreted as a band/category/frequency.

### Band and availability semantics

- ON4KST categories 2 and 3 are the main operational categories, but unexpected category values must fail safely.
- `NOT-QRV` overrides positive inferred band-availability hints.
- Unknown/missing frequency, QRB, QTF or similar external data must remain unavailable rather than becoming a fabricated zero/default.
- Features that depend on frequency should use the current/actual QRG according to current implemented rules; do not silently revert to a fixed 144 MHz default.
- Complete digit-only frequencies use their final three digits as the kHz part and are accepted only when the resulting MHz value lies within a supported `Band` range. The same full-frequency parser is used for station names and public or directed chat messages. Relative QRG rules and bare three-digit context handling remain separate.

### Threading

Conceptually:

```text
thread-safe canonical domain state in core
        |   kst4contest.observe: ObservableRoster / ObservableValue
        |
        | hand-off via UiDispatcher
        v
mirror on the user-interface thread (FxRosterBinding -> ObservableList)
```

`core` contains no user-interface technology. It observes through `kst4contest.observe` and hands UI-visible work over through `UiDispatcher`, whose implementation is `AwtUiDispatcher` in `app-desktop`. It delivers to the AWT event dispatch thread, which is the thread Compose composes on. The former `JavaFxUiDispatcher` was deleted with the JavaFX lifecycle.

`MessageBusManagementThread` must not directly iterate or mutate a UI-bound collection; it works on snapshots. UI-visible changes cross the controller/UI boundary through the dispatcher.

A roster hands out immutable snapshots and has no incremental change protocol: listeners receive the whole new content. `RosterListBinding` mirrors or derives plain lists for the Compose states and must be disposed when its owner goes away — `shutdownRuntime()` releases all of them, which is what keeps a discarded runtime from surviving an operator profile switch.

A batch of changes belongs in one `SimpleRoster.mutate(...)`, not one call per element: with several thousand ON4KST users the difference is not cosmetic.

## Configuration and Layout Persistence

- Configuration and layout live in the `preferences.xml` of the **active operator profile**; see "Operator Profiles and Per-Profile Persistence" below. The current `preferences.xml` configuration version is 8. Version 8 adds the default-enabled `messageHandling_debugModeToFileEnabled` setting. Missing entries in older files retain the enabled default. Version 6 introduced optional managed leaf-column widths below `guiOptions`, identified by stable table and column IDs. Parent-column widths remain derived from their leaf columns.
- `guiOptions_darkModeActiveByDefault` is a functional, per-profile startup preference. Applying Settings updates it before the complete preference file is written. A temporary Design menu change affects only `guiOptions_darkModeActive` for the running session. Selective layout autosave reparses the current file and changes layout fields only, so it must not overwrite the startup default.
- `GUIstationMapClusteringEnabled` is a layout preference below `guiOptions`. It defaults to `true`, is selectively autosaved and controls only screen-based clustering of nearby map markers. Missing or malformed values retain the enabled default for backward compatibility.
- `GUIstationMapAlwaysOnTop` is a map-only layout preference below `guiOptions`. It defaults to `false`, is applied through the existing AWT window API and is included in full and selective preference writes. Missing or malformed values retain normal window behaviour.
- Stored widths always take precedence, including widths below the calculated minimum. Without a usable entry, a managed column measures its heading and a bounded initial content sample once when the first data arrives; DPI, font scale and typography participate in the Compose measurement key. Automatic widths are capped, remain session-only and are not recalculated for every chat event.
- Main-window and separate-monitor DXCluster/QSO tables use distinct layout IDs even though they share the underlying message stores.
- Window sizes and positions, relevant divider positions and managed column widths are selectively autosaved after a short debounce. A pending write is flushed during application shutdown.
- Window positions are persisted only as an **opt-in** (`guiOptions_rememberWindowPositions`, GUI tab, off by default; issue #59). Every secondary window and every `ComposeAlert`/profile-picker dialog opens centred on the screen the main window is on (`WindowPlacement`, fed by `WindowPlacementOnScreen`). A window the operator moved during the running session reopens there (shifted wholly into that screen's usable area), held in its `ComposeWindowHost` and tracked by `MoveTracker`; the remembered spot is dropped in favour of the main window's screen when its title bar cannot be reached on any connected screen. With the opt-in, operator moves (never window-manager nudges) are also stored per window id (the host's thread name) as `windowPosition` elements with the screen bounds they were made on, written by the layout autosave; a stored position is restored only when a screen with exactly those bounds is connected (`WindowPlacement.restore`), for the main window too. Order for secondary windows: session move, then stored position, then main window's screen. Opening sizes are clamped to the target screen's usable area without writing the clamped size back; an open window that rests on a screen too small for it is cut down (`WindowPlacement.fitted`), and enforced minimum sizes are capped to the smallest connected screen. Without a stored position the main window keeps the platform's placement; before it exists (update window, profile picker at startup) the platform decides as before. `GUIstationMapStagePositionXY` is an unused JavaFX leftover and must not be revived for this.
- Selective layout writes update the XML already on disk, preserve unknown XML nodes and must not persist unconfirmed functional settings from the current UI. **Save Settings** remains the full settings writer and includes the current layout.
- Full and selective writes are synchronized and replace `preferences.xml` atomically. Missing, unknown or malformed width entries do not prevent loading and fall back to initial sizing.
- Older configuration files require no migration. Older KST4Contest versions can ignore the additional elements; a complete rewrite by such a version may discard column widths without invalidating the remaining file.

## Operator Profiles and Per-Profile Persistence

- One operator profile owns one `preferences.xml`, one worked-station database, its own pair of stylesheets and its own interface language (all since release 1.50). Everything else under `~/.praktiKST/` stays global: audio files, DEM and terrain packages, the error log and the version-info feed.
- The **root profile** is the historic flat installation: `preferences.xml` and `praktiKST.db` directly below the application directory. It is never moved, and it always uses the common station database, because that database is the installation's own.
- Additional profiles live under `profiles/<profileId>/`. `profileId` is a stable, file-system-safe slug assigned once; renaming a profile changes only its display name and never moves a directory.
- The registry `profiles.xml` is created lazily. An installation that has only the flat layout gets no registry and no `profiles/` directory; the root profile is synthesised in memory. Startup with no or exactly one profile therefore asks nothing and writes nothing, and a downgrade to an older release is a no-op.
- The registry stores `profileId`, `displayName`, `rootProfile` and `sharedWorkedDatabase`, never a path. Both file names are derived in `OperatorProfilePaths` alone, so a stored path cannot drift apart from the flag that produced it.
- `sharedWorkedDatabase` resolves to a path, not to a schema change: a sharing profile points at the flat `praktiKST.db`, an owning profile at its own file. There is no owner column, and no SQL statement in `DBController` knows about profiles.
- A profile database is created **empty**. The bundled `/praktiKST.db` resource carries several thousand foreign callsigns and `user_version = 0`; seeding an additional profile from it would show a new operator foreign data and trigger the full callsign-normalization rebuild. Only the root installation is seeded from the resource. `preferences.xml` of a new profile *is* seeded from `/praktiKSTpreferences.xml`, because its defaults are what a first installation gets.
- Worked-state semantics are unchanged and now apply per database file: normalized base callsign as key, worked state shared across suffix variants, three-day expiry, manual reset.
- `stn_loginCallSign` and `stn_loginCallSignRaw` default to empty. The preferences reader treats an empty element as "not set" and falls back to the field default, so a non-empty default would make a profile created without credentials come up carrying a compiled-in callsign.
- Passwords remain plaintext per profile. Profiles separate configuration; they are explicitly not an access-control boundary. This is documented in both manuals.

### Runtime profile switching

- A switch tears the current runtime down through `Kst4ContestApplication.shutdownRuntime()` and builds a **new** `Kst4ContestApplication` instance. Reusing the instance is not possible: many controls are inline-initialised instance fields, so a second `start()` would re-parent mounted nodes and register every listener twice. The approach is only sound because the class holds no mutable static state.
- All exits run through `ApplicationRuntimeLauncher.exitApplication()`, including the main window's close request: a profile switch closes every window of the old runtime before the new one exists, so a window close must never be what ends the process. The former `Platform.setImplicitExit(false)` and `Application.stop()` are gone with JavaFX; a shutdown hook on `shutdownRuntime()` covers a SIGTERM instead, and the guard is atomic because the hook runs on its own thread.
- `shutdownRuntime()` is idempotent and must release everything that outlives a disconnect: the ON4KST supervisor thread, the sked reminder scheduler, the reachability executor, the PSTRotator retry scheduler, the station-list coalescing trigger, both view timers and every Compose window. Several of these were real leaks before; they only became visible once a second runtime could exist.
- `ApplicationConstants.sessionRuntimeUniqueId` must not be regenerated during a switch, so UDP readers started earlier still recognise their own poison pill.
- The layout autosave is flushed and then cancelled before a switch, so a pending debounced write cannot land after the profile changed.

## External Interfaces

Treat current implementation/tests and authoritative upstream documentation as source of truth before modifying any interface.

Known integration areas include:

- ON4KST chat;
- AirScout;
- UCXLog / DXLog UDP XML (`contactinfo`, `contactreplace`);
- Win-Test UDP;
- PSTRotator UDP;
- DXCluster;
- local SQLite persistence.

CR/LF framing, XML framing, ports/transports, callsign normalization and frequency formatting are protocol behaviour and must not be changed as incidental cleanup.

### Local DX Cluster output

- Local spots use a fixed 75-character, DXSpider-compatible payload line followed by two BEL characters and CRLF.
- The DX callsign begins in column 27 and occupies up to 12 characters. The 30-character comment begins in column 40, and the five-character UTC time begins in column 71.
- Spotter and frequency padding is calculated dynamically so frequencies from 50 MHz through 24 GHz do not shift the following fields.
- Comments are padded or truncated to exactly 30 characters. Automatic AirScout comments retain the locator first and use the compact form `JO51HK AP 1m/100%;4m/75%`.
- A DX callsign longer than 12 characters is rejected and logged rather than truncated.
- Trigger conditions, QRG recognition and normalisation, login, keepalive, multi-client delivery and the local-only trust boundary remain separate from line formatting.
- Automatic spots are suppressed for stations already worked on the band of the sender's known frequency (`MessageBusManagementThread.isAlreadyWorkedForDxClusterSpot`); when only the global Worked flag exists without any per-band flag (Simplelogfile source), the global flag counts, an unknown band keeps the previous behaviour, and manual spots from the map window and the settings test spot are not affected (#91).

### Logging and Worked-state persistence

- The Simplelogfile interpreter reads the selected text file after connection startup and then once per minute using a fixed built-in callsign pattern.
- Simplelogfile callsigns are normalized to base callsigns and set only the global Worked state for every active variant. They do not create per-band Worked or grid-square state.
- Simplelogfile-derived Worked state is not persisted in SQLite. The selected file is the durable source and is read again in each application session.
- The interpreter only adds positive runtime marks. It does not remove existing marks during the current session and does not reset automatically when a new contest starts. A database reset does not modify the file; callsigns contained in it are marked as worked again during the next periodic evaluation.
- A missing selected file is created. Read, path and creation failures are contained so the periodic timer remains alive; successful creation triggers a one-time, non-blocking UI notice with the exact path and setup/contest checks.
- Network-derived and manually assigned Worked, NOT-QRV and worked-grid state continues to use SQLite with its established lifetime and reset behaviour.
- Each completed initial ON4KST user list loads one SQLite Worked/NOT-QRV snapshot. `ChatController` applies that snapshot by normalized base callsign to every new category and suffix variant before the completed category is published. The same event-driven path runs again after a reconnect; startup synchronization does not depend on a fixed-delay timer.
- Automatic QRG updates require both an enabled source and valid incoming `RadioInfo` or Win-Test `STATUS` data. Merely enabling a source does not provide or validate a current QRG.
- UCXLog-compatible QSO packets and Win-Test `ADDQSO` packets are converted into one validated external-QSO state. Logger-specific numeric, metre and centimetre values and Win-Test band IDs are normalised once; the resolved band is then the sole source for per-band Worked and worked-grid state.
- A missing or unknown logger band sets only the global Worked state. Worked-grid state requires both a recognised project band and a valid locator; no band or locator is inferred. Packets without a usable callsign are discarded without terminating the listener.
- External logger threads do not read or mutate the user-list mirror. `ChatController` applies global and per-band Worked state to every active variant of the base callsign on the user-interface thread, through `UiDispatcher`, before evaluating a band-upgrade notice.
- Explicit **Mark as worked** context actions construct the same validated `ExternalLoggedQso` state and use the same controller/database path as logger `contactinfo` handling. The operation is additive and idempotent; it does not provide an Unwork action or create a parallel UI-only store.
- The established Win-Test handling for 24, 47 and 76 GHz remains unchanged. Their Worked flags are retained, while only frequencies represented by the project `Band` model can create worked-grid state.

### Win-Test log recovery

- Win-Test only broadcasts new QSOs. A listener started later never sees the earlier ones, so KST4Contest pulls them with the Win-Test `IHAVE` / `NEEDQSO` protocol, ported from the wtKST `WtLogSync` implementation. The answers are ordinary `ADDQSO` packets and reuse the established Worked path; the recovery itself never touches database or UI.
- The recovery is not configurable. It is bound to the existing Win-Test network listener, runs automatically once a station is detected through `HELLO` or `STATUS`, and stays active so gaps caused by lost broadcasts are refetched.
- The station-name filter remains a QRG-sync setting. Log recovery covers every station in the network, because each band station of a multi-station setup keeps its own log and contributes per-band Worked state.
- A QSO is identified by `StationName@LogUniqueID` plus the Win-Test QSO number. That identity deduplicates the answers of overlapping requests, so a recovered log is written once instead of once per resend.
- Win-Test framing must be resolved on the raw datagram bytes: the checksum byte is not valid ASCII and would otherwise corrupt the trailing fields, which carry the log ID of `ADDQSO` and the run-length inventory of `IHAVE`. A broken checksum discards `IHAVE` only; the established handling of the other message types is unchanged and still does not verify checksums.
- Win-Test answers broadcasts only; an identical unicast request to the same station stays unanswered (verified against Win-Test). Outgoing Win-Test packets therefore derive their broadcast address from the source address of received Win-Test packets, with the configured address as fallback for a station behind a router. A configured address pointing at a non-existent network raises no send error, so it silently disabled both log recovery and SKED handover before. Only genuine Win-Test message types update that address; internal control packets such as the poison pill must not redirect outgoing traffic.
- `IHAVE` inventories are run-length encoded and may be split, so the announced first row is honoured instead of assuming that an inventory starts at QSO number one. A station that never sends a usable inventory is served by a blind block fallback starting at QSO number one.

### Terrain data providers

- The active terrain profile provider is Open-Meteo using Copernicus GLO-90 data.
- The terrain profile cache lives in its own global database `~/.praktiKST/terrainprofilecache.db`. It is deliberately not part of an operator profile: terrain profiles are pure geometry derived from two locators and a sample count, and at a multi operator station both operators share one location, so a per-profile copy would only double the traffic against the terrain service.
- Cached entries are separated by owner identity through the primary key (`owner_callsign_raw` + `owner_locator6`). Earlier versions stored a single owner identity in a meta table and dropped the whole cache whenever the configured callsign or locator changed; with several operator profiles that would discard every computed profile on each switch. The old `TerrainProfileCache*` tables inside `praktiKST.db` are left in place and are still readable by older releases; the new file starts empty and refills itself.
- The highest point of a remote locator square (path analysis, Station tab switch, on by default) is found by `LocatorHighestPointResolver`: a 5 x 5 grid of cell centres, one batched request against the same Open-Meteo provider and rate limiter as the profile, cached in memory per locator for the session and not persisted. A failed lookup is retried after five minutes; until then the analysis falls back to the square centre and that result is not kept in the reachability cache. The lookup runs on the reachability thread only; request building peeks the cache without blocking. The optional own ground height ASL replaces the first profile sample's elevation, a resolved highest point the last one.
- `OfflineDemImportService` only prepares a local directory and copies manually selected Copernicus GLO-30 GeoTIFF files into it. Importing files does not activate an offline provider or change the active calculation chain.
- The station filter bar owns an operator-selected Reachability band override. `Auto` uses the shared `ReachabilityService` resolver; an explicit locally enabled band overrides it. Selecting a band, rendering or sorting the station table and applying filters may read cached results only. Online terrain work for this control remains behind the explicit **Calc selected** action for the current station.

### ON4KST session and authentication

- One KST4Contest connection authenticates one ON4KST TCP session with one local login callsign and one password.
- The TCP session uses one common locator for both categories; the locator is not part of authentication.
- The primary category is part of the initial login. A distinct second category is added to the same session through ON4KST Single Sign-on; it must not create a second TCP connection or local login.
- **Name in Chat** is a visible category-specific name field, not a login callsign or message destination. Private messages to the local station are addressed to the local login callsign.
- The visible **Name in Chat** field, message context, QRG and beacon configuration remain category-specific.
- The Settings connection buttons observe the `On4KstConnectionState` mirrored in `MainWindowSurroundings` by `ConnectionStateFeed`. Connect is available only in `DISCONNECTED`; Disconnect is available while the Connection Manager reports an active attempt or session. `CONNECTING`, `STOPPING` and other intermediate states therefore cannot start a duplicate connection, while a completed disconnect or failed attempt immediately enables a new one.

### ON4KST session liveness

- Only after the session is fully authenticated and synchronized, more than 90 seconds without inbound server data trigger one client-side `CK|\r\n` liveness probe. The trailing pipe matches the framing used by the server for its own `CK|\r\n` probe. The probe state belongs to the TCP session, so a two-category session still sends only one probe per idle phase.
- The live ON4KST test returned `OK|\r\n` for `CK|\r\n`. KST4Contest treats both that confirmed frame and the originally specified `OK\r\n` form as internal responses: it records inbound activity, confirms the outstanding probe and does not publish the response as chat content. Any other inbound server frame also confirms reachability and starts a new idle phase.
- A `CK` initiated by the server remains a separate protocol case and receives the established empty CRLF response. It must not be confused with the client-side probe.
- If no inbound frame arrives by about 210 seconds, the existing reconnect flow remains responsible for replacing the session.
- Liveness diagnostics contain the session id, opcode and timing only. They must not include credentials, complete server frames or normal chat messages.

### ON4KST replay history and diagnostic logging

- `MessageHistoryRecorder` is the single non-JavaFX owner of raw ON4KST session-history file I/O. Reader and writer threads only report complete frames to it.
- New diagnostic files live below `~/.praktiKST/debug/`. Existing files directly below `~/.praktiKST` are deliberately not migrated, renamed or removed.
- The current program session is stored at `~/.praktiKST/debug/Messagehistory.raw` as one `UTC<TAB>RX|TX<TAB>RAW_FRAME` event per line. Timestamps use UTC with milliseconds; TCP CR/LF terminators are not part of the raw frame.
- RX is recorded immediately after `BufferedReader.readLine()`, before liveness handling or internal filtering. TX is recorded only after the final frame has been written and flushed successfully.
- A non-empty current file is archived once when recording is first initialised for a new program session. Reconnects do not rotate it. Disabling closes the writer; re-enabling in the same program session appends to the same current file.
- `LOGINC`/`LOGIN` password fields are replaced by `<REDACTED>` through protocol-aware redaction. Other frames are not reconstructed or normalised for history output.
- History I/O failures are warning-level diagnostics and must never terminate reader, writer or message-processing threads or deliberately disconnect ON4KST.
- The default-enabled Messagehandling setting also controls additional application `FINE`/`INFO` records in `debug/kst4contest-errors.log`. `WARNING`/`SEVERE` records and exceptions remain file-logged regardless of that setting. File-log timestamps use UTC with milliseconds. The active file rotates at 20 MiB and at most five files including the active file are retained.
- The raw history is intended for future replay/simulation tooling and can contain personal chat and station data even though credentials are redacted.

## User Workflow / UI Invariants

- Contest operating speed and low-friction interaction are primary goals.
- Incidental code changes must not unexpectedly change selection, focus, sorting, tab state, map zoom or prefilled text.
- Map reset clears the selected target without changing zoom unless explicitly redesigned.
- **Grouping** changes only spatial clustering of the existing Compose map markers. It must not reload tiles or station data, request a new controller snapshot, or change zoom, viewport or selection.
- Base-callsign aggregation into one geographical marker happens before screen-based clustering. Disabling clustering displays each resulting positionable map station individually but never splits active variants of the same normalised base callsign into separate geographical markers.
- A hidden Station Map is not continuously refreshed. Relevant background changes mark it dirty; the next `showWindow()` performs one current full refresh before normal visible-window updates resume.
- Maidenhead grid construction depends only on viewport bounds, zoom and WebView size. Ordinary station-data refreshes must not rebuild or serialise the grid when those inputs are unchanged.
- Panning updates viewport and grid state through `notifyViewport()` without rebuilding station markers or screen-based clusters. Zoom changes, station-data changes and clustering toggles still rebuild the marker layer.
- Debug file logging reports Station Map refreshes only when total FX-thread refresh time reaches 100 ms. The diagnostic separates snapshot and render time and includes visible-member and station counts.
- Station selection preserves the established `/cq callsign` prefill behaviour.
- `StationSelectionState` is the canonical selection shared by the station table and Further Info panel. Its identity is the complete callsign plus chat category. Explicit actions carry a `StationSelectionSource` and increment a separate scroll-request revision even when the same station is selected again; roster reconciliation replaces the selected instance without moving the table.
- Recent-join state is transient domain state keyed by complete callsign plus chat category. Only live `UA5`/`UA2` frames can create an entry; the `UA0`/`UE` initial or reconnect snapshot and `UM3` profile updates never do. Duplicate live updates do not renew the timestamp, a leave removes the entry, and a genuine rejoin begins a new ten-minute interval. The local login callsign is excluded. The core `ObservableRoster` is mirrored through `UiDispatcher`; New-joined actions reuse the canonical station selection and scroll request.
- The station table starts with a case-insensitive ascending callsign sort. A user-selected sort remains authoritative across roster and filter updates. An externally selected filtered station is pinned into the table only; `filteredRows` remains the exact filter result used by the map.
- PM, Public, QSO-of-the-other, priority, timeline and individual map-marker actions route through the application-level station selector. Directed messages target the sender, except that an outgoing local PM targets its receiver. Public and QSO-of-the-other rows target their sender in the message category. Direct clicks and context clicks inside the station table select without issuing an automatic scroll request; selections originating in the map, messages, priority, New joined or other station controls issue one even when the identity was already selected.
- The selected-station message radio mode is independent of station identity and always retains the complete callsign plus chat category. `PM_TO_ME` is the private conversation with the local station plus public traffic from the selected station carrying the structured leading `(LOCALCALL)` address, `PM_TO_OTHER` is private traffic sent by the selected station to other participants, `PUBLIC` is traffic sent by it to `ALL`, and `NOTHING` applies no additional type/receiver restriction. An arbitrary callsign mention in prose is not a structured public address.
- ON4KST chat parsing records `messageDirectedToCommunity` from the receiver field and `messageDirectedToMe` from either a genuine private receiver or the exact leading public address. The selected-station Compose message mirror is initialized with the main window state, before the first selection callback, and roster changes recompute and push the filtered rows automatically.
- The map's **Reset filters** action uses the same `StationFilterState.reset()` path as the main filter bar and does not change sort, selection, zoom, viewport or the display-only Grid-color toggle. Individual raw-callsign markers retain an already selected category variant; otherwise they use the configured Main category. Spatial clusters zoom and never choose an arbitrary member.
- The user list and map share one station context-menu implementation. A secondary click selects a unique target through `StationSelectionState`; base-call markers or screen clusters containing several full-callsign/category identities first require an explicit target choice. Message snippets reuse the existing send-field action, and the context sked editor owns an isolated draft with the target fixed at opening time.
- Map markers draw only the display callsign; bands, `B+`, locator and QRG details remain in tooltips. When Grid color is enabled, the canvas draws a black outline and a 30 percent black fill overlay for four-character squares from `WorkedGrossFieldCache.snapshotWorkedGrossFieldsAny()`, independently of visible station markers. The cache is rebuilt from SQLite at startup and is the same any-band state used by **Only new grids**.
- The AP timeline renders the producer's four candidate lanes in order (two in-beam, two global priority) plus a separate sked lane. Its relative 0–30 minute axis normally uses five-minute ticks and increases the interval when labels would overlap. The vertical message splitter applies pane-specific minimums for the timeline, shortcut row and send row, so older stored divider positions cannot clip these fixed-height controls; stored positions remain the unconstrained baseline.
- Sending without an explicitly selected send category preserves the established Main-category fallback unless explicitly changed.
- The lower selected-station panel contains a vertical two-pane `SplitterPane`: the upper pane owns the independently scrollable message table, while the lower pane scrolls NOT-QRV and Sked above a pinned lookup group. The lower pane reserves 160 dp before stored divider positions are applied, so normal small-window layouts retain direct access to lookup actions; impossible smaller layouts keep the action groups scrollable. `GUIselectedCallSignSplitPane_dividerposition` persists this divider through the existing profile-scoped layout autosave. Station or message updates never reset it. Empty `DataTableView` instances retain their header and border and show an explicit empty-state label. Header metadata and message filters use width-aware flow layouts; the NOT-QRV and sked groups switch between a row and a column from the panel's actual Compose constraint, without duplicating their domain state.
- The selected-station sked picker takes its options exclusively from `MainWindowState.activeBands`, which originates in `BandOpportunityResolver.getEnabledStationBands(...)`. Station QRG and name evidence chooses the initial band but must not empty or narrow the operator's locally enabled choices. The detected-band header value is already localized and labelled by its producer, so the Compose panel renders it unchanged.
- Compact desktop buttons, text fields, choices and checkbox rows share `Density.CONTROL_HEIGHT` at 26 dp and `compactControlTextStyle()`. Material touch-target and component defaults must not reintroduce different visible heights. Compass filter buttons keep a common 28 dp width with reduced horizontal content padding; the send-row QRG fields reserve 96 dp and QTF fields 56 dp so normal contest values remain visible. The station filter bar groups related controls on shared theme surfaces while its outer flow rows continue to wrap from the station pane's actual width.
- `actualQTF` is initialised once from the active profile's `stn_qtfDefault` after preferences are read. Later PSTRotator feedback or manual runtime changes replace that value without changing the stored default, and a repeated preferences read must not overwrite a live heading. Manual QTF-heading transfer is enabled by default and persisted as the profile-scoped opt-out marker `stn_qtfFilterDirectionAsAntennaHeadingOptedOut`; absence of that marker migrates root and operator profiles to enabled, while subsequent writes preserve an explicit opt-out. The effective state is `!stn_pstRotatorEnabled && !optedOut`: PSTRotator 1 temporarily suspends transfer without erasing the user's choice, and PSTRotator 2 remains independent. Explicit QTF field entries and compass-button actions route directly to `actualQTF`, even when the QTF predicate is off; filter reset, checkbox changes and restored filter state never take that route. The coupling is one-way and never calls the rotor command path.

## Autoanswer / Beacon

- Automated-message loops must be prevented.
- Cooldown/minimum-interval rules must be preserved.
- A reply rejected before a complete valid TX item is queued must not consume cooldown.
- Current implementation/tests define the exact message markers and timer details.

## Build / Verification

- Use the repository Gradle wrapper (`.\gradlew.bat` on Windows).
- The project uses Java 21, Kotlin 2.2 and Compose Multiplatform 1.8.x at this context snapshot. JavaFX has been removed; no `org.openjfx` dependency remains.
- JUnit 5/Mockito, PMD and SpotBugs are part of the verification environment. JUnit and Mockito are test-only dependencies; nothing of them reaches the shipped app image.
- Test classes live under `src/test` only. Until #62 a few JUnit classes sat under `src/main`, compiled on every build and were never executed; that is the historical reason the two modules once declared JUnit and Mockito as `implementation` dependencies.
- A test failure fails the build. Only PMD and SpotBugs run with `ignoreFailures`, so their findings do not. Still read the test-result XML under `*/build/test-results/test/` rather than inferring a pass from the exit code.
- Static analysis is a report, not a gate, and since #110 both tools leave a file behind: PMD under `<module>/build/reports/pmd/main.{xml,html}`, SpotBugs under `<module>/build/reports/spotbugs/main.{xml,html}` (the plugin names the report after the source set, not the task). The PR workflow runs `pmdMain spotbugsMain`, counts `<violation` and `<BugInstance` elements per module into the job summary and uploads both report directories as the `static-analysis` artifact for 14 days. `i18n-generator` is Kotlin only, so PMD has no Java source there and writes no report.
- Coverage is measured with JaCoCo as a report only: no `jacocoTestCoverageVerification`, no threshold (rationale in `docs/superpowers/specs/2026-10-11-test-coverage-design.md`). `./gradlew jacocoTestReport` writes a per-module report under `*/build/reports/jacoco/test/` (XML and HTML); `./gradlew jacocoAggregatedReport` merges `core` and `app-desktop` into `build/reports/jacoco/aggregated/jacoco.xml` and `.../aggregated/html/`. Both run the tests first when needed. `i18n-generator` still gets its per-module report but is left out of the aggregated one: it is build tooling and says nothing about the shipped application. The PR workflow prints line and branch coverage per module and aggregated into the job summary and uploads the aggregated HTML as the `coverage-report` artifact for 14 days. The nightly and tagged-release workflows run the test job first; no packaging job starts before it has passed.

## Documentation Surfaces

- German and English manuals under `github_docs/`.
- Repository README.
- Eleventy-based project website under `website/`.
- Changelog/release communication.
- This technical project context under `docs/PROJECT_CONTEXT.md`.

After implementation use targeted documentation-impact checks. Do not run a complete manual audit unless explicitly requested, release preparation is broad, or targeted checks indicate systematic drift.

## Website / Deployment Relationship

The repository contains the KST4Contest website under `website/`, published separately from the desktop application build.

- Historical GHz-Tagung papers are published as German and English PDF assets below `website/src/assets/papers/` and explained on `/background/`. Their editable ODT sources remain outside the public Eleventy input tree and must never be copied into the generated site.
- The home-page news teaser is derived from the date-sorted Eleventy `news` tag collection. It deliberately renders nothing when that collection is empty; it must not be pinned to a release version.

Current website/deployment scripts and update-feed behaviour must be inspected before changes; do not rely on historical assumptions.

- `APPLICATION_CURRENT_VERSION` is the user-visible semantic version and must use the dotted `major.minor.patch` form. `APPLICATION_CURRENTVERSIONNUMBER` is retained only for older feeds and encodes patch releases by appending the patch digit, for example `1.43.1` as `1.431`.
- The tagged-release workflow creates the GitHub Release before building the website update feed. This ordering is required because `versionInfo.js` reads the published release body through the GitHub Releases API.
- Stable tags ship a Windows MSI installer (`KST4Contest-<tag>-windows-x64.msi`, Compose `packageMsi`, jpackage with WiX 3) next to the unchanged portable ZIP (issue #61). It installs per user without UAC and carries the fixed `upgradeUuid` in `app-desktop/build.gradle.kts`, which must never change because Windows Installer uses it to replace the installed version. Nightlies and beta tags stay ZIP-only: an MSI always carries `composePackageVersion`, so a nightly or beta MSI would have the same version as the following stable one, and jpackage's upgrade table only accepts strictly higher versions, so Windows Installer would refuse the stable over an installed beta. Installed and portable copies share `~/.praktiKST`. The website download data (`downloads.js`, `beta.js`) lists the MSI only when the release actually has that asset.
- Beta DEB and RPM packages keep `composePackageVersion` but carry the Fedora pre-release number `0.<suffix>` as package release (`beta-v1.50.0-beta-001` -> `1.50.0-0.beta.001`, `beta-v1.50.0-rc02` -> `1.50.0-0.rc02`, stable `v1.50.0` -> `1.50.0-1`), passed as `-PlinuxAppRelease` by the tagged-release workflow, because rpm and dpkg refuse an equal version and a beta user could otherwise not move to the stable (issue #113); nightlies and local builds keep release 1, and the test job fails a tag whose `X.Y.Z` differs from `composePackageVersion`.
- After publication, the workflow tests and builds the website, validates the expected Stable version, attaches `kst4ContestVersionInfo.xml` to the release and uploads the complete website build as a workflow artifact.

### Server-side website statistics

The detailed operator runbook is
[`website/ops/analytics/README.md`](../website/ops/analytics/README.md). This
section records only the durable architecture and operational boundaries.

#### Architecture and data flow

- Production runs on Ubuntu Server 24.04 with Nginx 1.24, Node.js 18.19.1 and GoAccess 1.8.1. GoAccess has GeoIP2/MMDB and OpenSSL support but no Zlib support; missing Zlib is intentional for this operating model.
- Nginx writes eligible `GET` page requests to a reduced, tab-separated website analytics log in addition to the normal operational log. It records server name, client IP, time, method, normalized path without query string, protocol, status, bytes and user agent; referrer and remote user are omitted.
- Nginx removes assets, downloads, status/monitoring paths, update feed, counter requests and known crawler clients from the website log. GoAccess supplies the second crawler-classification layer, anonymises IP addresses at the configured level and performs Country lookup locally through GeoLite2-Country.
- A second reduced Nginx log contains only case-sensitive `GET` requests with final status `200` for the exact path `/kst4ContestVersionInfo.xml`. It deliberately retains browser and bot requests and stays excluded from website page views, visits and the public visitor counter.
- The hourly systemd timer starts the Node.js generator. Each registered site and both reduced streams are processed from the optional uncompressed `.1` rotation followed by the current log; older `.gz` files are not part of regular processing. Logrotate retains raw logs for 14 days and requires `delaycompress` for this handover.
- GoAccess maintains one private report/database per registered site and one combined report/database. The generator then updates its separate daily visit-counter state, publishes `visitor-count.json` for enabled sites, and produces durable private website/update-information day and year views. Private reports are served with Basic Auth from `stats.hamradioonline.de`; the public home page requests only its same-origin counter file.
- The manual production rollout was completed and verified on 2026-09-14. The hourly timer is active. Durable website metrics are covered from 2026-09-13, and separate live update-information metrics are covered from 2026-09-14. Earlier dates remain unknown until a verified historical import extends either series.

#### Durable invariants and persistence

- GoAccess defines a visit by IP address, date and user agent. The public value is therefore an approximate visit total, not a count of unique people, and remains separate from page views.
- The public contract contains only `schemaVersion`, `visits`, `since` and `updatedAt`. The browser uses no analytics cookie, local storage or third-party request; failed or invalid responses leave the counter hidden.
- The site registry at `/etc/hamradioonline-analytics/sites.json` owns stable site IDs, hostnames, current uncompressed log paths, activation/coverage dates and output targets. `stats.hamradioonline.de` is never registered as an analytics site. Existing state cannot be continued with a changed hostname or activation/coverage identity without an explicit migration or reset decision.
- GoAccess databases below `/var/lib/hamradioonline-analytics/db` retain rolling 395-day detail through `--persist` and `--restore`. `/var/lib/hamradioonline-analytics/public-counter-state.json` retains daily visit values from activation onward and remains the durable business source for the lifetime public total. Its schema and public meaning are unchanged.
- `/var/lib/hamradioonline-analytics/private-metrics-state.json` permanently retains website page-view totals, absolute Country values and normalized path totals per day, plus update-information request totals and absolute Country values per day. It contains no per-request IP address, raw user agent or timestamp. Update hours and conservative client groups are retained only for the latest 14 `Europe/Berlin` calendar days. There is no permanent path-by-Country request matrix.
- Update-information requests are not program starts or user counts. Browsers and bots can fetch the XML, and existing application versions do not send a reliable KST4Contest-specific user agent; a Java user agent identifies only an unknown Java application.
- GoAccess 1.8.1 exposes Country data as `geolocation`; combined jobs explicitly enable `VIRTUAL_HOSTS` and require `vhosts`. Individual site jobs do not enable that panel. `geo_location` and `virtual_hosts` are invalid interface names.
- Private daily values are upserted rather than added. Regular hourly reprocessing and historical imports are repeatable; overlapping aggregates must be equal or monotonically more complete. The generator fails on inconsistent overlaps or when path sums differ from the existing GoAccess page-request definition.
- The explicit historical-import mode accepts verified Nginx combined or reduced TSV logs and inclusive coverage dates. Node.js reads `.gz` files without relying on Zlib support in GoAccess. Missing earlier history remains unknown rather than becoming zero, and the first covered date is stored and displayed.
- Generator inputs and staged GoAccess outputs are validated before publication. Files are replaced atomically, temporary normalized per-day inputs use private run directories and are removed after the run, dry-runs use temporary state without the production lock, and productive runs use `/run/hamradioonline-analytics/generator.lock`. Exit codes are 1 for runtime/publication errors, 2 for configuration errors and 3 when the production lock cannot be acquired.

#### Components, roles and external services

- `hamradio-analytics` is the locked, non-login service account which reads configuration, Country MMDB and reduced logs and writes service state. Nginx runs as `www-data`, writes both reduced logs and can read only reports and the public counter, not private databases or either state file. `stats-reader` is a local Nginx Basic Auth login whose password file remains outside Git.
- Root-managed program/configuration lives below `/opt/hamradioonline-analytics` and `/etc/hamradioonline-analytics`; service state lives below `/var/lib/hamradioonline-analytics`. Report/public directories are shared read-only with Nginx through group ownership; no ACL dependency exists.
- MaxMind is used only by `geoipupdate` to download GeoLite2-Country. Visitor lookups are local; Account ID and License Key remain in the protected server configuration. Let's Encrypt supplies TLS for the statistics vhost through the permanent ACME webroot and `/snap/bin/certbot`. GitHub supplies website source and deployment, not analytics processing.
- The website deploy checks out `origin/main`, builds the static site and publishes it to the Nginx document root. It does not install or overwrite analytics programs/configuration, systemd, Nginx, Logrotate, GeoIP, Basic Auth or Certbot state; those remain separate manual server operations.
- Passwords, hashes, deployment credentials, MaxMind credentials, ACME account data and private TLS keys must remain outside the repository and ordinary diagnostic output.

#### Open operational work

- The server has no comprehensive automated backup plan yet. A future server-wide plan must include both non-regenerable state files, GoAccess databases and protected operational configuration without extending the published 14-day raw-log retention.
- Historical production data has not been imported. The actual regular Nginx log format and complete available coverage must be verified before selecting an import range; missing history must not be represented as zero.
- The first real rotation after installing the XML log still requires an explicit operational check of both `.1` handovers, ownership/readability, subsequent generator success and absence of duplicate counting.
- The combined GoAccess 1.8.1 report remains unchanged. A finer Country-level graphic under `/combined/` is deliberately deferred; no decision has been made between a later GoAccess upgrade and a custom report integration.

## Important Decisions and Workarounds

- SO2R (issue #46): `RadioSlot` FIRST/SECOND; `RadioSlotRouter` assigns RadioInfo by `RadioNr` and Win-Test STATUS by station name, everything to FIRST while `trxSynch_so2rEnabled` is off. Radio 2 writes SECONDQRG and `actualQTF2`. Rotator logic lives in `RotatorControl`, one per radio; the compatibility retry is unchanged. MO2R and multi-PC are separate projects and need their own design.
- **Settings take effect with Apply, not while they are edited** (release 1.50,
  `docs/superpowers/specs/2026-10-16-settings-apply-drafts-design.md`): the settings window
  stages every edit in a `SettingsDraft` that the tab states read through; Apply/Close, Save
  and Connect apply it; the X, a profile switch and the exit ask about what is pending first
  (`SettingsWindow.settleThen`, Back cancels). Fields still holding text -- typed but not
  committed, or invalid -- are checked before anything is applied, so a refusal leaves Discard
  possible. Apply writes everything first and then runs each effect key once, in first-changed
  order, so a listener or server whose switch and port both changed restarts once, on the new
  port; a failing write or effect is reported and the rest still runs.
  Immediate on purpose: login credentials (locked while connected), language, colours, and the
  actions (profiles, worked database, test spot, file chooser, stylesheet export). A new
  setting goes through `draft.setting(...)` and gets a row in `DraftedSettingsTableTest`; one
  that writes `ChatPreferences` directly bypasses Apply. Corrections of stored values that are
  no edit (fallback band, Win-Test broadcast detection) run once in `repairStoredSettings`
  before the window is built, never inside a tab state.
- **The interface language has four kinds of text, and the rule is the addressee** (release
  1.50): interface text is translated; protocol text (the pipe-framed opcodes in
  `On4KstProtocol`, and the `/`-prefixed chat commands) and amateur shorthand (the
  `ChatPreferences` defaults, snippets, shortcuts, and QRA/QRG/QTF/LOS/Fresnel in labels) are
  not; and diagnostic text -- the `println` calls in `core` -- is not either, because a German
  stack trace is worse for whoever reads the bug report. Ask who reads it, not what it looks
  like.
- **`core` produces no display text.** It returns a key or an enum and `app-desktop` turns it
  into words. That is the language half of "core stays toolkit-free", and it also means no
  German string can reach a protocol or a log line by construction. Two violations predate the
  rule and are named in the stage 9 spec: `PathPropagationAssessment` and `PriorityCalculator`.
- **Incompleteness is allowed in a translation, incorrectness is not.** A missing key warns and
  falls back to English at runtime, so a partial contributed language can be submitted; an
  orphan key, a placeholder mismatch, a duplicate key or an unreadable file fail the build. The
  argument count always comes from the English base.
- **Anything built outside a composition needs its text read inside one.** Window titles, tab
  titles and column headers are all built once at startup -- the columns from Java -- so each
  holds a function rather than a string. A stored string freezes at whatever was true when the
  window opened, which is how the main window's title said "KST4Contest (Compose)" for a whole
  session and the tab strip stayed English while its contents switched.
- **A column's `id` is never translated.** It is what the remembered column widths and order
  are keyed on; translating it would read an operator's stored layout under names that no
  longer match.
- **The menu row's menus are a hand-rolled popup, not Material3's `DropdownMenu`** (release
  1.50): M3's position provider refuses any position closer than 48 dp to the window's top
  edge (`MenuVerticalMargin`), the margin is applied after the `offset` parameter and the
  provider is not replaceable — so every menu of a bar at the top of the window opened with a
  visible gap below it. `MenuRowPopupPositionProvider` plus a plain `Popup`/`Surface` in
  `MenuDefaults` styling opens flush under the bar; outside-click and Escape dismissal come
  with a focusable skiko popup for free, only the expand animation is gone. The combo boxes
  keep the stock `DropdownMenu`: they never sit at the window edge.

- **The palette has three sources, and the later one wins role by role** (release 1.50): the
  shipped stylesheet from the classpath, then the profile's own stylesheet file if present,
  then the six role overrides stored in that profile's preferences. `resolvePalette` is a pure
  function and reports the provenance of every role, which the colours tab shows per field --
  with two sources able to decide the same colour, that is the only thing that keeps "why is
  this colour what it is" answerable, and it is not optional.
- **A role is a selector and a property, not a property alone.** Three of the six are stated
  outside `.root` (text on `.label`, the separator on `.separator *.line`), and
  `-fx-background-color` alone appears a dozen times in the evening sheet. `JavaFxStylesheet.read`
  returns a complete palette with Modena fallbacks and is right for layer 1;
  `declaredRoles` returns only what a sheet actually names and is the only thing correct for
  layer 2. Measured: the shipped daylight sheet states no colour at all, the evening sheet five.
- **The root profile's layer 2 always exists, and that is not a bug.** `copyResourceIfRequired`
  writes `KST4ContestDefaultDay.css` and `…Evening.css` at every startup, and those are exactly
  the paths `paletteRelativeFileName(rootProfile, …)` resolves to — reading them is what fixes
  the long-standing defect that the copies were written and never read. Consequence to keep in
  mind: on the default installation most roles report *from file*, not *shipped*, with the same
  values. Anything keyed on provenance rather than on a colour's value will therefore misfire
  there; `textAccent` did, and repainted the evening accent on upgrade.
- **A file stating `-fx-base` and no `-fx-background` states the derived window surface too**,
  because that is what JavaFX does with it. The settings tab keeps the two roles independent —
  the spec separates them so an operator can set one without the other — but a hand-edited
  sheet follows the stylesheet language, or `-fx-base: #303030` would leave every window the
  shipped light grey.
- **The contrast guard's pairs come from `Theme.kt`'s Material mapping, not from the role
  names.** `background` and `surface` are both `windowBackground` with `labelTextFill` on them,
  and `primary` is `textAccent`, not `accent`; `base` reaches the screen only through the menu
  strip and the selection tint. Measuring text against `base` alone let an operator black out
  every window and hear nothing.
- **`Kst4ContestTheme` must read the palette from the `PaletteStore`'s state, never from
  `remember`.** That read is what makes a colour change recompose every open window;
  `remember(darkMode)` held the palette until someone toggled day/evening, so a change reached
  nothing. `ThemeReadsPaletteStoreTest` pins it.
- **The contrast guard is relative to the shipped sheet, not an absolute WCAG floor.** Measured:
  the shipped sheets have three pairs under 4.5 themselves (daylight accent 2.86, evening accent
  2.26, evening text-in-field about 2.9), so an absolute rule would warn on an untouched client
  and teach the operator to ignore warnings. It reports a pair only when it is both under the
  floor and worse than shipped, so the shipped state and any improvement never warn.
- **The "reset to the shipped colours" button never takes the active palette.** It receives the
  shipped palette as a required parameter and reads no composition local, so it has no path to
  the resolved one. An operator who sets text and surface to the same black cannot see any
  other control, this one included, unless it refuses their palette.

- Preserve full callsign/category identity while applying base-call normalisation only to specifically defined features.
- Keep canonical worker-thread domain state separate from the row lists the user interface draws from.
- The station map is a Compose canvas that fetches its own tiles. The JavaFX WebView, the bundled Leaflet resources, the CSS-3D-transform workaround and the `TileProxyServer` were removed with JavaFX; offline DEM, terrain packages and the profile cache are unaffected and must stay that way.
- Deliberate test data, comments and Easter eggs are preserved unless explicitly changed.
- Chat frames (issue #105, second slice): CH (live) and CR (history) go through one `MessageBusManagementThread.processChatFrame(fields, live)`. `classifyRoute` and `decideChatFrameEffects` are pure; history frames never play sounds, answer or spot, and keep their frequency extraction after publishing. Known oddities kept on purpose: TO_ME is case-sensitive, FROM_ME case-insensitive; the CR SERVER sender has no AirScout info.

## Planned Technical Direction

These are planned directions, not necessarily implemented behaviour:

- improve propagation/path modelling using higher-resolution terrain data, including Copernicus GLO-30;
- increase terrain/path sampling through a dedicated service/API;
- support high-precision station locations (e.g. extended Maidenhead locators or direct GPS coordinates) while preserving compatible standard display;
- improve terrain/Fresnel/diffraction/refraction modelling for VHF/UHF/microwave use;
- evaluate/implement richer tropospheric/scatter models;
- continue integration of aircraft-scatter and propagation data into reachability/contest workflows.

Before implementing planned items, re-check current decisions and obtain a fresh concept approval.

## Known Limitations / Maintenance Notes

- Historical project context is useful but may be stale; current code/tests win.
- External service/API behaviour must be verified against current upstream documentation when uncertain.
- Screenshots in manuals/website may need targeted replacement after visible UI changes; never fabricate them.
- `station_map_path_analysis.png` and `station_map_reset.png` predate the current header controls, callsign-only markers, shared station context menu and Worked-square overlay. Replace them with current screenshots when suitable source images are available; the website reuses `station_map_path_analysis.png` through `/manual/assets/`.

## Recent Significant Changes

### 2026-10-17 – Repository moved to the HamRadioOnline organisation

- The GitHub repository moved from `praktimarc/kst4contest` to `HamRadioOnline/kst4contest`. GitHub redirects the old repository, Pages, API and git URLs, so published links and release assets from before the move keep working.
- The Flatpak OSTree repository (branch `flatpak-repo`, served through GitHub Pages) is reached at `https://hamradioonline.github.io/kst4contest/`; its `kst4contest.flatpakrepo` and flatpakref metadata carry the new URL. Existing Flatpak remotes added under `praktimarc.github.io` keep updating through the Pages redirect.
- `website/scripts/validate-version-info.js` accepts release URLs from both owners because update feeds published before the move still carry the old one.

### 2026-08-25 – Durable project context introduced

- Added a persistent technical context layer so future agents/developers can understand architecture, invariants and cross-project dependencies without replaying chat history.
- Documentation maintenance uses targeted impact assessment rather than a full audit after every implementation.

## Related Projects / Integration Points

### KST4Contest website

- Source is maintained inside this repository under `website/`.
- User-facing feature/configuration changes may require a targeted website check.

### hamradioonline.de

- Serves as the broader amateur-radio umbrella site/infrastructure context.
- KST4Contest content/download/manual links and related knowledge content may intersect with the broader site strategy.

### Webserver / hosting infrastructure

- KST4Contest website and other hamradioonline services depend on the hosting environment.
- Operational details should be maintained in a private infrastructure context rather than duplicated into this public project context when sensitive.

### Planned propagation / terrain service

- Intended to provide richer terrain/propagation data (including higher-resolution Copernicus GLO-30-based processing) to KST4Contest and potentially related hamradioonline tooling.
- Interface contracts must be documented on both provider and consumer sides when they become concrete.
