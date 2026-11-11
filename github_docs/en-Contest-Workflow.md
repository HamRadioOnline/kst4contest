# Contest Workflow with KST4Contest

> You are reading the English version | [Deutsche Version](de-Contest-Workflow)

KST4Contest brings chat activity, station selection, known QRGs, worked status, skeds, aircraft scatter timing and other station data together in one interface. Its value does not come from one individual indicator, but from the way this information works together during an active contest.

This page describes a complete operating workflow. Individual functions and their technical limitations remain documented under [Features](en-Features), [User Interface](en-User-Interface), [Log Synchronisation](en-Log-Sync) and [AirScout Integration](en-AirScout-Integration).

---

## Purpose and Limitations

KST4Contest is intended to reduce the time between recognising an opportunity and attempting the actual QSO.

The programme can show, among other things:

- which stations are active,
- on which bands and QRGs they were most recently detected,
- which stations have already been worked,
- which additional bands may still be available,
- which candidates match the current antenna direction,
- when an aircraft scatter window is expected, and
- which station is currently working in a direction useful to the local station.

These remain decision aids. A high priority score is not a QSO probability. An aircraft geometry rated at 100% by AirScout does not guarantee a contact either. The operator must still decide whether a QRG is actually clear, whether the remote station is listening and whether the path works under the current conditions.

---

## Before the Contest

The important settings should be checked before the first interesting sked appears.

### Basic Configuration

Check at least:

- the local callsign, password and locator,
- the primary chat category,
- login and settings for the second category if it is used,
- locally enabled bands,
- antenna beamwidth,
- maximum useful distance,
- `MYQRG` and, where applicable, `SECONDQRG`,
- log synchronisation, and
- the required shortcuts, snippets and message variables.

Antenna beamwidth and maximum distance depend on the station. At DM5M, for example, the actual antenna system is represented by a beamwidth of 69° and a maximum distance of 900 km. These are not general recommended values.

Use **Save Settings** for permanent changes. After connecting, the `LINK` indicator should be green. Only then have login and user-list synchronisation been completed.

### Automatic Replies

The automatic QRG reply is part of the active contest workflow. It answers repeated QRG requests and removes some routine work from the chat operator.

This is different from the general automatic reply. The general reply can react to all incoming requests and is mainly useful while the station is temporarily not QRV or does not want to take part in sked operation. It avoids unnecessary follow-up work for both the local and requesting stations.

### Optional Connections

Enable only interfaces which are actually required and have already been tested:

- logging software or Simplelogfile,
- TRX synchronisation,
- AirScout,
- PSTRotator,
- Win-Test sked handover, and
- the local DX Cluster server.

A contest is not an ideal time to investigate radio conditions and a newly enabled network interface at the same time.

Before each contest, verify that the logging application writes the current contest file to the path selected for the Simplelogfile. A test QSO should appear as globally worked within one minute. KST4Contest does not reset Worked marks derived from this file automatically when the contest changes.

---

## Basic Contest Cycle

The normal operating cycle repeats:

1. Call CQ or run an agreed sked.
2. Monitor chat activity, the priority list, map and AP timeline.
3. Select a suitable candidate.
4. Decide between the local and remote QRG.
5. Attempt the QSO.
6. Log a successful contact immediately.
7. Check for another band opportunity.
8. Mark a meaningful failed attempt with **Sked fail**.
9. Return to CQ operation or continue with the next candidate.

KST4Contest keeps the required information together between these steps. Changing frequency, calling, listening and making the actual decision deliberately remain operator tasks.

---

## CQ Operation

During operation on a mainly fixed CQ frequency, `MYQRG` and `SECONDQRG` should match the frequencies actually in use. TRX synchronisation can update `MYQRG` automatically only when it is enabled and actually supplies valid packets. Before the contest, verify this with a real frequency change. Without a working automatic source, the value must be maintained manually.

The beacon can publish the current QRG, locator and antenna direction in the chat at regular intervals. Its variables are evaluated again for every transmission.

Disable the beacon while scanning across several frequencies. An automatically published QRG is useful only while it is still correct. When the logger supplies the frequency, KST4Contest switches the beacon off [by itself](en-Configuration#automatic-switch-off-after-a-frequency-change) once the radio has moved by more than 2 kHz. After every program start the beacon is off and has to be enabled again.

Shortcuts and snippets should cover the messages required regularly, for example:

- asking a station to listen on the local QRG,
- asking for the remote QRG,
- announcing a move to the remote QRG,
- confirming antenna direction, and
- proposing a sked.

Further details are available under [Macros and Variables](en-Macros-and-Variables).

---

## Selecting Candidates

The user list can be reduced to the current operating situation by combining QTF, QRB, Worked, band, activity, New Bands, Tropo and AirScout filters.

The priority list and AP timeline add further context:

- The priority score combines several known criteria.
- The AP timeline arranges skeds and expected aircraft scatter opportunities by time.
- The station map shows geographical position, antenna direction and radio path.
- Worked and band status help prevent unnecessary duplicate work.

![Priority list and evaluation information](priority_score_overview.png)

The score is a sorting aid. Before attempting a contact, continue to check the callsign, category, band, QRG, direction, distance and age of the underlying information.

A deliberate selection in the user list, priority list, timeline or map selects the concrete chat member. The complete visible callsign and its chat category are retained. KST4Contest then prepares `/cq CALLSIGN` in the send field.

---

## Using the Local or Remote QRG

When a useful propagation direction is detected and a suitable aircraft is available for a candidate, the remote station is often first asked to listen on the local QRG. This is particularly useful while CQ operation is already running there and the station can receive immediately without another change.

If the remote station does not respond, its own QRG is more suitable or it cannot use the requested frequency, the local station changes frequency. KST4Contest keeps the most recently detected QRGs available, so the operator does not have to search the complete chat history again.

A deliberate attempt on the QRG of a sked partner is also part of the normal workflow. The objective is not to remain on the local QRG at all costs, but to use the available opportunity with as little delay as possible.

Before changing frequency, check at least:

- the correct band,
- the complete target callsign,
- the remote station’s QRG,
- antenna direction,
- the expected aircraft scatter window, and
- whether the QRG is clear.

---

## Using Directional Opportunities

A directed message between two other stations may indicate that the sender’s antenna is pointing approximately towards the receiver. If this direction is also useful for the local station, KST4Contest temporarily displays the sender in green and bold.

![Directional opportunity displayed in green and bold](direction_opportunity_highlight.png)

The indication appears in the user list and associated views. With an appropriate cluster configuration and a known QRG, the opportunity may additionally be made available through the local DX Cluster.

Several relevant details are therefore already available when the opportunity appears:

- complete callsign,
- locator and direction,
- most recently detected QRG,
- band information,
- AirScout data, and
- the current Reachability or Tropo assessment.

The operator does not have to collect this information first. The remaining decision is whether the opportunity justifies briefly interrupting ongoing CQ operation.

At DM5M, practical evaluation of these opportunistic attempts has so far produced a success rate of approximately 35–40%. This value describes the experience of one particular station. It is not a general prediction and depends on factors such as band, distance, station equipment, response time and propagation conditions.

CQ operation or the next sked can continue immediately after the attempt.

---

## Planning and Evaluating Skeds

Enter a sked with the band actually intended for the contact and a realistic time. KST4Contest adds it to its internal sked management and takes it into account for reminders, the timeline and priority calculation.

Skeds are maintained only for the current programme session. They are not a persistent replacement for the contest log or operating notes.

If the Win-Test connection is enabled, KST4Contest additionally attempts to pass the sked to Win-Test. If no usable QRG exists or the band does not match, the internal sked remains available. Only the additional handover may be skipped.

### Failed 100% Aircraft Skeds

If a carefully prepared attempt fails despite an aircraft geometry rated at 100% by AirScout, mark the station with **Sked fail**.

The 100% indication is not a probability of completing the QSO. A failure under these conditions is nevertheless useful operating evidence that the path did not work with the current station configuration and current conditions.

The mark reduces that station’s priority for the remainder of the current session. This allows candidates without comparable negative operating evidence to be handled first.

**Sked fail** must not be treated as a permanent statement that the station cannot be worked. Different conditions, another band or a changed station configuration may produce a different result. The mark can be reset and is not retained after restarting the programme.

---

## After Every QSO: Logging and the Next Band

Enter a successful QSO in the connected logging programme immediately. Only then can worked status, band status, filters and priority evaluation be updated in time.

The available detail depends on the log source. Some interfaces provide band, QRG and locator, while simpler sources report only a global worked state.

Immediately after every log entry, check whether another common, locally enabled and unworked band is available for the same station. KST4Contest indicates this through `BAND+` and the station’s band information where the available data permits such an evaluation.

This check is useful in every form of multiband operation. The remote station can be coordinated directly to another band with a specific band and frequency before it turns its antenna away or starts another sked.

In plain terms: check the next possible QSO while the remote station is still available and the common context still exists.

If the station is not actually QRV on an indicated band, mark that band as NOT QRV. This removes the unusable opportunity from filters and evaluation instead of allowing it to reappear after every update.

---

## Multi-Category and Multiband Operation

Multi-category operation is not a secondary feature for multiband stations. Its main advantage is that information from two chat categories can be evaluated within one operating workflow.

The benefit is particularly large for:

- single-operator multiband stations,
- multi-operator multiband stations using one central chat coordinator, and
- stations which regularly attempt another band immediately after a QSO.

Worked state, known band activity and band opportunities can be evaluated together. The concrete message target still retains its complete callsign and chat category.

This allows the chat operator to coordinate a station directly from the first QSO to another band without searching again for callsign, QRG and band status. In practical operation, this can produce a very rapid sequence of usable QSO opportunities. This is where multi-category operation provides its greatest workflow advantage.

The same approach remains effective in single-operator operation. The operator still has to change bands personally, but the next useful opportunity is already prepared.

Multi-multi stations with several active chat operators benefit as well. Responsibilities, band changes and requests already in progress must then be coordinated clearly. Several operators having the same information is useful; several contradictory sked requests sent to the same station are not.

---

## Practical Examples

### DM5M: CQ First, More Skeds Later

During the first four to five hours of a VHF/UHF contest, DM5M operates mainly by calling CQ. The chat is monitored, but used only for a small number of deliberate interventions.

Sked activity increases later. Useful propagation directions, suitable aircraft scatter windows, unworked stations and additional band opportunities are then combined deliberately.

A suitable station is first asked to listen on the local QRG. If it does not respond or cannot use that QRG, DM5M moves to the remote station’s frequency. Planned attempts directly on a sked partner’s QRG are also part of the process.

A green and bold directional opportunity may briefly interrupt CQ operation. Calling or sked operation continues immediately after the attempt.

This is a practical example, not a required operating method. Other stations may start arranging skeds considerably earlier, change QRG continuously or make more intensive use of the chat from the beginning.

### G1YBB: Working Directional Opportunities Systematically

G1YBB uses the directional indication particularly consistently. Stations highlighted in green are checked systematically and worked where possible while normal CQ operation continues in parallel.

KST4Contest does not automate the QSO. Its advantage is that QRG, direction, aircraft scatter information and other evaluation data are already available when the opportunity appears. The remaining task is a quick operating decision.

Another particularly consistent operating method uses the station map as a geographical worklist:

1. The **wkd** filter removes already worked callsigns from the user list and therefore from the map.
2. G1YBB selects an interesting station on the map.
3. **Trigger cluster spot** passes the known QRG to Minos.
4. Selecting the spot moves Minos to that QRG.
5. The completed QSO is logged.
6. Log synchronisation updates the Worked state, and the filter removes the station from both the user list and the map.

The map consequently becomes a spatial list of the stations still to be worked. This workflow is optional. It requires reliable log synchronisation and a working DX Cluster connection and is mainly useful for operators who deliberately want to organise the contest in this geographical way.

[Watch G1YBB demonstrate this workflow.](https://www.youtube.com/watch?v=lMQZMiSHlUI)

---

## Optional Interfaces in the Workflow

| Interface | Contest task |
|---|---|
| [Log Synchronisation](en-Log-Sync) | Updates worked and band status after a QSO |
| [AirScout](en-AirScout-Integration) | Supplies aircraft scatter candidates and expected time windows |
| [PSTRotator](en-Configuration) | Receives or sets the antenna direction |
| [Win-Test](en-Log-Sync) | Can additionally pass entered skeds to Win-Test |
| [DX Cluster Server](en-DX-Cluster-Server) | Passes detected opportunities to connected logging programmes |
| [Station Map](en-User-Interface) | Shows stations, directions, selection and radio path |

None of these interfaces is mandatory for basic chat operation. Their value appears when they are configured reliably and shorten a specific manual task.

---

## What KST4Contest Does Not Decide

KST4Contest does not decide:

- whether a QRG is actually clear,
- whether the remote station can currently listen,
- whether an aircraft will enable a QSO,
- whether a calculated radio path works under the current conditions,
- whether ongoing CQ operation should be interrupted for an opportunity, or
- which candidate has the greatest value for the current contest strategy.

The programme assembles the available information and keeps it current. The final decision remains with the operator. This is not a limitation of the workflow; it is the part for which we are still operating radios rather than spreadsheets.
