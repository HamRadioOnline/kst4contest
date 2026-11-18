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
    void rerunSetupWizardWhileSwitchInProgressReturnsFalseAndClearsRequest() {
        OperatorProfileSelection previous = ActiveOperatorProfile.get();
        OperatorProfile profile = new OperatorProfile("wizard-guard-test", "Wizard Guard Test", false, false);
        ActiveOperatorProfile.set(new OperatorProfileSelection(profile, "preferences.xml", "worked.db", false));
        assertTrue(ApplicationRuntimeLauncher.beginSwitch());
        try {
            assertFalse(ApplicationRuntimeLauncher.rerunSetupWizard());
            assertFalse(ApplicationRuntimeLauncher.consumeSetupWizardRequest(),
                    "a refused rerun must not leave a wizard request behind");
        } finally {
            ApplicationRuntimeLauncher.endSwitch();
            ApplicationRuntimeLauncher.consumeSetupWizardRequest();
            ActiveOperatorProfile.set(previous);
        }
    }
}
