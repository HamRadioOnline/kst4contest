package kst4contest.view.compose.tabs

import kst4contest.controller.ChatController
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.ImmediateDraft
import kst4contest.view.compose.SettingsDraft
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class BeaconTabStateTest(@TempDir private val tempDir: Path) {

    /*
     * The rules are pinned with a draft that applies at once; what the draft changes is
     * pinned by the tests that pass a real one.
     */
    private inner class Session(
        private val templateRefusal: String? = null,
        val draft: SettingsDraft = ImmediateDraft(),
    ) {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        var timerRestarts = 0
        var validated = mutableListOf<String>()

        fun state() = BeaconTabState(
            prefs,
            draft,
            validateBeaconTemplate = { template ->
                validated.add(template)
                templateRefusal?.let { throw IllegalArgumentException(it) }
            },
            restartBeaconTimer = { timerRestarts++ },
            mainCategoryName = "144 MHz and up",
            secondCategoryName = "50 MHz",
        )
    }

    @Test
    fun `enable changes immediately while template and interval wait for apply`() {
        val s = Session(draft = SettingsDraft())
        s.prefs.setBcn_beaconIntervalInMinutesMainCat(5)
        s.prefs.setBcn_beaconsEnabledMainCat(false)
        val state = s.state()

        state.bcn_beaconsEnabledMainCat = true
        assertNull(state.commitBeaconTextMainCat("CQ MYCALL"))
        assertNull(state.commitBeaconInterval("7"))

        assertTrue(s.prefs.isBcn_beaconsEnabledMainCat(), "the runtime switch did not take effect immediately")
        assertEquals(5, s.prefs.getBcn_beaconIntervalInMinutesMainCat())
        assertEquals(0, s.timerRestarts, "the timer restarted before Apply")
        assertEquals(7, state.beaconIntervalMinutes, "the tab shows the edit")
        assertEquals("CQ MYCALL", state.beaconTextMainCat)

        s.draft.apply()

        assertTrue(s.prefs.isBcn_beaconsEnabledMainCat())
        assertEquals("CQ MYCALL", s.prefs.getBcn_beaconTextMainCat())
        assertEquals(7, s.prefs.getBcn_beaconIntervalInMinutesSecondCat())
        assertEquals(1, s.timerRestarts)
    }

    @Test
    fun `enable changes do not become pending draft edits`() {
        val s = Session(draft = SettingsDraft())
        s.prefs.setBcn_beaconsEnabledMainCat(false)
        val state = s.state()

        state.bcn_beaconsEnabledMainCat = true

        assertTrue(s.prefs.isBcn_beaconsEnabledMainCat())
        assertFalse(s.draft.hasPendingChanges)
    }

    @Test
    fun `discarded beacon edits leave the timer and the templates alone`() {
        val s = Session(draft = SettingsDraft())
        s.prefs.setBcn_beaconTextMainCat("CQ MYCALL")
        val state = s.state()

        state.commitBeaconTextMainCat("CQ MYCALL MYQRG")
        state.commitBeaconInterval("9")
        s.draft.discard()

        assertEquals("CQ MYCALL", s.prefs.getBcn_beaconTextMainCat())
        assertEquals(0, s.timerRestarts)
    }

    @Test
    fun `the enable flags reach the preferences`() {
        val s = Session()
        val state = s.state()

        state.bcn_beaconsEnabledMainCat = true
        state.bcn_beaconsEnabledSecondCat = true

        assertTrue(s.prefs.isBcn_beaconsEnabledMainCat())
        assertTrue(s.prefs.isBcn_beaconsEnabledSecondCat())

        state.bcn_beaconsEnabledSecondCat = false
        assertFalse(s.prefs.isBcn_beaconsEnabledSecondCat())
        assertTrue(s.prefs.isBcn_beaconsEnabledMainCat())
    }

    @Test
    fun `a valid beacon template is validated and stored verbatim`() {
        val s = Session()
        val state = s.state()
        val template = "CQ MYCALL MYLOC QRV MYQRG"

        assertNull(state.commitBeaconTextMainCat(template))

        assertEquals(template, s.prefs.getBcn_beaconTextMainCat(),
            "MYCALL and MYQRG are resolved by MessageVariableResolver; "
                + "any rewriting would break that silently")
        assertEquals(listOf(template), s.validated)
    }

    @Test
    fun `the second category has its own template`() {
        val s = Session()
        val state = s.state()

        state.commitBeaconTextSecondCat("CQ MYCALL on MYQRG")

        assertEquals("CQ MYCALL on MYQRG", s.prefs.getBcn_beaconTextSecondCat())
        assertEquals("Hi, pse call us", s.prefs.getBcn_beaconTextMainCat(),
            "the main-category template is untouched")
    }

    @Test
    fun `a refused template is reported and the stored one stands`() {
        val s = Session("Message too long")
        s.prefs.setBcn_beaconTextMainCat("CQ MYCALL")
        val state = s.state()

        val refusal = state.commitBeaconTextMainCat("x".repeat(500))

        assertNotNull(refusal)
        assertEquals("CQ MYCALL", s.prefs.getBcn_beaconTextMainCat(),
            "the JavaFX field restored the previous template on an alert")
    }

    @Test
    fun `the shared interval never reads below the minimum`() {
        val s = Session()
        s.prefs.setBcn_beaconIntervalInMinutesMainCat(0)

        assertEquals(ChatController.MIN_BEACON_INTERVAL_MINUTES, s.state().beaconIntervalMinutes)
    }

    @Test
    fun `a valid interval is written to both categories and restarts the timer`() {
        val s = Session()
        val state = s.state()

        assertNull(state.commitBeaconInterval("7"))

        assertEquals(7, s.prefs.getBcn_beaconIntervalInMinutesMainCat())
        assertEquals(7, s.prefs.getBcn_beaconIntervalInMinutesSecondCat(),
            "the legacy second-category value stays synchronized with the shared interval")
        assertEquals(1, s.timerRestarts)
    }

    @Test
    fun `an interval below the minimum is refused and the timer is left alone`() {
        val s = Session()
        s.prefs.setBcn_beaconIntervalInMinutesMainCat(5)
        val state = s.state()

        assertNotNull(state.commitBeaconInterval("0"))

        assertEquals(5, s.prefs.getBcn_beaconIntervalInMinutesMainCat())
        assertEquals(0, s.timerRestarts,
            "restarting the timer per rejected keystroke would disturb the cooldown")
    }

    /** Issue #171: one minute was allowed and was what the template shipped. */
    @Test
    fun `an interval below five minutes is refused and five is accepted`() {
        val s = Session()
        s.prefs.setBcn_beaconIntervalInMinutesMainCat(9)
        val state = s.state()

        assertNotNull(state.commitBeaconInterval("4"))
        assertEquals(9, state.beaconIntervalMinutes)

        assertNull(state.commitBeaconInterval("5"))
        assertEquals(5, state.beaconIntervalMinutes)
    }

    @Test
    fun `an interval that is not a whole number is refused`() {
        val s = Session()
        s.prefs.setBcn_beaconIntervalInMinutesMainCat(5)
        val state = s.state()

        assertNotNull(state.commitBeaconInterval("2,5"))

        assertEquals(5, s.prefs.getBcn_beaconIntervalInMinutesMainCat())
    }

    @Test
    fun `the category names are the labels the tab shows`() {
        val s = Session()

        assertEquals("144 MHz and up", s.state().mainCategoryName)
        assertEquals("50 MHz", s.state().secondCategoryName)
    }
}
