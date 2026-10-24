package kst4contest.view.compose.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import kst4contest.view.map.MapCallsignRawSnapshot
import kst4contest.view.map.MapViewport
import kst4contest.view.map.TileFetcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * What the map actually puts on the canvas.
 *
 * Asserted on the rendered image, because that is the only place the answers live: the
 * drawing code takes a viewport and paints, and no amount of reading it proves a dot
 * came out 12 px wide. These are the acceptance criteria of the projection rewrite and
 * of the grid planner, in the form the operator experiences them.
 *
 * Tiles never arrive here. The fetcher is asked asynchronously and the test renders one
 * frame, so what is measured is the map's own drawing over its own background — which is
 * also the case an operator meets without a network.
 */
class ComposeStationMapDrawingTest {

    private val canvasWidth = 900
    private val canvasHeight = 600

    /** The default station-dot border, `#4da6ff` in the Leaflet stylesheet. */
    private val dotBorder = 0x4DA6FF

    /** The home marker and the antenna beam, `#ff4d4d`. */
    private val homeColour = 0xFF4D4D

    /** The Maidenhead label colour, `#63067a`. */
    private val gridLabel = 0x63067A

    private val darkMapBackground = 0x23282D

    private val workedGridBorder = 0x000000

    /*
     * An empty cache and a source that always fails, so that "tiles never arrive" holds by
     * construction. The default fetcher read the user's disk cache and the real tile
     * server, and on CI a tile now and then made it in before the frame was rendered.
     */
    private val offlineTileFetcher = TileFetcher(
        Files.createTempDirectory("kst4contest-tile-cache-test"),
        { throw IOException("no tile source in this test") },
    )

    private fun station(call: String, locator: String, lat: Double, lon: Double) =
        MapCallsignRawSnapshot(
            call, call, locator, lat, lon, "", emptyMap(),
            false, false, false, false, 0.0, 0.0, 0, 0L,
        )

    private fun render(
        zoom: Float,
        markers: List<MapCallsignRawSnapshot> = listOf(station("DN9APW", "JO50JP", 51.0, 10.0)),
        ownLocator6: String = "",
        workedGrossFields: Set<String> = emptySet(),
        darkMode: Boolean = true,
    ): BufferedImage {
        val scene = ImageComposeScene(canvasWidth, canvasHeight, Density(1f)) {
            ComposeStationMap(
                markers = markers,
                tileFetcher = offlineTileFetcher,
                mapState = MapState(zoom = zoom, centerLon = 10.0, centerLat = 51.0),
                groupingEnabled = false,
                selectedMarkerCallsign = null,
                ownLocator6 = ownLocator6,
                antennaAzimuthDeg = 90.0,
                antennaBeamWidthDeg = 0.0,
                maxQrbKm = 0.0,
                onMarkerClicked = null,
                workedGrossFields = workedGrossFields,
                darkMode = darkMode,
                modifier = Modifier.fillMaxSize(),
            )
        }
        try {
            return ImageIO.read(ByteArrayInputStream(scene.render().encodeToData()!!.bytes))
        } finally {
            scene.close()
        }
    }

    private fun countColour(image: BufferedImage, rgb: Int): Int {
        var count = 0
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                if ((image.getRGB(x, y) and 0xFFFFFF) == rgb) count++
            }
        }
        return count
    }

    /** The widest horizontal run of a colour, which for a ring is its outer diameter. */
    private fun widestRun(image: BufferedImage, rgb: Int): Int {
        var widest = 0
        for (y in 0 until image.height) {
            var first = -1
            var last = -1
            for (x in 0 until image.width) {
                if ((image.getRGB(x, y) and 0xFFFFFF) == rgb) {
                    if (first < 0) first = x
                    last = x
                }
            }
            if (first >= 0) widest = maxOf(widest, last - first + 1)
        }
        return widest
    }

    /** The mean position of a colour, or null when it is absent. */
    private fun centroidOf(image: BufferedImage, rgb: Int): Pair<Double, Double>? {
        var sumX = 0L
        var sumY = 0L
        var count = 0
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                if ((image.getRGB(x, y) and 0xFFFFFF) == rgb) {
                    sumX += x
                    sumY += y
                    count++
                }
            }
        }
        return if (count == 0) null else Pair(sumX.toDouble() / count, sumY.toDouble() / count)
    }

    /**
     * The invariant the projection rewrite exists to make obvious: a size written in the
     * drawing code is the size that reaches the canvas. The previous version reached the
     * same geometry by dividing each size by the zoom fraction inside a scaled canvas,
     * which held only as long as every single site remembered to divide.
     */
    @Test
    fun `a station dot is the same size at every zoom`() {
        val widths = listOf(6.0f, 6.3f, 6.5f, 6.9f, 11.5f, 16.25f).map { widestRun(render(it), dotBorder) }

        assertTrue(widths.all { it > 0 }) { "no station dot was drawn at all: $widths" }
        assertEquals(1, widths.distinct().size, "the dot changed size across zooms: $widths")
    }

    /**
     * Home, the beam and the path line have to sit in the same coordinate system as the
     * stations.
     *
     * In the previous version they did not: `scale(scaleFactor)` closed before the beam
     * block, which then used the unscaled formula while the markers above it had been
     * drawn scaled. At an integer zoom the factor is 1 and the two agreed; at any zoom
     * with a fraction the beam started somewhere other than home.
     */
    @Test
    fun `home sits exactly on a station at the same position, at every zoom`() {
        /* Off the centre of the view on purpose: at the centre a scaling error is zero. */
        val home = kst4contest.locatorUtils.Location("JO50JP")
        val homeLat = home.latitude.toDegrees()
        val homeLon = home.longitude.toDegrees()
        val stationOnTopOfHome = listOf(station("DL0HOME", "JO50JP", homeLat, homeLon))

        for (zoom in listOf(6f, 6.5f, 7.25f, 8.25f)) {
            /*
             * Two renders of the same place rather than one: the station dot is drawn
             * over the beam layer and would simply hide the home dot.
             */
            val withStationOnly = render(zoom, markers = stationOnTopOfHome, ownLocator6 = "")
            val withHomeOnly = render(zoom, markers = emptyList(), ownLocator6 = "JO50JP")

            val homeCentre = centroidOf(withHomeOnly, homeColour)
            val stationCentre = centroidOf(withStationOnly, dotBorder)

            assertTrue(homeCentre != null && stationCentre != null) {
                "at zoom $zoom: home=$homeCentre station=$stationCentre"
            }
            assertTrue(
                abs(homeCentre!!.first - stationCentre!!.first) <= 1.0 &&
                    abs(homeCentre.second - stationCentre.second) <= 1.0
            ) {
                "at zoom $zoom home sits at $homeCentre but the station at the same place is at $stationCentre"
            }
        }
    }

    @Test
    fun `an unreachable tile source leaves the map in its own colour, not the system grey`() {
        val image = render(6f, markers = emptyList())

        assertTrue(countColour(image, darkMapBackground) > image.width * image.height / 2) {
            "the map background should cover the canvas while no tiles have arrived"
        }
    }

    /** `#63067a`, centred in the cell — not white, not in the corner. */
    @Test
    fun `grid labels are drawn in the colour the Leaflet map used`() {
        assertTrue(countColour(render(6f), gridLabel) > 0) { "no grid label was drawn" }
    }

    /**
     * At six-digit precision the grid has far more cells than it has room for labels.
     * The planner answers that with a stride; without it every cell got one and the map
     * disappeared under text, which is what the 30 September screenshot shows.
     */
    @Test
    fun `grid labels stay sparse where the cells are small`() {
        val image = render(12f)
        val labelPixels = countColour(image, gridLabel)
        val canvasPixels = image.width * image.height

        assertTrue(labelPixels < canvasPixels / 20) {
            "grid labels cover $labelPixels of $canvasPixels pixels, which is a carpet rather than a grid"
        }
    }

    /** Four-digit squares at zoom 6, two-digit fields at zoom 4. */
    @Test
    fun `the grid gets finer as the map zooms in`() {
        val atFour = countColour(render(4f), gridLabel)
        val atSix = countColour(render(6f), gridLabel)

        assertTrue(atFour > 0 && atSix > 0) { "labels missing: zoom 4 -> $atFour, zoom 6 -> $atSix" }
        assertTrue(atSix > atFour) {
            "a four-digit grid should show more labels than a two-digit one: $atSix vs $atFour"
        }
    }

    @Test
    fun `worked gross fields use a visible black border in both themes`() {
        val dark = render(6f, markers = emptyList(), workedGrossFields = setOf("JO51"))
        val light = render(6f, markers = emptyList(), workedGrossFields = setOf("JO51"), darkMode = false)

        assertTrue(countColour(dark, workedGridBorder) > 0) {
            "the Evening map has no black Worked-grid border"
        }
        assertTrue(countColour(light, workedGridBorder) > 0) {
            "the Day map has no black Worked-grid border"
        }
    }

    @Test
    fun `worked gross field interior is covered by thirty percent black`() {
        val viewport = MapViewport(10.0, 51.0, 6.0, canvasWidth.toDouble(), canvasHeight.toDouble())
        val cell = visibleWorkedGrossFieldCells(viewport, setOf("JO51")).single()
        val x = viewport.screenX(cell.westLon() + (cell.eastLon() - cell.westLon()) * 0.25).toInt()
        val y = viewport.screenY(cell.northLat() + (cell.southLat() - cell.northLat()) * 0.25).toInt()

        for (darkMode in listOf(false, true)) {
            val withoutWorked = render(6f, markers = emptyList(), darkMode = darkMode)
            val withWorked = render(
                6f,
                markers = emptyList(),
                workedGrossFields = setOf("JO51"),
                darkMode = darkMode,
            )
            val before = withoutWorked.getRGB(x, y)
            val after = withWorked.getRGB(x, y)

            for (shift in listOf(16, 8, 0)) {
                val beforeChannel = (before shr shift) and 0xFF
                val afterChannel = (after shr shift) and 0xFF
                val expected = (beforeChannel * 0.70).roundToInt()
                assertTrue(abs(afterChannel - expected) <= 2) {
                    "Worked fill in darkMode=$darkMode changed channel $shift from " +
                        "$beforeChannel to $afterChannel; expected $expected for 30% black"
                }
            }
        }
    }

    /**
     * Every grid line has to be drawn once.
     *
     * One stroked rectangle per cell paints each interior line twice — once by the cell
     * on either side — so at the specified 0.48 a shared edge reaches about 0.73 while
     * the outermost line stays at 0.48. The result is a grid of visibly uneven weight,
     * which is the kind of thing that makes the map not look like the original. Leaflet
     * drew each line once.
     *
     * Asserted as "no pixel carries more line colour than one full pass can put there".
     * Antialiasing of a 1.4 px line at a fractional position legitimately produces many
     * shades BELOW that value; none can legitimately exceed it.
     */
    @Test
    fun `no grid line is painted over itself`() {
        /* #e1e7ec at 0.48 over #23282d — one pass, fully covering a pixel. */
        val singlePassRed = (0.48 * 0xE1 + 0.52 * 0x23).toInt()

        val image = render(6f, markers = emptyList())

        /*
         * Only rows that cross vertical lines. A row lying on a horizontal grid line is
         * bright across its whole width, and where the two cross they legitimately
         * overlap — Leaflet's polylines crossed too.
         */
        var brightest = 0
        for (y in 0 until image.height) {
            var lit = 0
            var rowBrightest = 0
            for (x in 0 until image.width) {
                val pixel = image.getRGB(x, y) and 0xFFFFFF
                if (pixel == darkMapBackground) continue
                lit++
                val red = (pixel shr 16) and 0xFF
                if (red > rowBrightest) rowBrightest = red
            }
            val crossesAHorizontalLine = lit > image.width / 10
            if (!crossesAHorizontalLine && rowBrightest > brightest) brightest = rowBrightest
        }

        assertTrue(brightest > 0) { "no grid line was drawn at all" }
        assertTrue(brightest <= singlePassRed + 2) {
            "a grid pixel reached $brightest where one pass tops out at $singlePassRed — " +
                "the line was painted more than once"
        }
    }
}
