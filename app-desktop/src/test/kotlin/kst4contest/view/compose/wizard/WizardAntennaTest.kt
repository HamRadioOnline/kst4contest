package kst4contest.view.compose.wizard

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * The antenna and power answers: always proposals taken from what is stored, never empty,
 * and the site elevation read the way the Station tab reads it.
 */
class WizardAntennaTest {

    private fun prefs(configure: ChatPreferences.() -> Unit = {}): ChatPreferences =
        ChatPreferences(Path.of("wizard-antenna-test-unused.xml")).apply(configure)

    private fun stored() = prefs {
        setStn_antennaBeamWidthDeg(28.0)
        setStn_pathAnalysisOwnAntennaGainDbi(16.5)
        setStn_pathAnalysisOwnAntennaHeightMeters(18.0)
        setStn_pathAnalysisOwnTxPowerWatts(400.0)
        setStn_pathAnalysisOwnHeightAslMeters(612.0)
    }

    @Test
    fun theAnswersStartAsTheStoredValues() {
        val antenna = WizardAntenna(stored())

        assertEquals(28.0, antenna.beamWidthDeg)
        assertEquals(16.5, antenna.gainDbi)
        assertEquals(18.0, antenna.heightMeters)
        assertEquals(400.0, antenna.txPowerWatts)
        assertEquals("612", antenna.heightAslText, "a whole number is shown without a decimal point")
        assertTrue(antenna.complete)
    }

    @Test
    fun anUnsetSiteElevationIsAnEmptyField() {
        val antenna = WizardAntenna(prefs { setStn_pathAnalysisOwnHeightAslMeters(Double.NaN) })

        assertEquals("", antenna.heightAslText)
        assertFalse(antenna.heightAslRefused, "empty means the elevation model, not an error")
        assertTrue(antenna.complete)
    }

    @Test
    fun aFractionalSiteElevationKeepsItsFraction() {
        val antenna = WizardAntenna(prefs { setStn_pathAnalysisOwnHeightAslMeters(250.5) })

        assertEquals("250.5", antenna.heightAslText)
    }

    @Test
    fun theSiteElevationIsReadTheWayTheStationTabReadsIt() {
        val antenna = WizardAntenna(stored())

        listOf("", "  ", "250", "250,5", "250.5", "-100").forEach {
            antenna.heightAslText = it
            assertFalse(antenna.heightAslRefused, "'$it' must be accepted")
        }
        listOf("abc", "99999", "-501", "12 m").forEach {
            antenna.heightAslText = it
            assertTrue(antenna.heightAslRefused, "'$it' must be refused")
            assertFalse(antenna.complete)
        }
    }

    @Test
    fun aNumberThatIsNotOneMakesThePageIncomplete() {
        val antenna = WizardAntenna(stored())

        antenna.gainDbi = Double.NaN
        assertFalse(antenna.complete)

        antenna.gainDbi = 12.0
        antenna.txPowerWatts = Double.POSITIVE_INFINITY
        assertFalse(antenna.complete)
    }

    @Test
    fun writingPutsTheAnswersIntoThePreferences() {
        val target = prefs()
        val antenna = WizardAntenna(stored())
        antenna.beamWidthDeg = 35.0
        antenna.heightAslText = "250,5"

        antenna.writeTo(target)

        assertEquals(35.0, target.getStn_antennaBeamWidthDeg())
        assertEquals(16.5, target.getStn_pathAnalysisOwnAntennaGainDbi())
        assertEquals(18.0, target.getStn_pathAnalysisOwnAntennaHeightMeters())
        assertEquals(400.0, target.getStn_pathAnalysisOwnTxPowerWatts())
        assertEquals(250.5, target.getStn_pathAnalysisOwnHeightAslMeters())
    }

    @Test
    fun anEmptySiteElevationIsWrittenAsUnset() {
        val target = prefs { setStn_pathAnalysisOwnHeightAslMeters(612.0) }
        val antenna = WizardAntenna(stored())
        antenna.heightAslText = ""

        antenna.writeTo(target)

        assertTrue(target.getStn_pathAnalysisOwnHeightAslMeters().isNaN(), "empty hands the elevation back to the model")
    }

    @Test
    fun loadingTheStoredValuesAgainDropsWhatWasTyped() {
        val antenna = WizardAntenna(stored())
        antenna.beamWidthDeg = 99.0
        antenna.gainDbi = 1.0
        antenna.heightMeters = 2.0
        antenna.txPowerWatts = 3.0
        antenna.heightAslText = "abc"

        antenna.loadStored()

        assertEquals(28.0, antenna.beamWidthDeg)
        assertEquals(16.5, antenna.gainDbi)
        assertEquals(18.0, antenna.heightMeters)
        assertEquals(400.0, antenna.txPowerWatts)
        assertEquals("612", antenna.heightAslText)
        assertTrue(antenna.complete)
    }
}
