package kst4contest.view;

import kst4contest.controller.ActiveOperatorProfile;
import kst4contest.controller.OperatorProfilePaths;
import kst4contest.controller.OperatorProfileStore;
import kst4contest.model.OperatorProfile;
import kst4contest.model.OperatorProfileSelection;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the lifecycle of the running application.
 *
 * <p>Switching the operator profile tears the current runtime down completely and builds
 * a fresh one in the same process. Rebinding the existing windows is not an option: the
 * user interface is built from the chat controller outwards, with several hundred
 * references to the active preferences, and many controls are instance fields created
 * once. A new {@link Kst4ContestApplication} instance gets fresh controls, which is safe
 * here because the class keeps no mutable static state.</p>
 */
public final class ApplicationRuntimeLauncher {

    private static final Logger LOGGER = Logger.getLogger(ApplicationRuntimeLauncher.class.getName());

    private static Kst4ContestApplication currentRuntime;

    /**
     * Whether the next runtime start shows the setup wizard although the profile is set up.
     * Set by the menu entry and cleared by the start that honours it.
     */
    private static final java.util.concurrent.atomic.AtomicBoolean SETUP_WIZARD_REQUESTED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * Guards {@link #switchProfile(OperatorProfile)} against re-entry while window teardown
     * inside {@link Kst4ContestApplication#shutdownRuntime()} is still pumping events.
     */
    private static final java.util.concurrent.atomic.AtomicBoolean SWITCH_IN_PROGRESS =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private ApplicationRuntimeLauncher() {
        // Utility class.
    }

    /**
     * Registers the runtime that is currently live.
     *
     * @param runtime the running application instance
     */
    public static void setCurrent(final Kst4ContestApplication runtime) {
        currentRuntime = runtime;
    }

    /**
     * Returns the runtime that is currently live.
     *
     * @return the running application instance, or null before the first startup
     */
    public static Kst4ContestApplication getCurrent() {
        return currentRuntime;
    }

    /**
     * Shuts the application down.
     *
     * <p>Releases the live runtime's resources explicitly: after a profile switch that is
     * no longer the instance the process started with.</p>
     */
    public static void exitApplication() {

        if (currentRuntime != null) {
            currentRuntime.shutdownRuntime();
        }

        System.exit(0);
    }

    /** Asks the next runtime start to show the setup wizard. */
    static void requestSetupWizard() {
        SETUP_WIZARD_REQUESTED.set(true);
    }

    /**
     * Tells a starting runtime whether the wizard was asked for, and forgets the request.
     *
     * @return true exactly once per request
     */
    static boolean consumeSetupWizardRequest() {
        return SETUP_WIZARD_REQUESTED.getAndSet(false);
    }

    /**
     * Marks a profile switch as in progress if none is currently running.
     *
     * @return true if this call obtained the mark
     */
    static boolean beginSwitch() {
        return SWITCH_IN_PROGRESS.compareAndSet(false, true);
    }

    /** Clears the in-progress mark of a profile switch. */
    static void endSwitch() {
        SWITCH_IN_PROGRESS.set(false);
    }

    /**
     * Rebuilds the runtime of the active profile with the setup wizard in front of it.
     *
     * <p>The running runtime is torn down first and the wizard shown afterwards, not the
     * other way round: a runtime shutting down flushes its pending layout write, and that
     * must not land on the file the wizard has just written.</p>
     *
     * <p>The wizard is requested only once this call owns the switch. A call refused
     * because a switch is already under way therefore leaves that switch's request alone:
     * clearing it would let the running switch end the session and then start without the
     * wizard it was made for.</p>
     *
     * @return true if the new runtime was built
     */
    public static boolean rerunSetupWizard() {
        OperatorProfileSelection active = ActiveOperatorProfile.get();

        if (active == null) {
            return false;
        }

        if (!beginSwitch()) {
            return false;
        }

        try {
            requestSetupWizard();

            boolean switched = false;
            try {
                switched = performSwitch(active.getProfile());
            } finally {
                if (!switched) {
                    // The request is this call's own, and no start consumed it.
                    consumeSetupWizardRequest();
                }
            }
            return switched;
        } finally {
            endSwitch();
        }
    }

    /**
     * Replaces the running runtime with one bound to another operator profile.
     *
     * @param targetProfile profile to activate
     * @return true if the new runtime was built; false also when another switch is
     *         already under way
     */
    public static boolean switchProfile(final OperatorProfile targetProfile) {

        if (!beginSwitch()) {
            return false;
        }

        try {
            return performSwitch(targetProfile);
        } finally {
            endSwitch();
        }
    }

    /** The switch itself. The caller holds the switch-in-progress mark. */
    private static boolean performSwitch(final OperatorProfile targetProfile) {

        if (targetProfile == null) {
            return false;
        }

        new OperatorProfileStore().recordLastUsed(targetProfile.getProfileId());

        if (currentRuntime != null) {
            currentRuntime.shutdownRuntime();
        }

        ActiveOperatorProfile.set(OperatorProfilePaths.resolve(targetProfile));

        Kst4ContestApplication nextRuntime = new Kst4ContestApplication();

        try {
            nextRuntime.startRuntime();
        } catch (Exception e) {
            // The previous runtime is already gone, so there is nothing left to return to.
            LOGGER.log(Level.SEVERE, "Could not start the selected operator profile", e);
            Kst4ContestApplication.alertWindowEvent(
                    "The operator profile could not be started: " + e.getMessage()
                            + "\n\nKST4Contest has to be closed.");
            System.exit(1);
            return false;
        }

        setCurrent(nextRuntime);
        return true;
    }
}
