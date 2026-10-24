package kst4contest.view.compose.tabs

import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.Form
import kst4contest.view.compose.TooltipCard

/**
 * The Station tab: own station, server, active bands and path-analysis defaults.
 *
 * The controls stage their edits in the window's draft (see StationTabState); only the
 * login credentials write through at once.
 *
 * @param onRefused shows why a committed entry was refused (the own ground height)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationTab(state: StationTabState, onRefused: (String) -> Unit = {}) {
    val strings = LocalStrings.current

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        /*
         * These are the login credentials, so they are locked while the session is
         * logged in: the running connection would not pick up a change.
         */
        Form.section(strings.stationOwnSection) {
            Form.text(strings.stationCallsign, state.stn_loginCallSign, state.loginFieldsEnabled) {
                state.stn_loginCallSign = it
            }
            Form.text(strings.stationNameMain, state.stn_loginNameMainCat, state.loginFieldsEnabled) {
                state.stn_loginNameMainCat = it
            }
            Form.text(strings.stationNameSecond, state.stn_loginNameSecondCat, state.loginFieldsEnabled) {
                state.stn_loginNameSecondCat = it
            }
            Form.text(strings.stationLocator, state.stn_loginLocatorMainCat, state.loginFieldsEnabled) {
                state.stn_loginLocatorMainCat = it
            }
            Form.password(strings.stationPassword, state.stn_loginPassword, state.loginFieldsEnabled) {
                state.stn_loginPassword = it
            }
        }

        Form.section(strings.stationChatSection) {
            Form.choice(
                label = strings.stationMainCategory,
                items = state.availableCategories,
                selected = state.loginChatCategoryMain,
                describe = state::describeCategory,
                enabled = state.loginFieldsEnabled,
                onSelect = state::selectMainCategory,
            )
            Form.check(strings.stationSecondChatEnabled, state.loginToSecondChatEnabled) {
                state.loginToSecondChatEnabled = it
            }
            Form.choice(
                label = strings.stationSecondCategory,
                items = state.secondCategoryOptions,
                selected = state.loginChatCategorySecond,
                describe = state::describeCategory,
                enabled = state.loginFieldsEnabled,
                onSelect = state::selectSecondCategory,
            )
        }

        Form.section(strings.stationServerSection) {
            Form.text(strings.stationServerHost, state.stn_on4kstServersDns) { state.stn_on4kstServersDns = it }
            Form.int(strings.stationServerPort, state.stn_on4kstServersPort) { state.stn_on4kstServersPort = it }
        }

        Form.flowingSection(strings.stationBandsSection) {
            Form.check("50 MHz", state.stn_bandActive50) { state.stn_bandActive50 = it }
            Form.check("70 MHz", state.stn_bandActive70) { state.stn_bandActive70 = it }
            Form.check("144 MHz", state.stn_bandActive144) { state.stn_bandActive144 = it }
            Form.check("432 MHz", state.stn_bandActive432) { state.stn_bandActive432 = it }
            Form.check("1240 MHz", state.stn_bandActive1240) { state.stn_bandActive1240 = it }
            Form.check("2300 MHz", state.stn_bandActive2300) { state.stn_bandActive2300 = it }
            Form.check("3400 MHz", state.stn_bandActive3400) { state.stn_bandActive3400 = it }
            Form.check("5600 MHz", state.stn_bandActive5600) { state.stn_bandActive5600 = it }
            Form.check("10 GHz", state.stn_bandActive10G) { state.stn_bandActive10G = it }
        }

        Form.section(strings.stationDefaultsSection) {
            Form.decimal(strings.stationMaxQrb, state.stn_maxQRBDefault) { state.stn_maxQRBDefault = it }
            Form.decimal(
                label = strings.stationQtf,
                value = state.stn_qtfDefault,
                isValid = { it.isFinite() && it in 0.0..360.0 },
            ) { state.stn_qtfDefault = it }
            Form.decimal(strings.stationBeamWidth, state.stn_antennaBeamWidthDeg) { state.stn_antennaBeamWidthDeg = it }
        }

        Form.panelSection(strings.stationManualAntennaHeadingSection) {
            val headingOption: @Composable () -> Unit = {
                Form.check(
                    strings.stationUseQtfFilterDirectionAsAntennaHeading,
                    state.stn_useQtfFilterDirectionAsAntennaHeading,
                    enabled = !state.stn_pstRotatorEnabled,
                ) { state.stn_useQtfFilterDirectionAsAntennaHeading = it }
            }
            if (state.stn_pstRotatorEnabled) {
                TooltipArea(
                    tooltip = { TooltipCard(strings.stationManualAntennaHeadingRotatorHint) },
                    content = headingOption,
                )
            } else {
                headingOption()
            }
        }

        Form.section(strings.stationRotatorSection) {
            Form.check(strings.stationRotatorEnabled, state.stn_pstRotatorEnabled) { state.stn_pstRotatorEnabled = it }
            Form.text(strings.stationRotatorHost, state.stn_pstRotatorHost) { state.stn_pstRotatorHost = it }
            Form.int(strings.stationRotatorPort, state.stn_pstRotatorPort) { state.stn_pstRotatorPort = it }
        }

        /*
         * Only with SO2R, as stored: Form.check and Form.int have no disabled state, and a
         * rotator 2 switch without SO2R would do nothing. Switching SO2R on shows the block
         * after Apply.
         */
        if (state.so2rEnabled) {
            Form.section(strings.stationRotator2Section) {
                Form.check(strings.stationRotator2Enabled, state.stn_pstRotator2Enabled) { state.stn_pstRotator2Enabled = it }
                Form.text(strings.stationRotator2Host, state.stn_pstRotator2Host) { state.stn_pstRotator2Host = it }
                Form.int(strings.stationRotator2Port, state.stn_pstRotator2Port) { state.stn_pstRotator2Port = it }
            }
        }

        Form.section(strings.stationPathSection) {
            Form.decimal(strings.stationOwnTxPower, state.stn_pathAnalysisOwnTxPowerWatts) { state.stn_pathAnalysisOwnTxPowerWatts = it }
            Form.decimal(strings.stationOwnAntennaGain, state.stn_pathAnalysisOwnAntennaGainDbi) { state.stn_pathAnalysisOwnAntennaGainDbi = it }
            Form.decimal(strings.stationOwnAntennaHeight, state.stn_pathAnalysisOwnAntennaHeightMeters) { state.stn_pathAnalysisOwnAntennaHeightMeters = it }
            /*
             * Committed on Enter or on leaving the field, not per keystroke: the stored
             * value reads back as "250.0", and a per-keystroke write would feed that back
             * into the field while the operator is still typing.
             */
            Form.committed(
                label = strings.stationOwnHeightAsl,
                stored = state.stn_pathAnalysisOwnHeightAslText,
                commit = state::commitOwnHeightAsl,
                onRefused = onRefused,
                isUnchanged = StationTabState::isOwnHeightAslUnchanged,
                current = { state.stn_pathAnalysisOwnHeightAslText },
            )
            Form.decimal(strings.stationTargetTxPower, state.stn_pathAnalysisDefaultTargetTxPowerWatts) { state.stn_pathAnalysisDefaultTargetTxPowerWatts = it }
            Form.decimal(strings.stationTargetAntennaGain, state.stn_pathAnalysisDefaultTargetAntennaGainDbi) { state.stn_pathAnalysisDefaultTargetAntennaGainDbi = it }
            Form.check(strings.stationTargetHighestPoint, state.stn_pathAnalysisDxHighestPointInSquare) { state.stn_pathAnalysisDxHighestPointInSquare = it }
            Form.text(strings.stationDemDirectory, state.stn_pathAnalysisDemRootDirectory) { state.stn_pathAnalysisDemRootDirectory = it }
        }
    }
}
