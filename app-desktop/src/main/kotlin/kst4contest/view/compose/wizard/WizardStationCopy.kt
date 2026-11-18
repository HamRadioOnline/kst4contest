package kst4contest.view.compose.wizard

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences

/**
 * Copies the station from one profile's preferences to another's.
 *
 * "The station" is what hangs on the site and the equipment: locator, bands, chat
 * categories, antenna and path-analysis values, log program, frequency source, DX cluster
 * server, rotators, AirScout. It is deliberately more than the setup wizard asks about -- a
 * further operator at the same station should inherit the second rotator and the Win-Test
 * station filter too, without ever seeing a question for them.
 *
 * It is deliberately not the operator: login, names, language, appearance, layout,
 * shortcuts, snippets, beacons, automatic answers, sounds, filters. Nor the server address
 * or the spotter callsign.
 *
 * Which setter belongs where is decided line by line in
 * `src/test/resources/wizard-station-setters.txt` and `wizard-other-setters.txt`; a test
 * holds this function and those lists together, and fails when ChatPreferences gains a
 * setter nobody classified.
 *
 * Nothing is written to a file here, and [from] is only read.
 */
internal fun copyStation(from: ChatPreferences, to: ChatPreferences) {
    // Location, bands and chats. The categories are copied by number: the two profiles
    // must not share one mutable ChatCategory.
    to.setStn_loginLocatorMainCat(from.getStn_loginLocatorMainCat())
    to.setLoginChatCategoryMain(from.getLoginChatCategoryMain()?.let { ChatCategory(it.categoryNumber) })
    to.setLoginChatCategorySecond(from.getLoginChatCategorySecond()?.let { ChatCategory(it.categoryNumber) })
    to.setLoginToSecondChatEnabled(from.isLoginToSecondChatEnabled())

    // Bands.
    to.setStn_bandActive50(from.isStn_bandActive50())
    to.setStn_bandActive70(from.isStn_bandActive70())
    to.setStn_bandActive144(from.isStn_bandActive144())
    to.setStn_bandActive432(from.isStn_bandActive432())
    to.setStn_bandActive1240(from.isStn_bandActive1240())
    to.setStn_bandActive2300(from.isStn_bandActive2300())
    to.setStn_bandActive3400(from.isStn_bandActive3400())
    to.setStn_bandActive5600(from.isStn_bandActive5600())
    to.setStn_bandActive10G(from.isStn_bandActive10G())

    // Antenna, defaults and everything the path analysis needs to know about the site.
    to.setStn_antennaBeamWidthDeg(from.getStn_antennaBeamWidthDeg())
    to.setStn_qtfDefault(from.getStn_qtfDefault())
    to.setStn_maxQRBDefault(from.getStn_maxQRBDefault())
    to.setStn_useQtfFilterDirectionAsAntennaHeading(from.isStn_useQtfFilterDirectionAsAntennaHeading())
    to.setStn_pathAnalysisOwnAntennaGainDbi(from.getStn_pathAnalysisOwnAntennaGainDbi())
    to.setStn_pathAnalysisOwnAntennaHeightMeters(from.getStn_pathAnalysisOwnAntennaHeightMeters())
    to.setStn_pathAnalysisOwnTxPowerWatts(from.getStn_pathAnalysisOwnTxPowerWatts())
    to.setStn_pathAnalysisOwnHeightAslMeters(from.getStn_pathAnalysisOwnHeightAslMeters())
    to.setStn_pathAnalysisDefaultTargetAntennaGainDbi(from.getStn_pathAnalysisDefaultTargetAntennaGainDbi())
    to.setStn_pathAnalysisDefaultTargetAntennaHeightMeters(from.getStn_pathAnalysisDefaultTargetAntennaHeightMeters())
    to.setStn_pathAnalysisDefaultTargetTxPowerWatts(from.getStn_pathAnalysisDefaultTargetTxPowerWatts())
    to.setStn_pathAnalysisDxHighestPointInSquare(from.isStn_pathAnalysisDxHighestPointInSquare())
    to.setStn_pathAnalysisDemRootDirectory(from.getStn_pathAnalysisDemRootDirectory())
    to.setStn_pathAnalysisDemDatasetId(from.getStn_pathAnalysisDemDatasetId())
    to.setStn_pathAnalysisVhfFeederLossPerStationDb(from.getStn_pathAnalysisVhfFeederLossPerStationDb())
    to.setStn_pathAnalysisFeederLossIncreaseDbPer200MHz(from.getStn_pathAnalysisFeederLossIncreaseDbPer200MHz())
    to.setStn_pathAnalysisMaxEstimatedFeederLossPerStationDb(from.getStn_pathAnalysisMaxEstimatedFeederLossPerStationDb())
    to.setStn_pathAnalysisRequiredSsbSignalDbm(from.getStn_pathAnalysisRequiredSsbSignalDbm())
    to.setStn_pathAnalysisRequiredCwSignalDbm(from.getStn_pathAnalysisRequiredCwSignalDbm())
    to.setStn_pathAnalysisContestMarginDb(from.getStn_pathAnalysisContestMarginDb())

    // Log sources. All of them as they are: a station may run several at once.
    to.setLogsynch_fileBasedWkdCallInterpreterEnabled(from.isLogsynch_fileBasedWkdCallInterpreterEnabled())
    to.setLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly(from.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly())
    to.setLogsynch_ucxUDPWkdCallListenerEnabled(from.isLogsynch_ucxUDPWkdCallListenerEnabled())
    to.setLogsynch_ucxUDPWkdCallListenerPort(from.getLogsynch_ucxUDPWkdCallListenerPort())
    to.setLogsynch_wintestNetworkListenerEnabled(from.isLogsynch_wintestNetworkListenerEnabled())
    to.setLogsynch_wintestNetworkPort(from.getLogsynch_wintestNetworkPort())
    to.setLogsynch_wintestNetworkBroadcastAddress(from.getLogsynch_wintestNetworkBroadcastAddress())
    to.setLogsynch_wintestNetworkStationNameOfKST(from.getLogsynch_wintestNetworkStationNameOfKST())
    to.setLogsynch_wintestNetworkStationIDOfKST(from.getLogsynch_wintestNetworkStationIDOfKST())
    to.setLogsynch_wintestNetworkStationNameOfWintestClient1(from.getLogsynch_wintestNetworkStationNameOfWintestClient1())
    to.setLogsynch_wintestNetworkStationNameOfWintestClient2(from.getLogsynch_wintestNetworkStationNameOfWintestClient2())
    to.setLogsynch_wintestNetworkSkedPushEnabled(from.isLogsynch_wintestNetworkSkedPushEnabled())
    to.setLogsynch_wintestNetworkSimulationEnabled(from.isLogsynch_wintestNetworkSimulationEnabled())
    to.setLogsynch_wintestSkedMode(from.getLogsynch_wintestSkedMode())

    // Frequency sources, SO2R and the DX cluster server for the log program.
    to.setLogsynch_wintestQrgSyncEnabled(from.isLogsynch_wintestQrgSyncEnabled())
    to.setLogsynch_wintestUsePassQrg(from.isLogsynch_wintestUsePassQrg())
    to.setTrxSynch_ucxLogUDPListenerEnabled(from.isTrxSynch_ucxLogUDPListenerEnabled())
    to.setTrxSynch_so2rEnabled(from.isTrxSynch_so2rEnabled())
    to.setNotify_dxClusterServerEnabled(from.isNotify_dxClusterServerEnabled())
    to.setNotify_dxclusterServerPort(from.getNotify_dxclusterServerPort())
    // The band a frequency given without one is completed with; it has to fit the bands above.
    to.setNotify_optionalFrequencyPrefix(from.getNotify_optionalFrequencyPrefix().get())

    // Rotators.
    to.setStn_pstRotatorEnabled(from.isStn_pstRotatorEnabled())
    to.setStn_pstRotatorHost(from.getStn_pstRotatorHost())
    to.setStn_pstRotatorPort(from.getStn_pstRotatorPort())
    to.setStn_pstRotator2Enabled(from.isStn_pstRotator2Enabled())
    to.setStn_pstRotator2Host(from.getStn_pstRotator2Host())
    to.setStn_pstRotator2Port(from.getStn_pstRotator2Port())

    // AirScout.
    to.setAirScout_asUDPListenerEnabled(from.isAirScout_asUDPListenerEnabled())
    to.setAirScout_asCommunicationPort(from.getAirScout_asCommunicationPort())
    to.setAirScout_asServerNameString(from.getAirScout_asServerNameString())
    to.setAirScout_asClientNameString(from.getAirScout_asClientNameString())
    to.setAirScout_autoBandSelectionEnabled(from.isAirScout_autoBandSelectionEnabled())
    to.setAirScout_asBandString(from.getAirScout_asBandString())
}
