package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.model.Band
import kst4contest.model.ChatMember
import kst4contest.model.ChatPreferences

/**
 * The bands a station can be tagged as not being on, under the labels the panel shows.
 *
 * @param read whether the station is QRV on this band
 * @param write sets that flag
 */
enum class NotQrvBand(
    val label: String,
    val band: kst4contest.model.Band,
    val read: (ChatMember) -> Boolean,
    val write: (ChatMember, Boolean) -> Unit,
) {
    B_50("50", kst4contest.model.Band.B_50, { it.isQrv50 }, { m, qrv -> m.isQrv50 = qrv }),
    B_70("70", kst4contest.model.Band.B_70, { it.isQrv70 }, { m, qrv -> m.isQrv70 = qrv }),
    B_144("144", kst4contest.model.Band.B_144, { it.isQrv144 }, { m, qrv -> m.isQrv144 = qrv }),
    B_432("432", kst4contest.model.Band.B_432, { it.isQrv432 }, { m, qrv -> m.isQrv432 = qrv }),
    B_23("23cm", kst4contest.model.Band.B_1296, { it.isQrv1240 }, { m, qrv -> m.isQrv1240 = qrv }),
    B_13("13cm", kst4contest.model.Band.B_2320, { it.isQrv2300 }, { m, qrv -> m.isQrv2300 = qrv }),
    B_9("9cm", kst4contest.model.Band.B_3400, { it.isQrv3400 }, { m, qrv -> m.isQrv3400 = qrv }),
    B_6("6cm", kst4contest.model.Band.B_5760, { it.isQrv5600 }, { m, qrv -> m.isQrv5600 = qrv }),
    B_3("3cm", kst4contest.model.Band.B_10G, { it.isQrv10G }, { m, qrv -> m.isQrv10G = qrv }),
}

/**
 * Which messages the panel lists for the selected station.
 *
 * One choice, not four switches: the JavaFX controls were a toggle group, so two can
 * never be on at once.
 */
enum class SelectedMessageFilter(val label: String) {
    PM_TO_ME("pm to me"),
    PM_TO_OTHER("pm to other"),
    PUBLIC("public msgs"),
    NOTHING("nothing");

    companion object {
        @JvmStatic
        fun fromPreferences(prefs: ChatPreferences): SelectedMessageFilter = when {
            prefs.isGuiOptions_defaultFilterPmToMe -> PM_TO_ME
            prefs.isGuiOptions_defaultFilterPmToOther -> PM_TO_OTHER
            prefs.isGuiOptions_defaultFilterPublicMsgs -> PUBLIC
            else -> NOTHING
        }
    }
}

/**
 * The panel below the station list, for whichever station is selected.
 *
 * Its "tag not qrv" marks are not display state: they are written into the station and
 * propagated to the active members, so a wrong one hides that station from every list
 * filtering on QRV. That is why they go through the controller rather than being kept
 * here.
 *
 * @param propagateNotQrv hands the changed station to the controller, which is what
 *        `propagateNotQrvStateToActiveMembers` does
 * @param createSked takes the station, the minutes, the band and the mode; returns
 *        whether it was accepted
 * @param turnAntennaTo turns the antenna to an azimuth in degrees, which is what
 *        `ChatController.rotateTo` does; null where no rotator can be driven
 * @param turnSecondAntennaTo the same for antenna 2 (rotator 2); null where it cannot be driven
 * @param secondAntennaOffered whether SO2R with rotator 2 is configured right now; called on
 *        every draw, so the supplied lambda has to read Compose state for the button to
 *        follow a settings Apply
 */
class SelectedStationState @JvmOverloads constructor(
    private val propagateNotQrv: (ChatMember) -> Unit,
    private val createSked: (ChatMember, Int, Band?, String?) -> Boolean,
    private val markSkedFail: (ChatMember) -> Unit = { },
    private val resetSkedFail: (ChatMember) -> Unit = { },
    private val onMessageFilterChange: (SelectedMessageFilter?) -> Unit = { },
    private val detectBands: (ChatMember) -> String = { "" },
    private val resolveDefaultSkedBand: (ChatMember) -> Band? = { null },
    private val turnAntennaTo: ((Double) -> Unit)? = null,
    private val turnSecondAntennaTo: ((Double) -> Unit)? = null,
    private val secondAntennaOffered: () -> Boolean = { false },
    val stationSelection: StationSelectionState = StationSelectionState(),
) {

    val selected: ChatMember?
        get() = stationSelection.selected

    /** Bumped on a tag so the panel redraws; the flags live on the station itself. */
    private var tagRevision: Int by mutableStateOf(0)

    var skedMinutes: Int by mutableStateOf(DEFAULT_SKED_MINUTES)

    var skedBand: Band? by mutableStateOf(null)

    var skedMode: String? by mutableStateOf(null)

    private var _messageFilter by mutableStateOf<SelectedMessageFilter?>(null)

    /**
     * Which messages the panel follows, or null until the operator picks one.
     *
     * JavaFX has the setSelected(true) on "pm to me" commented out, so its toggle group
     * starts with nothing on. Starting on a filter would silently hide messages nobody
     * asked to hide.
     */
    var messageFilter: SelectedMessageFilter?
        get() = _messageFilter
        set(value) {
            _messageFilter = value
            onMessageFilterChange(value)
        }

    var remindPm: Boolean by mutableStateOf(false)

    var reminderOffsets: String by mutableStateOf(REMINDER_OFFSETS.first())

    val canCreateSked: Boolean
        get() = selected != null

    val canLookUp: Boolean
        get() = !selected?.callSign.isNullOrBlank()

    /** Needs a rotator to drive and a finite heading to the selected station. */
    val canTurnAntenna: Boolean
        get() = turnAntennaTo != null && selected?.getQTFdirection()?.isFinite() == true

    /** Turns the antenna to the selected station's heading; does nothing when it cannot. */
    fun turnAntenna() {
        val heading = selected?.getQTFdirection() ?: return
        if (heading.isFinite()) turnAntennaTo?.invoke(heading)
    }

    /** SO2R with rotator 2 is configured, so the second turn button is offered at all. */
    val showsSecondAntenna: Boolean
        get() = turnSecondAntennaTo != null && secondAntennaOffered()

    /** Needs the button to be offered and a finite heading to the selected station. */
    val canTurnSecondAntenna: Boolean
        get() = showsSecondAntenna && selected?.getQTFdirection()?.isFinite() == true

    /** Turns antenna 2 to the selected station's heading; does nothing when it cannot. */
    fun turnSecondAntenna() {
        val heading = selected?.getQTFdirection() ?: return
        if (heading.isFinite() && showsSecondAntenna) turnSecondAntennaTo?.invoke(heading)
    }

    @JvmOverloads
    fun select(
        member: ChatMember?,
        source: StationSelectionSource = StationSelectionSource.PROGRAMMATIC,
    ) {
        if (StationIdentity.of(selected) != StationIdentity.of(member)) {
            skedBand = member?.let { resolveDefaultSkedBand(it) }
        }
        stationSelection.select(member, source)
    }

    fun isTaggedNotQrv(band: NotQrvBand): Boolean {
        @Suppress("UNUSED_EXPRESSION") tagRevision

        val member = selected ?: return false

        return !band.read(member)
    }

    /** Tagged means not QRV, so the station's QRV flag goes the other way. */
    fun tagNotQrv(band: NotQrvBand, tagged: Boolean) {
        val member = selected ?: return

        band.write(member, !tagged)
        tagRevision++
        propagateNotQrv(member)
    }

    fun tagNotQrvAll(tagged: Boolean) {
        val member = selected ?: return

        NotQrvBand.entries.forEach { it.write(member, !tagged) }
        tagRevision++
        propagateNotQrv(member)
    }

    /** @return whether a sked was created; false when nothing is selected. */
    fun createSked(): Boolean {
        val member = selected ?: return false

        return createSked(member, skedMinutes, skedBand, skedMode)
    }

    /**
     * Marks the path to this station as failed.
     *
     * Permanent until reset, and it lowers the priority score sharply — so it is an
     * answer to "I called and heard nothing", not to "not now".
     */
    fun markSkedFail() {
        selected?.let(markSkedFail)
    }

    fun resetSkedFail() {
        selected?.let(resetSkedFail)
    }

    fun detectedBands(): String {
        val member = selected ?: return ""
        val ignored = tagRevision
        return detectBands(member)
    }


    fun qrzComUrl(): String = QRZ_COM + selected?.callSign.orEmpty()

    fun qrzCqUrl(): String = QRZ_CQ + selected?.callSign.orEmpty()

    companion object {
        /** The minutes the JavaFX choice offered, and the one it started on. */
        val SKED_MINUTES = listOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 20)

        val SKED_MODES = listOf("SSB", "CW")

        /** How long before a sked the reminders come, in minutes. */
        val REMINDER_OFFSETS = listOf("2+1", "5+2+1", "10+5+2+1")

        private const val DEFAULT_SKED_MINUTES = 5
        private const val QRZ_COM = "https://www.qrz.com/db/"
        private const val QRZ_CQ = "https://www.qrzcq.com/call/"
    }
}
