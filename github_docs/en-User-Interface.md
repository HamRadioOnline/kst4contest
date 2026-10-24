# User Interface

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Benutzeroberflaeche)

## Connecting to the Chat

Before connecting for the first time, configure the local login callsign and password, the locator shared by the TCP session, and the primary chat category in the settings window. If a second category is required, enable and select it there. KST4Contest signs in once and adds the second category through Single Sign-on within the same TCP session.

The connection can be started in two ways:

- **Connect to …** in the settings window applies the values currently entered there and starts the connection.
- **File → Connect to …** uses the settings already applied in KST4Contest.

Use **Save Settings** if changed values should also be available after the next programme start.

An active connection can be terminated using **File → Disconnect** or **Disconnect** in the settings window. **Exit + disconnect** terminates the connection and then closes the programme.

If an established connection is lost unexpectedly, KST4Contest waits for a limited period and then attempts a controlled reconnect to ON4KST. A failed initial connection attempt no longer blocks the user interface.

The [`LINK` indicator](#status-bar-and-indicators) in the main window shows whether only the TCP connection exists or whether login and synchronisation have actually been completed.

---


## Main Window Overview

The main window consists of several areas:

### Status Bar and Indicators

The status bar is located at the top of the main window next to the menu.

![Status bar with ON4KST connection indicator](connection_status_indicator.png)

The permanently visible `LINK` indicator shows the actual state of the ON4KST connection:

| Indicator | Meaning |
|---|---|
| green `LINK` | Login and synchronisation of all configured chat categories have been completed |
| yellow `LINK…` | Connection, login, user-list synchronisation or controlled shutdown is in progress |
| red `LINK!` | No connection exists, or KST4Contest is waiting before an automatic reconnect |

The tooltip contains the internal connection state and a more detailed description of the current step. The indicator is not a button.

KST4Contest reports `ONLINE` only after login has been confirmed and the user lists of all configured categories have been received. The send field and **TX** remain disabled while the connection is still being established or resynchronised.

Additional indicators appear temporarily after certain events:

- `SKED` indicates that a sked reminder is due. The text contains the complete target callsign and the remaining time.
- `BAND+` appears after a log entry if at least one common, locally enabled and unworked band has been detected for the worked station.

Both indicators flash for approximately twelve seconds and then disappear. Their tooltip contains the complete message or derivation. Neither indicator is clickable.


### PM Window (top left)

The PM window shows private messages addressed to the local login callsign and the corresponding outgoing replies.

Messages not sent by the local station are also shown when their text contains the configured local login callsign, ignoring letter case. This applies to public messages addressed to `ALL` and to directed messages between other chat participants. The informal description **“gossip detection”** is particularly apt for the latter case.

The actual receiver, message text, chat category and routing remain unchanged. The message merely gains an additional representation in the PM window.

If [QSO Monitoring](en-Features#qso-sniffer-from-v131) is enabled, it additionally shows captured messages involving the monitored base callsigns. These entries receive a `Sniffed:` prefix containing the complete visible sender and receiver callsigns.

New rows not sent by the local station pass through six green age levels and return to the normal table colour after five minutes. Messages sent by the local station retain their separate highlight. The colour only indicates message age; it does not change content or routing.

Selecting an incoming row prepares a reply to the sender. For an outgoing message from the local station, the original receiver is restored as the message target instead. Caught and monitored rows do not trigger PM audio output.

Age levels: [Coloured PM Rows](en-Features#coloured-pm-rows-from-v125). Recognition and limitations: [PM Catching](en-Features#pm-catching-from-v11).

### User List (Chat Members)

The central table of all currently active chat users. Columns (depending on configuration):

| Column | Content |
|---|---|
| Callsign | Station callsign |
| Name | Name and additional information from the chat name field |
| QRA | Maidenhead locator |
| QRB | Distance in km |
| QTF | Direction in degrees |
| QRG | Most recent frequency detected in a chat message |
| Tropo | Result of the band-specific tropo or path assessment |
| Score | Current, numerically sortable priority score of the normalised base callsign |
| Act | Minutes since the most recent activity |
| AP | AirScout aircraft data, when enabled |
| worked | Per-band Worked, band-opportunity and grid-square status, plus `wkdany` |
| NOT QRV @ | Bands on which the station has manually been marked not QRV |
| Category | Chat category of this entry |

The QRG column shows the frequency most recently detected for a station. Missing trailing zeros are added for display purposes, so `144.21`, for example, is shown as `144.210`. If KST4Contest detects frequencies on several bands in succession, the column shows the latest match. The internal band information may still contain several current bands for that station.

Relative frequency information is first combined with a band context from the same sender which is no more than 30 minutes old. Only if no such context exists does KST4Contest use the global fallback band. Detection rules, examples and limitations: [QRG Detection](en-Features#qrg-detection).


### Worked, band and grid-square status

The subcolumns under **worked** use compact codes because several enabled bands leave little room for full descriptions. `X` marks a callsign worked on that band. `a` and `B+` identify an offered band which has not yet been worked. An appended `o` means that the four-character grid square has already been worked on this band.

The **wkdany** subcolumn is band-independent: `x` means that the callsign has been worked, `o` means that the grid square has been worked on any band, and `xo` means both.

Each status cell has a tooltip containing the legend and the state derived for that station. For the complete calculation, including NOT-QRV precedence, see [Worked Callsigns, New Bands and New Grid Squares](en-Features#worked-callsigns-new-bands-and-new-grid-squares).

![Band-specific Worked status and worked grid squares](worked_band_status.png)

On its first build, the user list is sorted alphabetically by complete callsign, ignoring letter case. New stations are inserted at the corresponding position. A sort subsequently selected through a column header is retained across new stations, data updates and filter changes. QRB is sorted numerically.

Station selection is shared by the user list, PM, Public and QSO-of-the-other tables, priority display, AP timeline and map. A selection made in another view scrolls the user list to the chat member's current position and highlights the row. If a filter hides that station, it is temporarily added to the user list; the stored filters and exactly filtered map view remain unchanged. Callsign and chat category jointly identify the station.

A callsign shown in bold marks a station which actually joined the chat during the current session. The emphasis remains visible for ten minutes. The initial user list and a complete resynchronisation after a reconnect do not create these markers. The Day design uses a contrasting dark text colour, and the Evening design uses an appropriately light colour.

A callsign displayed in green and bold indicates a directional opportunity derived from a directed message. The marker applies to the sender of that message and remains visible for no more than five minutes. Derivation and limitations: [Directional Opportunities from Directed Messages](en-Features#directional-opportunities-from-directed-messages).


### Send Field

The send field contains the prepared text for the next outgoing message.

When an operator deliberately selects a station in the user list using the mouse or keyboard, KST4Contest prepares a directed message:

```text
/cq CALLSIGN
```

The complete visible callsign, including any suffix, and the chat category of the selected station are retained. A target such as `9A0BB-70` is not shortened to `9A0BB`.

A background refresh, changed sorting order or filter update must not overwrite message text which has already been edited. Only an actual station selection by the operator prepares the `/cq` recipient again.

- **TX** or `Enter` sends the prepared text.
- `Esc` clears the send field.
- The send field and **TX** remain disabled until KST4Contest is fully connected to ON4KST.

Shortcuts, snippets and variables are described under [Macros and Variables](en-Macros-and-Variables).

### MYQRG and SECONDQRG Fields

The two QRG fields contain the local frequencies for the primary and secondary chat categories.

`MYQRG` can be updated by an enabled TRX synchronisation interface or entered manually when no automatic QRG source is active. `SECONDQRG` remains independent and contains the frequency used for the second category.

Selecting a station from the second chat does not change the meaning of these values: `MYQRG` continues to belong to the primary category and `SECONDQRG` to the secondary category.

Further details: [TRX Sync Settings](en-Configuration#trx-sync-settings).

### MYQTF Field

The MYQTF field shows the current antenna direction as a numerical angle in degrees.

If PSTRotator is enabled, the value is received automatically and the field cannot be edited manually. Without active rotator synchronisation, the antenna direction can be entered directly. The changed value is applied when the field loses focus.

The value affects, among other things:

- QTF filtering,
- the antenna-sector display on the station map,
- priority-score calculation,
- the AP timeline, and
- the `MYQTF` variable.

---

## Table Widths and Truncated Cell Values

When useful data first becomes available, KST4Contest sizes the columns of the user list, message views, DXCluster and QSO tables, and Worked database once from their headings and existing contents. Stored widths take precedence. The initial widths of **Name**, **AP** and **NOT QRV @** are capped so that one long value cannot displace the rest of the table. **Message** and similar free-text columns remain flexible and do not follow the longest message.

Manually changed column widths are saved automatically and restored at the next start. Later messages or station updates do not overwrite that choice.

If a normal text value does not fit in its cell, a tooltip shows the complete value. It appears only when the displayed text is actually truncated. Functional tooltips for QRA, Worked, band and similar states remain available. If such a cell is also truncated, the tooltip contains both the full value and the functional explanation.

### Message Text and Links

KST4Contest deliberately displays message text on a single line. This keeps a larger number of entries visible when chat activity is high. The disadvantage is obvious: if the **Message** column is narrow, not every message fits completely into its cell.

If the message text is wider than the visible cell, moving the mouse over that **Message** cell displays the complete content in a tooltip. No additional full-text tooltip is shown if the message already fits into the column.

Web addresses beginning with `http://`, `https://` or `www.` are displayed as links inside the message text. Clicking a link opens it in the operating system’s default browser. Other protocols are not treated as links.

![Truncated message text with full-text tooltip and clickable link](message_tooltip_and_link.png)

This avoids having to move the divider merely to read an individual long message. The divider can, of course, still be adjusted if a permanently wider message area is required.

---

## Filters and Reachability Controls

The filter bar is located above the chat-member table. Filters can be combined; a station remains visible only if it satisfies every active condition.

Subtly bordered surfaces group **QRB**, **QTF** including direction buttons, **Hide**, **Show** and **Reachability** by function. **Find** and **Reset filters** remain separate controls. The grouping changes presentation only; filter state, reset behaviour and map coupling remain shared.

![Wrapped filter bar in a narrow chat-member view](filter_bar_wrapped.png)

> **Note:** This screenshot still shows the previous filter-bar layout. It will be replaced after the new compact, wrapping layout has completed its visual check.

### Station Filters

| Control | Effect |
|---|---|
| **Show only QTF** | Shows only stations inside the selected antenna direction and configured beamwidth |
| **Show only QRB [km] <=** | Limits the list to the entered maximum distance |
| **Show only QRB [km] >** | Hides stations up to the entered minimum distance; together with the maximum it shows a distance range |
| **Find** | Filters by a complete or partial callsign |
| **wkd** | Hides base callsigns already worked on at least one supported band |
| individual band buttons | Hide stations already worked on that band or marked NOT QRV there |
| **Inactive stations** | Hides stations whose latest chat activity was more than 20 minutes ago |
| **Only new grids** | Shows only stations in four-character grid squares not yet worked on any band |
| **New bands** | Shows stations with at least one detected, locally enabled and unworked band opportunity |
| **Tropo >=0dB** | Shows stations with a calculated non-negative SSB margin |
| **AS next 5m** | Shows stations with a current AirScout window or one expected within the next five minutes |

By default, a valid direct entry in the QTF group or a compass-button click also updates the current local antenna heading. This explicit transfer works whether **Show only QTF** is on or off. Resetting or merely disabling the filter does not change the antenna heading. The profile-specific option **Use QTF filter direction as antenna heading** under **Station → Manual antenna heading** can disable this behaviour. PSTRotator 1 takes temporary priority without changing that saved choice.

For **New bands**, KST4Contest evaluates current QRGs, band information in the name field and active callsign variants together. Manual NOT-QRV marks take precedence.

The **Tropo >=0dB** filter removes only stations for which a completed calculation returned a negative margin. Stations with pending or failed calculations remain visible. Otherwise, a missing API result would incorrectly be treated as proof that the path is unsuitable.

### Grid Color

**Grid color** is not a filter. It only changes the presentation of the QRA cell and marks four-character grid squares which have already been worked.

The station remains visible regardless of this colour marker. **Reset filters** therefore does not disable **Grid color**.

### Reachability and Calc Selected

The **Reachability** dropdown selects the band used by the Tropo column, the Tropo filter and an explicitly requested path calculation.

- **Auto** derives the band from the station’s current QRG, band information in its name field and the supported chat category.
- An explicitly selected band overrides this automatic choice for the Reachability calculation.

Changing the dropdown does not start a calculation for the entire user list. With an online elevation-data source, that would be unnecessarily slow and multiply the number of external API requests.

**Calc selected** calculates only the currently selected station, using either the explicitly selected or automatically derived band. The result is then used by the Tropo column and the associated views.

### Resetting the Filters

**Reset filters** clears:

- the QTF filter,
- the QRB filter,
- the callsign search field,
- all Worked and band filters,
- **Inactive stations**,
- **Only new grids**,
- **New bands**,
- **Tropo >=0dB**, and
- **AS next 5m**.

The internal filter predicates are explicitly cleared as well. Resetting only the visible toggle buttons would not be sufficient.

The following settings are retained:

- **Grid color**, because it is a display option, and
- the **Reachability** selection, because it selects the calculation band rather than directly filtering the table.

### Behaviour in a Narrow View

The filter bar has no fixed width. QTF, Worked and Reachability controls initially use the available space in their respective rows.

When the middle divider is moved to the right and the chat-member area becomes narrower, controls wrap only when their actual required width no longer fits. Widening the area causes them to rearrange immediately.

In plain terms: the filters determine the table contents, but no longer enforce the minimum width of the entire right-hand side.

---

## Station Info Panel (Further Info)

The lower-right panel combines the messages associated with the selected station. This includes public messages, private messages to the local station and, where visible in the chat, private messages addressed to other stations.

The selected filter is a separate dimension which controls the message type shown for the selected station. Under **Settings → GUI**, its initial value can be set to:

- **pm to me**: private messages between the selected and local station, plus public messages from the selected station whose structured leading address names the local login callsign,
- **pm to other**: private messages sent by the selected station to other participants,
- **Public msgs**: public messages sent by the selected station to `ALL`,
- **nothing**: all messages involving the selected station in its chat category, without an additional message-type or receiver restriction.

A local callsign mentioned only somewhere in ordinary message text does not make a public line a **pm to me** message. A public line qualifies only when the ON4KST message begins with its explicit parenthesised address.

This setting changes the Further Info display only. Messages are neither discarded nor removed from the other message tables, and the filter can be changed at any time for the currently selected station.

A horizontal divider inside Further Info divides the available height between the message table and station actions. Moving it immediately makes the table taller or shorter. NOT-QRV and Sked use their own vertical scroll region in the lower area, while **Look up and show** remains pinned below it. The divider position is stored with the active root or operator profile's layout and is clamped to a usable value in a smaller window. Empty tables retain their column structure and show **No entries**.

The station header combines the callsign, chat category, QTF, QRB, last activity and detected bands in a compact flow. It does not repeat the locator; the locator remains available in the other station views. In a narrow panel, the metadata wraps onto additional lines without clipping. **Show messages** sits directly in front of the four message filters and flows with them.

The **NOT QRV** and **Sked alert** areas sit next to each other while the actual right-panel width is sufficient. Moving the splitter further to the right stacks the two bordered groups. Controls can also wrap inside their group, without introducing a horizontal scroll area.

The lower part of the panel contains per-band **Not QRV** marks for the selected station. Compact toggle buttons are shown for the bands enabled in the local station settings. **all** sets or removes the mark for every supported band, including bands which are not currently visible.

The change immediately affects the **NOT QRV @** column, band opportunities and the corresponding filters. It is stored in the internal database and restored after a restart.

![Per-band NOT-QRV marks in the Further Info panel](not_qrv_controls.png)

> **Screenshot note:** This image still shows the previous checkbox controls. It will be replaced after manual acceptance of the compact toggle buttons.

The current **Priority score** of the selected station is displayed in the same section.

**Sked fail** marks an unsuccessful attempt and strongly reduces the score of the normalised base callsign. **Reset fail** removes the mark. It applies to all active suffix and category variants of the station and remains active for the current program session.

The controls underneath are used to create a sked:

| Control | Meaning |
|---|---|
| **Sked in** | Time remaining until the sked |
| **Band** | Agreed band selected from the locally enabled bands |
| **Mode** | `SSB` or `CW` for a possible Win-Test handover |
| **Create sked** | Create the internal sked |
| **Remind-PM in** | Enable automatic reminder PMs |
| **2+1**, **5+2+1**, **10+5+2+1** | Times at which reminder PMs are sent before the sked |

![Sked controls in the Further Info section](sked_controls.png)

> **Screenshot note:** This image still shows the previous separate layout. It will be replaced after manual acceptance of the responsive **NOT QRV** and **Sked alert** groups.

The proposed band is derived from recent QRG and name information for the station. It can be changed explicitly before creating the sked. The mode selection only affects the Win-Test handover; the internal sked and reminder PMs work independently.

**Create sked** always creates the appointment inside KST4Contest first. If the Win-Test network listener is active, KST4Contest then attempts an additional handover to Win-Test. If no QRG matching the selected band can be found or Win-Test cannot be reached, the internal sked, its priority contribution and any scheduled reminders remain intact.

The complete derivation and limitations are described under [Skeds and Sked Reminders](en-Features#skeds-and-sked-reminders).

---

## Priority List

The compact bar is located on the right-hand side between the user list and the Further Info section. The **New joined** group appears on the left when needed, while the independent **Priority** group remains on the right.

**New joined** shows actual chat joins for ten minutes, newest first. Up to five stations are displayed directly when space permits; additional entries, or entries moved out at a narrow width, remain available in the compact **more** dropdown. The initial user list, a complete resynchronisation, profile-only updates and the local callsign do not create entries. A station is removed immediately when it leaves. A later real rejoin starts a new ten-minute period. Complete callsign and chat category remain separate identities.

Clicking an entry uses the shared station selection, updates Further Info and scrolls the user list to the station when necessary.

The **Priority** group displays the two currently highest-ranked candidates directly in the main window:

```text
Priority:  1 CALLSIGN SCORE  2 CALLSIGN SCORE  more
```

Clicking either candidate selects the corresponding active chat member. The complete callsign, including its suffix and chat category, is used.

The **more** button opens a separate window containing up to 15 candidates. The list is sorted by descending score. Double-clicking an entry selects the candidate and closes the window.

![Priority Score, compact candidate list and Further Info controls](priority_score_overview.png)

> **Note:** This screenshot still shows the previous Further Info geometry. It will be replaced after the new scrollable, height-constrained station panel has completed its visual check.

Stations with a score of `0` are not included in the priority list. They remain visible in the user list so that the reason for their exclusion can be examined and, for example, an incorrect NOT-QRV mark can be changed.

The score is calculated for the normalised base callsign. Several active variants such as `9A0BB-2` and `9A0BB-70` may therefore display the same value in the user list. They nevertheless remain separate message targets.

New messages, AirScout data, skeds and status changes request a new calculation. A periodic refresh also runs in the background. A briefly outdated order is therefore not an error.

Calculation and limitations: [Priority Score and Priority List](en-Features#priority-score-and-priority-list-from-v140).

---

## AP Timeline

The AP timeline shows opportunities in the upcoming window relative to now. Its subtle minute axis runs from `0 min` on the left to `+30 min` on the right. Ticks normally use five-minute intervals; a narrow view increases the interval so the labels remain readable.

The four candidate lanes directly reflect the existing priority selection: **In beam 1**, **In beam 2**, **Priority 1** and **Priority 2**. **Skeds** have a separate lane. The existing colour coding, antenna emphasis and click-to-select behaviour remain in effect.

---

## Station Map

The station map can be opened in two ways:

- **Windows → Show / hide station map** opens or closes the map window.
- **Show on map** in the **Further Info** panel opens the map and focuses the selected station.

The map uses the stations which remain visible after applying the current user-list filters. Its header shows the number of displayed stations and indicates a filtered view with `filtered view active`.

![Station map with a selected station and visible path analysis](station_map_path_analysis.png)

> **Screenshot note:** This image still shows the previous marker label and neither the new context menu nor the Worked-square overlay. It will be replaced after manual map acceptance.

### Selecting a Station

A marker displays only its callsign as visible text. Locator, detected bands, `B+` state and the most recently known QRGs remain available in its tooltip.

A single station marker can be selected directly. KST4Contest then:

1. selects the corresponding chat member,
2. scrolls the main user list to that entry,
3. updates the **Further Info** panel, and
4. prepares the complete visible callsign as the `/cq` recipient.

Chat logins with the same normalised base callsign and position may share one marker. They nevertheless remain separate message targets inside KST4Contest.

When **Grouping** is selected, markers which are too close together at lower zoom levels are displayed as a cluster containing the number of stations. Clicking the cluster zooms into that area. A concrete station is selected only after an individual marker becomes visible and is clicked. Clearing the checkbox displays every positionable station as an individual marker at every zoom level. Its tooltip explains the compact switch's full purpose.

The change takes effect immediately without changing the current zoom, viewport or station selection. It is saved automatically and restored at the next programme start. If no value has been stored yet, **Grouping** remains selected so existing installations retain the previous behaviour.

This switch controls only the spatial clusters on screen. Active chat variants of the same normalised base callsign may still share one geographical marker and remain separate message targets regardless of this setting.

Right-clicking a marker or a row in the user list opens the same station context menu. It contains **Message snippets**, **Create sked…**, band-specific **Tag NOT QRV** and **Mark as worked** actions, and **Calculate reachability**. When a marker represents several complete callsigns or chat categories, the concrete target must be chosen first. No action is applied arbitrarily to the first entry.

The snippet action only extends the send field and does not send by itself. The sked editor keeps the target captured when it opened; a later selection change does not alter that draft. **Mark as worked** uses the same additive Worked and database path as a received logger `contactinfo` and never removes an existing mark.

**Calculate reachability** explicitly starts the existing path calculation for the chosen band. When invoked from the user list, KST4Contest opens the map and displays the loading state, result or error for that exact target.

For a selected station, the header additionally shows:

- the complete callsign,
- locator,
- QRB and QTF,
- detected active bands,
- any available `B+` band opportunity, and
- the most recently known QRGs.

Long header content is shortened. The complete text remains available in its tooltip.

**Reset filters** invokes the same action as the button in the user list. It clears all enabled station filters, callsign search and direction filters. The display-only **Grid color** option remains unchanged, as do sorting, station selection, zoom and viewport. **Always on top** affects only the map window, is applied immediately and is saved automatically for the operator profile. It is disabled when no value has been stored.

While **Grid color** is enabled, the map marks persistently known, already worked four-character Maidenhead squares with a black outline and a 30 percent black fill overlay. The overlay uses the same persistent Worked-grid state as **Only new grids**, so a square remains visible even when it currently contains no chat station. Neither outline nor fill overlay is drawn while **Grid color** is disabled.

### Clearing the Selection with Reset View

**Reset view** clears the selected station without changing the map position or zoom level.

It:

- clears the selected station,
- clears the selection in the main user list,
- removes the connection line to the remote station,
- discards a pending analysis for the previous station, and
- removes the right-hand analysis panel.

The map itself remains at the previously selected position and zoom level. This function is therefore not a geographical reset to the local station.

![Station map after Reset view without a selected station](station_map_reset.png)

Selecting another individual marker restores the station selection and analysis panel.

### Triggering a DX Cluster Spot

**Trigger cluster spot** is visible only while a station is selected. It sends one spot to logging programmes connected to the built-in local DX Cluster server.

This requires:

- the local DX Cluster server to be enabled,
- at least one connected cluster client, and
- a usable QRG for the selected station.

The spot is not sent to a public Internet cluster.

### Path Analysis

The terrain profile is displayed below the map. The right-hand analysis panel includes, among other things:

- the data source and number of elevation samples,
- the analysis frequency,
- the Earth-curvature or refraction model,
- radio and terrain horizons,
- Fresnel-zone clearance,
- detected obstructions,
- the link budget,
- estimated received power, and
- a summarised path assessment.

The analysis uses the same centrally derived band as the Reachability functions. A band explicitly selected in the **Reachability** dropdown is taken into account.

These values remain technical estimates. Buildings, vegetation, local obstructions, current propagation conditions and unknown station parameters may substantially change the real result.

### Hiding the Path Analysis

**Hide path analysis** hides both the terrain profile and the right-hand analysis panel, leaving more space for the map.

![Station map with hidden path analysis](station_map_compact.png)

The **Path analysis is hidden** message and **Show path analysis** button remain visible, so the function can be restored directly.

If no station is selected when the analysis is shown again, no empty right-hand panel is displayed. It is recreated only after a specific station has been selected.

The setting is stored and restored at the next programme start.

The divider between the map and detail panel can be moved horizontally. Longer values wrap in a narrow detail panel; a vertical scrollbar appears if the available height is insufficient.

Detailed derivation and limitations: [Station Map and Path Analysis](en-Features#station-map-and-path-analysis-from-v141).

---

## Global Message Tabs and Monitor Window

The lower part of the main window contains three global message tabs. Their contents do not depend on the station currently selected in the user list.

| Tab | Content |
|---|---|
| **Public messages** | Public chat messages, CQ calls and beacons |
| **DXCluster messages** | DX cluster messages received through ON4KST |
| **QSO of the other** | Directed messages between two other stations |

Clicking a row under **Public messages** or **QSO of the other** selects its sender as the current station and scrolls the user list to the matching callsign and chat category. The global tab contents remain unchanged.

![Global message tabs in the main window](global_message_tabs.png)

In **QSO of the other**, sender and receiver are displayed separately. **Last QRG TX** and **Last QRG RX** contain the frequencies most recently known for the two stations. They do not necessarily represent the frequency discussed in the displayed conversation.

**wkd TX?** and **wkd RX?** show the global Worked state of the two base callsigns. These values are not band-specific.

The **DXCluster messages** tab shows the reporting and reported stations, their locators, QRG, message text and the global Worked state of the reported station. Which fields are actually available depends on the message received from the ON4KST server.

Message text remains on one line. If a cell is too narrow, its complete content is available in a tooltip. Web addresses in the message text are clickable.

### Separate Monitor Window

KST4Contest additionally opens the **Cluster & QSO of the other** window. It shows DX cluster messages in the upper table and directed messages between other stations in the lower table.

![Separate monitor window for DX cluster traffic and directed messages between other stations](cluster_qso_monitor.png)

The vertical divider position and window size are saved automatically. The DXCluster and QSO tables use their own column widths in this window, so changing the monitor layout does not alter the corresponding main-window tabs.

The window can be hidden and restored through:

```text
Windows → Hide cluster / stranger QSOs
Windows → Show cluster / stranger QSOs
```

The main-window tabs and separate monitor window use the same underlying data. Hiding the monitor window therefore neither stops message processing nor removes messages from the tabs.

Derivation and limitations: [Global Message Views](en-Features#global-message-views).

---

## Menu

### File

- **Connect to …** starts the connection using the settings already applied in KST4Contest.
- **Disconnect** terminates the current ON4KST connection without closing KST4Contest.
- **Exit + disconnect** terminates the connection and then closes the programme.

The Connect and Disconnect entries are enabled or disabled according to the current connection state.

### Options

- **Set QRG as name in Chat (main category)** sends `/SETNAME` containing the current `MYQRG` to the primary chat category.
- **Show me as away in chat** sends `/AWAY`.
- **Show me as active in chat** sends `/BACK`.
- **Show options** shows or hides the settings window.

Functions which communicate with the server are available only after the ON4KST connection has been established completely.

### Windows

- **Hide cluster / stranger QSOs** and **Show cluster / stranger QSOs** hide or restore the separate cluster and QSO monitor window.
- **hide options** and **show options** hide or restore the settings window.
- **Use dark mode design** activates the dark colour scheme for the running session.
- **Use default mode design** restores the standard light colour scheme for the running session.
  The startup colour scheme is set per profile on the **GUI** settings tab under **Design**.
- **Show / hide station map** opens or closes the separate station-map and path-analysis window.

---

## Window Sizes and Dividers

KST4Contest automatically stores programme-window sizes and positions, relevant dividers and manually changed table-column widths in the configuration file after a short delay. Any pending layout update is written when the programme exits. **Save Settings** is not required for these changes, but still stores the complete current state, including the layout.

This includes the horizontal divider between the message table and the station actions in Further Info. Its value belongs to the active root or operator profile; when restoring it, KST4Contest clamps the value to the height available in the current window.

The main window is additionally checked against the visible area of the primary screen during startup. If the stored size is too large, KST4Contest reduces and moves the window so that it remains accessible. The complete process is described under [Screen-Aware Main Window Sizing](en-Features#screen-aware-main-window-sizing-from-v141).

The other programme windows do not currently use this additional size restriction. If, for example, the separate monitor window appears too large after moving to a smaller screen, correcting it manually is sufficient; the new size is saved automatically.

If the layout has become inconvenient, first move the dividers and column widths back to usable positions. Deleting the configuration file also resets the UI values, but it removes the other stored programme settings as well. It should therefore be used only when the interface cannot be restored in another way.


---

## Operating Tips

- **Keep the settings window open**: This provides quick access to the beacon controls.
- **Right-click in the user list**: Opens the snippet menu and additional actions, including QRZ.com profiles and NOT-QRV marks.
- **Press Enter while working in the chat**: If the send field contains text, Enter sends it directly even when another control has focus.
- **Stop the beacon while scanning**: Disable the beacon while moving through frequencies to avoid flooding the chat with unnecessary messages.
