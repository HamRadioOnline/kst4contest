package kst4contest.view.compose

import kst4contest.model.BeaconStatusSnapshot
import kst4contest.model.BeaconStatusSnapshot.AggregateState
import kst4contest.model.BeaconStatusSnapshot.CategoryStatus
import kst4contest.model.BeaconStatusSnapshot.InactiveReason
import kst4contest.view.i18n.LanguageStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

class BeaconStatusPresentationTest {
    @Test
    fun `partial status keeps the inactive category reason in English`() {
        val presentation = BeaconStatusPresentation.from(snapshot(AggregateState.PARTIAL))

        assertEquals("1/2", presentation.label)
        assertTrue(presentation.tooltip.contains("Main category: active"))
        assertTrue(presentation.tooltip.contains("primary QRG missing or invalid"))
    }

    @Test
    fun `the same category reasons are available in German`() {
        val language = LanguageStore({ "de" }, {}, Locale.UK)
        try {
            val presentation = BeaconStatusPresentation.from(
                snapshot(AggregateState.PARTIAL),
                language.strings,
            )

            assertTrue(presentation.tooltip.contains("Hauptkategorie: aktiv"))
            assertTrue(presentation.tooltip.contains("Haupt-QRG fehlt oder ist ungültig"))
            assertFalse(presentation.tooltip.contains("144."))
        } finally {
            language.use("en")
        }
    }

    @Test
    fun `failed style overrides an active category`() {
        val main = CategoryStatus(true, true, true, InactiveReason.USER_DISABLED)
        val second = CategoryStatus(false, false, false, InactiveReason.NOT_ACTIVATED_THIS_SESSION)
        val presentation = BeaconStatusPresentation.from(
            BeaconStatusSnapshot(AggregateState.FAILED, main, second),
        )

        assertEquals("FAILED", presentation.label)
        assertEquals(ThreadStatusVisualStyle.BEACON_FAILED, presentation.style)
        assertTrue(presentation.tooltip.contains("send failed"))
    }

    private fun snapshot(state: AggregateState): BeaconStatusSnapshot = BeaconStatusSnapshot(
        state,
        CategoryStatus(true, true, false, InactiveReason.USER_DISABLED),
        CategoryStatus(true, false, false, InactiveReason.PRIMARY_QRG_UNAVAILABLE),
    )
}
