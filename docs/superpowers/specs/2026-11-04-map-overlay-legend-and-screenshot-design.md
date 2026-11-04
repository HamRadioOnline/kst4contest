# Map Overlay Legend and Screenshot Design

## Intent

Make the two map overlays understandable without consulting the manual and replace the outdated general map image with the supplied screenshot of an active QRB range.

## Behaviour

- The map header shows a compact red **Antenna** legend item only while the antenna sector can be drawn.
- It shows a compact blue **Active filters** legend item only while a valid QTF or QRB overlay is active.
- A localized tooltip explains that red represents the actual antenna heading, beamwidth and maximum QRB, while blue represents active QTF and QRB filters.
- The existing responsive `FlowRow` may wrap the legend with the other header controls.
- Overlay rendering, filtering, antenna heading and rotor behaviour do not change.

## Screenshot

- Store the supplied image as `github_docs/station_map_spatial_filters.png` without altering its pixels.
- Use it in the German and English UI manual and on the Station Map website feature page.
- Describe the visible range correctly as minimum and maximum QRB, not QTF.
- Retain `station_map_path_analysis.png` where the visible path-analysis panels are the subject.

## Constraints

- Reuse the exact overlay colours rather than duplicating colour literals.
- Keep English and German UI strings and documentation aligned.
- Do not stage, commit, push or publish any change.
