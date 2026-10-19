package kst4contest.view.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.model.ChatMessage
import kst4contest.model.ClusterMessage

/**
 * The cluster and QSO monitor.
 *
 * Two live tables, one above the other, with a divider the operator can drag. This
 * is the first window in which the table carries data that changes while the operator
 * watches, which is why the row identity rules in DataTableState matter here and not in
 * the settings window: in Etappe 2 the visible defects all came from that coupling, not
 * from the table itself.
 */
object MonitorWindow {

    private val host = ComposeWindowHost("monitor-window")

    @JvmStatic
    val isOpen: Boolean
        get() = host.isOpen

    @JvmStatic
    fun applyDarkMode(darkMode: Boolean) = host.applyDarkMode(darkMode)

    @JvmStatic
    fun close() = host.close()

    /**
     * @param clusterTable fed from `getLst_clusterMemberList`
     * @param qsoTable fed from `toOtherMessages` over the global chat message list
     */
    @JvmStatic
    fun show(
        clusterTable: DataTableState<ClusterMessage>,
        qsoTable: DataTableState<ChatMessage>,
        /** The divider between the two tables, remembered in the profile (#144). */
        splitter: SplitterState,
        darkMode: Boolean,
        baseFontSizeSp: Float,
        /**
         * The palette of the active profile, handed to the theme. Null draws the shipped
         * palette.
         */
        paletteStore: PaletteStore? = null,
        /**
         * The interface language of the active profile, handed to the window. Null draws the
         * base language.
         */
        languageStore: kst4contest.view.i18n.LanguageStore? = null,
        widthDp: Float,
        heightDp: Float,
        onResized: (Float, Float) -> Unit,
    ) {
        host.show(
            title = { kst4contest.view.i18n.CurrentStrings.get().monitorTitle },
            darkMode = darkMode,
            baseFontSizeSp = baseFontSizeSp,
            paletteStore = paletteStore,
            languageStore = languageStore,
            widthDp = widthDp,
            heightDp = heightDp,
            onResized = onResized,
        ) { _ ->
            MonitorContent(clusterTable, qsoTable, splitter)
        }
    }
}

/** Internal rather than private so the window's body can be drawn headless in a test. */
@Composable
internal fun MonitorContent(
    clusterTable: DataTableState<ClusterMessage>,
    qsoTable: DataTableState<ChatMessage>,
    splitter: SplitterState,
) {
    val strings = kst4contest.view.i18n.LocalStrings.current

    Surface(modifier = Modifier.fillMaxSize()) {
        /*
         * Each heading sits in the pane of its table, so dragging the divider moves the
         * heading with the table it names rather than leaving it stranded above the other.
         */
        SplitterPane(
            state = splitter,
            orientation = SplitterOrientation.VERTICAL,
            modifier = Modifier.fillMaxSize().padding(4.dp),
            panes = listOf(
                { MonitorPane(strings.monitorDxCluster, clusterTable) },
                { MonitorPane(strings.messagesQsoOfTheOther, qsoTable) },
            ),
        )
    }
}

@Composable
private fun <T> MonitorPane(heading: String, table: DataTableState<T>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(heading, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
        DataTableView(table, modifier = Modifier.weight(1f).fillMaxWidth())
    }
}
