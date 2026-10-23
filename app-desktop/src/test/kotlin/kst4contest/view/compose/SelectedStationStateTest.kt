package kst4contest.view.compose

import kst4contest.model.ChatMember
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * The panel below the station list, for the station currently selected.
 *
 * Its nine "tag not qrv" marks are written back to the station and propagated to the
 * active members, so getting one wrong hides a station from every list that filters on
 * it. The sked controls create a real reminder. Both are worth pinning.
 */
class SelectedStationStateTest {

    @TempDir
    lateinit var tempDir: Path

    private class Session {
        val propagated = mutableListOf<String>()
        var skedsCreated = 0
        var lastSkedMinutes: Int? = null
        var lastSkedMode: String? = null
        val failMarked = mutableListOf<String>()
        val failReset = mutableListOf<String>()

        fun state() = SelectedStationState(
            propagateNotQrv = { propagated.add(it.callSign.orEmpty()) },
            createSked = { _, minutes, _, mode ->
                skedsCreated++
                lastSkedMinutes = minutes
                lastSkedMode = mode
                true
            },
            markSkedFail = { failMarked.add(it.callSign.orEmpty()) },
            resetSkedFail = { failReset.add(it.callSign.orEmpty()) },
        )
    }

    private fun member(call: String) = ChatMember().apply { callSign = call }

    @Test
    fun turnAntennaSendsTheSelectedStationsHeading() {
        val headings = mutableListOf<Double>()
        val state = SelectedStationState(
            propagateNotQrv = { },
            createSked = { _, _, _, _ -> true },
            turnAntennaTo = { headings.add(it) },
        )
        val station = member("DL1ABC")
        station.setQTFdirection(145.7)
        state.select(station)

        assertTrue(state.canTurnAntenna)
        state.turnAntenna()

        assertEquals(listOf(145.7), headings)
    }

    @Test
    fun turnAntennaIsUnavailableWithoutARotatorOrAHeading() {
        val withoutRotator = SelectedStationState(propagateNotQrv = { }, createSked = { _, _, _, _ -> true })
        withoutRotator.select(member("DL1ABC").apply { setQTFdirection(145.7) })
        assertFalse(withoutRotator.canTurnAntenna)

        val headings = mutableListOf<Double>()
        val withoutHeading = SelectedStationState(
            propagateNotQrv = { },
            createSked = { _, _, _, _ -> true },
            turnAntennaTo = { headings.add(it) },
        )
        withoutHeading.select(member("DL1ABC"))
        assertFalse(withoutHeading.canTurnAntenna)
        withoutHeading.turnAntenna()
        assertTrue(headings.isEmpty())
    }

    @Test
    fun `without a selection there is nothing to act on`() {
        val state = Session().state()

        assertNull(state.selected)
        assertFalse(state.canCreateSked, "a sked needs a station to be with")
        assertFalse(state.canLookUp, "there is no callsign to look up")
    }

    @Test
    fun `tagging a band writes it to the station and propagates`() {
        val session = Session()
        val state = session.state()
        val dn9apw = member("DN9APW")
        state.select(dn9apw)

        state.tagNotQrv(NotQrvBand.B_144, true)

        assertFalse(dn9apw.isQrv144, "not QRV means the QRV flag goes false")
        assertEquals(listOf("DN9APW"), session.propagated,
            "every list filtering on QRV has to learn about it")
    }

    @Test
    fun `tag not qrv all covers every band`() {
        val session = Session()
        val state = session.state()
        val member = member("DO5AMF")
        state.select(member)

        state.tagNotQrvAll(true)

        NotQrvBand.entries.forEach { band ->
            assertTrue(state.isTaggedNotQrv(band), "band ${band.label}")
        }
        assertFalse(member.isQrv50)
        assertFalse(member.isQrv10G)
    }

    @Test
    fun `clearing tag not qrv all puts every band back`() {
        val state = Session().state()
        val member = member("DO5AMF")
        state.select(member)
        state.tagNotQrvAll(true)

        state.tagNotQrvAll(false)

        NotQrvBand.entries.forEach { band ->
            assertFalse(state.isTaggedNotQrv(band), "band ${band.label}")
        }
        assertTrue(member.isQrv144)
    }

    @Test
    fun `selecting another station shows that station's tags`() {
        val state = Session().state()
        val tagged = member("DN9APW").apply { isQrv432 = false }
        val untagged = member("DO5AMF")

        state.select(tagged)
        assertTrue(state.isTaggedNotQrv(NotQrvBand.B_432))

        state.select(untagged)
        assertFalse(state.isTaggedNotQrv(NotQrvBand.B_432),
            "the panel shows the selected station, not the one before it")
    }

    @Test
    fun `a sked needs a selection and is created once`() {
        val session = Session()
        val state = session.state()
        state.select(member("DN9APW"))
        state.skedMinutes = 10

        assertTrue(state.canCreateSked)
        assertTrue(state.createSked())

        assertEquals(1, session.skedsCreated)
        assertEquals(10, session.lastSkedMinutes)
    }

    @Test
    fun `creating a sked without a selection does nothing`() {
        val session = Session()
        val state = session.state()

        assertFalse(state.createSked())
        assertEquals(0, session.skedsCreated)
    }

    @Test
    fun `the message filter is one choice, not four`() {
        val state = Session().state()

        state.messageFilter = SelectedMessageFilter.PM_TO_ME
        assertEquals(SelectedMessageFilter.PM_TO_ME, state.messageFilter)

        state.messageFilter = SelectedMessageFilter.PUBLIC
        assertEquals(SelectedMessageFilter.PUBLIC, state.messageFilter,
            "the JavaFX radio buttons were one toggle group; two cannot be on")
    }

    @Test
    fun `public messages can be the configured initial radio mode`() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml")).apply {
            isGuiOptions_defaultFilterPublicMsgs = true
        }

        assertEquals(SelectedMessageFilter.PUBLIC, SelectedMessageFilter.fromPreferences(prefs))
    }

    /**
     * No filter is chosen until the operator chooses one. JavaFX has the
     * setSelected(true) on "pm to me" commented out, so the group starts with nothing on,
     * and starting on a filter would silently hide messages the operator never asked to
     * hide.
     */
    @Test
    fun `no message filter is chosen to begin with`() {
        assertNull(Session().state().messageFilter)
    }

    @Test
    fun `the lookup addresses carry the selected callsign`() {
        val state = Session().state()
        state.select(member("DN9APW"))

        assertTrue(state.qrzComUrl().endsWith("DN9APW"))
        assertTrue(state.qrzCqUrl().endsWith("DN9APW"))
    }

    @Test
    fun `the sked offers the minutes the JavaFX choice offered`() {
        assertEquals(
            listOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 20),
            SelectedStationState.SKED_MINUTES,
        )
        assertEquals(5, Session().state().skedMinutes, "the default it selected")
    }

    @Test
    fun `the sked offers the two modes`() {
        assertEquals(listOf("SSB", "CW"), SelectedStationState.SKED_MODES)
    }

    @Test
    fun `the reminder offers the three offset patterns`() {
        assertEquals(listOf("2+1", "5+2+1", "10+5+2+1"), SelectedStationState.REMINDER_OFFSETS)
    }

    @Test
    fun `the chosen mode reaches the sked`() {
        val session = Session()
        val state = session.state()
        state.select(member("DN9APW"))
        state.skedMode = "CW"

        state.createSked()

        assertEquals("CW", session.lastSkedMode)
    }

    @Test
    fun `marking a sked as failed names the selected station`() {
        val session = Session()
        val state = session.state()
        state.select(member("DN9APW"))

        state.markSkedFail()

        assertEquals(listOf("DN9APW"), session.failMarked)
    }

    @Test
    fun `resetting the failure names it too`() {
        val session = Session()
        val state = session.state()
        state.select(member("DO5AMF"))

        state.resetSkedFail()

        assertEquals(listOf("DO5AMF"), session.failReset)
    }

    @Test
    fun `marking a failure without a selection does nothing`() {
        val session = Session()

        session.state().markSkedFail()

        assertTrue(session.failMarked.isEmpty())
    }

    @Test
    fun `antenna 2 is offered only with SO2R and turns rotator 2`() {
        val headings2 = mutableListOf<Double>()
        var offered = false
        val state = SelectedStationState(
            propagateNotQrv = { },
            createSked = { _, _, _, _ -> true },
            turnAntennaTo = { },
            turnSecondAntennaTo = { headings2.add(it) },
            secondAntennaOffered = { offered },
        )
        state.select(member("DL1ABC").apply { setQTFdirection(145.7) })

        assertFalse(state.showsSecondAntenna)
        offered = true
        assertTrue(state.showsSecondAntenna)
        assertTrue(state.canTurnSecondAntenna)

        state.turnSecondAntenna()

        assertEquals(listOf(145.7), headings2)
    }

    @Test
    fun `antenna 2 is greyed out without a heading`() {
        val state = SelectedStationState(
            propagateNotQrv = { },
            createSked = { _, _, _, _ -> true },
            turnSecondAntennaTo = { },
            secondAntennaOffered = { true },
        )
        state.select(member("DL1ABC"))

        assertTrue(state.showsSecondAntenna)
        assertFalse(state.canTurnSecondAntenna)
    }

    @Test
    fun `antenna 2 offer follows the surroundings flag without a new selection`() {
        val surroundings = MainWindowSurroundings()
        val state = SelectedStationState(
            propagateNotQrv = { },
            createSked = { _, _, _, _ -> true },
            turnSecondAntennaTo = { },
            secondAntennaOffered = { surroundings.secondAntennaOffered },
        )
        state.select(member("DL1ABC"))

        assertFalse(state.showsSecondAntenna)
        surroundings.secondAntennaOffered = true
        assertTrue(state.showsSecondAntenna)
    }
}
