package kst4contest.view.compose.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeDialog
import kst4contest.ApplicationConstants
import kst4contest.controller.LoginConfiguration
import kst4contest.controller.OperatorProfilePaths
import kst4contest.controller.OperatorProfileStore
import kst4contest.model.ChatPreferences
import kst4contest.utils.ApplicationFileUtils
import kst4contest.view.compose.Kst4ContestTheme
import kst4contest.view.compose.WindowPlacementOnScreen
import kst4contest.view.compose.openOnEventThread
import kst4contest.view.compose.tabs.LocalFileDialogOwner
import java.awt.Dialog
import java.awt.Dimension
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import javax.swing.WindowConstants

/** How a run of the setup wizard ended. */
enum class SetupWizardOutcome {
    /** The profile is set up, or its file could not be read: the wizard did not open. */
    NOT_SHOWN,

    /** Closed without saving; the file is as it was. */
    LATER,

    /** The answers are in the file. */
    SAVED,

    /** The answers are in the file and the operator wants the session started. */
    SAVED_AND_CONNECT,
}

/**
 * Writes the answers and says how the wizard ends.
 *
 * @return the outcome to close with, or null when the file could not be written -- the
 *         wizard then stays open, because "saved" would be a lie
 */
internal fun saveFromWizard(
    state: SetupWizardState,
    prefs: ChatPreferences,
    connect: Boolean,
): SetupWizardOutcome? =
    when {
        !state.writeTo(prefs) -> null
        connect -> SetupWizardOutcome.SAVED_AND_CONNECT
        else -> SetupWizardOutcome.SAVED
    }

/**
 * The setup wizard's window.
 *
 * A modal [ComposeDialog] opened through [openOnEventThread], the construction
 * OperatorProfilePickerWindow uses and for the reason given there: a wait that does not
 * pump events deadlocks once the caller is the event thread, and this one is called from
 * `main` at startup and from the event thread when the menu rebuilds the runtime.
 */
object SetupWizardWindow {

    /** Runs before a profile's runtime exists, so there is no configured size to read. */
    private const val DIALOG_FONT_SIZE_SP = 12f

    private val DIALOG_SIZE = Dimension(640, 620)

    /**
     * Opens the wizard and blocks until it is closed.
     *
     * @param prefs the profile's preferences; written only by Save
     * @return never [SetupWizardOutcome.NOT_SHOWN]
     */
    fun show(state: SetupWizardState, prefs: ChatPreferences): SetupWizardOutcome {
        /* The window's X leaves this untouched: closing is "later". */
        val outcome = AtomicReference(SetupWizardOutcome.LATER)

        openOnEventThread { open(state, prefs, outcome) }

        return outcome.get()
    }

    private fun open(
        state: SetupWizardState,
        prefs: ChatPreferences,
        outcome: AtomicReference<SetupWizardOutcome>,
    ) {
        val languageStore = wizardLanguageStore(state)
        var saveFailed by mutableStateOf(false)

        val dialog = ComposeDialog(owner = null, modalityType = Dialog.ModalityType.APPLICATION_MODAL)
        dialog.title = languageStore.strings.wizardTitle
        dialog.size = DIALOG_SIZE
        WindowPlacementOnScreen.centreOnMainWindowScreen(dialog)
        /* Disposed and not hidden: a hidden dialog keeps its window and Skia layer alive. */
        dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        dialog.setContent {
            Kst4ContestTheme(
                darkMode = prefs.isGUI_darkModeActiveByDefault(),
                baseFontSizeSp = DIALOG_FONT_SIZE_SP,
            ) {
                CompositionLocalProvider(LocalFileDialogOwner provides dialog) {
                    SetupWizardContent(
                        state = state,
                        languageStore = languageStore,
                        saveFailed = saveFailed,
                        onLater = { dialog.dispose() },
                        onSave = { connect ->
                            val saved = saveFromWizard(state, prefs, connect)
                            if (saved == null) {
                                saveFailed = true
                            } else {
                                outcome.set(saved)
                                dialog.dispose()
                            }
                        },
                    )
                }
            }
        }

        dialog.isVisible = true
    }
}

/** Another profile that might serve as a donor: who it is and where its preferences lie. */
internal class DonorCandidate(val profileId: String, val displayName: String, val file: Path)

/**
 * Reads the candidates that can serve as donors: the file exists, can be read, and holds a
 * login an operator entered. A candidate whose file is the same file as [activeFile] (for
 * example through a symbolic link) is skipped so a profile never becomes its own donor.
 *
 * A candidate's file is opened through `ChatPreferences(Path)` and never through the
 * constructor that takes a name below the application directory: that one seeds a missing
 * file from the bundled template, and looking for a donor must not create a profile's file.
 * Nothing here writes.
 */
internal fun loadDonors(
    candidates: List<DonorCandidate>,
    activeFile: Path? = null,
): List<WizardDonor> =
    candidates.mapNotNull { candidate ->
        if (!Files.isRegularFile(candidate.file)) return@mapNotNull null
        if (activeFile != null && Files.exists(activeFile) &&
            runCatching { Files.isSameFile(candidate.file, activeFile) }.getOrDefault(false)
        ) {
            return@mapNotNull null
        }

        val prefs = ChatPreferences(candidate.file)
        // A throwing read counts as failed, as for the profile being set up.
        if (!runCatching { prefs.readPreferencesFromXmlFile() }.getOrDefault(false)) return@mapNotNull null
        if (LoginConfiguration.isUnconfigured(prefs)) return@mapNotNull null

        WizardDonor(candidate.profileId, candidate.displayName, prefs)
    }

/** Decides whether the wizard opens for a profile, and opens it. */
object SetupWizardLauncher {

    /**
     * Opens the wizard when the profile needs it, before the profile's runtime is built.
     *
     * @param preferencesRelativeFileName the profile's preferences file below the
     *        application directory; a missing one is seeded from the bundled template, as
     *        the runtime itself would do
     * @param forced the operator asked for the wizard from the menu
     */
    @JvmStatic
    fun runIfNeeded(preferencesRelativeFileName: String, forced: Boolean): SetupWizardOutcome =
        run(
            ChatPreferences(preferencesRelativeFileName),
            forced,
            {
                loadDonors(
                    otherProfiles(preferencesRelativeFileName),
                    activeFile = Path.of(
                        ApplicationFileUtils.getFilePath(
                            ApplicationConstants.APPLICATION_NAME,
                            preferencesRelativeFileName,
                        )
                    ),
                )
            },
            SetupWizardWindow::show,
        )

    /**
     * Every registered profile but the one being set up, with the file its preferences lie
     * in. Empty on an installation without a profile registry: there is only the root
     * profile then, and it is the active one.
     */
    private fun otherProfiles(activeRelativeFileName: String): List<DonorCandidate> =
        OperatorProfileStore().loadProfiles().mapNotNull { profile ->
            val relativeFileName = OperatorProfilePaths.preferencesRelativeFileName(profile)
            if (relativeFileName == activeRelativeFileName) return@mapNotNull null

            DonorCandidate(
                profile.profileId,
                profile.displayName,
                Path.of(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, relativeFileName)),
            )
        }

    /** Without other profiles; for callers and tests that have none. */
    internal fun run(
        prefs: ChatPreferences,
        forced: Boolean,
        show: (SetupWizardState, ChatPreferences) -> SetupWizardOutcome,
    ): SetupWizardOutcome = run(prefs, forced, { emptyList() }, show)

    /**
     * @param prefs bound to the profile's file and not read yet
     * @param donors reads the other profiles; called only once the wizard is certain to
     *        open, so an ordinary start reads no file but its own
     * @param show opens the window; replaced in the tests
     */
    internal fun run(
        prefs: ChatPreferences,
        forced: Boolean,
        donors: () -> List<WizardDonor>,
        show: (SetupWizardState, ChatPreferences) -> SetupWizardOutcome,
    ): SetupWizardOutcome {
        /*
         * A file that cannot be read is not a profile to set up: the wizard would start from
         * compiled-in defaults and Save would write them over whatever the file holds. The
         * runtime deals with such a file as it always did. A throwing read counts as failed too.
         */
        if (!runCatching { prefs.readPreferencesFromXmlFile() }.getOrDefault(false)) {
            return SetupWizardOutcome.NOT_SHOWN
        }

        if (!forced && !LoginConfiguration.isUnconfigured(prefs)) return SetupWizardOutcome.NOT_SHOWN

        return show(SetupWizardState(prefs, donors()), prefs)
    }
}
