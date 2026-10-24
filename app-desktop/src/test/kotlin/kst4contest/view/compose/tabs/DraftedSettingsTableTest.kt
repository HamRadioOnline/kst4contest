package kst4contest.view.compose.tabs

import kst4contest.model.Band
import kst4contest.model.ChatPreferences
import kst4contest.observe.SimpleRoster
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.i18n.LanguageStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/**
 * Every drafted setting of every tab, with a real [SettingsDraft]: unchanged in the
 * preferences after the edit, changed after apply, unchanged after discard.
 *
 * The tab tests pin each tab's rules with a draft that applies at once, so they cannot tell
 * a setting that goes through the draft from one that writes ChatPreferences directly. This
 * table can, setter by setter. A setting added to a tab belongs in it; one that is meant to
 * stay immediate (login credentials, language, colours, actions) belongs in the spec instead.
 */
class DraftedSettingsTableTest {

    @TempDir
    lateinit var directory: Path

    /** All drafted tabs over one preferences file and one draft. */
    private class Fixture(val prefs: ChatPreferences, val monitored: SimpleRoster<String> = SimpleRoster()) {
        val draft = SettingsDraft()
        val station = StationTabState(prefs, draft, { }, { })
        val logSynch = LogSynchTabState(prefs, draft, { }, { })
        val trx = TrxSynchTabState(prefs, draft) { }
        val airscout = AirscoutTabState(prefs, draft)
        val notification = NotificationTabState(prefs, draft, monitored, { }, { null })
        val shortcuts = ShortcutsTabState(prefs, draft, { }, { })
        val beacon = BeaconTabState(prefs, draft, { }, { }, "Microwave", "144/432 MHz")
        val messages = MessageHandlingTabState(prefs, draft) { }
        val gui = GuiOptionsTabState(prefs, draft, LanguageStore({ "en" }, { }, Locale.UK))
    }

    /** One drafted setting: how the tab changes it, and how the stored value is read. */
    private class Case(val name: String, val change: (Fixture) -> Unit, val stored: (Fixture) -> Any?)

    private fun flag(name: String, read: (Fixture) -> Boolean, write: (Fixture, Boolean) -> Unit) =
        Case(name, { f -> write(f, !read(f)) }, read)

    private fun cases(): List<Case> = listOf(
        // Station, all but the login credentials
        Case("station beam width", { it.station.stn_antennaBeamWidthDeg += 1.5 }, { it.prefs.getStn_antennaBeamWidthDeg() }),
        flag("station 10G", { it.station.stn_bandActive10G }) { f, v -> f.station.stn_bandActive10G = v },
        flag("station 1240", { it.station.stn_bandActive1240 }) { f, v -> f.station.stn_bandActive1240 = v },
        flag("station 144", { it.station.stn_bandActive144 }) { f, v -> f.station.stn_bandActive144 = v },
        flag("station 2300", { it.station.stn_bandActive2300 }) { f, v -> f.station.stn_bandActive2300 = v },
        flag("station 3400", { it.station.stn_bandActive3400 }) { f, v -> f.station.stn_bandActive3400 = v },
        flag("station 432", { it.station.stn_bandActive432 }) { f, v -> f.station.stn_bandActive432 = v },
        flag("station 50", { it.station.stn_bandActive50 }) { f, v -> f.station.stn_bandActive50 = v },
        flag("station 5600", { it.station.stn_bandActive5600 }) { f, v -> f.station.stn_bandActive5600 = v },
        flag("station 70", { it.station.stn_bandActive70 }) { f, v -> f.station.stn_bandActive70 = v },
        Case("station max QRB", { it.station.stn_maxQRBDefault += 1.5 }, { it.prefs.getStn_maxQRBDefault() }),
        Case("station server host", { it.station.stn_on4kstServersDns += "x" }, { it.prefs.getStn_on4kstServersDns() }),
        Case("station server port", { it.station.stn_on4kstServersPort += 1 }, { it.prefs.getStn_on4kstServersPort() }),
        Case("station target gain", { it.station.stn_pathAnalysisDefaultTargetAntennaGainDbi += 1.5 },
            { it.prefs.getStn_pathAnalysisDefaultTargetAntennaGainDbi() }),
        Case("station target power", { it.station.stn_pathAnalysisDefaultTargetTxPowerWatts += 1.5 },
            { it.prefs.getStn_pathAnalysisDefaultTargetTxPowerWatts() }),
        Case("station DEM directory", { it.station.stn_pathAnalysisDemRootDirectory += "x" },
            { it.prefs.getStn_pathAnalysisDemRootDirectory() }),
        Case("station own gain", { it.station.stn_pathAnalysisOwnAntennaGainDbi += 1.5 },
            { it.prefs.getStn_pathAnalysisOwnAntennaGainDbi() }),
        Case("station own height", { it.station.stn_pathAnalysisOwnAntennaHeightMeters += 1.5 },
            { it.prefs.getStn_pathAnalysisOwnAntennaHeightMeters() }),
        Case("station own power", { it.station.stn_pathAnalysisOwnTxPowerWatts += 1.5 },
            { it.prefs.getStn_pathAnalysisOwnTxPowerWatts() }),
        // Through the field's commit, which validates and then stages.
        Case("station own ground height ASL", { it.station.commitOwnHeightAsl("312") },
            { it.prefs.getStn_pathAnalysisOwnHeightAslMeters() }),
        flag("station target on highest point", { it.station.stn_pathAnalysisDxHighestPointInSquare }) { f, v ->
            f.station.stn_pathAnalysisDxHighestPointInSquare = v
        },
        flag("station rotator", { it.station.stn_pstRotatorEnabled }) { f, v -> f.station.stn_pstRotatorEnabled = v },
        flag("station QTF filter heading sync", { it.station.stn_useQtfFilterDirectionAsAntennaHeading }) { f, v ->
            f.station.stn_useQtfFilterDirectionAsAntennaHeading = v
        },
        Case("station rotator host", { it.station.stn_pstRotatorHost += "x" }, { it.prefs.getStn_pstRotatorHost() }),
        Case("station rotator port", { it.station.stn_pstRotatorPort += 1 }, { it.prefs.getStn_pstRotatorPort() }),
        Case("station QTF", { it.station.stn_qtfDefault += 1.5 }, { it.prefs.getStn_qtfDefault() }),
        flag("station second chat", { it.station.loginToSecondChatEnabled }) { f, v -> f.station.loginToSecondChatEnabled = v },

        // Log synch, all but the file chooser
        flag("log file polling", { it.logSynch.logsynch_fileBasedWkdCallInterpreterEnabled }) { f, v ->
            f.logSynch.logsynch_fileBasedWkdCallInterpreterEnabled = v
        },
        flag("log UCX listener", { it.logSynch.logsynch_ucxUDPWkdCallListenerEnabled }) { f, v ->
            f.logSynch.logsynch_ucxUDPWkdCallListenerEnabled = v
        },
        Case("log UCX port", { it.logSynch.logsynch_ucxUDPWkdCallListenerPort += 1 },
            { it.prefs.getLogsynch_ucxUDPWkdCallListenerPort() }),
        flag("log Win-Test listener", { it.logSynch.logsynch_wintestNetworkListenerEnabled }) { f, v ->
            f.logSynch.logsynch_wintestNetworkListenerEnabled = v
        },
        Case("log Win-Test port", { it.logSynch.logsynch_wintestNetworkPort += 1 }, { it.prefs.getLogsynch_wintestNetworkPort() }),
        Case("log Win-Test station name", { it.logSynch.logsynch_wintestNetworkStationNameOfKST += "x" },
            { it.prefs.getLogsynch_wintestNetworkStationNameOfKST() }),
        Case("log Win-Test broadcast", { it.logSynch.logsynch_wintestNetworkBroadcastAddress = "10.9.8.255" },
            { it.prefs.getLogsynch_wintestNetworkBroadcastAddress() }),

        // TRX synch
        flag("trx UCX QRG", { it.trx.trxSynch_ucxLogUDPListenerEnabled }) { f, v -> f.trx.trxSynch_ucxLogUDPListenerEnabled = v },
        flag("trx Win-Test QRG", { it.trx.logsynch_wintestQrgSyncEnabled }) { f, v -> f.trx.logsynch_wintestQrgSyncEnabled = v },
        flag("trx pass QRG", { it.trx.logsynch_wintestUsePassQrg }) { f, v -> f.trx.logsynch_wintestUsePassQrg = v },
        Case("trx station filter", { it.trx.logsynch_wintestNetworkStationNameOfWintestClient1 += "x" },
            { it.prefs.getLogsynch_wintestNetworkStationNameOfWintestClient1() }),

        // Airscout
        flag("airscout listener", { it.airscout.airScout_asUDPListenerEnabled }) { f, v -> f.airscout.airScout_asUDPListenerEnabled = v },
        Case("airscout server", { it.airscout.airScout_asServerNameString += "X" }, { it.prefs.getAirScout_asServerNameString() }),
        Case("airscout client", { it.airscout.airScout_asClientNameString += "X" }, { it.prefs.getAirScout_asClientNameString() }),
        Case("airscout port", { it.airscout.airScout_asCommunicationPort += 1 }, { it.prefs.getAirScout_asCommunicationPort() }),
        flag("airscout auto band", { it.airscout.airScout_autoBandSelectionEnabled }) { f, v ->
            f.airscout.airScout_autoBandSelectionEnabled = v
        },
        Case("airscout band", { f -> f.airscout.airScout_asBandString = ((f.airscout.airScout_asBandString.toLong()) + 1).toString() },
            { it.prefs.getAirScout_asBandString() }),

        // Notification, all but the test spot
        flag("sound simple", { it.notification.notify_playSimpleSounds }) { f, v -> f.notification.notify_playSimpleSounds = v },
        flag("sound CW", { it.notification.notify_playCWCallsignsOnRxedPMs }) { f, v -> f.notification.notify_playCWCallsignsOnRxedPMs = v },
        flag("sound voice", { it.notification.notify_playVoiceCallsignsOnRxedPMs }) { f, v ->
            f.notification.notify_playVoiceCallsignsOnRxedPMs = v
        },
        flag("band upgrade hint", { it.notification.notify_bandUpgradeHintOnLogEnabled }) { f, v ->
            f.notification.notify_bandUpgradeHintOnLogEnabled = v
        },
        flag("band upgrade boost", { it.notification.notify_bandUpgradePriorityBoostEnabled }) { f, v ->
            f.notification.notify_bandUpgradePriorityBoostEnabled = v
        },
        Case("cluster server", { f -> f.notification.enableDxClusterServer(!f.notification.notify_dxClusterServerEnabled) },
            { it.prefs.isNotify_dxClusterServerEnabled() }),
        Case("cluster port", { f -> f.notification.commitDxClusterServerPort((f.notification.notify_dxclusterServerPort + 1).toString()) },
            { it.prefs.getNotify_dxclusterServerPort() }),
        Case("cluster spotter", { f ->
            f.notification.commitSpotterCallSign(if (f.notification.notify_DXCSrv_SpottersCallSign == "DL1ABC") "DO5AMF" else "DL1ABC")
        }, { it.prefs.getNotify_DXCSrv_SpottersCallSign().get() }),
        Case("cluster fallback band", { f ->
            f.notification.notify_optionalFrequencyPrefix =
                if (f.notification.notify_optionalFrequencyPrefix == Band.B_432) Band.B_144 else Band.B_432
        }, { it.prefs.getNotify_optionalFrequencyPrefix().get() }),
        Case("monitoring list", { it.notification.addMonitoredCallSign("DL1ABC") }, { it.monitored.snapshot() }),

        // Shortcuts
        Case("shortcut buttons", { f ->
            f.shortcuts.shortcuts.addAtTop("cq")
            f.shortcuts.commitShortcuts()
        }, { it.prefs.getLst_txtShortCutBtnList().snapshot() }),
        Case("text snippets", { f ->
            f.shortcuts.snippets.addAtTop("73")
            f.shortcuts.commitSnippets()
        }, { it.prefs.getLst_txtSnipList().snapshot() }),

        // Beacon
        flag("beacon main", { it.beacon.bcn_beaconsEnabledMainCat }) { f, v -> f.beacon.bcn_beaconsEnabledMainCat = v },
        flag("beacon second", { it.beacon.bcn_beaconsEnabledSecondCat }) { f, v -> f.beacon.bcn_beaconsEnabledSecondCat = v },
        Case("beacon text main", { it.beacon.commitBeaconTextMainCat(it.beacon.beaconTextMainCat + " x") },
            { it.prefs.getBcn_beaconTextMainCat() }),
        Case("beacon text second", { it.beacon.commitBeaconTextSecondCat(it.beacon.beaconTextSecondCat + " x") },
            { it.prefs.getBcn_beaconTextSecondCat() }),
        Case("beacon interval", { it.beacon.commitBeaconInterval((it.beacon.beaconIntervalMinutes + 1).toString()) },
            { it.prefs.getBcn_beaconIntervalInMinutesMainCat() }),

        // Message handling
        flag("auto answer", { it.messages.autoAnswerEnabled }) { f, v -> f.messages.autoAnswerEnabled = v },
        Case("auto answer text", { it.messages.autoAnswerText += " x" }, { it.prefs.getMessageHandling_autoAnswerTextMainCat() }),
        flag("QRG answer", { it.messages.autoAnswerToQRGRequestEnabled }) { f, v -> f.messages.autoAnswerToQRGRequestEnabled = v },
        flag("debug to file", { it.messages.debugModeToFileEnabled }) { f, v -> f.messages.debugModeToFileEnabled = v },

        // GUI, all but the language
        flag("gui dark by default", { it.gui.gUI_darkModeActiveByDefault }) { f, v -> f.gui.gUI_darkModeActiveByDefault = v },
        flag("gui filter nothing", { it.gui.guiOptions_defaultFilterNothing }) { f, v -> f.gui.guiOptions_defaultFilterNothing = v },
        flag("gui filter pm to me", { it.gui.guiOptions_defaultFilterPmToMe }) { f, v -> f.gui.guiOptions_defaultFilterPmToMe = v },
        flag("gui filter pm to other", { it.gui.guiOptions_defaultFilterPmToOther }) { f, v ->
            f.gui.guiOptions_defaultFilterPmToOther = v
        },
        flag("gui filter public", { it.gui.guiOptions_defaultFilterPublicMsgs }) { f, v -> f.gui.guiOptions_defaultFilterPublicMsgs = v },
        flag("gui fresh call hint", { it.gui.guiOptions_showFreshCallHintInBandColumns }) { f, v ->
            f.gui.guiOptions_showFreshCallHintInBandColumns = v
        },
        flag("gui gross field hint", { it.gui.guiOptions_showGrossFieldWorkedHintInBandColumns }) { f, v ->
            f.gui.guiOptions_showGrossFieldWorkedHintInBandColumns = v
        },
    )

    @TestFactory
    fun everyDraftedSettingWaitsForApply(): List<DynamicTest> = cases().map { case ->
        DynamicTest.dynamicTest(case.name) {
            val f = Fixture(ChatPreferences(fresh().resolve("preferences.xml")))
            val before = case.stored(f)

            case.change(f)
            assertEquals(before, storedNow(case, f), "${case.name} took effect before Apply")

            f.draft.apply()
            assertNotEquals(before, storedNow(case, f), "${case.name} did not take effect with Apply")
        }
    }

    @TestFactory
    fun everyDraftedSettingIsLeftAloneByDiscard(): List<DynamicTest> = cases().map { case ->
        DynamicTest.dynamicTest(case.name) {
            val f = Fixture(ChatPreferences(fresh().resolve("preferences.xml")))
            val before = case.stored(f)

            case.change(f)
            f.draft.discard()

            assertEquals(before, storedNow(case, f), "${case.name} changed although discarded")
        }
    }

    /*
     * The stored value, read past the draft: a flag case reads through the tab, which shows
     * the draft, so it reads from tab states over the same preferences with a draft of their own.
     */
    private fun storedNow(case: Case, f: Fixture): Any? = case.stored(Fixture(f.prefs, f.monitored))

    private fun fresh(): Path = Files.createTempDirectory(directory, "case")
}
