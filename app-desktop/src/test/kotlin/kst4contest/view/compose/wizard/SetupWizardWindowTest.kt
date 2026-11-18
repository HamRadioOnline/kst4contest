package kst4contest.view.compose.wizard

import kst4contest.model.ChatPreferences
import kst4contest.testing.Await.awaitUntil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.awt.Window
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * The wizard's dialog must open from both threads that open it: `main` at startup, and the
 * event thread when the menu rebuilds the runtime. A latch-based wait deadlocked the profile
 * picker in exactly the second case; see OperatorProfilePickerWindow.
 */
class SetupWizardWindowTest {

    private fun prefs() = ChatPreferences(Path.of("setup-wizard-window-test-unused.xml"))

    private fun showingDialog(): Window? =
        Window.getWindows().firstOrNull { it.isShowing && it.javaClass.name.contains("ComposeDialog") }

    private val closer = Executors.newSingleThreadExecutor()

    /** Closes the dialog the way its X does, as soon as it is on screen. */
    private fun disposeOnceShowing(): Future<*> =
        closer.submit {
            awaitUntil(Duration.ofSeconds(10), "the wizard's dialog never appeared") {
                showingDialog() != null
            }
            EventQueue.invokeLater { showingDialog()?.dispose() }
        }

    @Test
    fun closingTheWindowCountsAsLater() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")

        try {
            val closing = disposeOnceShowing()
            val prefs = prefs()

            val outcome = SetupWizardWindow.show(SetupWizardState(prefs), prefs)

            assertEquals(SetupWizardOutcome.LATER, outcome)
            assertEquals("", prefs.getStn_loginCallSign(), "closing the window wrote something")
            closing.get(1, TimeUnit.SECONDS)
        } finally {
            closer.shutdownNow()
        }
    }

    @Test
    fun opensAndReturnsWhenCalledOnTheEventThread() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")

        val worker = Executors.newSingleThreadExecutor()
        try {
            val closing = disposeOnceShowing()
            val prefs = prefs()
            val call = worker.submit {
                EventQueue.invokeAndWait { SetupWizardWindow.show(SetupWizardState(prefs), prefs) }
            }

            // A deadlock here has no timeout of its own: without one this never completes.
            call.get(20, TimeUnit.SECONDS)
            closing.get(1, TimeUnit.SECONDS)
        } finally {
            worker.shutdownNow()
            closer.shutdownNow()
        }
    }
}
