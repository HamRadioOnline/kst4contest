package kst4contest.controller;

import kst4contest.model.ChatPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When a profile counts as not set up yet. The setup wizard appears on exactly this
 * condition, so a wrong answer either nags a working station or leaves a newcomer alone
 * with the template's callsign.
 */
class LoginConfigurationTest {

    @TempDir
    Path temporaryDirectory;

    private ChatPreferences preferences(String callsign, String password) {
        ChatPreferences preferences = new ChatPreferences(temporaryDirectory.resolve("unused.xml"));
        preferences.setStn_loginCallSign(callsign);
        preferences.setStn_loginPassword(password);
        return preferences;
    }

    @Test
    void anEmptyCallsignIsUnconfigured() {
        assertTrue(LoginConfiguration.isUnconfigured(preferences("", "secret")));
        assertTrue(LoginConfiguration.isUnconfigured(preferences("   ", "secret")));
    }

    @Test
    void theTemplateCredentialsAreUnconfigured() {
        ChatPreferences template = preferences("DO5AMF", "changeMe");

        assertTrue(LoginConfiguration.carriesTemplateCredentials(template));
        assertTrue(LoginConfiguration.isUnconfigured(template));
    }

    @Test
    void theTemplateCallsignWithItsOwnPasswordIsConfigured() {
        // The callsign belongs to a real operator; only the pair marks an untouched template.
        ChatPreferences owner = preferences("DO5AMF", "a-real-password");

        assertFalse(LoginConfiguration.carriesTemplateCredentials(owner));
        assertFalse(LoginConfiguration.isUnconfigured(owner));
    }

    @Test
    void aRealLoginIsConfigured() {
        assertFalse(LoginConfiguration.isUnconfigured(preferences("DL0TEST", "secret")));
        assertFalse(LoginConfiguration.isUnconfigured(preferences("DL0TEST/P", "")));
    }

    @Test
    void theConstantsMatchTheBundledTemplate() throws IOException {
        Path templateCopy = temporaryDirectory.resolve("template.xml");
        try (InputStream template = ChatPreferences.class
                .getResourceAsStream(ChatPreferences.PREFERENCE_RESOURCE)) {
            assertNotNull(template, "bundled resource " + ChatPreferences.PREFERENCE_RESOURCE);
            Files.copy(template, templateCopy, StandardCopyOption.REPLACE_EXISTING);
        }

        ChatPreferences fromTemplate = new ChatPreferences(templateCopy);
        assertTrue(fromTemplate.readPreferencesFromXmlFile());

        assertTrue(LoginConfiguration.carriesTemplateCredentials(fromTemplate),
                "the template's credentials changed; a fresh installation would no longer "
                        + "be recognised as unconfigured");
    }
}
