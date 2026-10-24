package kst4contest.model;

import kst4contest.observe.MutableValue;
import kst4contest.observe.ObservableRoster;
import kst4contest.observe.SimpleValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives every public single-argument setter of {@link ChatPreferences} to a value that
 * differs from the compiled-in default, writes the preferences to XML, reads them into a
 * fresh instance and compares every getter. The setters are discovered by reflection so a
 * newly added preference is covered automatically; a field that is not persisted on purpose
 * must be listed in {@link #NOT_PERSISTED_SETTERS} with a reason.
 */
class ChatPreferencesRoundTripTest {

    /**
     * Setters whose value is intentionally not written to the XML file. Each entry names the
     * evidence in {@code ChatPreferences}. A field that is lost by accident must not be added
     * here silently; a known persistence bug is listed with a "BUG:" prefix so the test stays
     * green while the report stays loud.
     */
    private static final Map<String, String> NOT_PERSISTED_SETTERS = Map.ofEntries(
            Map.entry("setStoreAndRestorePreferencesFileName",
                    "Path of the XML file itself; set by the constructors, never a child "
                            + "element of <praktiKST>"),
            Map.entry("setChatState",
                    "Declared as 'working variable only for use by primarystage (title bar)'; "
                            + "writePreferencesToXmlFile never emits it"),
            Map.entry("setProgramVersion",
                    "Compiled-in about text; writePreferencesToXmlFile never emits it"),
            Map.entry("setActualQTF",
                    "Runtime antenna direction ('will be updated by user at runtime!'); "
                            + "writePreferencesToXmlFile never emits it"),
            Map.entry("setMessageHandling_autoAnswerTextSecondCat",
                    "Written as <autoAnswerTextSecondCat>, but readPreferencesFromXmlFile "
                            + "deliberately mirrors the main-category text over it because the UI "
                            + "exposes one shared auto-answer setting (see the comment above "
                            + "'messageHandling_autoAnswerTextSecondCat = "
                            + "messageHandling_autoAnswerTextMainCat')"),
            Map.entry("setMessageHandling_autoAnswerEnabledSecondCat",
                    "Same deliberate mirroring as the second-category auto-answer text; the "
                            + "main-category flag wins after every read"),
            Map.entry("setBcn_contestScoreSum",
                    "DEAD FIELD: declared with accessors only, no caller in core or app-desktop, "
                            + "never written nor read")
    );

    /**
     * Setters that are an alias for another setter and therefore have no getter of their own.
     * The canonical setter is covered by the reflective loop.
     */
    private static final Map<String, String> ALIAS_SETTERS = Map.ofEntries(
            Map.entry("setLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup",
                    "Capital-S alias of setLogsynch_storeWorkedCallSignsFileNameUDPMessageBackup")
    );

    /**
     * Setters whose argument is validated or parsed, so the generic "distinctive value per
     * type" would be rejected. The value here is a valid, non-default one.
     */
    private static final Map<String, Object> SPECIAL_VALUES = Map.ofEntries(
            Map.entry("setGuiOptions_language", "de"),
            Map.entry("setMYQRGFirstCat", "144.321.00"),
            Map.entry("setMYQRGSecondCat", "432.321.00"),
            Map.entry("setAirScout_asBandString", "4320000"),
            // Defaults to NaN ("elevation model decides"), and NaN + 7.5 is still NaN.
            Map.entry("setStn_pathAnalysisOwnHeightAslMeters", 312.5)
    );

    @TempDir
    Path temporaryDirectory;

    @Test
    void everySetterSurvivesTheXmlRoundTrip() throws Exception {
        Path preferencesFile = temporaryDirectory.resolve("roundtrip.xml");
        ChatPreferences defaults = new ChatPreferences(preferencesFile);
        ChatPreferences written = new ChatPreferences(preferencesFile);

        List<Method> setters = settersUnderTest();
        assertTrue(setters.size() > 100,
                "Expected the reflective scan to find the ~130 setters, found " + setters.size());

        List<String> unbuildable = new ArrayList<>();
        List<String> withoutGetter = new ArrayList<>();
        List<String> unchangedByValue = new ArrayList<>();
        Map<Method, Method> getterBySetter = new LinkedHashMap<>();

        for (Method setter : setters) {
            Method getter = findGetter(setter);
            if (getter == null) {
                withoutGetter.add(setter.getName());
                continue;
            }
            Object valueToSet = distinctiveValue(setter, getter.invoke(defaults));
            if (valueToSet == null) {
                unbuildable.add(setter.getName() + "(" + setter.getParameterTypes()[0].getSimpleName() + ")");
                continue;
            }
            setter.invoke(written, valueToSet);
            getterBySetter.put(setter, getter);

            if (sameValue(getter.invoke(defaults), getter.invoke(written))) {
                unchangedByValue.add(setter.getName() + " still reads " + describe(getter.invoke(written)));
            }
        }

        assertEquals(List.of(), withoutGetter,
                "Setters without a matching getter (add them to ALIAS_SETTERS with a reason)");
        assertEquals(List.of(), unbuildable,
                "Setters whose parameter type this test cannot construct");
        assertEquals(List.of(), unchangedByValue,
                "Setters whose distinctive value equals the default (the round trip could not "
                        + "detect a loss for them)");

        assertTrue(written.writePreferencesToXmlFile(), "writing " + preferencesFile + " failed");

        ChatPreferences restored = new ChatPreferences(preferencesFile);
        assertTrue(restored.readPreferencesFromXmlFile(), "reading " + preferencesFile + " failed");

        List<String> lost = new ArrayList<>();
        for (Map.Entry<Method, Method> entry : getterBySetter.entrySet()) {
            Method getter = entry.getValue();
            Object expected = getter.invoke(written);
            Object actual = getter.invoke(restored);
            if (!sameValue(expected, actual)) {
                lost.add(entry.getKey().getName() + ": written=" + describe(expected)
                        + " restored=" + describe(actual));
            }
        }

        assertEquals(List.of(), lost,
                "Fields that did not survive writePreferencesToXmlFile -> readPreferencesFromXmlFile");
    }

    @Test
    void bundledTemplatePreferencesLoadWithKnownValues() throws IOException {
        Path templateCopy = temporaryDirectory.resolve("template.xml");
        try (InputStream template = ChatPreferences.class
                .getResourceAsStream(ChatPreferences.PREFERENCE_RESOURCE)) {
            assertNotNull(template, "bundled resource " + ChatPreferences.PREFERENCE_RESOURCE);
            Files.copy(template, templateCopy, StandardCopyOption.REPLACE_EXISTING);
        }

        ChatPreferences fromTemplate = new ChatPreferences(templateCopy);
        assertTrue(fromTemplate.readPreferencesFromXmlFile());

        // Values the seeded template carries today; the station block uses the legacy tag names.
        assertEquals("DO5AMF", fromTemplate.getStn_loginCallSign());
        assertEquals("JO54ME", fromTemplate.getStn_loginLocatorMainCat());
        assertEquals(2, fromTemplate.getLoginChatCategoryMain().getCategoryNumber());
        assertEquals(3, fromTemplate.getLoginChatCategorySecond().getCategoryNumber());
        assertTrue(fromTemplate.isLoginToSecondChatEnabled());
        assertEquals(1333.0, fromTemplate.getStn_maxQRBDefault());
        assertEquals(12060, fromTemplate.getLogsynch_ucxUDPWkdCallListenerPort());
        assertEquals(8000, fromTemplate.getNotify_dxclusterServerPort());
        assertEquals(9872, fromTemplate.getAirScout_asCommunicationPort());
        assertTrue(fromTemplate.getLst_txtShortCutBtnList().snapshot().contains("sked"));
        assertTrue(fromTemplate.isStn_bandActive144());
        assertFalse(fromTemplate.isStn_bandActive10G());
        assertTrue(fromTemplate.isStn_useQtfFilterDirectionAsAntennaHeading());
        assertTrue(fromTemplate.isQtfFilterDirectionAsAntennaHeadingEnabled());
    }

    @Test
    void legacyFileWithoutConfigVersionMapsOldTagNamesOntoCurrentFields() throws IOException {
        Path legacyFile = temporaryDirectory.resolve("legacy.xml");
        Files.writeString(legacyFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <station>
                        <LoginCallSign>DL1ABC-2</LoginCallSign>
                        <LoginPassword>secret</LoginPassword>
                        <LoginDisplayedName>Old Name</LoginDisplayedName>
                        <LoginLocator>JO31AA</LoginLocator>
                        <ChatCategory>3</ChatCategory>
                    </station>
                    <notifications>
                        <notify_dxclusterServerPort>8123</notify_dxclusterServerPort>
                    </notifications>
                </praktiKST>
                """);

        ChatPreferences legacy = new ChatPreferences(legacyFile);
        assertTrue(legacy.readPreferencesFromXmlFile());

        assertEquals("DL1ABC-2", legacy.getStn_loginCallSign());
        assertEquals("secret", legacy.getStn_loginPassword());
        assertEquals("Old Name", legacy.getStn_loginNameMainCat());
        assertEquals("JO31AA", legacy.getStn_loginLocatorMainCat());
        assertEquals(3, legacy.getLoginChatCategoryMain().getCategoryNumber());
        assertEquals(8123, legacy.getNotify_dxclusterServerPort());
        assertTrue(legacy.isStn_useQtfFilterDirectionAsAntennaHeading());
        assertTrue(legacy.isQtfFilterDirectionAsAntennaHeadingEnabled());
        // A tag that is missing keeps the compiled-in default.
        assertEquals(new ChatPreferences(legacyFile).getStn_on4kstServersPort(),
                legacy.getStn_on4kstServersPort());
    }

    // ---------------------------------------------------------------------------------------
    // reflection helpers
    // ---------------------------------------------------------------------------------------

    private static List<Method> settersUnderTest() {
        List<Method> setters = new ArrayList<>();
        for (Method method : ChatPreferences.class.getMethods()) {
            if (method.getDeclaringClass() != ChatPreferences.class
                    || Modifier.isStatic(method.getModifiers())
                    || !method.getName().startsWith("set")
                    || method.getParameterCount() != 1
                    || NOT_PERSISTED_SETTERS.containsKey(method.getName())
                    || ALIAS_SETTERS.containsKey(method.getName())) {
                continue;
            }
            setters.add(method);
        }
        setters.sort(Comparator.comparing(Method::getName));
        return setters;
    }

    private static Method findGetter(Method setter) {
        String property = setter.getName().substring(3);
        for (String prefix : new String[] {"get", "is"}) {
            try {
                return ChatPreferences.class.getMethod(prefix + property);
            } catch (NoSuchMethodException ignored) {
                // try the next prefix
            }
        }
        return null;
    }

    /**
     * Builds a value that differs from the current one. Returns null when the parameter type
     * cannot be constructed here.
     */
    private static Object distinctiveValue(Method setter, Object currentValue) {
        Object special = SPECIAL_VALUES.get(setter.getName());
        if (special != null) {
            return special;
        }

        Class<?> type = setter.getParameterTypes()[0];
        String name = setter.getName().substring(3);
        Object current = unwrap(currentValue);

        if (type == boolean.class) {
            return !(current instanceof Boolean b && b);
        }
        if (type == int.class) {
            return (current instanceof Integer i ? i : 0) + 7;
        }
        if (type == long.class) {
            return (current instanceof Long l ? l : 0L) + 7L;
        }
        if (type == double.class) {
            return (current instanceof Double d ? d : 0.0) + 7.5;
        }
        if (type == String.class) {
            return "rt-" + name;
        }
        if (type == double[].class) {
            double[] base = current instanceof double[] array && array.length > 0
                    ? array : new double[] {0.0, 0.0};
            double[] changed = new double[base.length];
            for (int i = 0; i < base.length; i++) {
                // Window positions default to NaN ("not yet placed"); NaN + 7 is still NaN.
                changed[i] = Double.isNaN(base[i]) ? 100.0 + i : base[i] + 7.0 + i;
            }
            return changed;
        }
        if (Collection.class.isAssignableFrom(type)) {
            return List.of("rt-" + name + "-a", "rt-" + name + "-b");
        }
        if (type == ChatCategory.class) {
            int currentNumber = current instanceof Integer i ? i : 2;
            return new ChatCategory(currentNumber == 5 ? 6 : 5);
        }
        if (type == MutableValue.class) {
            return new SimpleValue<>(current instanceof Double d ? d + 7.5 : 7.5);
        }
        return null;
    }

    /** Reduces observable wrappers and domain objects to plain comparable values. */
    private static Object unwrap(Object value) {
        if (value instanceof MutableValue<?> mutable) {
            return mutable.get();
        }
        if (value instanceof ObservableRoster<?> roster) {
            return roster.snapshot();
        }
        if (value instanceof ChatCategory category) {
            return category.getCategoryNumber();
        }
        return value;
    }

    private static boolean sameValue(Object left, Object right) {
        Object a = unwrap(left);
        Object b = unwrap(right);
        if (a instanceof double[] x && b instanceof double[] y) {
            return Arrays.equals(x, y);
        }
        if (a instanceof Double x && b instanceof Double y) {
            return Math.abs(x - y) < 1e-9;
        }
        return Objects.equals(a, b);
    }

    private static String describe(Object value) {
        Object plain = unwrap(value);
        if (plain instanceof double[] array) {
            return Arrays.toString(array);
        }
        return String.valueOf(plain);
    }
}
