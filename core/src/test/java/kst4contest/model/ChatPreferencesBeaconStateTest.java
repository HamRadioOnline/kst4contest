package kst4contest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChatPreferencesBeaconStateTest {

	@TempDir
	Path tempDir;

	@Test
	void beaconActivationChangesAreObservableAndIndependent() {
		ChatPreferences preferences = preferences("observable.xml");
		List<Boolean> mainChanges = new ArrayList<>();
		List<Boolean> secondChanges = new ArrayList<>();
		preferences.bcnBeaconsEnabledMainCatProperty().addListener(mainChanges::add);
		preferences.bcnBeaconsEnabledSecondCatProperty().addListener(secondChanges::add);

		preferences.setBcn_beaconsEnabledMainCat(true);
		preferences.setBcn_beaconsEnabledSecondCat(true);
		preferences.setBcn_beaconsEnabledMainCat(false);

		assertFalse(preferences.isBcn_beaconsEnabledMainCat());
		assertTrue(preferences.isBcn_beaconsEnabledSecondCat());
		assertEquals(List.of(true, false), mainChanges);
		assertEquals(List.of(true), secondChanges);
	}

	@Test
	void aNewPreferencesObjectStartsWithBothBeaconsOff() {
		ChatPreferences preferences = preferences("fresh.xml");

		assertFalse(preferences.isBcn_beaconsEnabledMainCat());
		assertFalse(preferences.isBcn_beaconsEnabledSecondCat());
	}

	/** Issue #171: a beacon has to be switched on by the operator in every session. */
	@Test
	void beaconActivationIsNeitherWrittenToTheFileNorRestoredFromIt() throws IOException {
		ChatPreferences preferences = preferences("session-only.xml");
		preferences.setBcn_beaconsEnabledMainCat(true);
		preferences.setBcn_beaconsEnabledSecondCat(true);
		assertTrue(preferences.writePreferencesToXmlFile());

		String written = Files.readString(tempDir.resolve("session-only.xml"));
		assertTrue(written.contains("<bcn_beaconsEnabledMainCat>false</bcn_beaconsEnabledMainCat>"), written);
		assertTrue(written.contains("<beaconCQEnabled>false</beaconCQEnabled>"), written);
		assertTrue(written.contains("<bcn_beaconsEnabledSecondCat>false</bcn_beaconsEnabledSecondCat>"), written);
		assertTrue(written.contains("<beaconCQEnabledSecondCat>false</beaconCQEnabledSecondCat>"), written);

		ChatPreferences restored = preferences("session-only.xml");
		restored.readPreferencesFromXmlFile();

		assertFalse(restored.isBcn_beaconsEnabledMainCat());
		assertFalse(restored.isBcn_beaconsEnabledSecondCat());
	}

	/** Files of earlier releases carry the stored switch; it must not start a beacon any more. */
	@Test
	void aFileOfAnEarlierReleaseWithEnabledBeaconsLoadsThemOffAndKeepsTextAndInterval() throws IOException {
		Files.writeString(tempDir.resolve("legacy.xml"),
				"<praktiKST><beaconCQ>"
						+ "<beaconCQText>CQ LEGACY</beaconCQText>"
						+ "<beaconCQIntervalMinutes>15</beaconCQIntervalMinutes>"
						+ "<beaconCQEnabled>true</beaconCQEnabled>"
						+ "<bcn_beaconsEnabledMainCat>true</bcn_beaconsEnabledMainCat>"
						+ "<beaconCQEnabledSecondCat>true</beaconCQEnabledSecondCat>"
						+ "<bcn_beaconsEnabledSecondCat>true</bcn_beaconsEnabledSecondCat>"
						+ "</beaconCQ></praktiKST>");

		ChatPreferences restored = preferences("legacy.xml");
		restored.readPreferencesFromXmlFile();

		assertFalse(restored.isBcn_beaconsEnabledMainCat());
		assertFalse(restored.isBcn_beaconsEnabledSecondCat());
		assertEquals("CQ LEGACY", restored.getBcn_beaconTextMainCat());
		assertEquals(15, restored.getBcn_beaconIntervalInMinutesMainCat());
	}

	/** The switch is session state: reading the file again must not end a running beacon. */
	@Test
	void readingTheFileAgainLeavesABeaconOfTheRunningSessionOn() {
		ChatPreferences preferences = preferences("reread.xml");
		assertTrue(preferences.writePreferencesToXmlFile());
		preferences.setBcn_beaconsEnabledMainCat(true);

		preferences.readPreferencesFromXmlFile();

		assertTrue(preferences.isBcn_beaconsEnabledMainCat());
	}

	/** The template seeds every first installation and every new operator profile. */
	@Test
	void theBundledTemplateShipsBothBeaconsOffAndAnIntervalAboveTheMinimum() throws IOException {
		try (InputStream template = ChatPreferences.class.getResourceAsStream(ChatPreferences.PREFERENCE_RESOURCE)) {
			Files.copy(template, tempDir.resolve("template.xml"));
		}

		ChatPreferences seeded = preferences("template.xml");
		seeded.readPreferencesFromXmlFile();

		assertFalse(seeded.isBcn_beaconsEnabledMainCat());
		assertFalse(seeded.isBcn_beaconsEnabledSecondCat());
		assertEquals(10, seeded.getBcn_beaconIntervalInMinutesMainCat());
		assertEquals(10, seeded.getBcn_beaconIntervalInMinutesSecondCat());
	}

	private ChatPreferences preferences(String fileName) {
		return new ChatPreferences(tempDir.resolve(fileName));
	}
}
