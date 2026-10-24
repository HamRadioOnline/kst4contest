package kst4contest.view.compose

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ManualQtfHeadingSynchronizerTest(@param:TempDir private val directory: Path) {

    @Test
    fun `valid explicit QTF updates actual heading when synchronization is enabled`() {
        val prefs = preferences().apply {
            actualQTF.set(20.0)
        }
        assertTrue(applyExplicitQtfHeading(prefs, 315.5))
        assertEquals(315.5, prefs.actualQTF.get(), 0.0001)
        assertEquals(135.0, prefs.stn_qtfDefault, 0.0001)
    }

    @Test
    fun `primary rotator prevents manual QTF synchronization`() {
        val prefs = preferences().apply {
            setStn_pstRotatorEnabled(true)
            actualQTF.set(82.0)
        }
        assertFalse(applyExplicitQtfHeading(prefs, 180.0))
        assertEquals(82.0, prefs.actualQTF.get(), 0.0001)
    }

    @Test
    fun `disabled option and invalid angles leave actual heading unchanged`() {
        val prefs = preferences().apply {
            setStn_useQtfFilterDirectionAsAntennaHeading(false)
            actualQTF.set(82.0)
        }
        listOf(90.0, Double.NaN, Double.POSITIVE_INFINITY, -0.1, 360.1).forEach { qtf ->
            assertFalse(applyExplicitQtfHeading(prefs, qtf), "unexpectedly accepted $qtf")
            assertEquals(82.0, prefs.actualQTF.get(), 0.0001)
        }
    }

    @Test
    fun `zero and 360 follow the existing antenna heading convention`() {
        val prefs = preferences()
        assertTrue(applyExplicitQtfHeading(prefs, 0.0))
        assertEquals(0.0, prefs.actualQTF.get(), 0.0001)
        assertTrue(applyExplicitQtfHeading(prefs, 360.0))
        assertEquals(360.0, prefs.actualQTF.get(), 0.0001)
    }

    private fun preferences() = ChatPreferences(directory.resolve("preferences.xml"))
}
