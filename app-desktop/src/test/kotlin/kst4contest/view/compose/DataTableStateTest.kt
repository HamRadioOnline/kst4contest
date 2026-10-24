package kst4contest.view.compose

import androidx.compose.runtime.snapshots.Snapshot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * The rules of the multi-column table.
 *
 * Row identity, selection across a refresh and stable sorting are the three places
 * where Etappe 2 lost time, and all three were found by watching the window rather
 * than by a test. They are pinned here.
 */
class DataTableStateTest {

    private class Station(val callSign: String, var qrb: Int)

    private class Spot(val text: String)

    private fun stationTable() = DataTableState(
        columns = listOf(
            DataColumn(id = "callsign", title = "Callsign", value = { s: Station -> s.callSign }),
            DataColumn(
                id = "qrb",
                title = "QRB",
                value = { s: Station -> s.qrb.toString() },
                comparator = compareBy { s: Station -> s.qrb },
            ),
        ),
        rowKey = RowKeys.byValue { s: Station -> s.callSign },
    )

    @Test
    fun `an initial sort is applied once and keeps later rows in order`() {
        val table = DataTableState(
            columns = listOf(
                DataColumn(
                    id = "callsign",
                    title = "Callsign",
                    value = { station: Station -> station.callSign },
                    comparator = compareBy(String.CASE_INSENSITIVE_ORDER) { station: Station ->
                        station.callSign.trim()
                    },
                ),
            ),
            rowKey = RowKeys.byValue { station: Station -> station.callSign },
            initialSortColumnId = "callsign",
        )

        table.replaceRows(listOf(Station("do5amf", 1), Station("DL1ABC", 2)))
        assertEquals(listOf("DL1ABC", "do5amf"), table.visibleRows.map { it.callSign })

        table.replaceRows(
            listOf(Station("ZZ9ZZZ", 3), Station("do5amf", 1), Station("AA1AAA", 4), Station("DL1ABC", 2)),
        )

        assertEquals(
            listOf("AA1AAA", "DL1ABC", "do5amf", "ZZ9ZZZ"),
            table.visibleRows.map { it.callSign },
        )
    }

    @Test
    fun `a user sort replaces the initial sort and survives row and filter updates`() {
        val table = DataTableState(
            columns = listOf(
                DataColumn(id = "callsign", title = "Callsign", value = { station: Station -> station.callSign }),
                DataColumn(
                    id = "qrb",
                    title = "QRB",
                    value = { station: Station -> station.qrb.toString() },
                    comparator = compareBy { station: Station -> station.qrb },
                ),
            ),
            rowKey = RowKeys.byValue { station: Station -> station.callSign },
            initialSortColumnId = "callsign",
        )
        table.replaceRows(listOf(Station("B", 300), Station("A", 100)))

        table.toggleSort("qrb")
        table.rowFilter = { it.qrb >= 100 }
        table.replaceRows(listOf(Station("C", 200), Station("B", 300), Station("A", 100)))

        assertEquals("qrb", table.sortedColumnId)
        assertEquals(listOf("A", "C", "B"), table.visibleRows.map { it.callSign })
    }

    @Test
    fun `a selected filtered row can be pinned only in the table view`() {
        val table = DataTableState(
            columns = listOf(DataColumn(id = "callsign", title = "Callsign", value = { s: Station -> s.callSign })),
            rowKey = RowKeys.byValue { station: Station -> station.callSign },
            keepSelectionVisible = true,
        )
        val hidden = Station("DL1HIDE", 100)
        val shown = Station("DL2SHOW", 500)
        table.replaceRows(listOf(hidden, shown))
        table.rowFilter = { it.qrb >= 300 }

        table.select(hidden)

        assertEquals(listOf("DL2SHOW"), table.filteredRows.map { it.callSign }, "the shared map filter changed")
        assertEquals(
            listOf("DL1HIDE", "DL2SHOW"),
            table.visibleRows.map { it.callSign },
            "the selected station was not made reachable in the table",
        )
    }

    @Test
    fun `the same selected row can request scrolling more than once`() {
        val table = stationTable()
        val selected = Station("DL1ABC", 100)
        table.replaceRows(listOf(selected))
        table.select(selected)

        table.requestScrollToSelection()
        val first = table.scrollRequestRevision
        table.requestScrollToSelection()

        assertTrue(table.scrollRequestRevision > first)
    }

    @Test
    fun `the selection follows the row, not its position`() {
        val table = stationTable()
        val dn9apw = Station("DN9APW", 100)
        table.replaceRows(listOf(dn9apw, Station("DO5AMF", 200)))
        table.select(dn9apw)

        // Something arrives above it, as happens constantly in a live list.
        table.replaceRows(listOf(Station("DL1ABC", 50), Station("DN9APW", 100), Station("DO5AMF", 200)))

        assertEquals("DN9APW", table.selectedRow?.callSign,
            "the station the operator picked is still that station after it moved down")
    }

    @Test
    fun `a selected row that disappears clears the selection`() {
        val table = stationTable()
        val gone = Station("DN9APW", 100)
        table.replaceRows(listOf(gone, Station("DO5AMF", 200)))
        table.select(gone)

        table.replaceRows(listOf(Station("DO5AMF", 200)))

        assertNull(table.selectedRow,
            "sliding the selection onto the neighbour would point the operator at "
                + "someone they did not choose")
    }

    @Test
    fun `equal rows keep the order they arrived in`() {
        val table = stationTable()
        table.replaceRows(
            listOf(Station("A", 100), Station("B", 100), Station("C", 100)),
        )
        table.toggleSort("qrb")

        assertEquals(
            listOf("A", "B", "C"),
            table.visibleRows.map { it.callSign },
            "the sort is stable; Etappe 2 lost this and rows jumped",
        )
    }

    @Test
    fun `a column with a comparator sorts by it, not by its text`() {
        val table = stationTable()
        table.replaceRows(listOf(Station("A", 9), Station("B", 100)))

        table.toggleSort("qrb")

        assertEquals(listOf("A", "B"), table.visibleRows.map { it.callSign },
            "9 before 100 numerically; by text it would be the other way round")
    }

    @Test
    fun `a column without a comparator sorts by the shown text`() {
        val table = stationTable()
        table.replaceRows(listOf(Station("DO5AMF", 1), Station("DL1ABC", 2)))

        table.toggleSort("callsign")

        assertEquals(listOf("DL1ABC", "DO5AMF"), table.visibleRows.map { it.callSign })
    }

    @Test
    fun `clicking the sorted column again reverses it`() {
        val table = stationTable()
        table.replaceRows(listOf(Station("A", 1), Station("B", 2)))

        table.toggleSort("qrb")
        assertTrue(table.sortAscending)

        table.toggleSort("qrb")
        assertFalse(table.sortAscending)
        assertEquals(listOf("B", "A"), table.visibleRows.map { it.callSign })
    }

    /**
     * The third click clears the sorting and puts the rows back in arrival order. JavaFX
     * TableView cycles ascending, descending, unsorted for free; losing that meant an
     * operator who had sorted the message list once could never get back to "newest at
     * the bottom" without restarting.
     */
    @Test
    fun `a third click clears the sorting and restores arrival order`() {
        val table = stationTable()
        table.replaceRows(listOf(Station("B", 2), Station("A", 1)))

        table.toggleSort("qrb")
        assertEquals(listOf("A", "B"), table.visibleRows.map { it.callSign })

        table.toggleSort("qrb")
        assertEquals(listOf("B", "A"), table.visibleRows.map { it.callSign })

        table.toggleSort("qrb")
        assertNull(table.sortedColumnId)
        assertEquals(listOf("B", "A"), table.visibleRows.map { it.callSign }, "arrival order not restored")
    }

    /** After clearing, the next click on the same column starts ascending again. */
    @Test
    fun `sorting the same column after clearing starts over ascending`() {
        val table = stationTable()
        table.replaceRows(listOf(Station("B", 2), Station("A", 1)))

        table.toggleSort("qrb")
        table.toggleSort("qrb")
        table.toggleSort("qrb")
        table.toggleSort("qrb")

        assertEquals("qrb", table.sortedColumnId)
        assertTrue(table.sortAscending)
        assertEquals(listOf("A", "B"), table.visibleRows.map { it.callSign })
    }

    /** Switching to another column is a fresh ascending sort, not a continuation. */
    @Test
    fun `switching columns does not inherit the cycle`() {
        val table = stationTable()
        table.replaceRows(listOf(Station("B", 2), Station("A", 1)))

        table.toggleSort("qrb")
        table.toggleSort("qrb")
        table.toggleSort("callsign")

        assertEquals("callsign", table.sortedColumnId)
        assertTrue(table.sortAscending)
    }

    @Test
    fun `an unknown column is ignored rather than clearing the sorting`() {
        val table = stationTable()
        table.replaceRows(listOf(Station("A", 1)))
        table.toggleSort("qrb")

        table.toggleSort("a-column-this-release-no-longer-has")

        assertEquals("qrb", table.sortedColumnId,
            "a stored layout naming a dropped column must not undo the sorting")
    }

    @Test
    fun `the sorted order is computed once, not on every read`() {
        /*
         * visibleRows is read on every recomposition. Sorting there means sorting up
         * to thirty thousand rows per inbound message, on the thread that draws, and
         * a column without a comparator would call its value function once per
         * comparison on top of that.
         */
        var valueCalls = 0
        val table = DataTableState(
            columns = listOf(
                DataColumn(
                    id = "callsign",
                    title = "Callsign",
                    value = { s: Station -> valueCalls++; s.callSign },
                ),
            ),
            rowKey = RowKeys.byValue { s: Station -> s.callSign },
        )
        table.replaceRows(listOf(Station("C", 1), Station("A", 2), Station("B", 3)))
        table.toggleSort("callsign")

        table.visibleRows
        val afterFirstRead = valueCalls
        repeat(20) { table.visibleRows }

        assertEquals(afterFirstRead, valueCalls,
            "reading the sorted rows again must not sort them again")
    }

    @Test
    fun `rows read once and then replaced are the new rows`() {
        /*
         * The regression this pins: caching the sorted order in a plain field meant
         * that once the cache was warm the getter never read `rows` again, so nothing
         * subscribed to it and the table went on showing what it had. On screen that
         * looked like a monitor window that stayed empty while the JavaFX tab beside it
         * filled up, and only redrew when an unrelated click forced it to.
         */
        val table = stationTable()
        table.replaceRows(listOf(Station("DN9APW", 1)))
        table.toggleSort("callsign")

        assertEquals(listOf("DN9APW"), table.visibleRows.map { it.callSign })

        table.replaceRows(listOf(Station("DN9APW", 1), Station("DO5AMF", 2)))

        assertEquals(listOf("DN9APW", "DO5AMF"), table.visibleRows.map { it.callSign },
            "a row that arrived after the first read has to be there")
    }

    @Test
    fun `reading the rows subscribes to them even when the order is already computed`() {
        /*
         * This is the defect a value-checking test cannot see. Compose repaints a
         * composable when state it *read* changes. A cache that returns early stops
         * reading `rows`, so the next recomposition — triggered by anything else —
         * leaves the composable subscribed to nothing, and later rows never arrive on
         * screen. That is what a monitor window sitting empty beside a filling JavaFX
         * tab looks like, redrawing only when an unrelated click forces it.
         */
        val table = stationTable()
        table.replaceRows(listOf(Station("DN9APW", 1)))
        table.toggleSort("callsign")
        table.visibleRows

        var reads = 0
        Snapshot.observe(readObserver = { reads++ }) { table.visibleRows }

        assertTrue(reads > 0,
            "a read that touches no state means the table will not repaint when the "
                + "rows change")
    }

    @Test
    fun `changing the sorting after a read re-sorts`() {
        val table = stationTable()
        table.replaceRows(listOf(Station("B", 1), Station("A", 2)))
        table.toggleSort("callsign")
        assertEquals(listOf("A", "B"), table.visibleRows.map { it.callSign })

        table.toggleSort("callsign")

        assertEquals(listOf("B", "A"), table.visibleRows.map { it.callSign })
    }

    @Test
    fun `unsorted rows stay in the order they were given`() {
        val table = stationTable()
        table.replaceRows(listOf(Station("C", 3), Station("A", 1), Station("B", 2)))

        assertEquals(listOf("C", "A", "B"), table.visibleRows.map { it.callSign })
    }

    @Test
    fun `two identical messages are two rows, not one`() {
        /*
         * The reason event lists need reference identity: a key built from their
         * fields merges them, and repeating a macro is ordinary operator behaviour.
         */
        val table = DataTableState(
            columns = listOf(DataColumn(id = "text", title = "Message", value = { s: Spot -> s.text })),
            rowKey = RowKeys.byReference(),
        )
        val first = Spot("tnx om, 73")
        val second = Spot("tnx om, 73")

        table.replaceRows(listOf(first, second))
        table.select(second)

        assertEquals(2, table.visibleRows.size)
        assertSame(second, table.selectedRow, "the second one, not the first")
        assertFalse(table.isSelected(first))
    }

    @Test
    fun `the key a row is drawn under is its identity, not its position`() {
        /*
         * The list draws rows keyed by this. A positional key would reshuffle every
         * row whenever something arrives at the top, which in a live list is constant.
         */
        val table = stationTable()
        val dn9apw = Station("DN9APW", 100)
        table.replaceRows(listOf(dn9apw, Station("DO5AMF", 200)))

        val keyAtTop = table.keyOf(dn9apw)

        table.replaceRows(listOf(Station("DL1ABC", 50), dn9apw))

        assertEquals(keyAtTop, table.keyOf(dn9apw), "same row, same key, new position")
    }

    @Test
    fun `a reference key stays with its row across a refresh`() {
        val table = DataTableState(
            columns = listOf(DataColumn(id = "text", title = "Message", value = { s: Spot -> s.text })),
            rowKey = RowKeys.byReference(),
        )
        val kept = Spot("first")
        table.replaceRows(listOf(kept))
        table.select(kept)

        // The roster hands back the same instances plus a new one.
        table.replaceRows(listOf(Spot("newer"), kept))

        assertSame(kept, table.selectedRow)
    }

    /**
     * The filter reads fields of the row objects, which change in place: a station's QRB,
     * its worked flags, its last activity. forceRedraw is how the application says so, and
     * the cached visible rows must be filtered again after it (#21).
     */
    @Test
    fun `a redraw filters rows again whose fields changed in place`() {
        val table = stationTable()
        val moving = Station("DN9APW", 100)
        table.replaceRows(listOf(moving, Station("DO5AMF", 400)))
        table.rowFilter = { it.qrb > 300 }
        assertEquals(listOf("DO5AMF"), table.visibleRows.map { it.callSign })

        moving.qrb = 500
        table.forceRedraw()

        assertEquals(listOf("DN9APW", "DO5AMF"), table.visibleRows.map { it.callSign }.sorted(),
            "the cached rows from before the change were handed back")
    }
}

/**
 * Column widths survive a restart, and the table is what remembers them.
 *
 * Storage is ChatPreferences.getTableColumnWidth/setTableColumnWidth, which is
 * toolkit-free; only the JavaFX glue around it stays behind. The ids are part of the
 * stored layout, so this is also where a renamed column loses an operator's sizing.
 */
class DataTableColumnWidthTest {

    private class Station(val callSign: String)

    private class RecordingStore : ColumnWidthStore {
        val stored = mutableMapOf<String, Double>()
        var saveRequests = 0

        override fun width(tableId: String, columnId: String): Double? = stored["$tableId/$columnId"]

        override fun setWidth(tableId: String, columnId: String, width: Double) {
            stored["$tableId/$columnId"] = width
        }

        override fun requestSave() {
            saveRequests++
        }
    }

    private fun table(store: ColumnWidthStore) = DataTableState(
        columns = listOf(
            DataColumn(id = "callsign", title = "Callsign", value = { s: Station -> s.callSign }),
            DataColumn(id = "qra", title = "QRA", value = { _: Station -> "" }),
        ),
        rowKey = RowKeys.byValue { s: Station -> s.callSign },
        tableId = "dx-cluster-monitor",
        widths = store,
    )

    @Test
    fun `a stored width is used`() {
        val store = RecordingStore()
        store.stored["dx-cluster-monitor/callsign"] = 140.0

        assertEquals(140.0, table(store).widthOf("callsign"))
    }

    @Test
    fun `a stored narrow width wins over the automatic width`() {
        val store = RecordingStore()
        store.stored["dx-cluster-monitor/callsign"] = 36.0
        val table = table(store)
        val column = table.columns.first { it.id == "callsign" }

        assertEquals(36f, effectiveColumnWidthDp(table, column, mapOf("callsign" to 140f)))
    }

    @Test
    fun `the automatic width is used only without a stored width`() {
        val table = table(RecordingStore())
        val column = table.columns.first { it.id == "callsign" }

        assertEquals(140f, effectiveColumnWidthDp(table, column, mapOf("callsign" to 140f)))
    }

    @Test
    fun `a column never sized yet has no stored width`() {
        assertNull(table(RecordingStore()).widthOf("qra"),
            "the caller falls back to the column's own weight, rather than to a "
                + "number invented here")
    }

    @Test
    fun `resizing stores the width and asks for a save`() {
        val store = RecordingStore()
        val table = table(store)

        table.resizeColumn("callsign", 180.0)

        assertEquals(180.0, table.widthOf("callsign"))
        assertEquals(1, store.saveRequests,
            "saving is debounced by the layout autosave, not written per pixel")
    }

    @Test
    fun `resizing a column this table does not have is ignored`() {
        val store = RecordingStore()
        val table = table(store)

        table.resizeColumn("a-column-from-another-table", 180.0)

        assertTrue(store.stored.isEmpty())
        assertEquals(0, store.saveRequests)
    }

    @Test
    fun `a width at or below zero is refused`() {
        val store = RecordingStore()
        val table = table(store)
        table.resizeColumn("callsign", 120.0)

        table.resizeColumn("callsign", 0.0)

        assertEquals(120.0, table.widthOf("callsign"),
            "a drag past the left edge must not store a column the operator cannot "
                + "grab again")
    }

    @Test
    fun `a table without a width store simply has no stored widths`() {
        val table = DataTableState(
            columns = listOf(DataColumn(id = "c", title = "C", value = { _: Station -> "" })),
            rowKey = RowKeys.byValue { s: Station -> s.callSign },
        )

        table.resizeColumn("c", 100.0)

        assertNull(table.widthOf("c"), "the two monitor tables may be built before "
            + "preferences are reachable; that must not throw")
    }
}

/**
 * The reference keys must not turn into a memory leak, and they are handed out from
 * two threads at once.
 *
 * Both stores behind these tables are capped on purpose — ten thousand cluster
 * messages, thirty thousand chat messages — so that memory cannot grow without bound.
 * A key map holding a strong reference to every row it ever saw would undo that cap
 * from the outside.
 */
class ReferenceRowKeysTest {

    private class Spot(val text: String)

    private fun table(keys: RowKeySource<Spot>) = DataTableState(
        columns = listOf(DataColumn(id = "text", title = "Message", value = { s: Spot -> s.text })),
        rowKey = keys,
    )

    @Test
    fun `keys of rows that have gone are released`() {
        val keys = RowKeys.byReference<Spot>()
        val table = table(keys)
        val first = (1..100).map { Spot("message $it") }

        table.replaceRows(first)
        // Keys are handed out when the list draws, which is what this stands in for.
        first.forEach { table.keyOf(it) }
        assertEquals(100, keys.size(), "one key per row on screen")

        // The store trims, as ChatController does to stay inside its cap.
        val remaining = first.takeLast(11)
        table.replaceRows(remaining)
        remaining.forEach { table.keyOf(it) }

        assertEquals(11, keys.size(),
            "a key map that keeps every row it ever saw undoes the cap the store keeps")
    }

    @Test
    fun `a row that stays keeps its key across a refresh`() {
        val keys = RowKeys.byReference<Spot>()
        val table = table(keys)
        val kept = Spot("kept")

        table.replaceRows(listOf(kept, Spot("other")))
        val before = keys.keyOf(kept)

        table.replaceRows(listOf(Spot("newer"), kept))

        assertEquals(before, keys.keyOf(kept), "same row, same key")
    }

    @Test
    fun `keys handed out from several threads at once are unique`() {
        // The rows are replaced from the JavaFX thread while Compose draws on its own.
        val keys = RowKeys.byReference<Spot>()
        val rows = (1..500).map { Spot("message $it") }
        val handedOut = java.util.concurrent.ConcurrentHashMap.newKeySet<Any>()
        val threads = (1..4).map {
            Thread { rows.forEach { row -> handedOut.add(keys.keyOf(row)) } }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(500, handedOut.size,
            "two rows sharing a key make the list throw; one row with two keys makes "
                + "it jump — both were seen in Etappe 2")
    }
}

/**
 * Columns can be dragged into another order, as the JavaFX TableView allowed.
 *
 * Within the session only: JavaFX did not persist the order either — TableLayoutManager
 * stores widths and ChatPreferences has no column order at all — and 1.50 is a
 * functionally equal port, not an improvement on it.
 */
class ColumnOrderTest {

    private class Station(val callSign: String)

    private fun table() = DataTableState(
        columns = listOf("a", "b", "c").map { id ->
            DataColumn(id = id, title = id.uppercase(), value = { _: Station -> id })
        },
        rowKey = RowKeys.byValue { s: Station -> s.callSign },
    )

    @Test
    fun `columns start in the order they were declared`() {
        assertEquals(listOf("a", "b", "c"), table().orderedColumns.map { it.id })
    }

    @Test
    fun `a column dragged to the front lands there`() {
        val table = table()

        table.moveColumn("c", 0)

        assertEquals(listOf("c", "a", "b"), table.orderedColumns.map { it.id })
    }

    @Test
    fun `a column dragged to the end lands there`() {
        val table = table()

        table.moveColumn("a", 2)

        assertEquals(listOf("b", "c", "a"), table.orderedColumns.map { it.id })
    }

    @Test
    fun `a target outside the table is clamped rather than dropping the column`() {
        val table = table()

        table.moveColumn("b", 99)
        assertEquals(listOf("a", "c", "b"), table.orderedColumns.map { it.id })

        table.moveColumn("b", -5)
        assertEquals(listOf("b", "a", "c"), table.orderedColumns.map { it.id })
    }

    @Test
    fun `moving a column this table does not have changes nothing`() {
        val table = table()

        table.moveColumn("a-column-from-another-table", 0)

        assertEquals(listOf("a", "b", "c"), table.orderedColumns.map { it.id })
    }
}
