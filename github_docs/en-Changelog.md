# Changelog

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Changelog)

Version history of KST4Contest / PraktiKST.

Published Stable versions and their application packages are available under [GitHub Releases](https://github.com/HamRadioOnline/kst4contest/releases). This page also lists changes from the current development version where they have already been implemented and tested.

---

## v1.50 (in development)

**Operator profiles**

Several operators sharing one computer can now use their own callsigns, locators and layouts without overwriting each other's configuration. This implements [Issue #57](https://github.com/HamRadioOnline/kst4contest/issues/57).

### New

- **New user interface:** the interface moved from JavaFX to Compose Multiplatform. All windows — main window, station map, settings, monitor, update window and dialogs — are rebuilt; operation and layout stay as they were. The program no longer needs a JavaFX runtime.

- **German and English:** the interface is available in German and English. The language is chosen on the **GUI** tab under **Language**; the default is the system language. Switching takes effect at once, without a restart. Callsigns and technical abbreviations such as **Call** and **Act** stay untranslated.

- **Adjustable colours:** on the new **Colours** tab, the colours of the light and the dark design can be adjusted one by one. The window warns when a combination becomes hard to read and offers ways back to the previous state or to the shipped colours. The stylesheet can also be edited by hand from there.

- **Operator profiles:** every profile has its own `preferences.xml`, and therefore its own settings and its own window layout. Profiles are managed on the new **Profiles** tab of the settings window: create, duplicate, rename, delete and activate.

- **Shared or own worked stations:** each profile decides whether it gets its own worked database or uses the common station database. A multi operator station with a single station log shares the worked state; two operators with different callsigns on one computer keep it apart.

- **Choosing a profile at startup:** with only one profile, KST4Contest asks nothing at startup and behaves exactly as before. From two profiles on, a small selection appears with the last used profile preselected; Enter or a double click start immediately. The `--profile=<name>` argument skips the selection.

- **Switching profiles while running:** **File > Switch operator profile...** closes the connection and rebuilds the user interface with the settings of the selected profile, without restarting the program.

- **Minimum distance filter:** next to **Show only QRB [km] <=** the station filter bar has **Show only QRB [km] >**, which hides every station up to the entered distance. With both switched on, the list shows a distance range, for example only stations between 300 and 800 km. Stations without a known distance are hidden while either filter is on, and the station map follows the filter. Like the maximum, the value only lasts for the running session. This implements [Issue #21](https://github.com/HamRadioOnline/kst4contest/issues/21).

- **Windows installer:** next to the portable ZIP package, stable releases now also ship an MSI installer for Windows (`KST4Contest-v<version>-windows-x64.msi`). It installs KST4Contest for the current user account without administrator rights, adds a Start menu entry and a desktop shortcut, and replaces the existing installation on an update. Settings and database stay under `%USERPROFILE%\.praktiKST` and are shared with a portable copy. This implements the first stage of [Issue #61](https://github.com/HamRadioOnline/kst4contest/issues/61).

- **Own site height and highest point of the remote square in the path analysis:** the **Station** tab has two new settings in its path-analysis section. **Own ground height ASL, metres** replaces the elevation-model value at the own locator centre by the height of the actual site; left empty, the elevation model decides as before. **Target station on the highest point of its locator square**, on by default, ends the path at the highest of 25 sampled points of the remote locator square instead of at its centre, because contest stations usually operate from the high ground of their square. The lookup costs one additional request to the elevation service per remote square and session; if it fails, the analysis uses the square centre as before.

- **SO2R with two radios:** the new option **SO2R** on the **TRX sync** tab (off by default) keeps two radios apart. N1MM, DXLog and UCXLog are assigned by `RadioNr`: radio 1 updates MYQRG, radio 2 updates SECONDQRG. For Win-Test, enter one station name per radio. A second PSTRotator (own host and port, on the **Station** tab) adds **Turn ant2** next to **Turn ant1** and a QTF2 field next to QTF. Without SO2R everything stays as before. This implements the first part of [Issue #46](https://github.com/HamRadioOnline/kst4contest/issues/46).

### Changed

- **New home on GitHub:** the project moved from the `praktimarc` account to the [HamRadioOnline](https://github.com/HamRadioOnline) GitHub organisation and now lives at [HamRadioOnline/kst4contest](https://github.com/HamRadioOnline/kst4contest). Old links to the repository, the releases, the issues and the wiki redirect to the new address. The Flatpak repository moved as well and now lives at `https://hamradioonline.github.io/kst4contest/`; existing Flatpak installations keep updating through the redirect of the old address, new setups use the new address from the installation manual. The AUR packages point to the new address too.

- **The terrain profile cache is separate:** computed terrain profiles now live in their own shared file `terrainprofilecache.db` and are stored per owner. Previously the whole cache was dropped whenever the callsign or locator changed, which would have discarded every computed profile on each profile switch.

- **The default login callsign is empty:** if `preferences.xml` has no login callsign, the field now stays empty instead of falling back to a callsign compiled into the program. A newly created profile therefore deliberately starts without credentials.

- **PSTRotator azimuth with one decimal:** the azimuth sent to PSTRotator used to be cut to whole degrees, so a station at 145.7° turned the antenna to 145°. It is now rounded to one decimal, always with a dot as separator.

- **Dark mode per profile:** on the **GUI** tab each operator profile sets under **Design** whether KST4Contest starts light or dark. The entries in the **Windows** menu still switch quickly, but only for the running session. The colour scheme now also covers the update window and the top candidates list.

- **Settings take effect with Apply:** changes in the settings window no longer take effect while they are typed, but with **Apply/Close prefs** or **Save settings**; **Connect** applies them first as well. This also works during a running session. Closing the window, switching the operator profile or exiting with changes pending first asks whether to apply or discard them, or to go back. Colours, language and the login data on the **Station** tab still take effect immediately.

- **Windows open on the main window's screen:** the station map, monitor, settings, update window, top candidates and the notice dialogs used to open on the primary screen, even when the main window was on another one or the primary screen was switched off. They now open centred on the screen the main window is currently on. A window that was moved during the running session reopens there next time, as long as its title bar can still be reached on a connected screen. With the new option **Reopen windows across restarts where they were last moved to** on the **GUI** tab (off by default), the main window and all other windows also keep their position across restarts; a stored position is only used when the very same screen (same place and resolution) is connected again. A window larger than the screen is reduced to its usable area, also when it is moved onto a smaller screen later, for example a 640x480 monitor; the stored window size stays unchanged. This fixes [Issue #59](https://github.com/HamRadioOnline/kst4contest/issues/59).

### Fixed

- **Reconnect from Settings:** after **Disconnect**, **Connect to ...** remained disabled in the open Settings window. The buttons now follow the Connection Manager state, prevent duplicate attempts and become available again after a completed disconnect or failed attempt.

- **Reachability controls restored:** the Compose filter bar again contains **Reachability** with **Auto** and the locally enabled bands, plus **Calc selected**. Only an explicit calculation for the selected station may request online elevation data; table rendering, sorting and band selection use existing results only.

- **More usable on small screens:** the Further Info area is scrollable, empty tables show a clear empty state, filter rows wrap when required and the complete send text is available as a tooltip. Controls and the right-hand window areas are more clearly separated.

- **Diagnostic files grouped and bounded:** new error logs and ON4KST message-history files are created below `.praktiKST/debug`. Existing files in the former location remain unchanged. The error log rotates at 20 MiB and retains at most five generations including the active file.

- **Consistent font size:** on macOS and Linux JavaFX uses a slightly larger default font than on Windows, which cut off labels in fixed-size controls. KST4Contest now uses the same base font size on all systems; nothing changes on Windows.

- **Dark mode after a restart:** the main window always started light, even if dark mode had been active, while the station map stayed dark. All windows now start consistently with the profile's colour scheme.

- **Background resources are released:** the ON4KST supervisor thread, the sked reminder scheduler, the reachability executor, the PSTRotator retry scheduler and the map tile proxy are released when the chat controller is closed. They used to keep running until the program ended.

- **AP column sorts numerically:** the **AP [minutes / pot%]** column in the user list and in the directed messages was sorted by its text, which put `13 (75%)` above `3 (50%)`. It now sorts by the minutes of the first aeroplane and, for equal minutes, by the better potential first; empty and `nil` cells go to the bottom in ascending order. This fixes [Issue #87](https://github.com/HamRadioOnline/kst4contest/issues/87).
- **No automatic cluster spots for worked stations:** the local DX Cluster server generated spots from directional opportunities without checking the worked state. A spot is no longer created when the station has already been worked on the band of the detected frequency; without per-band information the global worked state counts. Manual spots from the map and the test spot in the settings are unchanged. This fixes [Issue #91](https://github.com/HamRadioOnline/kst4contest/issues/91).
- **QSO monitoring no longer picks up foreign entries:** besides the list of monitored callsigns, `preferences.xml` carried a second list `snifferWords` that never appeared in the user interface. On loading, its entries were nevertheless added to QSO monitoring as callsigns. The dead list is removed; a `snifferWords` element in older files is ignored. This fixes [Issue #103](https://github.com/HamRadioOnline/kst4contest/issues/103).
- **No second-category locator any more:** internally there was a setting for a separate locator of the second chat category that appeared neither in the user interface nor in `preferences.xml`, so it always kept its compiled-in default. During login that default was compared with the station's locator and a warning was emitted that the second locator is ignored. ON4KST uses one locator per TCP session anyway; the setting and the warning are removed. This fixes [Issue #104](https://github.com/HamRadioOnline/kst4contest/issues/104).
- **Linux beta packages upgrade to the stable package:** DEB and RPM packages of a beta tag carried the same version number as the following stable package, so `dnf` and `dpkg` refused the upgrade as an equal version. Beta packages now carry a pre-release number (`1.50.0-0.beta.001`) below the stable package (`1.50.0-1`), and the release run aborts when the tag does not match `composePackageVersion`. This fixes [Issue #113](https://github.com/HamRadioOnline/kst4contest/issues/113).

- **UCXLog packets are no longer cut off:** after a short UDP packet, the listener for UCXLog-compatible loggers read every longer following packet only up to the length of the short one, so log entries could arrive incomplete. In addition, disconnecting now closes the listener's socket immediately instead of waiting for the next timeout. UCXLog packets and AirScout replies are now always decoded as UTF-8 instead of the system default, so umlauts in station names and operator callsigns no longer turn into garbage on Windows. This fixes [Issue #115](https://github.com/HamRadioOnline/kst4contest/issues/115).

- **UCXLog/DXLog listener keeps running:** an unwritable backup file for received QSOs or a RadioInfo packet without frequency or mode used to stop the listener for UCXLog-compatible loggers until the next restart, so neither worked stations nor QRG updates arrived any more. The backup entry or the incomplete packet is now skipped, and the listener keeps receiving. Any other unexpected error while processing a packet now drops only that packet and shows a status message instead of stopping the listener.

### Upgrade notes

- Existing installations are **not** modified: `preferences.xml` and `praktiKST.db` stay exactly where they are and become the **Default** profile. No file is moved or copied.
- A profile registry only appears when the second profile is created. Anyone using a single profile will not notice the change.
- Going back to an older KST4Contest release stays possible; it finds its files unchanged.
- Passwords are still stored in clear text in each profile's `preferences.xml`. Profiles separate configuration; they are not an access control mechanism.

---

## v1.43.1 (2026-09-03)

**Corrected version metadata**

v1.43.1 contains the same functional changes as v1.43.0. It corrects the application and build metadata used for display, ON4KST identification and update comparison. Some metadata in the first v1.43.0 package set still identified the build as version 1.42.

### Fixed

- **Consistent semantic version:** The user-visible application version now uses the complete `1.43.1` form. The compact value `1.431` remains only in the deprecated numeric field required for compatibility with older update feeds.

- **Tagged-release update feed:** The website build, version-feed validation and artifact upload now run as actual workflow steps after the GitHub Release has been published. They were previously indented into the release action's artifact list and therefore skipped.

Users of v1.43.0 should install v1.43.1. The functionality is unchanged; the update only corrects the version metadata and release workflow.

The corrected version is available as [Release v1.43.1](https://github.com/HamRadioOnline/kst4contest/releases/tag/v1.43.1).

---

## v1.43.0 (2026-09-03)

**More reliable log synchronisation, persistent layouts and better DX Cluster compatibility**

v1.43 concentrates on reliability around external log data and long-running contest operation. It also adds practical control over table layouts and station grouping on the map.

### Added

- **Optional map clustering:** **Group nearby stations** immediately enables or disables spatial grouping at lower zoom levels. The stored setting changes neither the viewport nor the selected station; aggregation of active variants sharing one base callsign remains independent. This implements [Issue #79](https://github.com/HamRadioOnline/kst4contest/issues/79).

- **Automatic table-layout persistence:** Tables receive useful widths when their first meaningful contents arrive. Manually changed column widths, window sizes and relevant dividers are written to `preferences.xml` after a short delay. The main and monitor windows keep separate DXCluster and QSO-of-the-other layouts.

- **Tooltips for truncated table values:** Normal table cells expose their complete text when the visible column is too narrow, without replacing functional tooltips or clickable links.

- **Simplelogfile creation notice:** When the selected file does not exist, KST4Contest creates it and displays a notice with the file path, a concrete test procedure and a link to the relevant manual section.

### Changed

- **Robust Simplelogfile evaluation:** The selected file is evaluated once per minute and closed after every pass so that the logging application can replace or rotate it. Detected callsigns apply the global Worked state to every active suffix variant of the base callsign. Disabling the function prevents any file access, while read or creation errors no longer terminate the periodic task.

- **Consistent external logger bands:** UCXLog-compatible packets and Win-Test events use one shared band normalisation. Numeric values, metre and centimetre designators and the existing Win-Test IDs now set the same Worked flags and worked grid squares. Values including `2320`, `5760` and `10368` are handled reliably; a missing or unknown band sets only the global Worked state.

- **Compact full-frequency recognition:** Complete frequencies without a decimal separator are accepted across the supported bands, with the final three digits interpreted as the kHz part. Bare three-digit numbers still require recognisable frequency context so that signal reports and unrelated numbers are not treated as QRGs.

- **DXSpider-compatible spot format:** Local DX Cluster spots use a fixed 75-character payload line with the DX callsign in column 27, a 30-character comment field and UTC time in column 71. The format remains stable up to 24 GHz. Overlong DX callsigns are rejected and logged instead of being silently truncated. This resolves [Issue #86](https://github.com/HamRadioOnline/kst4contest/issues/86).

- **Active ON4KST connection probe:** A quiet chat server is checked with an explicit session-wide probe before the connection is treated as dead. Heartbeats and probe frames retain the required CR/LF framing.

- **Reliable private-message age highlighting:** Incoming private messages use the defined green age levels for up to five minutes. Locally sent messages retain their separate style, and empty or reused table rows return to the normal design instead of keeping an obsolete highlight.

### Fixed

- **Worked state after login:** Persisted SQLite Worked information is loaded and applied before each initial ON4KST user list is published. Reconnects, both categories and all active variants of a base callsign therefore start with the correct state. This resolves [Issue #85](https://github.com/HamRadioOnline/kst4contest/issues/85).

- **False disconnect during quiet periods:** A valid ON4KST connection is no longer closed merely because the server currently has no activity lines to send.

### Documentation and packaging

- The German and English manuals were revised against the implementation. A new contest-workflow chapter connects the individual functions into a practical operating sequence, while the sections on dual chat, private-message handling, QRG synchronisation, Simplelogfile evaluation and configuration were clarified.

- The website feature pages now describe band and direction opportunities, the station map, QRG handling, filters, global message views, private-message handling and logger synchronisation in more detail.

- The AUR package definitions were updated for v1.43.0.

The complete v1.43.0 functionality is available in [Release v1.43.0](https://github.com/HamRadioOnline/kst4contest/releases/tag/v1.43.0). Because its embedded version metadata is inconsistent, v1.43.1 is the recommended package set.

---

## v1.42.0 (2026-08-22)

**Shared band context, session-based ON4KST connection and signed macOS packages**

v1.42 brings several previously separate calculations together. Band information, Worked status, NOT-QRV marks, callsign suffixes and frequencies are now used more consistently by the user list, station map, priority calculation and external interfaces.

### Added

- **Visible ON4KST connection state:** A compact `LINK` indicator in the main window displays the actual state of the ON4KST connection. Green means fully authenticated and synchronised, yellow indicates connection setup or synchronisation, and red indicates a lost connection, an error or the delay before the next connection attempt.

- **Shared band-opportunity calculation:** A central `BandOpportunityResolver` evaluates recent QRGs, band designators in station names, active callsign variants, Worked information and NOT-QRV marks. The user list, **New bands**, band-upgrade hint, Priority Score, station map and automatic band selection now use the same basis.

- **Extended band status display:** The band columns now distinguish:
  - `X` for worked on this band,
  - `a` for an offered and unworked band of a completely new callsign,
  - `B+` for an offered and unworked band of a callsign already worked elsewhere, and
  - `o` for a locator square already worked on this band.

  The codes can be combined, for example as `ao` or `B+o`. The additional `a` and `o` indicators can be hidden separately in the GUI settings.

- **50 and 70 MHz support:** Both bands are available in station settings, Worked and NOT-QRV handling, the user list, filters, the internal database, UCXLog processing and the Win-Test listener. Existing databases are extended with the required columns.

- **Global message tabs:** Public messages, ON4KST DX cluster messages and directed messages between other stations can be displayed directly in the main window. The existing separate monitor window remains available and uses the same underlying message stores.

- **Manual QTF input:** The current antenna direction can be changed directly in KST4Contest when PSTRotator is not being used.

- **`MYQTF` message variable:** The current antenna direction can be inserted as a numeric angle in degrees into shortcuts, snippets and other supported message texts.

- **Filter reset:** A dedicated reset button reliably removes the active user-list filter predicates.

- **Map clustering:** Stations close to each other are grouped at lower zoom levels. The selected station and relevant direction opportunities remain individually visible.

- **Hideable path analysis:** The terrain profile and analysis section of the station map can be hidden completely. The selected state is stored and restored at the next application start.

### Changed

- **Session-based ON4KST connection lifecycle:** Each socket, reader, writer, message bus and queue now belongs to an explicitly identified connection session. Delayed threads from an obsolete connection can therefore no longer process data or close its replacement. `ONLINE` is reported only after the login has been accepted and all requested user lists have been received. Connection setup, login and synchronisation use bounded timeouts, while heartbeats, missing inbound traffic, EOF and read or write failures trigger controlled reconnect attempts with backoff where appropriate.

- **Validated ON4KST protocol commands:** Outgoing frames are built centrally and checked for valid categories, locators and prohibited frame delimiters. Because ON4KST maintains one locator per TCP session, the main locator is used for both chat categories and a conflicting secondary configuration is logged instead of sending contradictory commands to the server.

- **More precise QRG recognition:** Complete and relative frequency references continue to be recognised. Bare three-digit numbers are treated as QRGs only when a frequency context is available. Signal reports, band designators and unrelated numbers therefore produce fewer false frequencies.

- **Station-specific frequency context:** For relative QRGs, KST4Contest first uses a band context for the same station which is no more than 30 minutes old. The globally configured fallback band is used only when this context is unavailable.

- **Fallback band dropdown:** The global fallback can only be selected from supported band values. It applies to the complete QRG parser, not merely to DX cluster spots.

- **Consistent QRG formatting:** Frequencies are displayed with at least three decimal places in the user list and message tables.

- **Band-aware AirScout and path calculations:** KST4Contest derives a realistic frequency for each station from its current QRG and known band information. AirScout receives canonical band values. The temporary 430 MHz fallback has been replaced with 432 MHz.

- **Shared propagation-frequency selection:** AirScout, **Calc selected** and the station-map path analysis use the same `PropagationFrequencyResolver`. A band selected explicitly in the Reachability dropdown is respected for manual calculations.

- **Separate callsign variants:** Active chat members are distinguished by their complete callsign, including suffix, and by chat category. `DN9APW`, `DN9APW-2` and similar logins therefore remain separate message targets.

- **Shared base-callsign information:** Worked flags, NOT-QRV information and Priority Scores continue to be evaluated across variants of the same base callsign. Separate message targets no longer result in contradictory Worked information.

- **Corrected Priority Score eligibility:** Stations without a common available band, or with an overriding NOT-QRV mark, are no longer offered as priority candidates. An open band at a station already worked elsewhere can receive a separate Priority Boost.

- **Extended sked creation:** The band is selected from those enabled locally. `SSB` or `CW` is selected explicitly for the Win-Test handover instead of attempting to infer the mode unreliably from the band.

- **More precise Win-Test sked handover:** The QRG must belong to the selected band. Visible KST suffixes are removed for the logging target while portable callsign components are preserved. `ADDSKED` timestamps are generated correctly. A failed handover does not remove the internal KST4Contest sked.

- **Exact sked targets:** The timeline and automatic reminders use the complete visible KST callsign. A sked for `DN9APW-2` is not accidentally sent to another variant of the same base callsign.

- **Reworked beacon and automatic replies:** Both chat categories use one shared timer while retaining separate enable switches and message texts. The minimum permitted interval is one minute and message texts are limited to 120 characters. The stored beacon state is restored from the configuration at startup.

- **Central variable resolution:** Message variables used by beacons, shortcuts, snippets and other generated text are processed by one shared resolver.

- **Improved message tables:** Truncated messages show their complete text in a tooltip. Recognised web addresses can be opened in the system browser.

- **More compact filter bar:** Filters retain their compact arrangement at normal widths and wrap only when the available space is genuinely insufficient. This allows the centre divider to be moved further to the right.

- **DXLog full-log import:** In addition to `contactinfo`, the UCXLog-compatible UDP listener processes `contactreplace`. This allows a complete log broadcast by DXLog.net to be imported.

- **Guarded automatic QRG updates:** `MYQRG` is updated only by an enabled interface which actually supplies valid `RadioInfo` or Win-Test `STATUS` packets. An enabled source which provides no data does not remove the need for a functional check or manual QRG maintenance.

- **Improved version comparison:** Versions are compared semantically so that patch releases and Nightly versions are not misclassified by conversion to a floating-point number.

### Fixed

- **Reliable initial user list:** Invalid or incomplete `UA0` member records are rejected and logged individually without preventing alphabetically following members from being processed. Valid entries are staged per category and published as one complete snapshot when the first corresponding `UE` end marker is received.

- **User list disappearing after login:** ON4KST may send additional `UE` frames for the same category after name, state or other live updates. Repeated end markers are now detected and ignored so that an already populated user list cannot be replaced by an empty snapshot.

- **Failed initial connection and lost sockets:** An unavailable server during startup no longer sends KST4Contest into an endless or busy-wait loop. The user interface remains responsive and further attempts use bounded reconnect backoff. Sockets closed by the server, or connections without inbound traffic for an excessive period, are also detected reliably.

- **Message-bus diagnostics:** Correctly processed ON4KST frames are no longer reported additionally as `Critical, detected unhandled Chatmessage`. Only genuinely unknown frames reach the fallback diagnostic branch.

- **Password in diagnostic output:** The ON4KST password is no longer written in plain text to the console or error log during connection setup.

- **Long-running station-selection failure:** Chat members managed by the message thread have been decoupled from the JavaFX view. Simultaneous data and table updates therefore no longer cause broken selection models or concurrent-modification problems after longer runtimes.

- **No phantom chat members from UM3:** Historical or additional server messages no longer create user-list entries for stations which are not actually logged into the chat.

- **Messages to callsigns with suffixes:** Several active variants of the same base callsign no longer overwrite each other. This resolves [Issue #73](https://github.com/HamRadioOnline/kst4contest/issues/73).

- **DX cluster locators:** The reporting and reported station no longer receive the same locator accidentally. This resolves [Issue #48](https://github.com/HamRadioOnline/kst4contest/issues/48).

- **Worked state in “QSO of the other”:** The Worked columns again use the correct transmitting and receiving station.

- **Missing SECONDAP data:** A missing second aircraft-scatter opportunity no longer produces an invalid text selection and JavaFX exception when the field is edited.

- **Historical callsigns:** Highlighting or selecting a callsign which is no longer present in the active user list no longer causes an exception.

- **Win-Test sked time and callsign handling:** Timestamps, band-to-QRG assignment, KST suffixes and portable callsigns are handled correctly.

- **Filter reset:** All filter predicates are actually removed, and the visible button state again matches the effective filter state.

### Documentation and packaging

- The German and English manuals have been systematically checked against the source code, extended and supplied with current screenshots. The documentation now describes not only the controls, but also how information is derived and where the limits of a result lie.

- The AUR provides `kst4contest-bin`, `kst4contest` and `kst4contest-git` for Arch Linux.

- The download page distinguishes between Stable, Beta and Nightly and offers only packages which actually belong to the corresponding channel.

- Nightly packages are built automatically from the current `main` branch. Stable and Beta releases use predictable asset names for the supported platforms.

- **Signed and notarized macOS packages:** The DMG files for Apple Silicon and Intel are signed with an Apple Developer ID and notarized by Apple, with the notarization ticket stapled into the DMG. The first launch now works by double-clicking, without the previous detour through **Open** in the context menu, and the check also succeeds without an internet connection. This applies to Nightly, Beta and Stable packages alike. The Windows packages remain unsigned.

- **Correct bundle identifier and version on macOS:** The application now identifies itself as `de.x08.KST4Contest` rather than `kst4contest.view`, and carries its actual version number in the bundle. Previously every release reported version `1.0` in Finder's **Get Info** panel. Existing settings are unaffected, because KST4Contest stores its data in `~/.praktiKST/` rather than keying it to the bundle identifier.

### Known limitations

- The active terrain provider uses Open-Meteo with Copernicus GLO-90 data and no more than 100 elevation samples per path.

- The atmospheric K factor is currently fixed at `4/3`.

- An antenna height of 10 metres above ground is assumed for the remote station.

- Aircraft-scatter information from AirScout and the terrain analysis in the station map remain separate assessments.

- More detailed configuration of station height, frequency and K factor is being tracked in [Issue #74](https://github.com/HamRadioOnline/kst4contest/issues/74).

The published version is available as [Release v1.42.0](https://github.com/HamRadioOnline/kst4contest/releases/tag/v1.42.0).

---

## v1.41.1 (2026-07-08)

**Hotfix for text input and focus handling**

### Fixed

- The message input field was unexpectedly cleared after some time or during particular UI updates.
- Filtering or selecting a station could move the input focus back to the send field unintentionally.

The corrected version is available as [Release v1.41.1](https://github.com/HamRadioOnline/kst4contest/releases/tag/v1.41.1).

---
## v1.41.0 (2026-07-01)

**Station map, bounded message stores and screen-aware main window**

### Added

- **Station map:** An interactive OpenStreetMap-based map displays active chat members for which a usable locator is available.

- **Antenna sector and connection line:** The map shows the current local QTF, configured antenna beamwidth, maximum QRB and the path to the selected station.

- **Maidenhead grid:** A locator grid adapted to the current zoom level provides geographical orientation.

- **Terrain profile:** An elevation profile for a selected station can be requested from the Open-Meteo Elevation API. The active source uses Copernicus GLO-90 data and no more than 100 evenly distributed samples.

- **Geometrical path analysis:** The calculation includes line of sight, Earth curvature using `k = 4/3`, radio and terrain horizons, the first Fresnel zone and a rough obstruction estimate.

- **Local map proxy:** Leaflet is bundled with the application. Map tiles are loaded through a local proxy so that no external JavaScript library is required at runtime. An Internet connection is still required for OpenStreetMap tiles and online elevation data.

### Changed

- **Bounded message stores:** The global chat-message list is reduced from more than 30,000 entries to 25,000. The separate DX cluster store is reduced from more than 10,000 entries to 8,000.

- **Screen-aware startup size:** The main window is checked against the visible area of the primary screen at startup and reduced or repositioned when necessary.

- **More compact user interface:** Several UI sections were adapted for smaller displays and movable dividers.

### Scope of the map function

The station map and AirScout may refer to the same remote station, but their calculations remain separate. Aircraft used by AirScout are not included in the terrain profile.

Classes for Copernicus GLO-30, offline DEM imports and additional terrain providers existed in the source tree but were not part of the active calculation chain in v1.41. The elevation source actually used was Open-Meteo based on Copernicus GLO-90.

---


## v1.40 (2026-02-16)
**Major Feature Release: Score System, AP Timeline, Win-Test, PSTRotator**

**New:**
- **Chatmember Score System**: Every chat member is automatically scored based on antenna direction, activity time, message count, active bands, frequencies, sked direction (degrees), and other factors. Top candidates are highlighted in a dedicated list.
- **AP Timeline**: For each minute of possible aircraft arrival, up to 4 highly-scored stations are shown that should be workable. Aircraft with the highest potential are preferred over the fastest arrival. Chat members whose antenna is not pointing towards you are shown transparently.
- **Win-Test Support** (Beta since v1.31, now fully configurable): Log synchronisation, frequency parsing and **sked handover via UDP** fully integrated. Can be enabled/disabled in Preferences.
- **PSTRotator Interface** (Beta since v1.31, now fully configurable): Rotator position updates directly from KST4Contest. Can be enabled/disabled in Preferences.
- **QSO Sniffer**: Messages from configurable callsign lists are automatically forwarded to the PM window.
- **Band Alert for logged stations**: When a station is logged, a hint appears if that station has another active band that you are also QRV on.
- **Sked Reminder ALERT**: A sked alarm with automatic messages in configurable intervals (2+1 / 5+2+1 / 10+5+2+1 minutes before the sked) can be set up for each chat member, plus acoustic and visual notification.
- **Load chat history on startup**: Chat server history is loaded on connect to immediately see active members and recent messages.
- **Skedfail button**: In the FurtherInfo panel, a sked failure can be marked for a chat member, which lowers their priority score.

**Changed:**
- AP notes added to internal DX cluster spots.
- Chat member table scrolling follows the current message selection automatically.
- Generic auto-reply and QRG auto-reply now fire a maximum of once every 45 seconds per callsign (prevents spam and message ping-pong).
- New saveable settings: ServerDNS/Port, PSTRotator interface, Win-Test interface, callsign sniffer, Dark Mode on by default.
- Date column removed from chat table (time only – saves space).

**Fixed:**
- User list now automatically sorted on every new member sign-on.
- Posonpill messages now terminate exactly one client instance (no longer affects all instances or wtKST).
- wtKST: crash on KST4Contest disconnection fixed.
- Multiple issues with callsign suffixes like `/p`, `-2`, etc. fixed throughout.
- `QTFDefault` was not saved correctly → fixed.
- AirScout watchlist (ASWATCHLIST) was not being updated → fixed.
- Dark Mode: QRG fields not displayed at full size → fixed.
- Version number display corrected.

---

## v1.31 (2025-12-13)
**Win-Test + PSTRotator Beta, QSO Sniffer, DNS Hotfix**

**New:**
- **Win-Test support** (Beta, not yet deactivatable): Log synchronisation and frequency parsing.
- **PSTRotator support** (Beta, not yet deactivatable).
- **QSO Sniffer**: Messages from configurable callsigns are forwarded to the PM window.

**Changed:**
- **DNS server changed**: From `www.on4kst.info` to `www.on4kst.org` (hotfix). The DNS server is now configurable in Preferences.

**Fixed:**
- Endless loop in error case freezes the client → fixed.

---

## v1.266 (2025-10-03)
**AirScout Fix for Callsigns with Suffix**

**Fixed:**
- AirScout interface did not work when the login callsign contained a suffix (e.g. `9A1W-2`). AirScout cannot handle this format – only the base callsign without suffix is now passed to AirScout.

*(Bug reported and tested by 9A2HM / Kreso – many thanks!)*

---

## v1.265 (2025-09-28)
**Direction Buttons Stay Coloured When Active**

**Fixed:**
- Direction buttons (N / NE / E etc.) now keep their highlight colour when activated, making the active state immediately visible.

---

## v1.264 (2025-08-02)
**Simplelogfile: Improved Callsign Recognition**

**Fixed:**
- Callsigns like `S53CC`, `S51A`, etc. were not being marked as worked in the SimpleLogFile interpreter → recognition pattern improved.

*(Bug reported by Boris, S53CC – thank you!)*

---

## v1.263 (2025-06-08)
**AirScout Communication and Login Name**

**Changed:**
- AirScout communication fundamentally revised: Only stations with QRB < max-QRB are now sent to AirScout.
- Query interval extended from 12 seconds to **60 seconds**.
- Significantly less computation load and message traffic → more stable AirScout tracking.
- Name of the AS client and AS server is now configurable from the Preferences (was previously hardcoded to "KST" / "AS").

**Fixed:**
- "Track in AirScout" button was very sluggish → greatly improved by new communication logic.
- Name in chat is now saveable (bug fixed).
- Visual corrections before and after login.
- Bug fixed that was reported by 9A2HM (Kreso).

---

## v1.262 (2025-05-21)
**Freeze Fix for Early Message Delivery**

**Fixed:**
- ON4KST sometimes delivers messages before login is complete. This caused errors in the message processing engine → now fixed.

---

## v1.26 (2025-05)
**Multi-Channel Login and Dark Mode**

**New:**
- **Dark Mode**: Toggle via `Window → Use Dark Mode`.
- **Multi-channel login**: Simultaneous login to two chat categories.
- **Opposite station multi-callsign login tagging**: Support for stations with multiple callsigns.

**Changed:**
- Colouring mechanism revised: Colours can now be customised via CSS.

**Fixed:**
- Station tagging completely revised and corrected.

---

## v1.251 (2025-02)
**Bugfix for UDP Broadcast Spot Info**

**Fixed:**
- Problem reading UDP broadcast spot information fixed (reported by Steve Clements – thank you!).
- Station tagging (further improved).

---

## v1.25 (2025-02)
**Wishlist Time**

**New:**
- **New settings tab: Messagehandling**
  - Auto-reply to incoming messages configurable.
  - Automatic reply with own CQ QRG when someone asks for it.
  - Configurable default filter for the userinfo window *(for Gianluca :-) )*.
- **Coloured PM rows**: New private messages appear red and fade every 30 seconds from yellow to white *(idea by IU3OAR, Gianluca)*.

**Fixed:**
- Stations with suffixes like "-2" and "-70" were not being marked as worked → now ignored, station is correctly marked.

---

## v1.24 (2024-11)
**Wishlist + DX Cluster Spots**

**New:**
- Button to open the **QRZ.com profile** of the selected station.
- Button to open the **QRZ-CQ profile** of the selected station.
- **DX Cluster Server integration**: Direction warnings are sent as spots to the logging software (when QRG is known).

*(Coloured PM row feature also added – tnx Gianluca)*

---

## v1.23 (2024-10)
**Built-in DX Cluster Server**

**New:**
- KST4Contest now contains a **built-in DX cluster server**.
- Generates DX cluster spots and sends them to the logging software when a direction warning is triggered and a QRG is known.
- Spotter callsign must differ from the contest callsign (for correct filtering in the logging software).

*(Idea by OM0AAO, Viliam Petrik – thank you!)*

---

## v1.22 (2024-05)
**Usability Improvements and AirScout Button Fix**

**New:**
- New variables (tnx OM0AAO, Viliam Petrik):
  - `MYLOCATORSHORT`
  - `MYQRGSHORT`
  - `QRZNAME`

**Changed:**
- Send field focus: After clicking a callsign in the user list, the send field immediately receives focus – no double-click needed *(tnx Gianluca)*.

**Fixed:**
- Worked-station filter is now live: Worked stations disappear immediately when the filter is activated *(tnx Gianluca)*.
- QRB sorting was lexicographic → now numeric *(tnx Alessandro Murador)*.
- AirScout "Show Path" button: Click now maximises AirScout and correctly shows the path.

---

## v1.21 (2024-04)
**Usability Improvements**

**Changed:**
- Window sizes and divider positions are saved in the configuration file when clicking "Save Settings" and restored on startup.
- Filter section as flowpane → better display on smaller screens.

---

## v1.2 (2024-04)
**Band Selection and NOT-QRV Tags**

**New:**
- **Band selection**: Selectable in Preferences which bands are active. Only buttons and fields for selected bands appear in the UI. Save and restart required.
- **NOT-QRV tags per station and band**: Stations can be marked as "not QRV" for each band. Combinable with the user list filter.
- **QTF arrow**: The "Show path in AS" button now shows an arrow with the QTF of the selected station.

---

## Earlier Versions

### v1.1
First publicly released version. Core features:
- Worked marking via Simplelogfile and UDP
- Sked direction highlighting
- QRG detection
- Text snippets and shortcuts
- AirScout interface (first version)
- Interval beacon
- PM catching for public messages containing your own callsign
- Update notification service

---

## Planned Features

- ~~Lifetime for worked status (automatic reset)~~ ✅ **Implemented in v1.40** (3-day lifetime, no manual reset needed anymore)
- Filtering the "Cluster & QSO of others" window to own QTF
- Further topography-based calculations for direction warnings
