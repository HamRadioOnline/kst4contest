package kst4contest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

		preferences.setBcn_beaconsEnabledMainCat(false);
		preferences.setBcn_beaconsEnabledSecondCat(true);

		assertFalse(preferences.isBcn_beaconsEnabledMainCat());
		assertTrue(preferences.isBcn_beaconsEnabledSecondCat());
		assertEquals(List.of(false), mainChanges);
		assertEquals(List.of(true), secondChanges);
	}

	@Test
	void explicitWritePersistsBeaconActivationButAnUnsavedChangeDoesNot() {
		ChatPreferences preferences = preferences("persistence.xml");
		preferences.setBcn_beaconsEnabledMainCat(false);
		preferences.setBcn_beaconsEnabledSecondCat(true);
		assertTrue(preferences.writePreferencesToXmlFile());

		preferences.setBcn_beaconsEnabledMainCat(true);
		preferences.setBcn_beaconsEnabledSecondCat(false);
		assertTrue(preferences.writeLayoutPreferencesToXmlFile());

		ChatPreferences restored = preferences("persistence.xml");
		restored.readPreferencesFromXmlFile();

		assertFalse(restored.isBcn_beaconsEnabledMainCat());
		assertTrue(restored.isBcn_beaconsEnabledSecondCat());
	}

	private ChatPreferences preferences(String fileName) {
		return new ChatPreferences(tempDir.resolve(fileName));
	}
}
