package kst4contest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Files;
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
		preferences.setBcn_beaconsEnabledMainCat(false);
		preferences.setBcn_beaconsEnabledSecondCat(true);

		assertFalse(preferences.isBcn_beaconsEnabledMainCat());
		assertTrue(preferences.isBcn_beaconsEnabledSecondCat());
		assertEquals(List.of(true, false), mainChanges);
		assertEquals(List.of(true), secondChanges);
	}

	@Test
	void activationIsSessionOnlyAndEveryFullWriteStoresFalse() throws Exception {
		ChatPreferences preferences = preferences("persistence.xml");
		preferences.setBcn_beaconsEnabledMainCat(true);
		preferences.setBcn_beaconsEnabledSecondCat(true);
		assertTrue(preferences.writePreferencesToXmlFile());

		String xml = Files.readString(tempDir.resolve("persistence.xml"));
		assertTrue(xml.contains("<beaconCQEnabled>false</beaconCQEnabled>"));
		assertTrue(xml.contains("<bcn_beaconsEnabledMainCat>false</bcn_beaconsEnabledMainCat>"));
		assertTrue(xml.contains("<beaconCQEnabledSecondCat>false</beaconCQEnabledSecondCat>"));
		assertTrue(xml.contains("<bcn_beaconsEnabledSecondCat>false</bcn_beaconsEnabledSecondCat>"));

		preferences.setBcn_beaconsEnabledMainCat(true);
		preferences.setBcn_beaconsEnabledSecondCat(true);
		assertTrue(preferences.writeLayoutPreferencesToXmlFile());

		ChatPreferences restored = preferences("persistence.xml");
		restored.readPreferencesFromXmlFile();

		assertFalse(restored.isBcn_beaconsEnabledMainCat());
		assertFalse(restored.isBcn_beaconsEnabledSecondCat());
	}

	@Test
	void storedActivationIsIgnoredAndIntervalsBelowFiveAreNormalized() throws Exception {
		Path file = tempDir.resolve("legacy.xml");
		Files.writeString(file, """
				<?xml version="1.0" encoding="UTF-8"?>
				<praktiKST><beaconCQ>
				  <beaconCQEnabled>true</beaconCQEnabled>
				  <beaconCQEnabledSecondCat>true</beaconCQEnabledSecondCat>
				  <beaconCQIntervalMinutes>1</beaconCQIntervalMinutes>
				  <beaconCQIntervalMinutesSecondCat>9</beaconCQIntervalMinutesSecondCat>
				</beaconCQ></praktiKST>
				""");

		ChatPreferences restored = new ChatPreferences(file);
		assertTrue(restored.readPreferencesFromXmlFile());

		assertFalse(restored.isBcn_beaconsEnabledMainCat());
		assertFalse(restored.isBcn_beaconsEnabledSecondCat());
		assertEquals(5, restored.getBcn_beaconIntervalInMinutesMainCat());
		assertEquals(9, restored.getBcn_beaconIntervalInMinutesSecondCat());
	}

	private ChatPreferences preferences(String fileName) {
		return new ChatPreferences(tempDir.resolve(fileName));
	}
}
