package kst4contest.view.compose

import kst4contest.model.ChatPreferences

private const val MIN_QTF = 0.0
private const val MAX_QTF = 360.0

/**
 * Applies a deliberate filter direction to the runtime antenna heading without sending a rotor command.
 *
 * @return true when the runtime heading was updated
 */
internal fun applyExplicitQtfHeading(prefs: ChatPreferences, qtf: Double): Boolean {
    if (!qtf.isFinite() || qtf !in MIN_QTF..MAX_QTF) return false
    if (!prefs.isQtfFilterDirectionAsAntennaHeadingEnabled) return false

    prefs.actualQTF.set(qtf)
    return true
}
