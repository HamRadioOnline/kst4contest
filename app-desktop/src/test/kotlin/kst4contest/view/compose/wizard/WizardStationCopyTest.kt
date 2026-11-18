package kst4contest.view.compose.wizard

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.io.File
import java.lang.reflect.Modifier
import java.nio.file.Path

/**
 * What "the station" is when one profile takes it from another.
 *
 * The two lists under src/test/resources are the decision, one line per ChatPreferences
 * setter. A setter in neither list fails the first test, so a new setting cannot be forgotten
 * here: somebody has to say whether another operator at the same station should inherit it.
 */
class WizardStationCopyTest {

    private fun lines(name: String): Set<String> =
        File("src/test/resources/$name").readLines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    private val stationSetters = lines("wizard-station-setters.txt")
    private val otherSetters = lines("wizard-other-setters.txt")

    /** Every public setter with one parameter, the shape every preference has. */
    private fun allSetters(): Set<String> =
        ChatPreferences::class.java.methods
            .filter { it.name.startsWith("set") && it.parameterCount == 1 && Modifier.isPublic(it.modifiers) }
            .map { it.name }
            .toSet()

    private fun prefs(configure: ChatPreferences.() -> Unit = {}): ChatPreferences =
        ChatPreferences(Path.of("wizard-station-copy-test-unused.xml")).apply(configure)

    @Test
    fun everySetterIsClassified() {
        val all = allSetters()

        assertEquals(
            emptySet<String>(),
            all - stationSetters - otherSetters,
            "new ChatPreferences setters: add each to wizard-station-setters.txt (and to copyStation) " +
                "or to wizard-other-setters.txt",
        )
        assertEquals(emptySet<String>(), (stationSetters + otherSetters) - all, "listed setters that no longer exist")
        assertEquals(emptySet<String>(), stationSetters.intersect(otherSetters), "a setter cannot be in both lists")
    }

    @Test
    fun copyStationCallsExactlyTheStationSetters() {
        val target = Mockito.mock(ChatPreferences::class.java)

        copyStation(prefs(), target)

        val called = Mockito.mockingDetails(target).invocations
            .map { it.method.name }
            .filter { it.startsWith("set") }

        assertEquals(stationSetters, called.toSet(), "copyStation and wizard-station-setters.txt disagree")
        assertEquals(called.size, called.toSet().size, "a setter was called twice")
        assertTrue(
            Mockito.mockingDetails(target).invocations.none { it.method.name == "writePreferencesToXmlFile" },
            "copying must not write a file",
        )
    }

    @Test
    fun theStationArrivesIncludingWhatTheWizardNeverAsks() {
        val donor = prefs {
            setStn_loginLocatorMainCat("JO50AA")
            setStn_bandActive144(true)
            setStn_bandActive1240(true)
            setStn_bandActive432(false)
            setLoginChatCategoryMain(ChatCategory(2))
            setLoginToSecondChatEnabled(true)
            setLoginChatCategorySecond(ChatCategory(3))
            setStn_qtfDefault(270.0)
            setStn_maxQRBDefault(800.0)
            setStn_pathAnalysisDemRootDirectory("/srv/dem")
            setStn_pathAnalysisContestMarginDb(9.0)
            setTrxSynch_so2rEnabled(true)
            setStn_pstRotator2Enabled(true)
            setStn_pstRotator2Port(12020)
            // Several log sources at once: the wizard's own question could not express this.
            setLogsynch_wintestNetworkListenerEnabled(true)
            setLogsynch_fileBasedWkdCallInterpreterEnabled(true)
            setLogsynch_wintestNetworkStationNameOfWintestClient1("RUN")
            setLogsynch_wintestUsePassQrg(true)
            setAirScout_asBandString("4320000")
        }
        val target = prefs {
            setStn_bandActive432(true)
            setLoginChatCategoryMain(ChatCategory(3))
        }

        copyStation(donor, target)

        assertEquals("JO50AA", target.getStn_loginLocatorMainCat())
        assertTrue(target.isStn_bandActive144())
        assertTrue(target.isStn_bandActive1240())
        assertFalse(target.isStn_bandActive432(), "a band the donor does not have goes off")
        assertEquals(2, target.getLoginChatCategoryMain().categoryNumber)
        assertTrue(target.isLoginToSecondChatEnabled())
        assertEquals(3, target.getLoginChatCategorySecond().categoryNumber)
        assertEquals(270.0, target.getStn_qtfDefault())
        assertEquals(800.0, target.getStn_maxQRBDefault())
        assertEquals("/srv/dem", target.getStn_pathAnalysisDemRootDirectory())
        assertEquals(9.0, target.getStn_pathAnalysisContestMarginDb())
        assertTrue(target.isTrxSynch_so2rEnabled())
        assertTrue(target.isStn_pstRotator2Enabled())
        assertEquals(12020, target.getStn_pstRotator2Port())
        assertTrue(target.isLogsynch_wintestNetworkListenerEnabled())
        assertTrue(target.isLogsynch_fileBasedWkdCallInterpreterEnabled())
        assertEquals("RUN", target.getLogsynch_wintestNetworkStationNameOfWintestClient1())
        assertTrue(target.isLogsynch_wintestUsePassQrg())
        assertEquals("4320000", target.getAirScout_asBandString())
    }

    @Test
    fun whatBelongsToTheOperatorStays() {
        val donor = prefs {
            setStn_loginCallSign("DL0TEST")
            setStn_loginPassword("donor-secret")
            setStn_loginNameMainCat("Donor")
            setStn_loginNameSecondCat("Donor SHF")
            setGuiOptions_language("en")
            setGUI_darkModeActiveByDefault(true)
            setStn_on4kstServersDns("donor.example")
            setNotify_DXCSrv_SpottersCallSign("DL0TEST")
            setBcn_beaconTextMainCat("donor beacon")
        }
        val target = prefs {
            setStn_loginCallSign("DM5XX")
            setStn_loginPassword("own-secret")
            setStn_loginNameMainCat("Own")
            setStn_loginNameSecondCat("Own SHF")
            setGuiOptions_language("de")
            setGUI_darkModeActiveByDefault(false)
            setStn_on4kstServersDns("own.example")
            setNotify_DXCSrv_SpottersCallSign("DM5XX")
            setBcn_beaconTextMainCat("own beacon")
        }

        copyStation(donor, target)

        assertEquals("DM5XX", target.getStn_loginCallSign())
        assertEquals("own-secret", target.getStn_loginPassword())
        assertEquals("Own", target.getStn_loginNameMainCat())
        assertEquals("Own SHF", target.getStn_loginNameSecondCat())
        assertEquals("de", target.getGuiOptions_language())
        assertFalse(target.isGUI_darkModeActiveByDefault())
        assertEquals("own.example", target.getStn_on4kstServersDns())
        assertEquals("DM5XX", target.getNotify_DXCSrv_SpottersCallSign().get())
        assertEquals("own beacon", target.getBcn_beaconTextMainCat())
    }

    @Test
    fun theDonorIsNotChangedAndSharesNoObjectWithTheTarget() {
        val donor = prefs { setLoginChatCategoryMain(ChatCategory(2)) }
        val target = prefs()

        copyStation(donor, target)
        target.getLoginChatCategoryMain().categoryNumber = 7

        assertEquals(2, donor.getLoginChatCategoryMain().categoryNumber, "the two profiles must not share a category object")
    }
}
