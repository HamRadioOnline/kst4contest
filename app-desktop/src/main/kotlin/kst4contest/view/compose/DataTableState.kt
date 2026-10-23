package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.util.IdentityHashMap

/**
 * How a cell is painted beyond its text. Both colours are optional; null keeps the
 * table's own.
 */
data class CellAccent(val background: Color? = null, val foreground: Color? = null)

/**
 * One column of a data table.
 *
 * The shape is taken from what the station list of Etappe 5 actually does, not from
 * what the two monitor tables of Etappe 4 need. Those use the simple cases only —
 * cutting the interface to fit them would produce one the station list cannot use,
 * which is how the single-column DataTable of Etappe 3b came about.
 *
 * @param id stable across releases; the stored column widths are keyed by it, so
 *        renaming one loses the operator's layout
 * @param value what the cell shows
 * @param tooltip built from the row **and** the displayed value, because the JavaFX
 *        cells do exactly that — the QRA column explains its grid status that way
 * @param accent colouring per cell, again from row and value together
 * @param comparator sorts this column; without one the displayed text decides
 */
class DataColumn<T>(
    /**
     * Never translated: this is what the stored column layout is keyed on, so a language
     * change must leave it alone or every operator's remembered widths and order would be
     * read under names that no longer match.
     */
    val id: String,
    /**
     * Read when the header is drawn, not stored as text. The column lists are built once at
     * startup, outside any composition; a stored title would stay in the language that was
     * in force then while everything around it switched.
     */
    val title: () -> String,
    val value: (T) -> String,
    val tooltip: ((T, String) -> String)? = null,
    val accent: ((T, String) -> CellAccent?)? = null,
    val comparator: Comparator<T>? = null,
    val weight: Float = 1f,
    /**
     * Columns sharing a group are drawn under one heading, as the JavaFX table drew
     * its worked columns. Without it ten columns headed "wkdany 50 70 144 432 23 13
     * 9 6 3" say nothing about what they mean.
     */
    val group: String? = null,
) {

    /** For a title that genuinely never changes -- a ham abbreviation, or a test. */
    constructor(
        id: String,
        title: String,
        value: (T) -> String,
        tooltip: ((T, String) -> String)? = null,
        accent: ((T, String) -> CellAccent?)? = null,
        comparator: Comparator<T>? = null,
        weight: Float = 1f,
        group: String? = null,
    ) : this(id, { title }, value, tooltip, accent, comparator, weight, group)
}

/**
 * Gives every row a key that survives a refresh.
 *
 * There are two kinds of list here and they need different answers:
 *
 * - **Entity lists** — the station list, the worked-stations list. A row *is* a
 *   station, and the same station comes back updated. Its own key is the identity:
 *   [RowKeys.byValue] with the raw callsign.
 * - **Event lists** — the DX cluster and the QSO monitor. A row is something that
 *   happened. Two identical messages a second apart are genuinely two messages, so a
 *   key built from their fields would merge them: [RowKeys.byReference].
 *
 * There is deliberately no default. In Etappe 2 the visible defects came from row
 * identity — a key that was the position shifted on every insert at the top, and one
 * that was the text collapsed duplicates. Making the caller decide keeps that choice
 * where the knowledge is.
 */
interface RowKeySource<T> {

    fun keyOf(row: T): Any

    /**
     * Lets a source that remembers keys forget the rows that have gone.
     *
     * Called whenever the rows are replaced. Both stores behind these tables are
     * capped on purpose — ten thousand cluster messages, thirty thousand chat messages
     * — so that memory cannot grow without bound. A key map holding a strong reference
     * to every row it ever saw would undo that cap from the outside.
     */
    fun retainOnly(rows: List<T>) {}

    /** How many keys are being remembered. Zero for a source that remembers none. */
    fun size(): Int = 0
}

object RowKeys {

    /** For rows that carry their own identity; nothing is remembered. */
    fun <T> byValue(key: (T) -> String): RowKeySource<T> = object : RowKeySource<T> {
        override fun keyOf(row: T): Any = key(row)
    }

    /**
     * For append-only lists whose rows carry no identity.
     *
     * Keys are handed out on first sight and remembered by object reference, which
     * holds because the core rosters hand back the same instances on every snapshot.
     * A row that really is a new object gets a new key — which is the point.
     *
     * Synchronised throughout: the rows are replaced from the JavaFX thread while
     * Compose draws on its own, and an IdentityHashMap torn between two threads hands
     * out a duplicate key (the list then throws) or loses one (the row jumps). Both
     * were seen in Etappe 2.
     */
    fun <T> byReference(): RowKeySource<T> = object : RowKeySource<T> {

        private val keys = IdentityHashMap<T, Long>()
        private var next = 0L

        override fun keyOf(row: T): Any = synchronized(keys) {
            keys.getOrPut(row) { next++ }
        }

        override fun retainOnly(rows: List<T>) {
            synchronized(keys) {
                val surviving = IdentityHashMap<T, Boolean>()
                rows.forEach { surviving[it] = true }
                keys.keys.retainAll { surviving.containsKey(it) }
            }
        }

        override fun size(): Int = synchronized(keys) { keys.size }
    }
}

/**
 * Where column widths are kept between sessions.
 *
 * An interface rather than a direct call to ChatPreferences, so the rules can be
 * tested without one. The real implementation is two lines over
 * `getTableColumnWidth`/`setTableColumnWidth` plus `LayoutAutosave.requestSave`:
 * that storage is toolkit-free and stays as it is, while the JavaFX glue around it
 * does not come across.
 */
interface ColumnWidthStore {

    /** The stored width, or null when this column was never sized. */
    fun width(tableId: String, columnId: String): Double?

    fun setWidth(tableId: String, columnId: String, width: Double)

    /** Asks for a debounced save; widths must not be written per pixel of a drag. */
    fun requestSave()
}

/**
 * Optional shared row selection for tables whose selected entity is also displayed by
 * other components. Ordinary event tables keep their selection inside [DataTableState].
 */
interface TableSelectionModel<T> {

    val selected: T?

    /** Changes whenever [selected] changes, including a repeated explicit selection. */
    val selectionRevision: Long

    /** Changes only when the table should scroll to the current selection. */
    val scrollRequestRevision: Long

    fun select(row: T?)

    /** Replaces the selected instance after a roster refresh without requesting a scroll. */
    fun reconcile(row: T?)

    fun requestScroll()
}

/**
 * The state of a multi-column table: its rows, its sorting and its selection.
 *
 * Free of Compose UI so the rules can be tested, but the three mutable properties are
 * Compose state: a table bound to plain properties does not repaint, and with live
 * data that shows up as a stale display rather than as an unresponsive one — harder
 * to notice and worse in effect.
 */
class DataTableState<T> @JvmOverloads constructor(
    val columns: List<DataColumn<T>>,
    private val rowKey: RowKeySource<T>,
    /** Part of the stored layout: renaming it loses the operator's column sizing. */
    private val tableId: String = "",
    private val widths: ColumnWidthStore? = null,
    initialSortColumnId: String? = null,
    initialSortAscending: Boolean = true,
    private val selectionModel: TableSelectionModel<T>? = null,
    private val keepSelectionVisible: Boolean = false,
) {

    /**
     * The order the columns are shown in, by id.
     *
     * Within the session only. The JavaFX TableView let the operator drag a column
     * elsewhere but never stored where they put it — TableLayoutManager persists widths
     * and ChatPreferences has no column order at all — and 1.50 is a functionally equal
     * port, not an improvement on it.
     */
    private var order: List<String> by mutableStateOf(columns.map { it.id })

    /** The columns in the order they are shown. */
    val orderedColumns: List<DataColumn<T>>
        get() = order.mapNotNull { id -> columns.firstOrNull { it.id == id } }

    /**
     * Moves a column to another position.
     *
     * A target outside the table is clamped rather than refused: a drag that overshoots
     * the last header means "put it at the end", which is what the operator did with
     * the pointer.
     */
    fun moveColumn(columnId: String, toIndex: Int) {
        val from = order.indexOf(columnId)

        if (from < 0) {
            return
        }

        val target = toIndex.coerceIn(0, order.size - 1)

        if (target == from) {
            return
        }

        order = order.toMutableList().apply { add(target, removeAt(from)) }
    }

    /** Bumped on a resize so a table reading widths repaints. */
    private var widthRevision: Int by mutableStateOf(0)

    /**
     * The stored width of a column, or null when it was never sized.
     *
     * Null rather than a made-up number: the caller falls back to the column's own
     * weight, which is where the sensible default lives.
     */
    fun widthOf(columnId: String): Double? {
        @Suppress("UNUSED_EXPRESSION") widthRevision

        return widths?.width(tableId, columnId)
    }

    /**
     * Stores a new width for a column of this table.
     *
     * A width of zero or less is refused. A drag past the left edge would otherwise
     * store a column the operator cannot grab again, and the sizing survives restarts.
     */
    fun resizeColumn(columnId: String, width: Double) {
        if (width <= 0.0 || columns.none { it.id == columnId }) {
            return
        }

        widths?.let { store ->
            store.setWidth(tableId, columnId, width)
            store.requestSave()
            widthRevision++
        }
    }

    var rows: List<T> by mutableStateOf(emptyList())
        private set

    var sortedColumnId: String? by mutableStateOf(
        initialSortColumnId?.takeIf { requested -> columns.any { it.id == requested } },
    )
        private set

    var sortAscending: Boolean by mutableStateOf(initialSortAscending)
        private set

    private var selectedKey: Any? by mutableStateOf(null)

    private var internalSelectionRevision: Long by mutableStateOf(0L)

    private var internalScrollRequestRevision: Long by mutableStateOf(0L)

    private var filteredCache: List<T>? = null

    private var visibleCache: List<T>? = null

    private var cachedSelectionRevision: Long = Long.MIN_VALUE

    private var _rowFilter by mutableStateOf<((T) -> Boolean)?>(null, androidx.compose.runtime.neverEqualPolicy())
    
    /** Optional filter predicate. If set, [filteredRows] only includes matching rows. */
    var rowFilter: ((T) -> Boolean)?
        get() = _rowFilter
        set(value) {
            _rowFilter = value
            invalidateOrder()
        }

    /** Incremented to force Compose to redraw visible rows when mutable backend objects change without list structural changes. */
    var contentRevision by mutableStateOf(0)
        private set

    /**
     * Redraws the rows and filters them again. The filter reads mutable row fields (a
     * station's QRB, worked and QRV flags, its last activity), so a cached [visibleRows]
     * from before the change could keep a row the filter now excludes, or hide one it
     * now lets through -- in the list and in the station map, which draws the same rows.
     */
    fun forceRedraw() {
        invalidateOrder()
        contentRevision++
    }

    /**
     * The rows in the order they are shown.
     *
     * Computed once per change and then handed back. This is read on every
     * recomposition, and sorting here would mean sorting up to thirty thousand rows
     * per inbound message on the thread that draws — with a column that has no
     * comparator calling its value function once per comparison on top.
     */
    val filteredRows: List<T>
        get() {
            /*
             * Read before the cache is consulted, and that order is the whole point.
             * Compose repaints a composable when state it *read* changes; a cache that
             * returned early stopped reading these, so the next recomposition left the
             * table subscribed to nothing and later rows never reached the screen. It
             * showed as a monitor window sitting empty beside a filling JavaFX tab,
             * redrawing only when an unrelated click forced it.
             */
            val currentRows = rows
            val currentFilter = rowFilter
            contentRevision // read for the subscription: forceRedraw drops the cache
            val currentColumnId = sortedColumnId
            val ascending = sortAscending
            filteredCache?.let { return it }

            val filtered = if (currentFilter != null) currentRows.filter(currentFilter) else currentRows
            val sorted = sort(filtered, currentColumnId, ascending)

            filteredCache = sorted
            return sorted
        }

    /**
     * Rows drawn by this table. A station selected outside the table can optionally be
     * included even when the active filter rejects it; [filteredRows] remains the exact
     * shared filter result used by the map.
     */
    val visibleRows: List<T>
        get() {
            val currentRows = rows
            val filtered = filteredRows
            val revision = selectionRevision

            if (!keepSelectionVisible) {
                return filtered
            }

            if (visibleCache != null && cachedSelectionRevision == revision) {
                return visibleCache.orEmpty()
            }

            val selected = selectedRow
            val visible = if (selected == null || filtered.any { keyOf(it) == keyOf(selected) }) {
                filtered
            } else if (currentRows.any { keyOf(it) == keyOf(selected) }) {
                val filteredKeys = filtered.mapTo(HashSet<Any>()) { row -> keyOf(row) }
                val selectedKey = keyOf(selected)
                sort(currentRows.filter { row -> keyOf(row) == selectedKey || keyOf(row) in filteredKeys })
            } else {
                filtered
            }

            cachedSelectionRevision = revision
            visibleCache = visible
            return visible
        }

    private fun sort(
        source: List<T>,
        columnId: String? = sortedColumnId,
        ascending: Boolean = sortAscending,
    ): List<T> {
        val column = columns.firstOrNull { it.id == columnId } ?: return source

        /*
         * The displayed text is read once per row rather than once per comparison, and
         * Kotlin's sort is stable, so equal values keep their source order.
         */
        val order = column.comparator
            ?: compareBy(source.associateWith { column.value(it) }::getValue)

        return if (ascending) source.sortedWith(order) else source.sortedWith(order.reversed())
    }

    private fun invalidateOrder() {
        filteredCache = null
        visibleCache = null
    }

    /** The selected row, or null when nothing is selected or it has gone. */
    val selectedRow: T?
        get() = selectionModel?.selected
            ?: selectedKey?.let { key -> rows.firstOrNull { rowKey.keyOf(it) == key } }

    val selectionRevision: Long
        get() = selectionModel?.selectionRevision ?: internalSelectionRevision

    val scrollRequestRevision: Long
        get() = selectionModel?.scrollRequestRevision ?: internalScrollRequestRevision

    /**
     * Replaces the rows.
     *
     * The selection is kept **by key**, not by position: a station that moved because
     * something above it was inserted is still the station the operator picked. When
     * the selected row is gone the selection is dropped rather than sliding onto its
     * neighbour, which would silently point the operator at someone else.
     */
    fun replaceRows(newRows: List<T>) {
        val selectedIdentity = selectionModel?.selected?.let(rowKey::keyOf) ?: selectedKey

        rows = newRows
        rowKey.retainOnly(newRows)
        invalidateOrder()

        val replacement = selectedIdentity?.let { key -> newRows.firstOrNull { rowKey.keyOf(it) == key } }

        if (selectionModel != null) {
            if (selectedIdentity != null) {
                selectionModel.reconcile(replacement)
            }
        } else if (selectedIdentity != null) {
            selectedKey = replacement?.let(rowKey::keyOf)
            internalSelectionRevision++
        }
    }

    fun select(row: T?) {
        if (selectionModel != null) {
            selectionModel.select(row)
        } else {
            selectedKey = row?.let(rowKey::keyOf)
            internalSelectionRevision++
        }
        visibleCache = null
    }

    /** Requests navigation to the current selection without changing it. */
    fun requestScrollToSelection() {
        if (selectedRow == null) return

        if (selectionModel != null) {
            selectionModel.requestScroll()
        } else {
            internalScrollRequestRevision++
        }
    }

    /**
     * The key a row is drawn under. Its identity, never its position: a positional key
     * reshuffles every row whenever something arrives at the top, which in a live list
     * is constant.
     */
    fun keyOf(row: T): Any = rowKey.keyOf(row)

    fun isSelected(row: T): Boolean {
        val selected = selectedRow ?: return false
        return rowKey.keyOf(row) == rowKey.keyOf(selected)
    }

    /**
     * Sorts by a column, cycling ascending, descending, unsorted.
     *
     * The third click is not a flourish: JavaFX TableView gives that cycle for free, and
     * without it an operator who sorted the message list once could never get back to
     * arrival order — newest at the bottom — short of restarting.
     *
     * An unknown column is ignored rather than clearing the sorting: it can only come
     * from a stored layout that names a column this release no longer has.
     */
    fun toggleSort(columnId: String) {
        if (columns.none { it.id == columnId }) {
            return
        }

        when {
            sortedColumnId != columnId -> {
                sortedColumnId = columnId
                sortAscending = true
            }

            sortAscending -> sortAscending = false

            // Third click: back to the order the rows arrived in.
            else -> {
                sortedColumnId = null
                sortAscending = true
            }
        }

        invalidateOrder()
    }
}
