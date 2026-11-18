package kst4contest.view;

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
}
