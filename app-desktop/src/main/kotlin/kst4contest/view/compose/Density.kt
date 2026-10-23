package kst4contest.view.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The measurements of the settings windows, in one place so the tabs cannot drift
 * apart.
 *
 * Material 3 ships phone densities: a text field is 56dp tall, a button 40dp and
 * fully rounded, a checkbox reserves a 48dp touch target. The Station tab alone has
 * over thirty fields, so those defaults turn a settings window into a scrolling
 * exercise and make a desktop application look like a phone.
 *
 * Interactive controls share one minimum visible height. Their common font and padding
 * keep the standard size exact while still allowing a larger configured font to grow.
 */
internal object Density {

    /** Minimum for buttons, text fields, choices and checkbox rows in the desktop UI. */
    val CONTROL_HEIGHT = 26.dp

    /** Text stays vertically centred by the control instead of by extra padding. */
    val FIELD_CONTENT_PADDING = PaddingValues(horizontal = 6.dp, vertical = 0.dp)

    val BUTTON_CONTENT_PADDING = PaddingValues(horizontal = 6.dp, vertical = 0.dp)

    /** Square-ish, not a pill: the pill shape is the strongest mobile tell. */
    val BUTTON_SHAPE = RoundedCornerShape(4.dp)

    /** One row of a list or table. */
    val ROW_MIN_HEIGHT = CONTROL_HEIGHT

    /** Equal width for all eight compass-direction filter buttons. */
    val DIRECTION_BUTTON_WIDTH = 28.dp

    /** Two-letter compass labels need only a small inset inside their fixed width. */
    val DIRECTION_BUTTON_HORIZONTAL_PADDING = 2.dp

    /** A compact, independently scrollable message table in the selected-station pane. */
    val SELECTED_MESSAGE_TABLE_HEIGHT = 104.dp

    /** Enough room for the NOT-QRV and sked groups to remain usable side by side. */
    val SELECTED_ACTION_GROUPS_ROW_MIN_WIDTH = 520.dp

    val TAB_MIN_HEIGHT = 30.dp

    /**
     * Left and right of a tab title. Material reserves 90dp minimum width per tab,
     * which eleven tabs cannot live with; this is what a tab costs beyond its text.
     */
    val TAB_HORIZONTAL_PADDING = 10.dp

    /** The box itself; the 48dp touch target around it is switched off separately. */
    val CHECK_SIZE = 18.dp

    /** The label column of the forms, wide enough for the longest label in use. */
    val LABEL_COLUMN_WIDTH = 260.dp

    /** Between a label and its field, and between two fields. */
    val FIELD_GAP = 3.dp

    /** Between buttons in a row. */
    val BUTTON_GAP = 6.dp

    /** Around a selected row, and the hairline between rows. */
    val SELECTION_BORDER = 1.5.dp

    /** Kept free at the right edge so the scrollbar never sits on a control. */
    val SCROLLBAR_GUTTER = 12.dp

    /** Grab area between two columns; the line drawn inside it is a hairline. */
    val COLUMN_HANDLE_WIDTH = 5.dp

    val HAIRLINE = 1.dp
}
