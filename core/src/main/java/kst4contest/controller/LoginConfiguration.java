package kst4contest.controller;

import kst4contest.model.ChatPreferences;

/**
 * Tells whether a profile has login credentials an operator entered.
 *
 * <p>Two states count as unconfigured. An empty callsign is what every created or duplicated
 * profile starts with, because {@link OperatorProfileManagementService} clears the
 * credentials on purpose. The bundled template's credentials are what the root profile of a
 * fresh installation carries. Neither can log in as the operator, so neither needs a flag of
 * its own in {@code preferences.xml}.</p>
 */
public final class LoginConfiguration {

    /** Callsign shipped in {@code praktiKSTpreferences.xml}; held equal by a test. */
    public static final String TEMPLATE_CALLSIGN = "DO5AMF";

    /** Password shipped in {@code praktiKSTpreferences.xml}; held equal by a test. */
    public static final String TEMPLATE_PASSWORD = "changeMe";

    private LoginConfiguration() {
        // Utility class.
    }

    /**
     * Whether the preferences still hold the bundled template's credentials.
     *
     * <p>Only the pair counts. The callsign alone belongs to a real operator.</p>
     *
     * @param preferences preferences of the profile to judge
     * @return true for an untouched template
     */
    public static boolean carriesTemplateCredentials(final ChatPreferences preferences) {
        return TEMPLATE_CALLSIGN.equalsIgnoreCase(trimmed(preferences.getStn_loginCallSign()))
                && TEMPLATE_PASSWORD.equals(preferences.getStn_loginPassword());
    }

    /**
     * Whether the profile has no login an operator entered.
     *
     * @param preferences preferences of the profile to judge
     * @return true when the callsign is empty or the template's credentials are still there
     */
    public static boolean isUnconfigured(final ChatPreferences preferences) {
        return trimmed(preferences.getStn_loginCallSign()).isEmpty()
                || carriesTemplateCredentials(preferences);
    }

    private static String trimmed(final String value) {
        return value == null ? "" : value.trim();
    }
}
