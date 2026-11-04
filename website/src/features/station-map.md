---
title: Station Map and Path Analysis
icon: 🗺️
category: Contest Awareness
since: "1.41"
summary: Use the filtered chat-member list as a geographical contest worklist and examine the selected radio path with terrain, Fresnel and link-budget estimates.
description: KST4Contest projects the current filtered station context onto a map and provides an engineering estimate of the selected path.
tagsList:
  - station map
  - path analysis
  - terrain profile
  - Fresnel zone
  - link budget
  - Open-Meteo
  - Copernicus GLO-90
  - contest workflow
related:
  - station-filters
  - qrg-detection
  - band-recognition
  - directional-opportunities
  - dx-cluster
  - rotator-control
---

## Why use a map during a contest?

A table is efficient when callsigns, scores or QRBs need to be sorted. It is less immediate when the operator wants to see which unworked stations remain in one direction or how they relate to the current antenna sector.

Since version 1.41, the KST4Contest station map provides this geographical view of the same operating context. It is not an independent list. The filters applied to the chat-member table also determine which stations appear on the map.

![Station map with a selected path, red antenna sector and active blue QRB range](/manual/assets/station_map_spatial_filters.png)

## A geographical contest worklist

With the **wkd** filter enabled, worked callsigns disappear from the user list. They also disappear from the map. What remains is a spatial worklist of stations which may still be relevant to the contest.

The map retains the context already known by KST4Contest:

- normal, Worked, directional-opportunity and selected marker states;
- known bands and open `B+` opportunities;
- the configured antenna direction, beamwidth and maximum QRB as a visible sector;
- active QTF and QRB filters as a separate semi-transparent sector, circle or distance ring; and
- a Maidenhead locator grid for geographical orientation.

When either overlay is visible, the compact header legend identifies the red antenna sector and the blue active-filter area.

Only stations with a usable six-character locator can be positioned. Active chat variants of the same normalised base callsign are combined into one marker, while their actual message destinations remain separate.

At lower zoom levels, **Group nearby stations** keeps dense map areas readable by combining close markers into screen-based clusters. The grouping can be disabled immediately without changing the current viewport or selection, and KST4Contest remembers the choice. This setting does not split the geographical marker shared by active variants of the same base callsign.

Green has a specific meaning: it marks a directional opportunity derived from directed ON4KST messages. It does not merely mean that the marker happens to lie inside the local antenna sector.

## Selection remains connected to the chat

Selecting a marker selects the corresponding active chat member in the main window. KST4Contest scrolls to the entry in the user list, updates **Further Info** and prepares the complete visible callsign as the message target. Explicit selections from the user list, messages, Priority, New joined, Timeline and other station controls use the same navigation action.

The suffix and chat category are retained. Combining several active variants into one geographical marker therefore does not turn them into one interchangeable ON4KST login.

The selected path is shown as a high-contrast connection line. The map keeps the current zoom if both ends already fit and otherwise zooms out until the complete path is visible. Ordinary station updates and asynchronous analysis results do not take over the camera.

An explicit QTF entry or compass-button click centres the map towards that directional sector while preserving the zoom. This is map navigation only: it neither sends a rotor command nor changes the rules for the optional QTF-to-antenna-heading transfer.

If a filter hides the selected station, the marker and path remain visible as a separate selected target. The station is not added back to the filtered marker count or clustering result.

## G1YBB's map-driven workflow

G1YBB uses the map particularly consistently as a geographical contest worklist:

1. Enable the Worked filter so that already worked stations are removed.
2. Select an interesting station on the map.
3. Use **Trigger cluster spot** to send its known QRG to the connected logger.
4. Select the spot in Minos and move the radio to that QRG.
5. Complete and log the QSO.
6. Log synchronisation updates the Worked state, and the filter removes the station from both the user list and the map.

This creates a direct visual progression through the remaining stations. It is an optional and deliberately consistent operating method, not a prerequisite for using the map or KST4Contest.

[Watch G1YBB demonstrate this workflow.](https://www.youtube.com/watch?v=lMQZMiSHlUI)

## What does the path analysis calculate?

For the selected station, KST4Contest currently requests terrain elevations from the Open-Meteo elevation service. The provider uses Copernicus GLO-90 data, and one path is sampled with no more than 100 evenly distributed elevation points.

The resulting profile combines information including:

- terrain and the geometrical line between both antennas;
- radio and terrain horizons;
- the first Fresnel zone and its minimum clearance;
- possible Fresnel-zone intrusion and a rough diffraction estimate;
- a frequency selected from current QRG or band context; and
- an estimated link budget using power, antenna gain, feeder loss and path loss.

The **Frequency** shown in the panel is the value actually used for the calculation. It should be checked before interpreting the Fresnel zone or link budget, especially on the microwave bands.

## Engineering estimates, not measured conditions

The nominal resolution of the terrain model is not the distance between requested points. On a long path, 100 samples can leave substantial gaps, and narrow obstacles may remain undetected.

The calculation also cannot know the exact remote antenna height and pattern, buildings, vegetation, current atmospheric refractivity, interference or whether a detected QRG is still in use. Line of sight, Fresnel clearance, horizons and the link budget therefore remain technical estimates.

In practical terms: the map keeps the remaining contest stations geographically visible, while the path analysis exposes useful assumptions about one selected route. Neither view predicts propagation or guarantees a QSO.

[Read the complete map and path-analysis description in the manual.](/manual/en/features/#station-map-and-path-analysis-from-v141)

[Open the map controls in the user-interface manual.](/manual/en/user-interface/#station-map)

[Open the path-analysis and link-budget settings.](/manual/en/configuration/#path-analysis-and-link-budget)
