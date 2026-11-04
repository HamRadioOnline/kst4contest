package kst4contest.view.compose.map

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class MapDisplaySettingsStateTest {

    @TempDir
    lateinit var directory: Path

    @Test
    fun `refresh publishes applied beam geometry from preferences`() {
        val preferences = ChatPreferences(directory.resolve("preferences.xml")).apply {
            stn_antennaBeamWidthDeg = 50.0
            stn_maxQRBDefault = 750.0
        }
        val state = MapDisplaySettingsState(preferences)

        preferences.stn_antennaBeamWidthDeg = 72.0
        preferences.stn_maxQRBDefault = 1_100.0
        state.refreshFromPreferences()

        assertEquals(72.0, state.antennaBeamWidthDeg)
        assertEquals(1_100.0, state.maxQrbKm)
    }
}
