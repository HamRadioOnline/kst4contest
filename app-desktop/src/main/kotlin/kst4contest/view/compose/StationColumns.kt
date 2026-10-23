package kst4contest.view.compose

import kst4contest.model.ChatMember
import java.util.EnumSet

/**
 * The columns of the station list.
 *
 * The ids are not free to choose: they are the operator's stored column widths, so
 * renaming one loses their layout without a word. They are kept exactly as
 * `TableLayoutManager.column(...)` registered them.
 *
 * Every value survives a half-filled station. These rows come off the wire, a field
 * can be missing, and a crash in the station list during a contest is the worst
 * possible moment for one.
 *
 * @param tropoOf the reachability the controller computes; not carried by the row
 * @param priorityScoreOf the score the row is coloured by, also computed outside
 * @param formatBand formats a single band cell (worked / new / etc.)
 * @param activeBands the bands the operator has enabled in settings; only these
 *        get a worked column. Matches the JavaFX behaviour where the column is
 *        only added when `isStn_bandActiveXX()` returns true.
 */
object StationColumns {

    /** The heading the ten worked columns sit under. */
    const val WORKED_GROUP = "worked"

    /**
     * The identity a station row is drawn and selected under: the callsign and the chat
     * category. The category is part of it because the same callsign can be in two chats at
     * once, and AGENTS.md makes the category part of the chat identity; keyed by callsign
     * alone, the two rows collided (fixed in 21251377).
     *
     * Kept here rather than inline at the one call site so the station list can be tested
     * with the key it really uses. The text is exactly what the inline Java expression
     * produced, including "null" for a missing callsign.
     */
    fun rowKey(member: ChatMember): String =
        "${member.callSign}|${member.chatCategory?.categoryNumber ?: ""}"

    fun all(
        tropoOf: (ChatMember) -> String,
        priorityScoreOf: (ChatMember) -> Double,
        formatBand: (ChatMember, kst4contest.model.Band, Boolean) -> String,
        activeBands: EnumSet<kst4contest.model.Band> = EnumSet.allOf(kst4contest.model.Band::class.java),
    ): List<DataColumn<ChatMember>> = buildList {
        add(DataColumn(
            id = "callsign",
            title = { kst4contest.view.i18n.CurrentStrings.get().columnCallsign },
            value = { it.callSign.orEmpty() },
            comparator = compareBy(String.CASE_INSENSITIVE_ORDER) { member: ChatMember ->
                member.callSign?.trim().orEmpty()
            },
        ))
        add(DataColumn(id = "name", title = { kst4contest.view.i18n.CurrentStrings.get().columnName }, value = { it.name.orEmpty() }))
        add(DataColumn(id = "qra", title = "QRA", value = { it.qra.orEmpty() }, weight = 0.8f))
        add(DataColumn(
            id = "qrb",
            title = "QRB",
            value = { it.qrb?.toInt()?.toString().orEmpty() },
            comparator = compareBy { it.qrb ?: 0.0 },
            weight = 0.6f,
        ))
        add(DataColumn(
            id = "qtf",
            title = "QTF",
            value = { it.getQTFdirection()?.let { d -> String.format("%.2f°", d) }.orEmpty() },
            comparator = compareBy { it.getQTFdirection() ?: 0.0 },
            weight = 0.7f,
        ))
        add(DataColumn(id = "qrg", title = "QRG", value = { it.frequency?.get().orEmpty() }, weight = 0.8f))
        add(DataColumn(
            id = "airscout",
            title = { kst4contest.view.i18n.CurrentStrings.get().columnAirplane },
            value = { AirplaneCell.format(it.airPlaneReflectInfo) },
            accent = { _, rendered -> AirplaneCell.accentFor(rendered) },
            comparator = compareBy(AirplaneCell.order) { it.airPlaneReflectInfo },
            weight = 1.4f,
        ))
        add(DataColumn(id = "tropo", title = "Tropo", value = tropoOf, weight = 0.7f))
        add(DataColumn(
            id = "score",
            title = { kst4contest.view.i18n.CurrentStrings.get().columnScore },
            value = { priorityScoreOf(it).toInt().toString() },
            comparator = compareBy { priorityScoreOf(it) },
            weight = 0.6f,
        ))
        add(DataColumn(
            id = "activity",
            title = "Act",
            value = { minutesSince(it.activityTimeLastInEpoch) },
            comparator = compareBy { it.activityTimeLastInEpoch },
            weight = 0.5f,
        ))

        /* Individual band columns only when the operator has that band enabled. */
        if (kst4contest.model.Band.B_50 in activeBands)
            add(workedColumn("band-50", "50") { formatBand(it, kst4contest.model.Band.B_50, it.isWorked50) })
        if (kst4contest.model.Band.B_70 in activeBands)
            add(workedColumn("band-70", "70") { formatBand(it, kst4contest.model.Band.B_70, it.isWorked70) })
        if (kst4contest.model.Band.B_144 in activeBands)
            add(workedColumn("band-144", "144") { formatBand(it, kst4contest.model.Band.B_144, it.isWorked144) })
        if (kst4contest.model.Band.B_432 in activeBands)
            add(workedColumn("band-432", "432") { formatBand(it, kst4contest.model.Band.B_432, it.isWorked432) })
        if (kst4contest.model.Band.B_1296 in activeBands)
            add(workedColumn("band-1296", "23") { formatBand(it, kst4contest.model.Band.B_1296, it.isWorked1240) })
        if (kst4contest.model.Band.B_2320 in activeBands)
            add(workedColumn("band-2320", "13") { formatBand(it, kst4contest.model.Band.B_2320, it.isWorked2300) })
        if (kst4contest.model.Band.B_3400 in activeBands)
            add(workedColumn("band-3400", "9") { formatBand(it, kst4contest.model.Band.B_3400, it.isWorked3400) })
        if (kst4contest.model.Band.B_5760 in activeBands)
            add(workedColumn("band-5760", "6") { formatBand(it, kst4contest.model.Band.B_5760, it.isWorked5600) })
        if (kst4contest.model.Band.B_10G in activeBands)
            add(workedColumn("band-10g", "3") { formatBand(it, kst4contest.model.Band.B_10G, it.isWorked10G) })

        /* Worked-any is always shown, and belongs under the 'worked' group header just like in JavaFX. */
        add(workedColumn("worked-any", "wkdany", group = WORKED_GROUP, weight = 0.7f) { if (it.isWorked) "X" else "" })

        add(DataColumn(id = "not-qrv", title = "NOT QRV @", value = { qrvBands(it, activeBands) }, weight = 1.6f))
        add(DataColumn(
            id = "category",
            title = { kst4contest.view.i18n.CurrentStrings.get().columnCategory },
            value = { member ->
                member.chatCategory?.let { it.getChatCategoryName(it.categoryNumber) }.orEmpty()
            },
            weight = 1.2f,
        ))
    }

    /** A cross or nothing, and under the shared heading. */
    private fun workedColumn(
        id: String,
        title: String,
        group: String? = WORKED_GROUP,
        weight: Float = 0.35f,
        worked: (ChatMember) -> String,
    ): DataColumn<ChatMember> = DataColumn(
        id = id,
        title = title,
        value = worked,
        weight = weight,
        group = group,
    )

    /** How long ago the station was last heard, in whole minutes. */
    private fun minutesSince(epochSeconds: Long): String {
        if (epochSeconds <= 0L) {
            return ""
        }

        val seconds = (System.currentTimeMillis() / 1000L) - epochSeconds

        return if (seconds < 0) "" else (seconds / 60L).toString()
    }

    /**
     * The bands the station is **not** on, which is what the heading says.
     *
     * Every QRV flag starts true — a station is assumed to be on every band until it
     * says otherwise — so this column is empty for most rows and fills as the operator
     * or the server marks bands off.
     */
    private fun qrvBands(member: ChatMember, activeBands: Set<kst4contest.model.Band>): String = buildString {
        if (!member.isQrv50 && kst4contest.model.Band.B_50 in activeBands) append("6m ")
        if (!member.isQrv70 && kst4contest.model.Band.B_70 in activeBands) append("4m ")
        if (!member.isQrv144 && kst4contest.model.Band.B_144 in activeBands) append("2m ")
        if (!member.isQrv432 && kst4contest.model.Band.B_432 in activeBands) append("70cm ")
        if (!member.isQrv1240 && kst4contest.model.Band.B_1296 in activeBands) append("23cm ")
        if (!member.isQrv2300 && kst4contest.model.Band.B_2320 in activeBands) append("13cm ")
        if (!member.isQrv3400 && kst4contest.model.Band.B_3400 in activeBands) append("9cm ")
        if (!member.isQrv5600 && kst4contest.model.Band.B_5760 in activeBands) append("6cm ")
        if (!member.isQrv10G && kst4contest.model.Band.B_10G in activeBands) append("3cm ")
    }.trim()
}
