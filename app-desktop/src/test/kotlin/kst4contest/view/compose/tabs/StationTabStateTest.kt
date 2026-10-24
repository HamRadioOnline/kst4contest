package kst4contest.view.compose.tabs

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Test
import kst4contest.view.compose.SettingsDraft
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class StationTabStateTest(@param:TempDir private val tempDir: Path) {

    private inner class Session {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        var mainApplied: ChatCategory? = null
        var secondApplied: ChatCategory? = null
        var secondWasApplied = false
        val secondRotatorCalls = mutableListOf<Unit>()
        val draft = SettingsDraft()
        val state = StationTabState(
            prefs,
            draft,
            { mainApplied = it },
            { secondApplied = it; secondWasApplied = true },
            applySecondRotator = { secondRotatorCalls.add(Unit) },
        )
    }

    @Test
    fun `rotator 2 settings restart it once per apply however many changed`() {
        val s = Session()

        s.state.stn_pstRotator2Enabled = true
        s.state.stn_pstRotator2Host = "10.0.0.7"
        s.state.stn_pstRotator2Port = 12020
        val outcome = s.draft.apply()

        assertTrue(outcome.isClean, outcome.failures.toString())
        assertEquals(12020, s.prefs.getStn_pstRotator2Port())
        assertEquals(1, s.secondRotatorCalls.size)
    }

    @Test
    fun `rotator 2 on the port of rotator 1 is refused and stays in the draft`() {
        val s = Session()
        s.state.stn_pstRotatorPort = 12040

        s.state.stn_pstRotator2Port = 12040
        val outcome = s.draft.apply()

        assertEquals(1, outcome.failedWrites.size)
        assertTrue(outcome.failedWrites.single().contains("12040"), outcome.failedWrites.toString())
        assertEquals(12010, s.prefs.getStn_pstRotator2Port())
        assertEquals(12040, s.state.stn_pstRotator2Port, "the refused value is still shown")
    }

    @Test
    fun `rotator 1 onto the port of rotator 2 is refused while SO2R and rotator 2 are on`() {
        val s = Session()
        s.prefs.setTrxSynch_so2rEnabled(true)
        s.prefs.setStn_pstRotator2Enabled(true)

        s.state.stn_pstRotatorPort = 12010
        val outcome = s.draft.apply()

        assertEquals(1, outcome.failedWrites.size, outcome.failedWrites.toString())
        assertTrue(outcome.failedWrites.single().contains("12010"), outcome.failedWrites.toString())
        assertNotEquals(12010, s.prefs.getStn_pstRotatorPort())
    }

    @Test
    fun `rotator 1 onto the port of rotator 2 is accepted without SO2R`() {
        val s = Session()
        s.prefs.setStn_pstRotator2Enabled(true)

        s.state.stn_pstRotatorPort = 12010
        val outcome = s.draft.apply()

        assertTrue(outcome.isClean, outcome.failures.toString())
        assertEquals(12010, s.prefs.getStn_pstRotatorPort())
    }

    @Test
    fun `rotator 1 onto the port of rotator 2 is accepted while rotator 2 is off`() {
        val s = Session()
        s.prefs.setTrxSynch_so2rEnabled(true)

        s.state.stn_pstRotatorPort = 12010
        val outcome = s.draft.apply()

        assertTrue(outcome.isClean, outcome.failures.toString())
    }

    @Test
    fun `adjacent rotator ports are refused in both directions and distant ones accepted`() {
        for (p2 in intArrayOf(12041, 12039)) {
            val s = Session()
            s.state.stn_pstRotatorPort = 12040
            s.state.stn_pstRotator2Port = p2
            val outcome = s.draft.apply()
            assertEquals(1, outcome.failedWrites.size, "rotor 2 on $p2: ${outcome.failedWrites}")
        }
        for (p1 in intArrayOf(12011, 12009)) {
            val s = Session()
            s.prefs.setTrxSynch_so2rEnabled(true)
            s.prefs.setStn_pstRotator2Enabled(true)
            s.state.stn_pstRotatorPort = p1
            val outcome = s.draft.apply()
            assertEquals(1, outcome.failedWrites.size, "rotor 1 on $p1: ${outcome.failedWrites}")
        }
        val s = Session()
        s.prefs.setTrxSynch_so2rEnabled(true)
        s.prefs.setStn_pstRotator2Enabled(true)
        s.state.stn_pstRotatorPort = 12030
        assertTrue(s.draft.apply().isClean)
    }

    @Test
    fun `reading comes straight from the preferences`() {
        val s = Session()
        s.prefs.setStn_loginCallSign("DN9APW")
        s.prefs.setStn_on4kstServersPort(23001)

        assertEquals("DN9APW", s.state.stn_loginCallSign)
        assertEquals(23001, s.state.stn_on4kstServersPort)
    }

    @Test
    fun `a station setting waits in the draft until it is applied`() {
        val s = Session()

        s.state.stn_pstRotatorEnabled = true
        s.state.stn_qtfDefault = 135.0
        s.state.stn_on4kstServersPort = 23002

        assertFalse(s.prefs.isStn_pstRotatorEnabled(), "an edit took effect before Apply")
        assertNotEquals(23002, s.prefs.getStn_on4kstServersPort())
        assertTrue(s.state.stn_pstRotatorEnabled, "the tab shows the edited value")
        assertEquals(135.0, s.state.stn_qtfDefault, 0.001)

        s.draft.apply()

        assertTrue(s.prefs.isStn_pstRotatorEnabled())
        assertEquals(135.0, s.prefs.getStn_qtfDefault(), 0.001)
        assertEquals(23002, s.prefs.getStn_on4kstServersPort())
    }

    @Test
    fun `QTF filter heading synchronization waits in the draft until apply`() {
        val s = Session()

        s.state.stn_useQtfFilterDirectionAsAntennaHeading = false

        assertFalse(s.state.stn_useQtfFilterDirectionAsAntennaHeading)
        assertTrue(s.prefs.isStn_useQtfFilterDirectionAsAntennaHeading())

        s.draft.apply()

        assertFalse(s.prefs.isStn_useQtfFilterDirectionAsAntennaHeading())
    }

    @Test
    fun `enabling primary rotator preserves QTF filter heading preference in the draft`() {
        val s = Session()

        s.state.stn_pstRotatorEnabled = true

        assertTrue(s.state.stn_useQtfFilterDirectionAsAntennaHeading)
        assertTrue(s.prefs.isStn_useQtfFilterDirectionAsAntennaHeading(), "an edit took effect before Apply")

        s.draft.apply()

        assertTrue(s.prefs.isStn_pstRotatorEnabled())
        assertTrue(s.prefs.isStn_useQtfFilterDirectionAsAntennaHeading())
        assertFalse(s.prefs.isQtfFilterDirectionAsAntennaHeadingEnabled())
    }

    @Test
    fun `disabling primary rotator restores QTF filter heading synchronization`() {
        val s = Session()

        s.state.stn_pstRotatorEnabled = true
        s.state.stn_pstRotatorEnabled = false

        assertTrue(s.state.stn_useQtfFilterDirectionAsAntennaHeading)

        s.draft.apply()

        assertTrue(s.prefs.isQtfFilterDirectionAsAntennaHeadingEnabled())
    }

    @Test
    fun `explicit QTF heading opt out survives primary rotator changes`() {
        val s = Session()
        s.state.stn_useQtfFilterDirectionAsAntennaHeading = false
        s.state.stn_pstRotatorEnabled = true
        s.state.stn_pstRotatorEnabled = false

        assertFalse(s.state.stn_useQtfFilterDirectionAsAntennaHeading)

        s.draft.apply()

        assertFalse(s.prefs.isQtfFilterDirectionAsAntennaHeadingEnabled())
    }

    @Test
    fun `rotator 2 does not disable QTF filter heading synchronization`() {
        val s = Session()

        s.state.stn_useQtfFilterDirectionAsAntennaHeading = true
        s.state.stn_pstRotator2Enabled = true
        s.draft.apply()

        assertTrue(s.prefs.isStn_useQtfFilterDirectionAsAntennaHeading())
        assertTrue(s.prefs.isQtfFilterDirectionAsAntennaHeadingEnabled())
    }

    @Test
    fun `a discarded station setting leaves the preferences alone`() {
        val s = Session()
        s.prefs.setStn_bandActive144(true)

        s.state.stn_bandActive144 = false
        s.state.stn_bandActive432 = true
        s.draft.discard()

        assertTrue(s.prefs.isStn_bandActive144())
        assertFalse(s.prefs.isStn_bandActive432())
        assertTrue(s.state.stn_bandActive144, "after discarding the tab shows the stored value")
    }

    /*
     * The credentials stay immediate: they are locked while a session exists, and without
     * one Connect must use what the tab shows.
     */
    @Test
    fun `the login credentials still write through at once`() {
        val s = Session()

        s.state.stn_loginCallSign = "DO5AMF"
        s.state.stn_loginLocatorMainCat = "JO51IJ"
        s.state.stn_loginPassword = "secret"

        assertEquals("DO5AMF", s.prefs.getStn_loginCallSign())
        assertEquals("JO51IJ", s.prefs.getStn_loginLocatorMainCat())
        assertEquals("secret", s.prefs.getStn_loginPassword())
        assertFalse(s.draft.hasPendingChanges, "a credential went into the draft")
    }

    @Test
    fun `a band toggle round trips`() {
        val s = Session()

        s.state.stn_bandActive144 = true
        s.state.stn_bandActive432 = false
        s.draft.apply()

        assertTrue(s.prefs.isStn_bandActive144())
        assertEquals(false, s.prefs.isStn_bandActive432())
        assertTrue(s.state.stn_bandActive144)
    }

    @Test
    fun `selecting the main category writes both places it is held`() {
        val s = Session()
        val category = ChatCategory(2)

        s.state.selectMainCategory(category)

        assertEquals(2, s.prefs.getLoginChatCategoryMain().categoryNumber,
            "the stored setting must follow the selection")
        assertEquals(2, s.mainApplied?.categoryNumber,
            "so must the running session; the JavaFX window wrote both, and the "
                + "settings coverage test cannot see the ChatController write")
    }

    @Test
    fun `no second category is expressed as null`() {
        val s = Session()

        s.state.selectSecondCategory(null)

        assertNull(s.prefs.getLoginChatCategorySecond(),
            "the JavaFX window wrote null for 'no second chat'")
        assertTrue(s.secondWasApplied, "the running session must learn about it too")
        assertFalse(s.draft.hasPendingChanges, "a chat category went into the draft")
    }

    @Test
    fun `the second chat switch lives with the login settings`() {
        // It moved here from the GUI tab: the JavaFX checkbox stood on the Station tab
        // next to the category pickers, and it decides what the Connect button does.
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        val draft = SettingsDraft()
        val state = StationTabState(prefs, draft, { }, { })

        state.loginToSecondChatEnabled = true
        draft.apply()

        assertTrue(prefs.isLoginToSecondChatEnabled())
        assertEquals(true, state.loginToSecondChatEnabled)
    }

    @Test
    fun `the own height above sea level reads empty while the elevation model decides`() {
        val s = Session()

        assertEquals("", s.state.stn_pathAnalysisOwnHeightAslText)

        assertNull(s.state.commitOwnHeightAsl("312,5"))
        assertTrue(s.prefs.getStn_pathAnalysisOwnHeightAslMeters().isNaN(), "a commit took effect before Apply")
        assertEquals(312.5, s.state.stn_pathAnalysisOwnHeightAslMeters, 1e-9)
        assertEquals("312,5", s.state.stn_pathAnalysisOwnHeightAslText, "the field keeps the committed spelling")

        s.draft.apply()
        assertEquals(312.5, s.prefs.getStn_pathAnalysisOwnHeightAslMeters(), 1e-9)
        assertEquals("312,5", s.state.stn_pathAnalysisOwnHeightAslText)

        assertNull(s.state.commitOwnHeightAsl("  "))
        s.draft.apply()
        assertTrue(s.prefs.getStn_pathAnalysisOwnHeightAslMeters().isNaN(), "clearing hands it back to the model")
    }

    @Test
    fun `a discarded own height shows the stored value again`() {
        val s = Session()
        s.prefs.setStn_pathAnalysisOwnHeightAslMeters(400.0)

        assertNull(s.state.commitOwnHeightAsl("250"))
        assertEquals("250", s.state.stn_pathAnalysisOwnHeightAslText)

        s.draft.discard()

        assertEquals("400.0", s.state.stn_pathAnalysisOwnHeightAslText, "the discarded spelling is still shown")
        assertEquals(400.0, s.prefs.getStn_pathAnalysisOwnHeightAslMeters(), 1e-9)
    }

    @Test
    fun `an own height that does not parse or is out of range is not staged`() {
        val s = Session()
        assertNull(s.state.commitOwnHeightAsl("250"))

        listOf("25x", "9500", "-600", "NaN").forEach { entry ->
            val refusal = s.state.commitOwnHeightAsl(entry)
            assertTrue(refusal != null && refusal.contains(entry), "no refusal naming \"$entry\": $refusal")
            assertEquals(250.0, s.state.stn_pathAnalysisOwnHeightAslMeters, 1e-9, "\"$entry\" was staged")
        }

        s.draft.apply()
        assertEquals(250.0, s.prefs.getStn_pathAnalysisOwnHeightAslMeters(), 1e-9)
    }

    @Test
    fun `an own height draft is unchanged when it names the stored number`() {
        assertTrue(StationTabState.isOwnHeightAslUnchanged("250", "250.0"))
        assertTrue(StationTabState.isOwnHeightAslUnchanged("250,0", "250.0"))
        assertTrue(StationTabState.isOwnHeightAslUnchanged(" ", ""))
        assertFalse(StationTabState.isOwnHeightAslUnchanged("", "250.0"))
        assertFalse(StationTabState.isOwnHeightAslUnchanged("250", ""))
        assertFalse(StationTabState.isOwnHeightAslUnchanged("12.5", "12.0"))
        assertFalse(StationTabState.isOwnHeightAslUnchanged("9500", "250.0"), "an unparsable draft must be committed and refused")
    }

    @Test
    fun `parsing the own height`() {
        assertTrue(StationTabState.parseOwnHeightAsl("")!!.isNaN())
        assertEquals(-430.0, StationTabState.parseOwnHeightAsl("-430"))
        assertEquals(8848.0, StationTabState.parseOwnHeightAsl(" 8848 "))
        assertNull(StationTabState.parseOwnHeightAsl("9000.5"))
        assertNull(StationTabState.parseOwnHeightAsl("-"))
        assertNull(StationTabState.parseOwnHeightAsl("NaN"))
    }

    @Test
    fun `the highest point switch waits in the draft until it is applied`() {
        val s = Session()
        assertTrue(s.state.stn_pathAnalysisDxHighestPointInSquare, "on by default")

        s.state.stn_pathAnalysisDxHighestPointInSquare = false
        assertTrue(s.prefs.isStn_pathAnalysisDxHighestPointInSquare(), "the switch took effect before Apply")
        assertFalse(s.state.stn_pathAnalysisDxHighestPointInSquare)

        s.draft.apply()
        assertFalse(s.prefs.isStn_pathAnalysisDxHighestPointInSquare())
    }
}
