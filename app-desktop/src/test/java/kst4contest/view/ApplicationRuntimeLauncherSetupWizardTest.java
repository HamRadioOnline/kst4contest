package kst4contest.view;

import kst4contest.controller.ActiveOperatorProfile;
import kst4contest.model.OperatorProfile;
import kst4contest.model.OperatorProfileSelection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The request to show the setup wizard on the next runtime start is honoured exactly once.
 * A request that stuck would open the wizard on every later profile switch.
 */
class ApplicationRuntimeLauncherSetupWizardTest {

    @Test
    void aRequestIsHonouredOnce() {
        assertFalse(ApplicationRuntimeLauncher.consumeSetupWizardRequest(), "nothing was requested");

        ApplicationRuntimeLauncher.requestSetupWizard();

        assertTrue(ApplicationRuntimeLauncher.consumeSetupWizardRequest());
        assertFalse(ApplicationRuntimeLauncher.consumeSetupWizardRequest(), "the request must not stick");
    }

    @Test
    void switchGuardAllowsOnlyOneSwitchAtATime() {
        try {
            assertTrue(ApplicationRuntimeLauncher.beginSwitch());
            assertFalse(ApplicationRuntimeLauncher.beginSwitch(),
                    "a second switch must be refused while one is in progress");
        } finally {
            ApplicationRuntimeLauncher.endSwitch();
        }

        try {
            assertTrue(ApplicationRuntimeLauncher.beginSwitch(),
                    "ending the first switch must release the guard");
        } finally {
            ApplicationRuntimeLauncher.endSwitch();
        }
    }

    @Test
    void aRefusedRerunLeavesTheRequestOfTheRunningSwitchAlone() {
        OperatorProfileSelection previous = ActiveOperatorProfile.get();
        OperatorProfile profile = new OperatorProfile("wizard-guard-test", "Wizard Guard Test", false, false);
        ActiveOperatorProfile.set(new OperatorProfileSelection(profile, "preferences.xml", "worked.db", false));

        // A first rerun is under way: it holds the guard and its request waits for startRuntime.
        assertTrue(ApplicationRuntimeLauncher.beginSwitch());
        ApplicationRuntimeLauncher.requestSetupWizard();
        try {
            // A second one arrives while the first tears its windows down, and is refused.
            assertFalse(ApplicationRuntimeLauncher.rerunSetupWizard());

            assertTrue(ApplicationRuntimeLauncher.consumeSetupWizardRequest(),
                    "the refused rerun took the running switch's request with it: that switch "
                            + "would end the session and then start without the wizard");
        } finally {
            ApplicationRuntimeLauncher.endSwitch();
            ApplicationRuntimeLauncher.consumeSetupWizardRequest();
            ActiveOperatorProfile.set(previous);
        }
    }

    @Test
    void aRefusedRerunLeavesNoRequestOfItsOwn() {
        OperatorProfileSelection previous = ActiveOperatorProfile.get();
        OperatorProfile profile = new OperatorProfile("wizard-guard-test", "Wizard Guard Test", false, false);
        ActiveOperatorProfile.set(new OperatorProfileSelection(profile, "preferences.xml", "worked.db", false));

        // A plain profile switch is under way; nobody asked for the wizard.
        assertTrue(ApplicationRuntimeLauncher.beginSwitch());
        try {
            assertFalse(ApplicationRuntimeLauncher.rerunSetupWizard());

            assertFalse(ApplicationRuntimeLauncher.consumeSetupWizardRequest(),
                    "a refused rerun must not hand its wizard to the switch that refused it");
        } finally {
            ApplicationRuntimeLauncher.endSwitch();
            ApplicationRuntimeLauncher.consumeSetupWizardRequest();
            ActiveOperatorProfile.set(previous);
        }
    }
}
