package kst4contest.view;
import kst4contest.view.compose.SettingsDraft;
import kst4contest.view.compose.SettingsHost;
import kst4contest.view.compose.SettingsNotices;
import kst4contest.view.compose.SettingsTabsKt;
import kst4contest.view.compose.UpdateWindow;
import kst4contest.view.compose.UpdateWindowState;
import kst4contest.view.compose.ColumnWidthStore;
import kst4contest.view.compose.DataTableState;
import kst4contest.view.compose.MonitorColumns;
import kst4contest.view.compose.MonitorWindow;
import kst4contest.view.compose.RowKeys;
import kst4contest.view.compose.ReachabilityState;
import kst4contest.view.compose.BlinkingNotice;
import kst4contest.view.compose.ThreadStatusButtons;
import kst4contest.view.compose.StationColumns;
import kst4contest.view.compose.StationFilterState;
import kst4contest.view.compose.DirectedMessageColumns;
import kst4contest.view.compose.PublicMessageColumns;
import kst4contest.view.compose.SelectedMessageFilter;
import kst4contest.view.compose.SelectedStationMessageColumns;
import kst4contest.view.compose.SelectedStationMessagePredicate;
import kst4contest.view.compose.SelectedStationState;
import kst4contest.view.compose.DirectedMessageStationTarget;
import kst4contest.view.compose.StationIdentity;
import kst4contest.view.compose.StationSelectionSource;
import kst4contest.view.compose.StationSelectionState;
import kst4contest.view.compose.TopPriorityCandidatesWindow;
import kst4contest.view.compose.TopPriorityState;
import kst4contest.view.compose.TimelineState;
import kst4contest.view.compose.ChatInputState;
import kst4contest.view.compose.MainMenuState;
import kst4contest.view.compose.MainWindowState;
import kst4contest.view.compose.MainWindowSurroundings;
import kst4contest.view.compose.MainWindowHost;
import kst4contest.view.compose.MainWindowFrame;
import kst4contest.view.compose.MainWindowSize;
import kst4contest.view.compose.SplitterState;
import kst4contest.view.compose.SettingsWindow;
import kst4contest.controller.ScoreService;
import kst4contest.view.compose.SettingsWindow;
import kst4contest.controller.On4KstConnectionState;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import kst4contest.model.ContestSked; // The new model

import kst4contest.ApplicationConstants;
import kst4contest.view.compose.OperatorProfilePickerWindow;
import kst4contest.observe.MutableValue;
import kst4contest.observe.ObservableRoster;
import kst4contest.observe.SimpleValue;
import kst4contest.observe.UiDispatcher;

import java.awt.EventQueue;
import kst4contest.view.compose.ComposeAlert;
import kst4contest.view.compose.TimelineCandidate;
import kst4contest.view.compose.ConfirmKind;
import kst4contest.controller.ChatController;
import kst4contest.controller.MessageVariableResolver;
import kst4contest.controller.StatusUpdateListener;
import kst4contest.controller.Utils4KST;
import kst4contest.locatorUtils.DirectionUtils;
import kst4contest.model.*;


import kst4contest.logic.BandOpportunityResolver;
import kst4contest.utils.ApplicationFileLogging;
import kst4contest.utils.ApplicationFileUtils;
import kst4contest.utils.PlatformUtils;
import kst4contest.controller.ActiveOperatorProfile;
import kst4contest.controller.OperatorProfileStore;
import kst4contest.controller.OperatorProfileManagementService;
import kst4contest.controller.OperatorProfilePaths;
import kst4contest.model.OperatorProfile;
import kst4contest.model.OperatorProfileSelection;
import kst4contest.view.map.OfflineDemImportService;
import kst4contest.controller.WorkedGrossFieldCache;


public class Kst4ContestApplication implements StatusUpdateListener, SettingsHost  {
	
//	private static final Kst4ContestApplication dbcontroller = new DBController();
	// Null means Auto: use the lowest session band, or category fallback.

	// Enables optional color highlighting of the QRA/grid cell.
// The status text itself remains visible independently of this flag.

	private static final Logger LOGGER = Logger.getLogger(
			Kst4ContestApplication.class.getName());
	private static ApplicationFileLogging fileLogging;
	private static final String SIMPLE_LOG_MANUAL_URL =
			"https://kst4contest.hamradioonline.de/manual/en/log-sync/"
					+ "#method-1-universal-file-based-callsign-interpreter-simplelogfile";

	private boolean gridSquareHighlightEnabled = false;


	/**
	 * True for a very short moment when the operator explicitly interacts with the
	 * ChatMember table by mouse or keyboard.
	 *
	 * This flag is the distinction between:
	 * - real operator selection: may overwrite the send field with "/cq CALL "
	 * - artificial refresh/selection event: must not overwrite operator text
	 */


	private CoalescingTrigger userListRefreshCoalescer;
	private String pendingUserListUpdateReason = "";

	/**
	 * Remembers the last combined ChatMember table predicate that was applied.
	 *
	 * This avoids repeatedly assigning the same predicate to the FilteredList during
	 * periodic refreshes. Reassigning the predicate without a real filter change can
	 * invalidate the TableView and trigger artificial selection events.
	 */
	private Predicate<ChatMember> lastAppliedChatMemberFilterPredicate = null;


	private kst4contest.view.feed.TimelineFeed timelineFeed;
	private kst4contest.view.feed.SelectedStationMessagesFeed selectedStationMessagesFeed;
	private kst4contest.view.feed.ConnectionStateFeed connectionStateFeed;
			private LayoutAutosave layoutAutosave;

	private On4KstConnectionState lastDisplayedConnectionState;
	private String lastDisplayedConnectionDetail = "";


	/** The delay the former JavaFX pause transition used, so the feel does not change. */
	private static final int USER_LIST_REFRESH_DELAY_MS = 300;

	// Timeline: show at most N priority markers per minute bucket (minute 0/1 often has many planes)
	private static final int TIMELINE_PRIORITY_MARKERS_PER_MINUTE = 2;

	// Timeline: show 2 more in other directions
	private static final int TIMELINE_BEAM_MARKERS_PER_MINUTE = 2;


	// Keep in sync with TimelineView PREVIEW_TIME_MS (currently 30 minutes = 30L * 60L * 1000L)
	private static final long TIMELINE_PREVIEW_TIME_MS = 30L * 60L * 1000L;

	/**
	 * Reused show-all predicate for the station table.
	 *
	 * Using the same instance avoids unnecessary FilteredList invalidations when no
	 * station filters are active. Such invalidations can make the TableView selection
	 * model fire again although the operator did not select another station.
	 */
	private static final Predicate<ChatMember> SHOW_ALL_CHAT_MEMBER_PREDICATE = chatMember -> true;

	//recoloring of the chatmembers list is turned on and off here
	private static final boolean ENABLE_PRIORITY_SCORE_ROW_COLORING = false;




	/*
	 * Derived and never spelled out: this is the file the root profile's own stylesheet layer
	 * reads, under the name OperatorProfilePaths.paletteRelativeFileName builds from the same
	 * constant. Two independent literals of this value would have to stay equal for that to
	 * work, and a rename on one side would restore the "written but never read" defect
	 * silently, with a green build.
	 */
	public static final String STYLE_DEFAULTCSSDAY_FILE = ApplicationConstants.STYLECSSFILE_DEFAULT_DAYLIGHT;
	public static final String STYLE_DEFAULTCSSDAY_RESOURCE = "/" + ApplicationConstants.STYLECSSFILE_DEFAULT_DAYLIGHT;

	public static final String STYLE_DEFAULTCSSEVENING_FILE = ApplicationConstants.STYLECSSFILE_DEFAULT_EVENING;
	public static final String STYLE_DEFAULTCSSEVENING_RESOURCE = "/" + ApplicationConstants.STYLECSSFILE_DEFAULT_EVENING;

	/** Hands worker-thread results over to the AWT event thread, where Compose draws. */
	private final UiDispatcher uiDispatcher = new AwtUiDispatcher();

	/**
	 * Runs work on the user-interface thread. Package-private so the Compose bridge classes
	 * can hand their work to the same dispatcher rather than bringing a second one.
	 */
	void runOnUi(final Runnable work) {
		uiDispatcher.runOnUi(work);
	}

	/**
	 * The profile switch, reachable from the Compose menu.
	 *
	 * <p>A thin forwarder rather than a promotion of the private method: the switch is about
	 * to change its nature in task 14, and a second public entry point would be a second
	 * thing to change then.</p>
	 */
	void showOperatorProfileSwitchDialogFromCompose() {
		showOperatorProfileSwitchDialog();
	}

	/** Forwarders so the Compose send line runs the very same checks the send button does. */
	boolean isMessageAddressedToOwnCallsignFromCompose(final String messageText) {
		return isMessageAddressedToOwnCallsign(messageText);
	}

	ChatCategory resolveOutgoingChatCategoryFromCompose(
			final String outgoingText,
			final ChatMember selectedMember) {
		return resolveOutgoingChatCategory(outgoingText, selectedMember);
	}

	/**
	 * Follows the own QRG in the text field. Replaces the former
	 * {@code textProperty().bind(...)}: a MutableValue is not a JavaFX
	 * property, so the field is updated through a listener instead. Attaching
	 * twice is harmless; detaching releases the listener.
	 */



	/** Live mirrors of core rosters, handed to TableViews. Released on shutdown. */
	private final java.util.List<RosterListBinding<?>> rosterBindings = new java.util.ArrayList<>();

	private <T> java.util.List<T> mirrorOf(ObservableRoster<T> roster) {
		RosterListBinding<T> binding = RosterListBinding.mirror(roster, uiDispatcher);
		rosterBindings.add(binding);
		return binding.list();
	}

	/**
	 * Registers a mirror whose content is computed rather than copied. Used for the
	 * station list and the message tabs, which used to be FilteredList/SortedList
	 * views on a core collection.
	 */
	private <T> RosterListBinding<T> derivedBinding(
			java.util.function.Supplier<java.util.List<T>> derivation,
			ObservableRoster<?>... sources) {
		RosterListBinding<T> binding = RosterListBinding.derived(uiDispatcher, derivation, sources);
		rosterBindings.add(binding);
		return binding;
	}

	/** The visible station list, replacing the former SortedList on the TableView. */
	private RosterListBinding<ChatMember> chatMemberListBinding;

	/**
	 * The message view of the selected-station info window. Its filter is switched
	 * by the radio buttons of that window, so the binding has to be refreshed
	 * explicitly. The other message views keep the filter they were given in the
	 * controller and follow their source rosters on their own.
	 */
	private RosterListBinding<ChatMessage> selectedCallSignInfoMessageBinding;

	/**
	 * Sets the message filter of the selected-station info window and shows the
	 * result immediately, the way {@code FilteredList.setPredicate} did.
	 */
	private void applySelectedCallSignInfoFilter(Predicate<ChatMessage> filter) {
		chatcontroller.setSelectedCallSignInfoFilter(filter);
		if (selectedCallSignInfoMessageBinding != null) {
			selectedCallSignInfoMessageBinding.refresh();
			uiDispatcher.runOnUi(() -> {
				if (selectedStationMessagesFeed != null) {
					selectedStationMessagesFeed.push(selectedCallSignInfoMessageBinding.list());
				}
			});
		}
	}

	/** Applies one radio mode to one exact station identity. */
	private void applySelectedStationMessageFilter(
			SelectedMessageFilter filter,
			ChatMember selectedMember) {
		if (filter == null) {
			return;
		}

		applySelectedCallSignInfoFilter(SelectedStationMessagePredicate.forSelection(
				filter,
				selectedMember,
				chatcontroller.getChatPreferences().getStn_loginCallSign()));
	}

	/** Follows the PSTRotator QTF in the text field; replaces a JavaFX string binding. */





	ChatController chatcontroller;
	public ChatController getChatController() { return chatcontroller; }
	MessageVariableResolver messageVariableResolver;


						// the text later

	Timer timer_buildWindowTitle;
	Timer timer_updatePrivatemessageTable; // same here


	private void ensureStationMapSupportInitialized() {
		// Nothing to initialize for Compose map here
	}

	private void toggleStationMapWindow() {
		if (composeMainWindowState != null) {
			kst4contest.view.compose.map.StationMapWindow.INSTANCE.toggle(composeMainWindowState, paletteStore, languageStore);
		}
	}

	private void showSelectedCallsignOnMap() {
		if (composeMainWindowState != null) {
			if (!kst4contest.view.compose.map.StationMapWindow.INSTANCE.isShowing()) {
				kst4contest.view.compose.map.StationMapWindow.INSTANCE.toggle(composeMainWindowState, paletteStore, languageStore);
			}
		}
	}

	private void refreshStationMapIfVisible() {
		// Compose map state is reactive to main window state
	}

	/**
	 * Resolves a reasonable initial directory for the DEM tile file chooser.
	 *
	 * <p>Preference order:
	 * <ol>
	 *     <li>configured DEM root directory if it already exists</li>
	 *     <li>its parent directory if that exists</li>
	 *     <li>user home directory</li>
	 * </ol>
	 *
	 * @param configuredDemRootDirectory current DEM root directory text
	 * @return usable initial directory or null
	 */
	private File resolveInitialDirectoryForDemImport(String configuredDemRootDirectory) {
		if (configuredDemRootDirectory != null && !configuredDemRootDirectory.isBlank()) {
			File configuredDirectory = new File(configuredDemRootDirectory.trim());

			if (configuredDirectory.isDirectory()) {
				return configuredDirectory;
			}

			File parentDirectory = configuredDirectory.getParentFile();
			if (parentDirectory != null && parentDirectory.isDirectory()) {
				return parentDirectory;
			}
		}

		File userHomeDirectory = new File(System.getProperty("user.home"));
		return userHomeDirectory.isDirectory() ? userHomeDirectory : null;
	}

	/**
	 * helper DTO for planes and arriving time in minutes. Maybe
	 */
	private static final class NextApInfo {
		final AirPlane plane;
		final int arrivingMinutes;

		private NextApInfo(AirPlane plane, int arrivingMinutes) {
			this.plane = plane;
			this.arrivingMinutes = arrivingMinutes;
		}
	}

	/**
	 * Helper DTO for timeline building
	 */
	private static final class TimelineCandidateTmp {
		final kst4contest.controller.ScoreService.TopCandidate top;
		final ChatMember representativeMember;
		final NextApInfo nextAp;

		TimelineCandidateTmp(kst4contest.controller.ScoreService.TopCandidate top, ChatMember representativeMember, NextApInfo nextAp) {
			this.top = top;
			this.representativeMember = representativeMember;
			this.nextAp = nextAp;
		}
	}



    /**
     * Gets thread notifications and makes new statusbuttons at the top
     *
     * @param sourceName
     */
    private void updateStatusButton(String sourceName, ThreadStateMessage threadStateMessage) {
		if (composeMainWindowState != null) {
			composeMainWindowState.getThreadButtons().update(sourceName, threadStateMessage);
		}
	}

	/**
	 * Helps the view to format the RX Bands for a callsign, using the chatmembers frequencies detected MAP
	 * @param callSignRaw
	 * @param maxAgeMs
	 * @return
	 */
	private String formatDetectedRxBandsForCallsignRaw(String callSignRaw, long maxAgeMs) {

		if (callSignRaw == null) return kst4contest.view.i18n.CurrentStrings.get().getTooltipBandsNone();

		List<ChatMember> variants = chatcontroller.findActiveChatMembersByRawCall(callSignRaw);
		BandOpportunityResolver.Resolution bandResolution =
				BandOpportunityResolver.resolve(variants, System.currentTimeMillis(), maxAgeMs);
		EnumSet<Band> availableBands = bandResolution.getAvailableBands();

		if (availableBands.isEmpty()) {
			return kst4contest.view.i18n.CurrentStrings.get().getTooltipBandsNone();
		}

		Map<Band, ChatMember.ActiveFrequencyInfo> newestPerBand =
				new EnumMap<>(Band.class);

		for (ChatMember member : variants) {
			if (member == null || member.getKnownActiveBands() == null) continue;

			for (Map.Entry<Band, ChatMember.ActiveFrequencyInfo> entry
					: member.getKnownActiveBands().entrySet()) {

				Band band = entry.getKey();
				ChatMember.ActiveFrequencyInfo info = entry.getValue();
				if (band == null || info == null || !availableBands.contains(band)) continue;

				long ageMs = System.currentTimeMillis() - info.timestampEpoch;
				if (ageMs < 0L || (maxAgeMs > 0 && ageMs > maxAgeMs)) continue;

				ChatMember.ActiveFrequencyInfo existing = newestPerBand.get(band);
				if (existing == null || info.timestampEpoch > existing.timestampEpoch) {
					newestPerBand.put(band, info);
				}
			}
		}

		StringBuilder result = new StringBuilder(kst4contest.view.i18n.CurrentStrings.get().getTooltipBandsPrefix() + " ").append(
				availableBands.stream()
						.sorted()
						.map(this::bandToHumanLabel)
						.collect(java.util.stream.Collectors.joining(", "))
		);

		if (newestPerBand.isEmpty()) {
			return result.toString();
		}

		result.append(" | QRGs: ");
		boolean first = true;

		for (Band band : Band.values()) {
			ChatMember.ActiveFrequencyInfo info = newestPerBand.get(band);
			if (info == null) continue;

			if (!first) result.append(" | ");
			first = false;

			long ageMinutes = (System.currentTimeMillis() - info.timestampEpoch) / 60_000L;
			result.append(bandToHumanLabel(band))
					.append(" ")
					.append(String.format(Locale.US, "%.3f", info.frequency))
					.append(" MHz (")
					.append(ageMinutes)
					.append(" min ago)");
		}

		return result.toString();
	}

	/**
	 * Checks whether a station has known activity on the given band and that band is
	 * one of my own currently enabled bands, i.e. a new-band opportunity worth flagging
	 * with a star in that band's table cell.
	 *
	 * @param chatMember station row
	 * @param band       band to check
	 * @return true if the band cell should show a star
	 */
	private boolean isBandOfferForMainView(ChatMember chatMember, Band band) {
		if (chatMember == null || band == null || chatcontroller == null) {
			return false;
		}

		List<ChatMember> variants =
				chatcontroller.findActiveChatMembersByRawCall(chatMember.getCallSignRaw());
		if (variants.isEmpty()) {
			variants = List.of(chatMember);
		}

		BandOpportunityResolver.Resolution resolution =
				BandOpportunityResolver.resolve(variants, System.currentTimeMillis());
		EnumSet<Band> enabledBands =
				BandOpportunityResolver.getEnabledStationBands(chatcontroller.getChatPreferences());

		return resolution.getUnworkedEnabledBands(enabledBands).contains(band);
	}

	/**
	 * Checks whether the station's four-character grid square has already been worked
	 * specifically on the given band, regardless of which call was worked there. Mirrors
	 * the "o" semantics of the wkdAny column, just scoped to one band instead of any band.
	 * Can be turned off via
	 * {@link ChatPreferences#isGuiOptions_showGrossFieldWorkedHintInBandColumns()}.
	 *
	 * @param chatMember station row
	 * @param band       band to check
	 * @return true if the cell should show the "o" worked-grid-square hint
	 */
	private boolean isGrossFieldWorkedForBand(ChatMember chatMember, Band band) {
		if (chatMember == null || band == null || chatcontroller == null) {
			return false;
		}

		if (!chatcontroller.getChatPreferences().isGuiOptions_showGrossFieldWorkedHintInBandColumns()) {
			return false;
		}

		String qra = chatMember.getQra();
		if (qra == null || qra.isBlank()) {
			return false;
		}

		return chatcontroller.getWorkedGrossFieldCache().isGrossFieldWorked(band, qra);
	}

	/**
	 * Resolves the compact per-band status shown in the 144/432/23/13/9/6/3 table
	 * columns.
	 *
	 * <p>{@code X}/{@code a}/{@code B+}/(empty) describe the band-opportunity part:
	 * {@code X} if worked on this exact band, otherwise {@code a}/{@code B+} if the band
	 * is offered, enabled and unworked ({@code a} when the call has not been worked on
	 * any band yet, {@code B+} when it has), otherwise empty.</p>
	 *
	 * <p>{@code o} is an independent overlay, appended to whichever of the above applies,
	 * exactly like the {@code x}/{@code o}/{@code xo} combination in the wkdAny column:
	 * it marks that this band's grid square has already been worked, by any station.
	 * So e.g. {@code "a"}, {@code "ao"}, {@code "B+"}, {@code "B+o"} or just {@code "o"}
	 * can all appear.</p>
	 *
	 * @param chatMember     station row
	 * @param band           band this column represents
	 * @param workedThisBand true if the per-band worked flag for this exact band is set
	 * @return compact status text for the cell
	 */
	private String formatBandCellStatus(ChatMember chatMember, Band band, boolean workedThisBand) {
		String opportunity;

		boolean showFreshCallHint = chatcontroller != null
				&& chatcontroller.getChatPreferences().isGuiOptions_showFreshCallHintInBandColumns();

		if (workedThisBand) {
			opportunity = "X";
		} else if (isBandOfferForMainView(chatMember, band)) {
			opportunity = showFreshCallHint && chatMember != null && !chatMember.isWorked() ? "a" : "B+";
		} else {
			opportunity = "";
		}

		return isGrossFieldWorkedForBand(chatMember, band) ? opportunity + "o" : opportunity;
	}

	/**
	 * Builds the tooltip shown on a band-status cell: a fixed legend plus this row's
	 * resolved status for the given band.
	 */
	private String buildBandCellStatusTooltipText(ChatMember chatMember, Band band, String status) {
		StringBuilder tooltip = new StringBuilder(kst4contest.view.i18n.CurrentStrings.get().getTooltipBandStatusLegend());

		if (chatMember != null && band != null) {
			tooltip.append(kst4contest.view.i18n.CurrentStrings.get().tooltipBandStatusThisStation(
					bandToHumanLabel(band),
					status == null || status.isBlank() ? "-" : status));
		}

		return tooltip.toString();
	}


	private String bandToHumanLabel(kst4contest.model.Band b) {
		// Human-friendly labels for VHF/UHF/microwave contesting
		return switch (b) {
			case B_50 -> "6m";
			case B_70 -> "4m";
			case B_144 -> "2m";
			case B_432 -> "70cm";
			case B_1296 -> "23cm";
			case B_2320 -> "13cm";
			case B_3400 -> "9cm";
			case B_5760 -> "6cm";
			case B_10G -> "3cm";
			case B_24G -> "24G";
		};
	}

	/**
	 * Chooses a useful initial band for a new sked.
	 *
	 * <p>Recent frequency evidence has priority over station-name information.
	 * Manual NOT-QRV exclusions are respected. The operator can still select
	 * another locally enabled band from the dropdown.</p>
	 */
	private Band resolveDefaultSkedBand(ChatMember selectedMember,
	                                    EnumSet<Band> enabledBands) {

		if (selectedMember == null || enabledBands == null || enabledBands.isEmpty()) {
			return null;
		}

		List<ChatMember> variants =
				chatcontroller.findActiveChatMembersByRawCall(
						selectedMember.getCallSignRaw()
				);

		if (variants.isEmpty()) {
			variants = List.of(selectedMember);
		}

		BandOpportunityResolver.Resolution resolution =
				BandOpportunityResolver.resolve(
						variants,
						System.currentTimeMillis()
				);

		EnumSet<Band> availableBands = resolution.getAvailableBands();
		availableBands.retainAll(enabledBands);

		Band newestFrequencyBand = null;
		long newestTimestamp = Long.MIN_VALUE;
		long now = System.currentTimeMillis();

		for (ChatMember member : variants) {
			if (member == null || member.getKnownActiveBands() == null) {
				continue;
			}

			for (Map.Entry<Band, ChatMember.ActiveFrequencyInfo> entry
					: member.getKnownActiveBands().entrySet()) {

				Band band = entry.getKey();
				ChatMember.ActiveFrequencyInfo info = entry.getValue();

				if (band == null
						|| info == null
						|| !availableBands.contains(band)
						|| !band.isPlausible(info.frequency)) {
					continue;
				}

				long ageMs = now - info.timestampEpoch;
				if (ageMs < 0L
						|| ageMs > BandOpportunityResolver.RECENT_DYNAMIC_EVIDENCE_MAX_AGE_MS) {
					continue;
				}

				if (info.timestampEpoch > newestTimestamp) {
					newestTimestamp = info.timestampEpoch;
					newestFrequencyBand = band;
				}
			}
		}

		if (newestFrequencyBand != null) {
			return newestFrequencyBand;
		}

		if (!availableBands.isEmpty()) {
			return availableBands.iterator().next();
		}

		return enabledBands.iterator().next();
	}


	/**
	 * This method generates a BoderPane which shows some additional information about a callsign which had been
	 * selected either: <br/>
	 * - at the userlist <br/>
	 * - at the CQ message list (senders callsign)<br/>
	 * - at the PM message list (senders callsign)<br/><br/>
	 * Method gets its information source out of the original chatmember object of the userlist, not a copy
	 *
	 * @param selectedCallSignInfoStageChatMember
	 * @return
	 */


	/**
	 * Applies the currently selected station filters to the ChatMember FilteredList.
	 *
	 * This replaces the old predicate-property binding / forced refresh mechanics.
	 * The important point is that the TableView should only be invalidated when the
	 * effective filter really changed. Otherwise JavaFX may emit selection events
	 * during normal periodic updates although the operator did not click another
	 * station.
	 *
	 * <p>Regardless of whether the invalidation below was really necessary,
	 * re-evaluating the FilteredList predicate can make the TableView emit a
	 * selection-changed event for the still-selected station (e.g. because the
	 * FilteredList/SortedList rebuild produces a new ChatMember instance for the
	 * same callsign). This happens synchronously for every caller of this method,
	 * including the callsign search field's textProperty listener on every single
	 * keystroke. Without a guard, that spurious event reaches the table-selection
	 * listener and re-prepares/re-focuses the send text field, yanking keyboard
	 * focus away from wherever the operator currently is. Guard it the same way
	 * focusChatMemberAndPrepareCq guards its own programmatic selection.</p>
	 */
	private void applyChatMemberFilterPredicates() {
		if (chatcontroller == null
				|| chatMemberListBinding == null
				|| chatcontroller.getLst_chatMemberListFilterPredicates() == null) {
			return;
		}

		Predicate<ChatMember> combinedPredicate;

		if (chatcontroller.getLst_chatMemberListFilterPredicates().snapshot().isEmpty()) {
			combinedPredicate = SHOW_ALL_CHAT_MEMBER_PREDICATE;
		} else {
			combinedPredicate = chatcontroller.getLst_chatMemberListFilterPredicates()
					.snapshot()
					.stream()
					.reduce(SHOW_ALL_CHAT_MEMBER_PREDICATE, Predicate::and);
		}

		/*
		 * Do not reset the same show-all predicate over and over again.
		 *
		 * The periodic priority/user-list refresh calls this method regularly. If the
		 * visible station list is rebuilt without real filter changes, the TableView
		 * selection model may emit another selection event and the send-text field can
		 * be prepared again.
		 */
		if (combinedPredicate == lastAppliedChatMemberFilterPredicate) {
			return;
		}

		lastAppliedChatMemberFilterPredicate = combinedPredicate;

		/*
		 * The refresh used to be wrapped in programmaticChatMemberSelectionChange so the
		 * TableView's selection listener would not prepare the send text a second time.
		 * There is no such listener any more: the Compose station table reports a click
		 * directly through onStationSelected, and a mirror refresh emits nothing.
		 */
		chatMemberListBinding.refresh();
	}


	/**
	 * Helper method for furtherinfoPane
	 * @param s
	 * @return
	 */
	private static List<Integer> parseMinuteOffsets(String s) {
		if (s == null || s.isBlank()) return List.of();
		String[] parts = s.split("\\+");
		List<Integer> out = new ArrayList<>();
		for (String p : parts) {
			try {
				out.add(Integer.parseInt(p.trim()));
			} catch (Exception ignore) {}
		}
		return out;
	}



	private ChatMember resolveChatMemberForCallRawAndCategory(String callRaw, ChatCategory preferredCategory) {

		if (callRaw == null) return null;

		// 1) Prefer exact (callRaw + category)
		// One snapshot for both passes: the roster is consistent inside it.
		List<ChatMember> currentMembers = chatcontroller.getLst_chatMemberList().snapshot();

		for (ChatMember m : currentMembers) {
			if (m == null) continue;
			if (m.getCallSignRaw() == null) continue;
			if (!m.getCallSignRaw().equalsIgnoreCase(callRaw)) continue;

			if (preferredCategory != null && preferredCategory.equals(m.getChatCategory())) {
				return m;
			}
		}

		// 2) Fallback: any variant with same callsignRaw
		for (ChatMember m : currentMembers) {
			if (m == null) continue;
			if (m.getCallSignRaw() == null) continue;
			if (m.getCallSignRaw().equalsIgnoreCase(callRaw)) return m;
		}

		return null;
	}

	/**
	 * Focuses a ChatMember and prepares the send field.
	 *
	 * This overload is used by explicit operator actions such as timeline clicks or
	 * context actions. In those cases it is acceptable to overwrite the previous
	 * automatic /cq template, because the operator intentionally chose a station.
	 */
	private void focusChatMemberAndPrepareCq(ChatMember member) {
		focusChatMemberAndPrepareCq(member, true);
	}

	/**
	 * Focuses a ChatMember and optionally prepares the /cq command.
	 *
	 * The method selects the member in the table if possible, but guards that
	 * programmatic selection with programmaticChatMemberSelectionChange. This prevents
	 * the table selection listener from running the same logic a second time.
	 *
	 * @param member ChatMember that should become the active station
	 * @param forcePrepareCq true if /cq should be prepared even if the logical
	 *                       selection did not change
	 */
	private void focusChatMemberAndPrepareCq(ChatMember member, boolean forcePrepareCq) {
		if (member == null) {
			return;
		}

		/*
		 * This used to select and scroll to the row in the JavaFX station table first, for
		 * visual feedback. The Compose table scrolls itself when its selection changes, and
		 * the JavaFX one never had rows.
		 */
		handleChatMemberSelectionChanged(member, forcePrepareCq);
	}

	/**
	 * Central handler for a selected ChatMember.
	 *
	 * Selection changes can be emitted by JavaFX even when the user did not click a
	 * new station, for example after FilteredList/SortedList invalidations during
	 * periodic score updates.
	 *
	 * Therefore the send text is only auto-prepared when:
	 * - the logical station really changed, or
	 * - the caller explicitly forces preparation.
	 *
	 * Even then, the send field is protected by prepareCqTextForSelectedChatMember()
	 * so manually typed operator text is not destroyed.
	 */
	private void handleChatMemberSelectionChanged(ChatMember selectedMember, boolean forcePrepareCq) {
		if (selectedMember == null) {
			return;
		}

		/*
		 * Whether the info pane already shows this station. Compared against the
		 * member the pane was last built for, not against the selection model's
		 * previous value: a station entering or leaving the chat replaces the whole
		 * table mirror, which makes the model emit [null, sameMember]. Its previous
		 * value is then null and looks like a change, while the operator never left
		 * the station.
		 *
		 * Rebuilding the pane on such an event resets the message filter radio
		 * buttons to their configured defaults, which takes the operator's view away
		 * while they are still reading it. On a busy server that happens every few
		 * seconds.
		 */
		boolean infoPaneAlreadyShowsMember =
				isSameLogicalChatMember(selectedCallSignInfoStageChatMember, selectedMember);

		selectedCallSignInfoStageChatMember = selectedMember;

		if (chatcontroller != null && chatcontroller.getScoreService() != null) {
			chatcontroller.getScoreService().setSelectedChatMember(selectedMember);
		}

		if (!infoPaneAlreadyShowsMember) {
			/*
			 * Rebind the Compose "messages of the selected station" table to the newly
			 * selected member. This used to be a side effect of building the JavaFX
			 * info pane (generateFurtherInfoAbtSelectedCallsignBP, now deleted): that
			 * pane was never shown, but the binding it created feeds the Compose table.
			 * Only that live binding remains here.
			 */
			try {
				rebuildSelectedCallSignInfoMessageBinding();
			} catch (Exception exception) {
				System.out.println("KST4CApp: ERROR, selected member disappeared: " + exception.getMessage());
			}
		}

		/*
		 * prepareCqTextForSelectedChatMember() ends in requestFocus() on the send
		 * field. It must therefore only run when the station really changed, or when
		 * the operator selected a row themselves: the [null, sameMember] burst a
		 * mirror replacement produces would otherwise pull the keyboard focus away
		 * from whatever field the operator is typing in, every time a station enters
		 * or leaves the chat.
		 */
		if (forcePrepareCq || !infoPaneAlreadyShowsMember) {
			prepareCqTextForSelectedChatMember(selectedMember, forcePrepareCq);
		}
	}

	/**
	 * Prepares the send field with "/cq CALL " for the selected ChatMember.
	 *
	 * The ChatMember's category is stored together with the prepared text. This is
	 * required because a later table refresh may change the selected row while the
	 * operator is still editing the message. The send handler can then still send the
	 * message in the category of the callsign shown in the text field.
	 */
	private void prepareCqTextForSelectedChatMember(ChatMember member, boolean forceOverwrite) {
		if (member == null) {
			return;
		}

		rememberAutoPreparedCqTarget(member.getCallSign(), member.getChatCategory());
	}

	/**
	 * Convenience overload when only a callsign is known.
	 *
	 * No category is attached in this case. The send handler will later try to
	 * resolve the category from the visible /cq callsign and the active user list.
	 */
	private void rememberAutoPreparedCqTarget(String callSign) {
		rememberAutoPreparedCqTarget(callSign, null);
	}

	/**
	 * Prepares "/cq CALL " in the send field without destroying operator input.
	 *
	 * The field may only be overwritten when:
	 * - forceOverwrite is true,
	 * - the field is empty, or
	 * - the field still contains the previous automatically generated /cq template.
	 *
	 * If the operator already changed the text, this method leaves the field exactly
	 * as it is.
	 */
	private void rememberAutoPreparedCqTarget(String callSign, ChatCategory preparedCategory) {
		if (callSign == null || callSign.isBlank()) {
			return;
		}

		/*
		 * Remember which station the /cq text was prepared for, and in which category.
		 * resolveOutgoingChatCategory reads this: it connects the visible /cq command to
		 * the right chat category even when a refresh or a filter changes the selected row
		 * while the operator is still typing. The Compose chat input owns the text and its
		 * own overwrite guard, but it keeps no category, so this bookkeeping has no Compose
		 * counterpart and stays here.
		 *
		 * Recorded unconditionally. If the Compose input declined to overwrite operator
		 * text, the recorded callsign simply will not match the /cq target extracted from
		 * the visible text, and resolveOutgoingChatCategory falls through to its remaining
		 * sources — which is what it did before this was recorded at all.
		 */
		lastAutoPreparedCqTargetCallsign = normalizeCallsignForCategoryResolution(callSign);
		lastAutoPreparedCqTargetCategory = normalizeToActiveChatCategory(preparedCategory);
	}

	/**
	 * Compares two ChatMember objects by their active chat identity.
	 *
	 * <p>JavaFX may replace item instances during list refreshes, so object
	 * identity alone is not sufficient. The complete callsign and the chat
	 * category identify one active ON4KST login.</p>
	 *
	 * <p>The raw/base callsign must not be used here. Otherwise callsigns such as
	 * {@code 9A0BB-2} and {@code 9A0BB-70} would still be treated as the same
	 * selection inside one chat category.</p>
	 */
	private boolean isSameLogicalChatMember(ChatMember a, ChatMember b) {

		if (a == b) {
			return true;
		}

		if (a == null || b == null) {
			return false;
		}

		String aCallSign = a.getCallSign();
		String bCallSign = b.getCallSign();

		if (aCallSign == null
				|| bCallSign == null
				|| !aCallSign.equalsIgnoreCase(bCallSign)) {
			return false;
		}

		ChatCategory aCategory = a.getChatCategory();
		ChatCategory bCategory = b.getChatCategory();

		if (aCategory == bCategory) {
			return true;
		}

		if (aCategory == null || bCategory == null) {
			return false;
		}

		return aCategory.getCategoryNumber()
				== bCategory.getCategoryNumber();
	}

	/**
	 * Resolves the chat category for an outgoing message.
	 *
	 * Important: if the send field contains an explicit "/cq CALL ...", the target
	 * callsign in the text is authoritative. This prevents a later table refresh or
	 * selection event from sending an already typed message in the category of a
	 * different selected ChatMember.
	 */
	private ChatCategory resolveOutgoingChatCategory(String outgoingText, ChatMember selectedMember) {
		ChatCategory mainCategory = chatcontroller != null ? chatcontroller.getChatCategoryMain() : null;
		ChatCategory selectedMemberCategory = getActiveChatCategoryForMember(selectedMember);
		String cqTargetCallsign = extractCqTargetCallsign(outgoingText);

		if (cqTargetCallsign != null) {
			String normalizedTarget = normalizeCallsignForCategoryResolution(cqTargetCallsign);

			/*
			 * If this text was auto-prepared from a concrete ChatMember, keep that
			 * category attached to the text even if the table selection changes later.
			 */
			if (normalizedTarget.equals(lastAutoPreparedCqTargetCallsign)
					&& lastAutoPreparedCqTargetCategory != null) {
				return lastAutoPreparedCqTargetCategory;
			}

			/*
			 * If the currently selected member is the same station as the /cq target,
			 * its category is safe to use.
			 */
			if (selectedMember != null
					&& selectedMemberCategory != null
					&& chatMemberMatchesCallsign(selectedMember, normalizedTarget)) {
				return selectedMemberCategory;
			}

			/*
			 * Otherwise resolve the /cq target from the user list. This also protects
			 * manually typed /cq texts when a different station is selected.
			 */
			ChatCategory uniqueCategoryForTarget = findUniqueActiveCategoryForCallsign(normalizedTarget);
			if (uniqueCategoryForTarget != null) {
				return uniqueCategoryForTarget;
			}

			System.out.println("KST4CApp: WARNING, could not resolve chat category for explicit /cq target "
					+ cqTargetCallsign + "; using main chat category as safe fallback.");
			return mainCategory;
		}

		if (selectedMemberCategory != null) {
			return selectedMemberCategory;
		}

		return mainCategory;
	}

	/**
	 * Returns an active ChatCategory object for the member category.
	 *
	 * This avoids sending with stale category instances and maps the member category
	 * to the controller's current main/second category objects.
	 */
	private ChatCategory getActiveChatCategoryForMember(ChatMember member) {
		if (member == null) {
			return null;
		}
		return normalizeToActiveChatCategory(member.getChatCategory());
	}

	/**
	 * Maps any category object to the currently active main/second chat category.
	 *
	 * This is safer than directly reusing a category object from a ChatMember, because
	 * the controller owns the actual active category instances used for sending.
	 */
	private ChatCategory normalizeToActiveChatCategory(ChatCategory category) {
		if (category == null || chatcontroller == null) {
			return null;
		}

		String categoryNumber = category.getCategoryNumber() + "";

		if (chatcontroller.getChatCategoryMain() != null
				&& categoryNumber.equals(chatcontroller.getChatCategoryMain().getCategoryNumber() + "")) {
			return chatcontroller.getChatCategoryMain();
		}

		if (chatcontroller.getChatCategorySecondChat() != null
				&& categoryNumber.equals(chatcontroller.getChatCategorySecondChat().getCategoryNumber() + "")) {
			return chatcontroller.getChatCategorySecondChat();
		}

		return null;
	}

	/**
	 * Extracts the target callsign from a directed ON4KST command such as:
	 *
	 * /cq DL1ABC hello
	 *
	 * Returns null for normal/general messages that do not start with "/cq ".
	 */
	private String extractCqTargetCallsign(String outgoingText) {
		if (outgoingText == null) {
			return null;
		}

		String trimmedText = outgoingText.trim();
		if (trimmedText.length() < 5 || !trimmedText.toLowerCase(Locale.ROOT).startsWith("/cq ")) {
			return null;
		}

		String afterCommand = trimmedText.substring(4).trim();
		if (afterCommand.isBlank()) {
			return null;
		}

		String[] parts = afterCommand.split("\\s+", 2);
		return parts.length > 0 && !parts[0].isBlank() ? parts[0].trim() : null;
	}

	/**
	 * Searches the active user list for one unique category for a callsign.
	 *
	 * If the same callsign is present in both active chat channels, the result is not
	 * unique and null is returned. In that case the send handler falls back to the
	 * main category and logs a warning.
	 */
	private ChatCategory findUniqueActiveCategoryForCallsign(String normalizedCallsign) {
		if (normalizedCallsign == null || normalizedCallsign.isBlank()
				|| chatcontroller == null || chatcontroller.getLst_chatMemberList() == null) {
			return null;
		}

		ChatCategory foundCategory = null;

		for (ChatMember chatMember : chatcontroller.getLst_chatMemberList().snapshot()) {
			if (chatMember == null || !chatMemberMatchesCallsign(chatMember, normalizedCallsign)) {
				continue;
			}

			ChatCategory activeCategory = getActiveChatCategoryForMember(chatMember);
			if (activeCategory == null) {
				continue;
			}

			if (foundCategory == null) {
				foundCategory = activeCategory;
			} else if (foundCategory.getCategoryNumber() != activeCategory.getCategoryNumber()) {
				// Same callsign is present in multiple active chat channels; not unique.
				return null;
			}
		}

		return foundCategory;
	}

	/**
	 * Checks whether a ChatMember represents the given normalized callsign.
	 *
	 * Both getCallSign() and getCallSignRaw() are checked because different parts of
	 * the program use either the decorated/display callsign or the raw callsign.
	 */
	private boolean chatMemberMatchesCallsign(ChatMember chatMember, String normalizedCallsign) {
		if (chatMember == null || normalizedCallsign == null || normalizedCallsign.isBlank()) {
			return false;
		}

		String callSign = normalizeCallsignForCategoryResolution(chatMember.getCallSign());
		String callSignRaw = normalizeCallsignForCategoryResolution(chatMember.getCallSignRaw());

		return normalizedCallsign.equals(callSign) || normalizedCallsign.equals(callSignRaw);
	}

	/**
	 * Normalizes callsigns for safe comparisons.
	 */
	private String normalizeCallsignForCategoryResolution(String callsign) {
		return callsign == null ? "" : callsign.trim().toUpperCase(Locale.ROOT);
	}

//	private void focusChatMemberAndPrepareCq(ChatMember member) {
//		if (member == null) return;
//
//		// Try selecting in table if it is visible (nice UX), but do not depend on it
//		try {
//			if (tbl_chatMember != null && tbl_chatMember.getItems() != null && tbl_chatMember.getItems().contains(member)) {
//				tbl_chatMember.getSelectionModel().select(member);
//				tbl_chatMember.scrollTo(member);
//			}
//		} catch (Exception ignored) {
//			// ignore: table not ready or filtered
//		}
//
//		// Force selection effects regardless of filters/selection state
//		selectedCallSignInfoStageChatMember = member;
//		chatcontroller.getScoreService().setSelectedChatMember(member);
//
//		selectedCallSignFurtherInfoPane.getChildren().setAll(generateFurtherInfoAbtSelectedCallsignBP(member));
//
//		txt_chatMessageUserInput.clear();
//		txt_chatMessageUserInput.setText("/cq " + member.getCallSign() + " ");
//		txt_chatMessageUserInput.requestFocus();
//		txt_chatMessageUserInput.selectEnd();
//	}



	/**
	 * Creates the mirror that feeds the Compose "messages of the selected station" table.
	 *
	 * Lifted out of initFurtherInfoAbtCallsignMSGTable, which built the never-shown
	 * JavaFX table around it. The feed it drives does not depend on that table existing,
	 * so the binding must not either - otherwise deleting the table would silently empty
	 * the Compose table with a green build and no compile error, the exact failure class
	 * part 1 was written to prevent.
	 *
	 * Called once per selected-station info build, replacing the previous mirror rather
	 * than leaving it listening on the message roster - the same lifecycle the binding
	 * had while it lived inside the table builder.
	 */
	private void rebuildSelectedCallSignInfoMessageBinding() {
		if (selectedCallSignInfoMessageBinding != null) {
			selectedCallSignInfoMessageBinding.dispose();
			rosterBindings.remove(selectedCallSignInfoMessageBinding);
		}
		selectedCallSignInfoMessageBinding = derivedBinding(
				chatcontroller::selectedCallSignInfoMessages,
				chatcontroller.getLst_globalChatMessageList());
		selectedCallSignInfoMessageBinding.onChanged(rows -> {
			if (selectedStationMessagesFeed != null) {
				selectedStationMessagesFeed.push(rows);
			}
		});
	}

	/**
	 * Builds the lower global-message area.
	 *
	 * This TabPane is intentionally independent from the selected ChatMember.
	 * It contains global message streams only:
	 * - public/CQ messages
	 * - DXCluster messages
	 * - messages between other stations ("QSO of the other")
	 *
	 * The already initialized public-message table is passed in so its existing
	 * context menu and selection handling remain unchanged.
	 *
	 * DXCluster and QSO-of-the-other get their own TableViews on the same backing
	 * lists. This is important because the existing separate "Cluster & QSO of the other"
	 * window can keep using its own TableViews at the same time.
	 */

	/**
	 * initializes the tableview in which the cq- and beacon-texts are shown
	 * 
	 * @return
	 */






//	private BorderPane initTopPriorityListPane(TableView<ChatMember> tbl_chatMember, TextField txt_chatMessageUserInput) {
//
//		BorderPane pane = new BorderPane();
//		pane.setStyle("-fx-padding: 3;");
//
//		Label header = new Label("Top priority candidates");
//		header.getStyleClass().add("label");
//
//		ListView<kst4contest.controller.ScoreService.TopCandidate> listView = new ListView<>();
//		listView.setItems(mirrorOf(chatcontroller.getScoreService().topCandidates()));
//
//		listView.setCellFactory(lv -> new ListCell<>() {
//			@Override
//			protected void updateItem(kst4contest.controller.ScoreService.TopCandidate item, boolean empty) {
//				super.updateItem(item, empty);
//				if (empty || item == null) {
//					setText(null);
//					return;
//				}
//				// Keep it compact; score is mainly evaluated in FurtherInfo
//				setText(item.getDisplayCallSign() + "  |  score " + String.format(java.util.Locale.US, "%.0f", item.getScore()));
//			}
//		});
//
//		listView.setOnMouseClicked(evt -> {
//			if (evt.getClickCount() < 1) return;
//			kst4contest.controller.ScoreService.TopCandidate c = listView.getSelectionModel().getSelectedItem();
//			if (c == null) return;
//
//			ChatMember resolved = resolveChatMemberForTopCandidate(c);
//			if (resolved == null) return;
//
//			// Try to select in table (reuses existing selection logic)
//			if (tbl_chatMember.getItems().contains(resolved)) {
//				tbl_chatMember.getSelectionModel().select(resolved);
//				tbl_chatMember.scrollTo(resolved);
//			} else {
//				// Fallback: if filtered out, still show FurtherInfo + prepare /cq
//				selectedCallSignInfoStageChatMember = resolved;
//				chatcontroller.getScoreService().setSelectedChatMember(selectedCallSignInfoStageChatMember);
//
//				selectedCallSignFurtherInfoPane.getChildren().setAll(generateFurtherInfoAbtSelectedCallsignBP(resolved));
//				txt_chatMessageUserInput.clear();
//				txt_chatMessageUserInput.setText("/cq " + resolved.getCallSign() + " ");
//				txt_chatMessageUserInput.requestFocus();
//				txt_chatMessageUserInput.selectEnd();
//
//				// Keep ScoreService selection in sync
//				chatcontroller.getScoreService().setSelectedChatMember(resolved);
//			}
//		});
//
//		pane.setTop(header);
//		pane.setCenter(listView);
//		return pane;
//	}

	/**
	 * Builds the compact priority candidate area for the right side of the main UI.
	 *
	 * The former implementation showed the complete priority list permanently.
	 * That used a lot of vertical space although normally only the first one or two
	 * candidates are relevant during live operation.
	 *
	 * New behaviour:
	 * - show only Top 1 and Top 2 directly in the main window
	 * - click Top 1/Top 2 to select that candidate immediately
	 * - use "more" to open the full priority list in a separate window
	 */



	/**
	 * Resolves and selects a priority candidate.
	 *
	 * <p>The common selection helper is used deliberately. It updates Further Info,
	 * prepares the directed message and selects the corresponding table row when
	 * the row is currently visible. A candidate hidden by an active table filter
	 * remains usable without changing or resetting that filter.</p>
	 */
	private void selectTopCandidate(kst4contest.controller.ScoreService.TopCandidate candidate) {
		if (candidate == null) {
			return;
		}

		ChatMember resolved = resolveChatMemberForTopCandidate(candidate);

		if (resolved == null) {
			return;
		}

		/*
		 * This is an explicit operator action and therefore follows the same path
		 * as a timeline click. The exact callsign and category stored in the
		 * TopCandidate remain authoritative.
		 *
		 * focusChatMemberAndPrepareCq() also handles candidates which are currently
		 * hidden by a FilteredList. In that case the table row cannot be selected,
		 * but Further Info, ScoreService selection and the prepared /cq message are
		 * still updated.
		 */
		focusChatMemberAndPrepareCq(resolved);
	}

	/**
	 * Opens the complete priority candidate list in a separate window.
	 *
	 * This keeps the main UI compact while still making the full list available when
	 * the operator wants to inspect more than the first two candidates.
	 *
	 * The window is Compose. Its JavaFX predecessor was the only Stage this class
	 * constructed, and it carried the themed-scene registration and the shared system
	 * menu bar with it; both lose their only live caller here.
	 */
	private void showTopPriorityCandidatesWindow() {
		TopPriorityCandidatesWindow.show(
				() -> chatcontroller.getScoreService().topCandidates().snapshot(),
				candidate -> uiDispatcher.runOnUi(() -> selectTopCandidate(candidate)),
				chatcontroller.getChatPreferences().isGUI_darkModeActive(),
				SETTINGS_WINDOW_FONT_SIZE_SP,
				paletteStore,
				languageStore
		);
	}


	private ChatMember resolveChatMemberForTopCandidate(
			kst4contest.controller.ScoreService.TopCandidate c
	) {

		if (c == null
				|| c.getDisplayCallSign() == null
				|| c.getPreferredChatCategory() == null) {
			return null;
		}

		/*
		 * Resolve the concrete active login by full callsign and category. Falling
		 * back to callSignRaw could select a different suffix in the same category.
		 */
		return chatcontroller.findActiveChatMember(
				c.getDisplayCallSign(),
				c.getPreferredChatCategory()
		);
	}


	/**
	 * Pushes the current skeds and priority candidates into the Compose timeline strip
	 * above the send field. Driven by the sked list, the score-service pulse, the QTF
	 * change and an initial run; it reads the antenna azimuth fresh from preferences.
	 */
	private void updateTimelineVisuals() {
		if (chatcontroller == null) return;

		if (!uiDispatcher.isUiThread()) {
			uiDispatcher.runOnUi(this::updateTimelineVisuals);
			return;
		}

		List<ContestSked> skedsSnapshot = new ArrayList<>(chatcontroller.getActiveSkeds().snapshot());
		List<TimelineCandidate> candidates = buildTimelinePriorityCandidateEvents();

		if (composeMainWindowState != null) {
			if (timelineFeed != null) {
				timelineFeed.push(
						skedsSnapshot,
						candidates,
						chatcontroller.getChatPreferences().getActualQTF().get(),
						chatcontroller.getChatPreferences().getStn_antennaBeamWidthDeg()
				);
			}
		}
	}

	/**
	 * Build candidate markers for the timeline:
	 * - Use ScoreService TopCandidates (already sorted)
	 * - Resolve representative ChatMember (preferred category if possible)
	 * - Use "next airplane arriving minute" as time basis
	 * - Bucket by minute, keep top 1-2 per minute (config above)
	 */
	private List<TimelineCandidate> buildTimelinePriorityCandidateEvents() {

		if (chatcontroller.getScoreService() == null) return Collections.emptyList();

		long now = System.currentTimeMillis();

		// Snapshot to avoid concurrent modifications (TopCandidates list is FX observable)
		List<kst4contest.controller.ScoreService.TopCandidate> topSnapshot =
				new ArrayList<>(chatcontroller.getScoreService().topCandidates().snapshot());

		Map<Integer, List<TimelineCandidateTmp>> byMinute = new HashMap<>();

		for (kst4contest.controller.ScoreService.TopCandidate c : topSnapshot) {

			ChatMember representative = resolveChatMemberForTopCandidate(c);
			if (representative == null) continue;

			AirPlaneReflectionInfo apInfo = representative.getAirPlaneReflectInfo();

			// choose airplane by (highest potential) then (shortest time) within preview window
			int maxMinutes = (int) (TIMELINE_PREVIEW_TIME_MS / 60_000L);
			NextApInfo selectedAp = findBestAirplane(apInfo, maxMinutes);
			if (selectedAp == null) continue;

			long timeUntilMs = selectedAp.arrivingMinutes * 60_000L;
			if (timeUntilMs < 0 || timeUntilMs > TIMELINE_PREVIEW_TIME_MS) continue;

			int minuteBucket = selectedAp.arrivingMinutes;

			byMinute.computeIfAbsent(minuteBucket, k -> new ArrayList<>())
					.add(new TimelineCandidateTmp(c, representative, selectedAp));
		}

		List<TimelineCandidate> out = new ArrayList<>();

		for (Map.Entry<Integer, List<TimelineCandidateTmp>> e : byMinute.entrySet()) {

			int minuteBucket = e.getKey();
			List<TimelineCandidateTmp> bucket = e.getValue();

			// Highest score first
			bucket.sort((a, b) -> Double.compare(b.top.getScore(), a.top.getScore()));


			// 1) Pick top-N in beam -> lanes 0..1
			List<TimelineCandidateTmp> inBeam = new ArrayList<>();
			for (TimelineCandidateTmp tmp : bucket) {
				if (chatcontroller.isChatMemberInMyBeam(tmp.representativeMember)) {
					inBeam.add(tmp);
				}
			}

			// Avoid duplicates between beam and global selection
			Set<String> used = new HashSet<>();

			int beamTake = Math.min(TIMELINE_BEAM_MARKERS_PER_MINUTE, inBeam.size());
			for (int lane = 0; lane < beamTake; lane++) {

				TimelineCandidateTmp tmp = inBeam.get(lane);
				used.add(tmp.top.getCallSignRaw());

				double az = (tmp.representativeMember.getQTFdirection() != null) ? tmp.representativeMember.getQTFdirection() : 0.0;
				long timeUntilMs = minuteBucket * 60_000L;

				String tooltip = buildTimelineCandidateTooltip(tmp, minuteBucket);
				int potential = (tmp.nextAp != null && tmp.nextAp.plane != null) ? tmp.nextAp.plane.getPotential() : 0;

				out.add(new TimelineCandidate(
						tmp.top.getCallSignRaw(),
						tmp.top.getDisplayCallSign(),
						tmp.top.getPreferredChatCategory(),
						timeUntilMs,
						minuteBucket,
						lane, // lanes 0..1 = in-beam
						az,
						tmp.top.getScore(),
						potential,
						tooltip
				));
			}

			// 2) Pick top-N global distinct -> lanes 2..3
			int globalAdded = 0;
			for (TimelineCandidateTmp tmp : bucket) {

				if (globalAdded >= TIMELINE_PRIORITY_MARKERS_PER_MINUTE) break;
				if (used.contains(tmp.top.getCallSignRaw())) continue;

				double az = (tmp.representativeMember.getQTFdirection() != null) ? tmp.representativeMember.getQTFdirection() : 0.0;
				long timeUntilMs = minuteBucket * 60_000L;

				String tooltip = buildTimelineCandidateTooltip(tmp, minuteBucket);
				int potential = (tmp.nextAp != null && tmp.nextAp.plane != null) ? tmp.nextAp.plane.getPotential() : 0;

				int laneIndex = TIMELINE_BEAM_MARKERS_PER_MINUTE + globalAdded; // lanes 2..3

				out.add(new TimelineCandidate(
						tmp.top.getCallSignRaw(),
						tmp.top.getDisplayCallSign(),
						tmp.top.getPreferredChatCategory(),
						timeUntilMs,
						minuteBucket,
						laneIndex,
						az,
						tmp.top.getScore(),
						potential,
						tooltip
				));

				globalAdded++;
			}
		}

		out.sort(Comparator
				.comparingInt(TimelineCandidate::getMinuteBucket)
				.thenComparingInt(TimelineCandidate::getLaneIndex));

		return out;
	}

	private String buildTimelineCandidateTooltip(TimelineCandidateTmp tmp, int minuteBucket) {
		AirPlane p = tmp.nextAp.plane;

		String planeStr = "-";
		if (p != null) {
			planeStr = p.getApCallSign()
					+ " | " + p.getPotencialDescriptionAsWord()
					+ " | pot " + p.getPotential()
					+ " | dist " + p.getDistanceKm() + " km";
		}

		NextApInfo earliest = findEarliestAirplane(
				tmp.representativeMember.getAirPlaneReflectInfo(),
				(int) (TIMELINE_PREVIEW_TIME_MS / 60_000L)
		);

		String earliestStr = "";
		if (earliest != null && earliest.plane != null) {
			// only show if it differs from the selected/best plane minute
			if (earliest.arrivingMinutes != minuteBucket) {
				earliestStr = "\nearliest AP: +" + earliest.arrivingMinutes
						+ " min (pot " + earliest.plane.getPotential() + "%)";
			}
		}

		return tmp.top.getDisplayCallSign()
				+ "\nscore: " + String.format(Locale.US, "%.0f", tmp.top.getScore())
				+ "\nbest AP: +" + minuteBucket + " min"
				+ "\nplane: " + planeStr
				+ earliestStr;
	}

	/**
	 * Select the airplane that should drive timeline/sked decisions.
	 *
	 * Rule: prefer highest potential; if tied, prefer shortest arriving time.
	 * Only considers planes within [0..maxMinutes] to avoid dropping stations completely.
	 */
	private NextApInfo findBestAirplane(AirPlaneReflectionInfo apInfo, int maxMinutes) {
		if (apInfo == null) return null;
		if (apInfo.getRisingAirplanes() == null) return null;

		AirPlane best = null;
		int bestMin = Integer.MAX_VALUE;
		int bestPot = Integer.MIN_VALUE;

		for (AirPlane p : apInfo.getRisingAirplanes()) {
			if (p == null) continue;

			int m = p.getArrivingDurationMinutes();
			if (m < 0 || m > maxMinutes) continue;

			int pot = p.getPotential();

			// primary: potential DESC, secondary: time ASC
			if (best == null || pot > bestPot || (pot == bestPot && m < bestMin)) {
				best = p;
				bestPot = pot;
				bestMin = m;
			}
		}

		if (best == null) return null;
		return new NextApInfo(best, bestMin);
	}

	/**
	 * Select the earliest airplane (used for additional tooltip info).
	 * Rule: prefer shortest arriving time; if tied, prefer higher potential.
	 */
	private NextApInfo findEarliestAirplane(AirPlaneReflectionInfo apInfo, int maxMinutes) {
		if (apInfo == null) return null;
		if (apInfo.getRisingAirplanes() == null) return null;

		AirPlane best = null;
		int bestMin = Integer.MAX_VALUE;

		for (AirPlane p : apInfo.getRisingAirplanes()) {
			if (p == null) continue;

			int m = p.getArrivingDurationMinutes();
			if (m < 0 || m > maxMinutes) continue;

			if (m < bestMin) {
				bestMin = m;
				best = p;
			} else if (m == bestMin && best != null && p.getPotential() > best.getPotential()) {
				best = p;
			}
		}

		if (best == null || bestMin == Integer.MAX_VALUE) return null;
		return new NextApInfo(best, bestMin);
	}











	/**
	 * Switches every Compose window to the dark or light design for the running session.
	 * The per-profile startup design is not changed here.
	 */
	private void applyTheme(boolean darkMode) {
		LOGGER.info("Switching GUI design to " + (darkMode ? "dark" : "light") + " mode");

		chatcontroller.getChatPreferences().setGUI_darkModeActive(darkMode);

		MainWindowHost.applyDarkMode(darkMode);
		SettingsWindow.applyDarkMode(darkMode);
		UpdateWindow.applyDarkMode(darkMode);
		MonitorWindow.applyDarkMode(darkMode);
		kst4contest.view.compose.map.StationMapWindow.applyDarkMode(darkMode);
		TopPriorityCandidatesWindow.applyDarkMode(darkMode);
	}

	/**
	 * Title of the macOS connection state menu, e.g. "🟢 LINK: Connected".
	 */
	static String macOsConnectionStateMenuTitle(On4KstConnectionState state) {
		On4KstConnectionState effectiveState = state == null
				? On4KstConnectionState.DISCONNECTED : state;
		return switch (effectiveState) {
			case ONLINE -> "🟢 LINK: Connected";
			case CONNECTING, WAITING_FOR_LOGIN_PROMPT, AUTHENTICATING,
			     SYNCING_MAIN_CHAT, SYNCING_SECOND_CHAT -> "🟡 LINK: Connecting…";
			case STOPPING -> "🟡 LINK: Disconnecting…";
			case RECONNECT_WAIT -> "🔴 LINK: Reconnecting…";
			case DISCONNECTED -> "🔴 LINK: Disconnected";
		};
	}


	/*****************************************************
	 * Sked warning Initializing and functional section
	 ****************************************************/

	private void updateConnectionStateIndicator(
			On4KstConnectionState state,
			String detail
	) {
		if (!uiDispatcher.isUiThread()) {
			LOGGER.warning(
					"Connection indicator update arrived outside the JavaFX thread; "
							+ "rescheduling it safely");
			uiDispatcher.runOnUi(() -> updateConnectionStateIndicator(state, detail));
			return;
		}

		On4KstConnectionState effectiveState = state == null
				? On4KstConnectionState.DISCONNECTED : state;
		String stateDetail = detail == null || detail.isBlank()
				? effectiveState.name() : detail;
				
		/*
		 * One write, not two. This method used to push the same pair into the Compose
		 * surroundings twice - once here and once again below the tooltip update - which
		 * is invisible until one of the two starts carrying stale values.
		 */
		if (connectionStateFeed != null) {
			connectionStateFeed.push(state, detail);
		}

		logConnectionIndicatorTransition(effectiveState, stateDetail);
	}

	private void logConnectionIndicatorTransition(
			On4KstConnectionState newState,
			String newDetail
	) {
		boolean stateChanged = newState != lastDisplayedConnectionState;
		boolean detailChanged = !Objects.equals(
				newDetail, lastDisplayedConnectionDetail);
		if (!stateChanged && !detailChanged) {
			return;
		}

		On4KstConnectionState previousState = lastDisplayedConnectionState;
		Level logLevel = switch (newState) {
			case DISCONNECTED, RECONNECT_WAIT -> Level.WARNING;
			default -> Level.INFO;
		};

		LOGGER.log(logLevel,
				"ON4KST connection indicator: {0} -> {1}; detail: {2}",
				new Object[] {
						previousState == null ? "UNINITIALIZED" : previousState,
						newState,
						newDetail
				});

		lastDisplayedConnectionState = newState;
		lastDisplayedConnectionDetail = newDetail;
	}


	/**
	 * Shows the sked warning in the Compose main window's notice strip.
	 *
	 * The JavaFX blinking button this used to drive belonged to the window that is never
	 * shown; it is gone. The Compose notice carries its own show/hide timing, so the
	 * blink timeline went with the button rather than being reimplemented here.
	 */
	private void showBlinkingSkedWarnIndicator(String text) {
		if (composeMainWindowState != null) {
			uiDispatcher.runOnUi(() -> composeMainWindowState.getSkedNotice().show(text, text));
		}
	}


	/*****************************************************
	 * Band-Upgrade warning (after log entry) section
	 ****************************************************/

	private void maybeShowBandUpgradeIndicator(String key, ThreadStateMessage msg) {
		if (msg == null) return;

		String nick = msg.getThreadNickName() == null ? "" : msg.getThreadNickName().toLowerCase(Locale.ROOT);
		String k = key == null ? "" : key.toLowerCase(Locale.ROOT);

		boolean isBandUpgrade = k.contains("bandupgrade") || nick.contains("bandupgrade");
		if (!isBandUpgrade) return;

		String buttonText = msg.getRunningInformationTextDescription();
		if (buttonText == null || buttonText.isBlank()) buttonText = "BAND+";

		String tooltip = msg.getRunningInformation();
		if (tooltip == null || tooltip.isBlank()) tooltip = buttonText;

		final String finalButtonText = buttonText;
		final String finalTooltip = tooltip;

		uiDispatcher.runOnUi(() -> showBlinkingBandUpgradeIndicator(finalButtonText, finalTooltip));
	}

	/**
	 * Shows the band-upgrade hint in the Compose main window's notice strip.
	 *
	 * As with the sked warning, the JavaFX blinking button belonged to the never-shown
	 * window and is gone; the Compose notice owns its own timing.
	 */
	private void showBlinkingBandUpgradeIndicator(String buttonText, String tooltipText) {
		if (composeMainWindowState != null) {
			uiDispatcher.runOnUi(() -> composeMainWindowState.getBandUpgradeNotice().show(buttonText, tooltipText));
		}
	}

/**
 * End Band-Upgrade section
 */


	/**
	 * End Sked warning section
	 */



	/**
	 * Stores the last automatically prepared text in the send input field.
	 *
	 * This is used to distinguish between:
	 * - text that KST4Contest created automatically, e.g. "/cq DL1ABC "
	 * - text that the operator has already edited manually
	 *
	 * Only automatically prepared text may be overwritten by a later automatic
	 * preparation. Manually typed text must never be destroyed by a table refresh.
	 */

	/**
	 * Callsign behind the last automatically prepared /cq command.
	 *
	 * This is important for category resolution while sending. If the visible text is
	 * "/cq DL1ABC ..." and a later table refresh changes the selected ChatMember, the
	 * outgoing message must still be sent in the category that belonged to DL1ABC.
	 */
	private String lastAutoPreparedCqTargetCallsign = "";

	/**
	 * Chat category that belonged to the last automatically prepared /cq target.
	 *
	 * The send handler uses this as the strongest category hint when the current
	 * input still targets the same callsign.
	 */
	private ChatCategory lastAutoPreparedCqTargetCategory = null;

	/**
	 * Guard flag for programmatic table selections.
	 *
	 * Some UI actions intentionally select a row in the ChatMember table for visual
	 * feedback. Those programmatic selections must not run the normal user-selection
	 * handler again, otherwise /cq preparation can happen twice or at the wrong time.
	 */




	/**
	 * True once this runtime released its resources. Shutdown must stay idempotent
	 * because it is reached both through the JavaFX stop() callback and explicitly.
	 */
	/**
	 * Guards the teardown against running twice.
	 *
	 * <p>Atomic because two threads can reach it since the shutdown hook replaced
	 * {@code Application.stop()}: the user-interface thread through
	 * {@code ApplicationRuntimeLauncher.exitApplication}, and the hook's own thread on a
	 * SIGTERM. A plain field let both pass the check, which means a double disconnect, a
	 * double preferences flush, and {@code rosterBindings.forEach(dispose)} racing
	 * {@code rosterBindings.clear()}. {@code compareAndSet} is what makes the javadoc
	 * below true rather than nearly true.</p>
	 */
	private final java.util.concurrent.atomic.AtomicBoolean runtimeShutdownDone =
			new java.util.concurrent.atomic.AtomicBoolean();

//	Stage stage_selectedCallSignInfoStage;
	ChatMember selectedCallSignInfoStageChatMember;







	/**
	 * Resolves the operator profile this runtime works with.
	 *
	 * <p>Only executed on the very first launch. A profile switch sets the profile before
	 * building the new runtime, so the resolution is skipped there.</p>
	 *
	 * <p>An installation with no or exactly one profile is resolved without asking
	 * anything, which keeps the single operator startup exactly as it was.</p>
	 *
	 * @return true if the application may continue starting up
	 */
	private boolean resolveOperatorProfileIfRequired() {

		if (ActiveOperatorProfile.isInitialized()) {
			return true;
		}

		OperatorProfileBootstrap bootstrap = new OperatorProfileBootstrap();
		OperatorProfileSelection resolvedProfile = bootstrap.resolveAtStartup(
				new OperatorProfileStore(),
				CommandLineOptions.remembered(),
				OperatorProfilePickerWindow::showAndSelect);

		if (bootstrap.getStartupWarning() != null) {
			ComposeAlert.acknowledge(
					kst4contest.view.i18n.CurrentStrings.get().getAlertProfileTitle(),
					kst4contest.view.i18n.CurrentStrings.get().getAlertProfileNotFound(),
					bootstrap.getStartupWarning());
		}

		if (resolvedProfile == null) {
			System.exit(0);
			return false;
		}

		ActiveOperatorProfile.set(resolvedProfile);
		return true;
	}

	/**
	 * Returns the window title suffix naming the active operator profile.
	 *
	 * <p>Empty for the historic single profile installation, so nothing changes visually
	 * for operators who never create a second profile.</p>
	 *
	 * @return the suffix to append to a window title, never null
	 */
	private String buildOperatorProfileTitleSuffix() {

		OperatorProfileSelection activeProfile = ActiveOperatorProfile.get();

		if (activeProfile == null || activeProfile.getProfile().isRootProfile()) {
			return "";
		}

		return " - " + activeProfile.getProfile().getDisplayName();
	}

	/**
	 * Lets the operator pick another profile and rebuilds the runtime for it.
	 *
	 * <p>Offers to create a second profile when only one exists, because the menu entry
	 * is the discoverable place to find the feature at all.</p>
	 */
	private void showOperatorProfileSwitchDialog() {

		OperatorProfileStore profileStore = new OperatorProfileStore();
		List<OperatorProfile> selectableProfiles = profileStore.loadProfiles();

		if (selectableProfiles.size() < 2) {
			ComposeAlert.acknowledge(
					kst4contest.view.i18n.CurrentStrings.get().getAlertProfilesTitle(),
					kst4contest.view.i18n.CurrentStrings.get().getAlertOnlyOneProfile(),
					kst4contest.view.i18n.CurrentStrings.get().getAlertOnlyOneProfileBody(),
					chatcontroller.getChatPreferences().isGUI_darkModeActive());
			return;
		}

		OperatorProfileSelection activeProfile = ActiveOperatorProfile.get();
		String activeProfileId = activeProfile == null ? null : activeProfile.getProfile().getProfileId();

		Optional<OperatorProfile> chosenProfile =
				OperatorProfilePickerWindow.showAndSelect(selectableProfiles, activeProfileId);

		if (chosenProfile.isEmpty()) {
			return;
		}

		if (chosenProfile.get().getProfileId().equalsIgnoreCase(activeProfileId)) {
			return;
		}

		requestOperatorProfileSwitch(chosenProfile.get());
	}

	/**
	 * Confirms and performs a switch to another operator profile.
	 *
	 * <p>Shared by the File menu and the profile settings tab, so both ask the same
	 * question before giving up the running session.</p>
	 *
	 * @param targetProfile profile to activate
	 */
	private void requestOperatorProfileSwitch(OperatorProfile targetProfile) {

		if (targetProfile == null) {
			return;
		}

		/*
		 * Settings edits not applied yet are asked about first: applied, they land in the
		 * profile being left, which is the one they were made in; "back" cancels the switch.
		 * Without pending edits this continues at once, as before.
		 */
		SettingsWindow.settleThen(() -> {
			if (confirmOperatorProfileSwitch(targetProfile)) {
				ApplicationRuntimeLauncher.switchProfile(targetProfile);
			}
		});
	}

	/**
	 * Asks whether the running session may be given up for a profile switch.
	 *
	 * @param targetProfile profile the operator selected
	 * @return true if the switch may proceed
	 */
	private boolean confirmOperatorProfileSwitch(OperatorProfile targetProfile) {

		return ComposeAlert.confirm(
				kst4contest.view.i18n.CurrentStrings.get().getAlertSwitchProfileTitle(),
				kst4contest.view.i18n.CurrentStrings.get().alertSwitchProfileQuestion(targetProfile.getDisplayName()),
				kst4contest.view.i18n.CurrentStrings.get().getAlertSwitchProfileBody(),
				kst4contest.view.i18n.CurrentStrings.get().getDialogSwitchProfile(),
				kst4contest.view.i18n.CurrentStrings.get().getDialogCancel(),
				chatcontroller.getChatPreferences().isGUI_darkModeActive());
	}

	/**
	 * Releases every resource this runtime owns, without terminating the process.
	 *
	 * <p>Separated from {@link #stop()} so the same teardown can be reused when the
	 * operator switches to another profile and a fresh runtime is built afterwards.
	 * The method is idempotent and tolerates a runtime that never connected, because a
	 * switch may happen before the first login.</p>
	 */
	public void shutdownRuntime() {

		if (!runtimeShutdownDone.compareAndSet(false, true)) {
			return;
		}

		LOGGER.info("Application is shutting down and closing all resources");

		if (layoutAutosave != null) {
			// Flush before cancelling, otherwise a pending debounced write would either
			// be lost or land after a profile switch.
			layoutAutosave.flushPending();
			layoutAutosave.cancelPending();
		}

		cancelViewTimer(timer_buildWindowTitle);
		timer_buildWindowTitle = null;

//		timer_chatMemberTableSortTimer.purge();
//		timer_chatMemberTableSortTimer.cancel();

		cancelViewTimer(timer_updatePrivatemessageTable);
		timer_updatePrivatemessageTable = null;

		if (userListRefreshCoalescer != null) {
			userListRefreshCoalescer.cancel();
			userListRefreshCoalescer = null;
		}



		/*
		 * Every mirror holds a listener inside a core roster. Without releasing them
		 * a discarded runtime stays reachable across an operator profile switch.
		 */
		rosterBindings.forEach(RosterListBinding::dispose);
		rosterBindings.clear();
		chatMemberListBinding = null;
		selectedCallSignInfoMessageBinding = null;

		kst4contest.view.compose.map.StationMapWindow.INSTANCE.hide();
		TopPriorityCandidatesWindow.hide();

		closeOwnedStages();

		try {
			if (chatcontroller != null) {
				chatcontroller.disconnect(ApplicationConstants.DISCSTRING_DISCONNECT_AND_CLOSE);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception during disconnect", e);
		}
		chatcontroller.getMessageHistoryRecorder().close();
		closeFileLogging();
	}

	/**
	 * Cancels a timer created during user interface construction.
	 *
	 * @param timerToCancel timer to cancel, may be null when startup did not get that far
	 */
	private static void cancelViewTimer(Timer timerToCancel) {

		if (timerToCancel == null) {
			return;
		}

		timerToCancel.purge();
		timerToCancel.cancel();
	}


	/**
	 * Closes every window this runtime opened, so no stale window survives a profile
	 * switch. The map window is closed by its own dispose method.
	 */
	private void closeOwnedStages() {

		/*
		 * The settings window is a Compose window, not a Stage, so it has to be closed
		 * by name. It carries the Connect button, so a copy left over from the previous
		 * profile would block the new one from opening its own.
		 */
		SettingsWindow.close();
		UpdateWindow.close();
		MonitorWindow.close();
		releaseMonitorListeners();
		// Etappe 5c: Compose Fenster bleibt beim Profilwechsel offen
		// MainWindowHost.close();
		releaseComposeMainWindow();

	}

	/**
	 * Hands the window hosts the active profile's stored window positions. Set on every
	 * startup, so a profile switch replaces the previous profile's positions. The opt-in is
	 * read on each call, so applying the setting takes effect without a restart.
	 */
	private void installWindowPositionMemory(ChatPreferences prefs) {
		kst4contest.view.compose.WindowPlacementOnScreen.setMemory(
				new kst4contest.view.compose.PreferencesWindowPositionMemory(prefs, this::requestLayoutSave));
	}

	private void requestLayoutSave() {
		if (layoutAutosave != null) {
			layoutAutosave.requestSave();
		}
	}



	/**
	 * Builds the running application: profile, chat controller, listeners and windows.
	 *
	 * <p>Toolkit-free on purpose. An operator profile switch calls this on a fresh
	 * instance, and it must not need a JavaFX stage to do so.</p>
	 *
	 * @throws InterruptedException when startup is interrupted while waiting
	 * @throws IOException when a profile or resource file cannot be read
	 * @throws URISyntaxException when a configured address cannot be parsed
	 */
	public void startRuntime() throws InterruptedException, IOException, URISyntaxException {

		if (!resolveOperatorProfileIfRequired()) {
			return;
		}

		ApplicationRuntimeLauncher.setCurrent(this);

		ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, STYLE_DEFAULTCSSDAY_RESOURCE, STYLE_DEFAULTCSSDAY_FILE);
		ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, STYLE_DEFAULTCSSEVENING_RESOURCE, STYLE_DEFAULTCSSEVENING_FILE);
		ChatMember ownChatMemberObject = new ChatMember();
		OperatorProfileSelection activeOperatorProfile = ActiveOperatorProfile.get();

		// instantiate the Chatcontroller with the user object and the files of the active profile
		chatcontroller = new ChatController(
				ownChatMemberObject,
				this,
				activeOperatorProfile.getPreferencesRelativeFileName(),
				activeOperatorProfile.getWorkedDatabaseRelativeFileName(),
				activeOperatorProfile.isSeedWorkedDatabaseFromResource(),
				uiDispatcher);
		setDebugFileLoggingEnabled(chatcontroller.getChatPreferences()
				.isMessageHandling_debugModeToFileEnabled());
		layoutAutosave = new LayoutAutosave(
				chatcontroller.getChatPreferences()::writeLayoutPreferencesToXmlFile, uiDispatcher);
		installWindowPositionMemory(chatcontroller.getChatPreferences());
		// Every profile starts with its own configured design; the menu quick switch only lasts for the session.
		chatcontroller.getChatPreferences().setGUI_darkModeActive(
				chatcontroller.getChatPreferences().isGUI_darkModeActiveByDefault());
		/*
		 * The palette, before any window is built: every Compose window reads it, and a window
		 * built without it would draw the shipped colours until it was rebuilt.
		 */
		paletteStore = kst4contest.view.compose.PaletteStoreFactory.create(
				chatcontroller.getChatPreferences(), activeOperatorProfile.getProfile());

		languageStore = kst4contest.view.i18n.LanguageStoreFactory.create(
				chatcontroller.getChatPreferences(), java.util.Locale.getDefault());
		messageVariableResolver = new MessageVariableResolver(chatcontroller.getChatPreferences());
		chatcontroller.setStatusListener(this); //callback interface for updating Thread events in visual

		// 1. Timeline an die Sked-Liste binden
		chatcontroller.getActiveSkeds().addListener(skeds -> updateTimelineVisuals());

		// 1. bind table to the sked list
		chatcontroller.getScoreService().uiPulse().addListener(newVal -> {
			updateTimelineVisuals();
		});

		// Update the Compose timeline when the rotor direction changes. The azimuth is
		// read fresh from preferences inside updateTimelineVisuals, so no setter is kept.
		chatcontroller.getChatPreferences().getActualQTF().addListener(newV -> updateTimelineVisuals());

		/*
		 * Sked reminder: feeds the Compose notice strip. Lives here, after the dead
		 * scene construction, because that is the only part of start() still standing;
		 * it used to sit inside the never-shown JavaFX build next to the sked-warning
		 * button that part 3 removed.
		 */
		chatcontroller.lastUiReminderEvent().addListener(
				reminderEvent -> {
					if (reminderEvent == null) {
						return;
					}

					String text = "REMINDER: "
							+ reminderEvent.getCallSignRaw()
							+ "  T-"
							+ reminderEvent.getMinutesBefore()
							+ "m";

					uiDispatcher.runOnUi(
							() -> showBlinkingSkedWarnIndicator(text)
					);
				}
		);

		/**
		 * Window selected callsign information
		 * Works with a ChatMember variable, initialized by a selected-listener of the Chatmemberlist
		 */



		/**
		 * end Window selected callsign information
		 */

		/**
		 * Window Cluster & qso of the other
		 *
		 * Compose now; see openMonitorWindow and kst4contest.view.compose.MonitorWindow.
		 */
		openMonitorWindow();

		/*
		 * The Compose main window, beside this one, only on --compose-main-window. It is here
		 * rather than earlier because it reads the tables and the preferences this method has
		 * just finished building.
		 */
		openComposeMainWindowIfRequested();

		/**
		 * Window updates
		 *
		 * Compose now; see openUpdateWindowIfAvailable and
		 * kst4contest.view.compose.UpdateWindow.
		 */
		openUpdateWindowIfAvailable();

		/*****************************************************************************
		 *
		 * Settings window
		 *
		 * Compose now; see openSettingsWindow and kst4contest.view.compose. It is opened
		 * here because it carries the Connect button and is therefore the way into the
		 * chat, which is what the JavaFX window was shown for at startup too.
		 *
		 ****************************************************************************/
		openSettingsWindow();



		//initialize the timeline
		uiDispatcher.runOnUi(this::updateTimelineVisuals);

		/*
		 * Replaces Application.stop(), which only JavaFX ever called. This covers a SIGTERM
		 * and a closing terminal; the ordinary exit still goes through
		 * ApplicationRuntimeLauncher.exitApplication(), and shutdownRuntime is idempotent.
		 */
		Runtime.getRuntime().addShutdownHook(
				new Thread(this::shutdownRuntime, "kst4contest-shutdown"));
	}

	/**
	 * This is a helping class for providing information for the TimeLineView to give full information about the
	 * Chatmember objects on which the Sked object is referring to.
	 * @param sked
	 * @return
	 */
	private String buildSkedHoverInfo(ContestSked sked) {
		if (sked == null || sked.getTargetCallsign() == null) return "";

		String callRaw = sked.getTargetCallsign().trim().toUpperCase();

		ChatMember member = null;
		for (ChatMember cm : chatcontroller.getLst_chatMemberList().snapshot()) {
			if (cm == null || cm.getCallSignRaw() == null) continue;
			if (callRaw.equals(cm.getCallSignRaw().trim().toUpperCase())) {
				member = cm;
				break;
			}
		}
		if (member == null) return "";

		AirPlaneReflectionInfo ap = member.getAirPlaneReflectInfo();
		if (ap == null) return "";

		StringBuilder sb = new StringBuilder();
		sb.append("AP reachable: ").append(ap.getAirPlanesReachableCntr());

		if (ap.getRisingAirplanes() != null && !ap.getRisingAirplanes().isEmpty()) {
			AirPlane a0 = ap.getRisingAirplanes().get(0);
			sb.append("\nNext: ")
					.append(a0.getArrivingDurationMinutes())
					.append(" min (")
					.append(a0.getPotential())
					.append("%)");

			if (ap.getRisingAirplanes().size() > 1) {
				AirPlane a1 = ap.getRisingAirplanes().get(1);
				sb.append(" / ")
						.append(a1.getArrivingDurationMinutes())
						.append(" min (")
						.append(a1.getPotential())
						.append("%)");
			}
		}
		return sb.toString();
	}


	/**
	 * Formats the global worked/grid status for the worked-any column.
	 *
	 * <p>Status characters follow the user feedback:
	 * <ul>
	 *     <li>empty = call not worked, grid not worked</li>
	 *     <li>x = call worked</li>
	 *     <li>o = grid worked, call not worked</li>
	 *     <li>xo = call and grid worked</li>
	 * </ul>
	 *
	 * <p>The call status is based on {@link ChatMember#isWorked()}, i.e. worked-any.
	 * The grid status is based on the four-character grid square worked on any band.</p>
	 *
	 * @param member station row
	 * @return compact status text
	 */
	private String formatWorkedAnyGridStatus(ChatMember member) {
		if (member == null) {
			return "";
		}

		boolean callWorked = member.isWorked();
		boolean gridWorked = chatcontroller != null && chatcontroller.isGridSquareWorkedAny(member);

		if (callWorked && gridWorked) {
			return "xo";
		}

		if (callWorked) {
			return "x";
		}

		if (gridWorked) {
			return "o";
		}

		return "";
	}

	/**
	 * Builds a tooltip for the worked-any/grid-status cell.
	 *
	 * @param member station row
	 * @return tooltip text
	 */
	private String buildWorkedAnyGridStatusTooltip(ChatMember member) {
		if (member == null) {
			return "empty = call not worked, grid not worked\n"
					+ "x = call worked\n"
					+ "o = grid worked\n"
					+ "xo = call and grid worked";
		}

		String grossField = WorkedGrossFieldCache.extractGrossField(member.getQra());
		String gridText = grossField == null
				? kst4contest.view.i18n.CurrentStrings.get().getTooltipUnknownGrid()
				: kst4contest.view.i18n.CurrentStrings.get().tooltipGrid(grossField);

		return kst4contest.view.i18n.CurrentStrings.get().getTooltipWorkedStatusLegend()
				+ kst4contest.view.i18n.CurrentStrings.get().tooltipWorkedStatusThisStation(
						member.isWorked() ? kst4contest.view.i18n.CurrentStrings.get().getTooltipYes() : kst4contest.view.i18n.CurrentStrings.get().getTooltipNo(),
						chatcontroller != null && chatcontroller.isGridSquareWorkedAny(member)
								? kst4contest.view.i18n.CurrentStrings.get().getTooltipYes() : kst4contest.view.i18n.CurrentStrings.get().getTooltipNo(),
						gridText);
	}

//	/**
//	 *
//	 * resets the style of the not selected direction buttons
//	 *
//	 * REPLACED BY CSS USAGE
//	 * @deprecated
//	 * @param exceptThisButton
//	 * @return
//	 */
//	public boolean uiHelper_recolorQtfDirectionButtonsExceptThisOne(Button exceptThisButton) {
//
////		Button[] qtfButtons = new Button[8];
//
//		for (int i = 0; i < btnQtfButtonsAvl.length; i++) {
//
////			if (!btnQtfButtonsAvl[i].equals(exceptThisButton)) {
////				btnQtfButtonsAvl[i].setStyle("");
////			} else {
////				btnQtfButtonsAvl[i].setStyle("-fx-background-color:\n" +
////						"        linear-gradient(#f0ff35, #a9ff00),\n" +
////						"        radial-gradient(center 50% -40%, radius 200%, #b8ee36 45%, #80c800 50%);\n" +
////						"    -fx-background-radius: 6, 5;\n" +
////						"    -fx-background-insets: 0, 1;\n" +
////						"    -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.4) , 5, 0.0 , 0 , 1 );\n" +
////						"    -fx-text-fill: #395306;"); //Todo fancy button style
////			}
//
//		}
//
//		return true;
//
//	}


	/**
	 * Asks whether the chat connection may be given up, and quits when it may.
	 *
	 * <p>Blocking on purpose: the answer decides whether the application exits. The
	 * former {@code WindowEvent} parameter is gone with the JavaFX stage; both callers
	 * passed null and the body never read it.</p>
	 */
	private void closeWindowEvent() {
		System.out.println("Window close request ...");

		/*
		 * Settings edits not applied yet are asked about before the quit question; "back"
		 * cancels the exit. Without pending edits this continues at once, as before.
		 */
		SettingsWindow.settleThen(this::confirmAndExit);
	}

	/** Asks whether to quit, and quits on yes. */
	private void confirmAndExit() {
		/*
		 * ConfirmKind.YES and not the default: the JavaFX dialog used ButtonType.YES here,
		 * and JavaFX ordered YES ahead of CANCEL on Linux and Windows but behind it on
		 * macOS. The profile-switch confirmation used OK_DONE and orders the other way on
		 * Linux, so the two genuinely differ.
		 */
		boolean quit = ComposeAlert.confirm(
				kst4contest.view.i18n.CurrentStrings.get().getAlertQuitTitle(),
				null,
				kst4contest.view.i18n.CurrentStrings.get().getAlertQuitQuestion(),
				kst4contest.view.i18n.CurrentStrings.get().getDialogYes(),
				kst4contest.view.i18n.CurrentStrings.get().getDialogCancel(),
				chatcontroller.getChatPreferences().isGUI_darkModeActive(),
				ConfirmKind.YES);

		if (quit) {
			System.out.println("closewindowevent: exiting the application");

			// Routed through the launcher so the runtime that is actually live
			// releases its resources. After a profile switch that is no longer the
			// instance JavaFX would call stop() on.
			ApplicationRuntimeLauncher.exitApplication();
		}
	}

	/**
	 * Informs a user about a warning, shows given String in simple alertwindow
	 *
	 */
	public static void alertWindowEvent(String warning) {
		System.out.println("Alert due to ... " + warning);

		/*
		 * Static, and also called from ApplicationRuntimeLauncher after the runtime is
		 * already gone, so there are no preferences left to read a design from.
		 */
		ComposeAlert.show("WARNING", null, warning);
	}

//    public void updateStatusButtons() {
//        //TODO: Hier muss noch was hin
////        get
//    }


	public static void main(String[] args) {
		setupFileLogging();

		CommandLineOptions.remember(CommandLineOptions.parse(
				args == null ? null : java.util.List.of(args)));

		try {
			new Kst4ContestApplication().startRuntime();
		} catch (Exception startupProblem) {
			LOGGER.log(java.util.logging.Level.SEVERE, "Could not start KST4Contest", startupProblem);
			ComposeAlert.acknowledge(
					"KST4Contest",
					"KST4Contest could not be started.",
					String.valueOf(startupProblem.getMessage()));
			System.exit(1);
		}
	}


	private static void setupFileLogging() {
		if (fileLogging != null) {
			return;
		}
		try {
			Path logFile = Path.of(ApplicationFileUtils.getDebugFilePath(
					ApplicationConstants.APPLICATION_NAME,
					"kst4contest-errors.log"));
			// Start conservatively until the persisted preference has been loaded.
			fileLogging = new ApplicationFileLogging(logFile, false);
		} catch (IOException e) {
			System.err.println("Could not set up file logging: " + e.getMessage());
		}
	}

	private static void setDebugFileLoggingEnabled(boolean enabled) {
		if (fileLogging != null) {
			fileLogging.setDebugEnabled(enabled);
		}
	}

	private static void closeFileLogging() {
		if (fileLogging != null) {
			fileLogging.close();
			fileLogging = null;
		}
	}

    @Override
	public void onThreadStatusChanged(
			String key,
		    ThreadStateMessage threadStateMessage
	) {
		if (threadStateMessage == null) {
			LOGGER.log(Level.WARNING,
					"Ignoring empty thread-status update from source {0}",
					key == null ? "UNKNOWN" : key);
			return;
		}
		if (key == null || key.isBlank()) {
			LOGGER.log(Level.WARNING,
					"Ignoring thread-status update without a source key. Detail: {0}",
					threadStateMessage.getRunningInformation());
			return;
		}

		if ("ON4KST".equalsIgnoreCase(key)) {
			String detail = threadStateMessage.getRunningInformation();
			uiDispatcher.runOnUi(() -> updateConnectionStateIndicator(
					chatcontroller.getOn4KstConnectionState(), detail));
			return;
		}

		uiDispatcher.runOnUi(() -> updateStatusButton(key, threadStateMessage));
		maybeShowBandUpgradeIndicator(key, threadStateMessage);
	}


	public void onConnectionStateChanged(
			On4KstConnectionState state,
			String detail
	) {
		On4KstConnectionState effectiveState = state;
		if (effectiveState == null) {
			LOGGER.log(Level.WARNING,
					"Received ON4KST connection callback without a state; "
							+ "treating it as disconnected. Detail: {0}",
					detail);
			effectiveState = On4KstConnectionState.DISCONNECTED;
		}

		On4KstConnectionState stateToDisplay = effectiveState;
		uiDispatcher.runOnUi(() -> updateConnectionStateIndicator(stateToDisplay, detail));
	}

	@Override
	public void onSimpleLogFileCreated(Path filePath) {
		ComposeAlert.showWithLink(
				kst4contest.view.i18n.CurrentStrings.get().getAlertSimpleLogTitle(),
				kst4contest.view.i18n.CurrentStrings.get().getAlertSimpleLogHeader(),
				kst4contest.view.i18n.CurrentStrings.get().alertSimpleLogBody(filePath),
				kst4contest.view.i18n.CurrentStrings.get().getAlertSimpleLogLink(),
				SIMPLE_LOG_MANUAL_URL,
				chatcontroller.getChatPreferences().isGUI_darkModeActive());
	}


	/**
	 * Forces the station FilteredList to evaluate all active predicates again.
	 *
	 * <p>Several filters depend on mutable ChatMember fields, for example worked
	 * flags, AirScout windows or calculated Tropo values. Updating such fields does
	 * not necessarily create a JavaFX list-change event. Therefore we explicitly
	 * re-apply the combined predicate instead of adding/removing a dummy predicate.
	 * The dummy-predicate trick can corrupt SortedList mapping in JavaFX.</p>
	 *
	 * <p>applyChatMemberFilterPredicates() itself guards against the resulting
	 * spurious TableView selection events, so no extra guard is needed here.</p>
	 */
	private void forceChatMemberFilterRefresh() {
		updateComposeFilters();
		applyChatMemberFilterPredicates();
	}

	private void updateComposeFilters() {
		if (composeMainWindowState == null) return;
		kst4contest.view.compose.StationFilterState state = composeMainWindowState.getStationFilter();

		String text = state.getSearchText().trim();
		boolean hasText = !text.isEmpty();

		java.util.function.Predicate<ChatMember> unifiedPredicate = member -> {
			if (hasText) {
				String cs = member.getCallSign();
				if (cs == null || !cs.toUpperCase(java.util.Locale.ROOT).contains(text.toUpperCase(java.util.Locale.ROOT))) {
					return false;
				}
			}

			if (state.isOn(kst4contest.view.compose.StationFilter.ONLY_NEW_GRIDS) && !chatcontroller.isNewGridSquare(member)) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.TROPO_REACHABLE) && !isReachableViaTropoFilterMatch(member)) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.NEW_BANDS) && !isNewBandOpportunity(member)) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.AIRSCOUT_NEXT_5_MIN) && !hasAsWindowInNextMinutes(member, 5)) return false;

			if (!state.acceptsQrb(member.getQrb())) return false;

			if (state.isOn(kst4contest.view.compose.StationFilter.QTF)) {
				Double qtf = member.getQTFdirection();
				if (qtf == null || !kst4contest.locatorUtils.DirectionUtils.isAngleInRange(qtf, state.getQtfDegrees(), chatcontroller.getChatPreferences().getStn_antennaBeamWidthDeg())) return false;
			}

			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_INACTIVE)) {
				long inactiveMinutes = kst4contest.controller.Utils4KST.time_getSecondsBetweenEpochAndNow(member.getActivityTimeLastInEpoch() + "") / 60;
				if (inactiveMinutes > 20L) return false;
			}

			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_ANY) && member.isWorked()) return false;

			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_50) && (member.isWorked50() || !member.isQrv50())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_70) && (member.isWorked70() || !member.isQrv70())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_144) && (member.isWorked144() || !member.isQrv144())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_432) && (member.isWorked432() || !member.isQrv432())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_23) && (member.isWorked1240() || !member.isQrv1240())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_13) && (member.isWorked2300() || !member.isQrv2300())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_9) && (member.isWorked3400() || !member.isQrv3400())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_6) && (member.isWorked5600() || !member.isQrv5600())) return false;
			if (state.isOn(kst4contest.view.compose.StationFilter.HIDE_WORKED_3) && (member.isWorked10G() || !member.isQrv10G())) return false;

			return true;
		};

		composeMainWindowState.getStations().setRowFilter(unifiedPredicate::test);

		chatcontroller.clearChatMemberListFilterPredicates();
		chatcontroller.addChatMemberListFilterPredicate(unifiedPredicate);
	}

	@Override
	public void onUserListUpdated() {
		scheduleUserListRefresh(null);
	}

	@Override
	public void onUserListUpdated(final String reason) {
		scheduleUserListRefresh(reason);
	}

	private void scheduleUserListRefresh(final String diagnosticReason) {
		uiDispatcher.runOnUi(() -> {
			if (diagnosticReason != null && !diagnosticReason.isBlank()) {
				pendingUserListUpdateReason = diagnosticReason;
			}

			if (userListRefreshCoalescer == null) {
				userListRefreshCoalescer = new CoalescingTrigger(
						USER_LIST_REFRESH_DELAY_MS,
						() -> {
							final String completedDiagnosticReason = pendingUserListUpdateReason;
							pendingUserListUpdateReason = "";

							forceChatMemberFilterRefresh();

							if (composeMainWindowState != null) {
								composeMainWindowState.getStations().forceRedraw();
							}

							refreshStationMapIfVisible();

							if (!completedDiagnosticReason.isEmpty()) {
								System.out.println("KST4Capp, UI Update Trigger: "
										+ completedDiagnosticReason);
							}
						});
			}

			userListRefreshCoalescer.trigger();
		});
	}


//	public class MaidenheadLocatorMapPane extends Pane {
//
//		private static final double MAP_WIDTH = 800;
//		private static final double MAP_HEIGHT = 600;
//		private static final double CIRCLE_RADIUS = 5;
//		private static final double TEXT_OFFSET_X = 10;
//		private static final double TEXT_OFFSET_Y = -10;
//
//		public MaidenheadLocatorMapPane() {
//			setPrefSize(MAP_WIDTH, MAP_HEIGHT);
//		}
//
//		public void addLocator(String locator, Color color) {
//			double[] coords = locatorToCoordinates(locator);
//			Circle circle = new Circle(coords[0], coords[1], CIRCLE_RADIUS, color);
//			Text text = new Text(coords[0] + TEXT_OFFSET_X, coords[1] + TEXT_OFFSET_Y, locator);
//			getChildren().addAll(circle, text);
//		}
//
//		public void connectLocators(String locator1, String locator2) {
//			double[] coords1 = locatorToCoordinates(locator1);
//			double[] coords2 = locatorToCoordinates(locator2);
//			Line line = new Line(coords1[0], coords1[1], coords2[0], coords2[1]);
//			getChildren().add(line);
//
//			// Calculate distance between locators
//			double distance = calculateDistance(coords1, coords2);
//
//			// Calculate direction in degrees from locator1 to locator2
//			double direction = calculateDirection(coords1, coords2);
//
//			// Format distance to display only two decimal places
//			DecimalFormat df = new DecimalFormat("#.##");
//
//			// Create text for displaying distance and direction
//			Text distanceText = new Text((coords1[0] + coords2[0]) / 2, (coords1[1] + coords2[1]) / 2, "Distance: " + df.format(distance) + " km");
//			Text directionText = new Text((coords1[0] + coords2[0]) / 2, (coords1[1] + coords2[1]) / 2 + 20, "Direction: " + df.format(direction) + "°");
//			getChildren().addAll(distanceText, directionText);
//		}
//
//		// Helper method to convert Maidenhead locator string to coordinates
//		private double[] locatorToCoordinates(String locator) {
//			double lon = (locator.charAt(0) - 'A') * 20 - 180;
//			double lat = (locator.charAt(1) - 'A') * 10 - 90;
//			lon += (locator.charAt(2) - '0') * 2;
//			lat += (locator.charAt(3) - '0');
//			lon += (locator.charAt(4) - 'A') * 5.0 / 60;
//			lat += (locator.charAt(5) - 'A') * 2.5 / 60;
//
//			// Convert coordinates to map coordinates
//			double x = (lon + 180) / 360 * MAP_WIDTH;
//			double y = MAP_HEIGHT - (lat + 90) / 180 * MAP_HEIGHT;
//			return new double[]{x, y};
//		}
//
//		// Helper method to calculate distance between two coordinates (in km)
//		private double calculateDistance(double[] coords1, double[] coords2) {
//			double lon1 = Math.toRadians(coords1[0]);
//			double lat1 = Math.toRadians(coords1[1]);
//			double lon2 = Math.toRadians(coords2[0]);
//			double lat2 = Math.toRadians(coords2[1]);
//
//			double dlon = lon2 - lon1;
//			double dlat = lat2 - lat1;
//
//			double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
//			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//
//			// Earth radius in km
//			double radius = 6371;
//
//			return radius * c;
//		}
//
//		// Helper method to calculate direction in degrees from coords1 to coords2
//		private double calculateDirection(double[] coords1, double[] coords2) {
//			double lon1 = Math.toRadians(coords1[0]);
//			double lat1 = Math.toRadians(coords1[1]);
//			double lon2 = Math.toRadians(coords2[0]);
//			double lat2 = Math.toRadians(coords2[1]);
//
//			double dLon = lon2 - lon1;
//
//			double y = Math.sin(dLon) * Math.cos(lat2);
//			double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
//
//			double direction = Math.atan2(y, x);
//			direction = Math.toDegrees(direction);
//			direction = (direction + 360) % 360;
//
//			return direction;
//		}
//	}


	/**
	 * Returns the best currently selected ChatMember for actions that depend on a
	 * selected station.
	 *
	 * <p>The normal source is selectedCallSignInfoStageChatMember. As a fallback,
	 * the current table selection and the ScoreService selection are checked. If
	 * nothing is selected yet, null is returned. This is valid and must be handled
	 * by the caller.</p>
	 *
	 * @return selected ChatMember or null if no station is selected
	 */
	private ChatMember getEffectiveSelectedChatMember() {
		if (selectedCallSignInfoStageChatMember != null) {
			return selectedCallSignInfoStageChatMember;
		}

		/*
		 * The JavaFX station table used to sit between these two. It never received rows
		 * after the window was disconnected, so this branch could not fire.
		 */
		if (chatcontroller != null
				&& chatcontroller.getScoreService() != null
				&& chatcontroller.getScoreService().getSelectedChatMember() != null) {
			return chatcontroller.getScoreService().getSelectedChatMember();
		}

		return null;
	}




	/**
	 * Checks whether an outgoing private message targets the local callsign.
	 *
	 * @param messageText outgoing message text
	 * @return {@code true} if the /cq target is the local station
	 */
	private boolean isMessageAddressedToOwnCallsign(String messageText) {
		String targetCallsign = extractCqTargetCallsign(messageText);

		if (targetCallsign == null
				|| chatcontroller == null
				|| chatcontroller.getChatPreferences() == null) {
			return false;
		}

		String ownCallsign = chatcontroller.getChatPreferences().getStn_loginCallSign();
		return ownCallsign != null && targetCallsign.equalsIgnoreCase(ownCallsign.trim());
	}



	/**
	 * Resolves the chat category for an outgoing operator message.
	 *
	 * <p>If a station is selected, the message is sent in the category of that
	 * station. If no station is selected yet, the message is sent in the main chat
	 * category. This prevents NullPointerExceptions when the operator writes to the
	 * chat immediately after joining.</p>
	 *
	 * @param selectedMember selected station, may be null
	 * @return category for the outgoing message, normally main or second category
	 */
	private ChatCategory resolveOutgoingChatCategory(ChatMember selectedMember) {
		ChatCategory mainCategory = chatcontroller.getChatCategoryMain();
		ChatCategory secondCategory = chatcontroller.getChatCategorySecondChat();

		if (selectedMember == null || selectedMember.getChatCategory() == null) {
			return mainCategory;
		}

		String categoryNumber = selectedMember.getChatCategory().getCategoryNumber() + "";

		if (mainCategory != null
				&& categoryNumber.equals(mainCategory.getCategoryNumber() + "")) {
			return mainCategory;
		}

		if (secondCategory != null
				&& categoryNumber.equals(secondCategory.getCategoryNumber() + "")) {
			return secondCategory;
		}

		return mainCategory;
	}


	/**
	 * helper method, in cases of QRG ending qith 0 as double values, it will fill the ending 0 to the qrg string
	 * @param raw
	 * @return
	 */
	private static String formatQrgForUi(String raw) {

		if (raw == null) return "";
		String s = raw.trim();
		if (s.isEmpty()) return "";

		// Einheitlich Punkt als Dezimaltrenner
		s = s.replace(',', '.');

		// Wenn es nicht numerisch ist: einfach anzeigen wie es ist
		try {
			Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return s;
		}

		int dot = s.indexOf('.');
		if (dot < 0) {
			// keine Nachkommastellen -> .000 ergänzen
			return s + ".000";
		}

		String dec = s.substring(dot + 1);
		if (dec.length() >= 3) {
			// schon >= 3 Nachkommastellen -> unverändert lassen
			return s;
		}

		StringBuilder sb = new StringBuilder(s);
		while (dec.length() < 3) {
			sb.append('0');
			dec += "0";
		}
		return sb.toString();
	}




	/*************************************************************************************
	 * Settings window (Compose)
	 *
	 * The window, its eleven tabs and the rules of every field live in Kotlin under
	 * kst4contest.view.compose. What stays here is the work only this class can do:
	 * rebuilding JavaFX controls, and starting or dropping the ON4KST session.
	 ************************************************************************************/

	/**
	 * There is no configurable base font size; the JavaFX settings window used the
	 * platform default. This is the same value the operator profile picker uses, so both
	 * Compose windows read like the JavaFX ones next to them.
	 */
	private static final float SETTINGS_WINDOW_FONT_SIZE_SP = 12f;

	private final SettingsNotices settingsNotices = new SettingsNotices();

	/**
	 * The monitor window's roster subscriptions. Held so shutdownRuntime can release
	 * them: a discarded runtime that still feeds a closed window keeps itself alive.
	 */
	private java.util.function.Consumer<java.util.List<ClusterMessage>> monitorClusterListener;

	private java.util.function.Consumer<java.util.List<ChatMessage>> monitorChatListener;

	/** Releases the monitor window's roster subscriptions. */
	private void releaseMonitorListeners() {

		if (monitorClusterListener != null) {
			chatcontroller.getLst_clusterMemberList().removeListener(monitorClusterListener);
			monitorClusterListener = null;
		}

		if (monitorChatListener != null) {
			chatcontroller.getLst_globalChatMessageList().removeListener(monitorChatListener);
			monitorChatListener = null;
		}
	}

	/*****************************************************
	 * The Compose main window, opened beside the JavaFX one
	 ****************************************************/

	/** Held so the feeds can be released; a discarded runtime that still feeds a window lives on. */
	private MainWindowState composeMainWindowState;

	/**
	 * The palette of the active profile, handed to every Compose window.
	 *
	 * Built once per runtime start, which is also once per profile: a profile switch tears
	 * the runtime down and starts it again on a fresh application object, so there is no
	 * separate rebuild to do here.
	 */
	private kst4contest.view.compose.PaletteStore paletteStore;

	/**
	 * The interface language of the active profile, handed to every Compose window.
	 *
	 * Built once per runtime start, which is also once per profile: a profile switch tears the
	 * runtime down and starts it again on a fresh application object.
	 */
	private kst4contest.view.i18n.LanguageStore languageStore;
	private java.util.function.Consumer<java.util.List<ChatMember>> composeMemberListener;
	private java.util.function.Consumer<java.util.List<ChatMessage>> composeChatListener;
	private java.util.function.Consumer<java.util.List<ClusterMessage>> composeClusterListener;

	/**
	 * Opens the Compose main window beside the JavaFX one.
	 *
	 * <p>Only on --compose-main-window, and never a second time. The two run side by side so
	 * the operator can compare them against a live session, which is the only way the
	 * differences of the last three Etappen came to light.</p>
	 */
	private void openComposeMainWindowIfRequested() {

		// Etappe 5c: Compose Hauptfenster wird immer gestartet
		// if (!CommandLineOptions.remembered().isComposeMainWindowRequested()) {
		// 	return;
		// }

		// if (MainWindowHost.isOpen()) {
		// 	return;
		// }

		ColumnWidthStore widths = new ColumnWidthStore() {
			@Override
			public Double width(String tableId, String columnId) {
				java.util.OptionalDouble stored =
						chatcontroller.getChatPreferences().getTableColumnWidth(tableId, columnId);
				return stored.isPresent() ? stored.getAsDouble() : null;
			}

			@Override
			public void setWidth(String tableId, String columnId, double width) {
				chatcontroller.getChatPreferences().setTableColumnWidth(tableId, columnId, width);
			}

			@Override
			public void requestSave() {
				requestLayoutSave();
			}
		};

		/*
		 * Own table ids, deliberately. The Compose window's columns are the operator's to
		 * arrange separately while both windows are on screen; sharing the ids would let a
		 * drag in one silently rearrange the other.
		 */
		EnumSet<Band> enabledStationBands =
				BandOpportunityResolver.getEnabledStationBands(chatcontroller.getChatPreferences());
		ReachabilityState reachability = new ReachabilityState(
				enabledStationBands,
				member -> chatcontroller.getReachabilityService().resolveAutoBand(member),
				() -> composeMainWindowState == null
						? null : composeMainWindowState.getSelectedStation().getSelected(),
				(member, band) -> {
					chatcontroller.getReachabilityService()
							.calculateSelectedStationOnDemand(member, band);
					return kotlin.Unit.INSTANCE;
				},
				() -> {
					forceChatMemberFilterRefresh();
					if (composeMainWindowState != null) {
						composeMainWindowState.getStations().forceRedraw();
					}
					return kotlin.Unit.INSTANCE;
				});
		StationSelectionState stationSelection = new StationSelectionState();

		DataTableState<ChatMember> stations = new DataTableState<>(
				StationColumns.INSTANCE.all(
						member -> {
							Band band = reachability.resolveBand(member);
							chatcontroller.getReachabilityService()
									.ensureTropoMarginCalculated(member, band);
							return member.formatTropoSsbMarginForBand(band);
						},
						member -> parseTableDouble(formatPriorityScore(member)),
						(member, band, worked) -> formatBandCellStatus(member, band, worked),
						enabledStationBands),
				RowKeys.INSTANCE.byValue(StationColumns.INSTANCE::rowKey),
				"compose-stations",
				widths,
				"callsign",
				true,
				stationSelection,
				true);

		DataTableState<ChatMessage> directed = new DataTableState<>(
				DirectedMessageColumns.INSTANCE.all(
						() -> chatcontroller.getChatPreferences().getStn_loginCallSign(),
						chatcontroller::formatChatMessageTextForDisplay),
				RowKeys.INSTANCE.byReference(),
				"compose-directed-messages",
				widths);

		DataTableState<ChatMessage> publicMessages = new DataTableState<>(
				PublicMessageColumns.INSTANCE.all(),
				RowKeys.INSTANCE.byReference(),
				"compose-public-messages",
				widths);

		DataTableState<ClusterMessage> cluster = new DataTableState<>(
				MonitorColumns.INSTANCE.dxCluster(),
				RowKeys.INSTANCE.byReference(),
				"compose-dx-cluster",
				widths);

		DataTableState<ChatMessage> qsoOfTheOther = new DataTableState<>(
				MonitorColumns.INSTANCE.qsoOfTheOther(),
				RowKeys.INSTANCE.byReference(),
				"compose-qso-other",
				widths);

		DataTableState<ChatMessage> selectedStationMessages = new DataTableState<>(
				SelectedStationMessageColumns.INSTANCE.all(),
				RowKeys.INSTANCE.byReference(),
				"compose-selected-station-messages",
				widths);

		MainMenuState menu = new MainMenuState();
		MainWindowSurroundings surroundings = new MainWindowSurroundings(menu);
		surroundings.setSo2rEnabled(chatcontroller.getChatPreferences().isTrxSynch_so2rEnabled());
		surroundings.setSecondAntennaOffered(chatcontroller.getChatPreferences().isTrxSynch_so2rEnabled()
				&& chatcontroller.getChatPreferences().isStn_pstRotator2Enabled());

		SelectedStationState selectedStation = new SelectedStationState(
				member -> {
					chatcontroller.propagateNotQrvStateToActiveMembers(member);
					return kotlin.Unit.INSTANCE;
				},
				this::createSkedFromCompose,
				member -> kotlin.Unit.INSTANCE,
				member -> kotlin.Unit.INSTANCE,
				filter -> {
					if (filter == null) {
						return kotlin.Unit.INSTANCE;
					}
					applySelectedStationMessageFilter(filter, selectedCallSignInfoStageChatMember);
					return kotlin.Unit.INSTANCE;
				},
				member -> {
					if (member == null || member.getCallSignRaw() == null) {
						return "";
					}
					return formatDetectedRxBandsForCallsignRaw(member.getCallSignRaw(), 30L * 60L * 1000L);
				},
				member -> {
					if (member == null) return null;
					java.util.EnumSet<Band> enabledBands = kst4contest.logic.BandOpportunityResolver.getEnabledStationBands(chatcontroller.getChatPreferences());
					return resolveDefaultSkedBand(member, enabledBands);
				},
				azimuth -> {
					chatcontroller.rotateTo(kst4contest.controller.RadioSlot.FIRST, azimuth);
					return kotlin.Unit.INSTANCE;
				},
				azimuth -> {
					chatcontroller.rotateTo(kst4contest.controller.RadioSlot.SECOND, azimuth);
					return kotlin.Unit.INSTANCE;
				},
				surroundings::getSecondAntennaOffered,
				stationSelection);

		selectedStation.setMessageFilter(
				SelectedMessageFilter.fromPreferences(chatcontroller.getChatPreferences()));

		TopPriorityState topPriority = new TopPriorityState(
				this::composeTopStations,
				member -> selectStationInCompose(selectedStation, member, StationSelectionSource.PRIORITY));

		ChatInputState chatInput = new ChatInputState(new ComposeChatInputActions(this));

		composeMainWindowState = new MainWindowState(
				enabledStationBands,
				chatcontroller.getChatPreferences(),
				stations,
				new StationFilterState(),
				directed,
				publicMessages,
				cluster,
				qsoOfTheOther,
				selectedStationMessages,
				selectedStation,
				topPriority,
				new TimelineState(),
				chatInput,
				menu,
				surroundings,
				new BlinkingNotice(),
				new BlinkingNotice(),
				new ThreadStatusButtons(),
				new SplitterState(
						2,
						chatcontroller.getChatPreferences().getGUImainWindowLeftSplitPane_dividerposition(),
						SplitterState.DEFAULT_MIN_PANE_PX,
						positions -> {
							chatcontroller.getChatPreferences()
									.setGUImainWindowLeftSplitPane_dividerposition(positions);
							requestLayoutSave();
							return kotlin.Unit.INSTANCE;
						}),
				new SplitterState(
						MainWindowState.MESSAGE_PANE_COUNT,
						chatcontroller.getChatPreferences().getGUImessageSectionSplitpane_dividerposition(),
						SplitterState.DEFAULT_MIN_PANE_PX,
						positions -> {
							chatcontroller.getChatPreferences()
									.setGUImessageSectionSplitpane_dividerposition(positions);
							requestLayoutSave();
							return kotlin.Unit.INSTANCE;
						}),
				new SplitterState(
						MainWindowState.RIGHT_PANE_COUNT,
						chatcontroller.getChatPreferences().getGUImainWindowRightSplitPane_dividerposition(),
						SplitterState.DEFAULT_MIN_PANE_PX,
						positions -> {
							chatcontroller.getChatPreferences()
									.setGUImainWindowRightSplitPane_dividerposition(positions);
							requestLayoutSave();
							return kotlin.Unit.INSTANCE;
						}),
				reachability);

		/*
		 * The feed, not the window construction, owns the timeline's data from here.
		 * Created beside the state it fills so its lifetime is that state's lifetime
		 * and not that of a JavaFX node nobody shows.
		 */
		timelineFeed = new kst4contest.view.feed.TimelineFeed(
				composeMainWindowState.getTimeline(), uiDispatcher);
		selectedStationMessagesFeed = new kst4contest.view.feed.SelectedStationMessagesFeed(
				composeMainWindowState.getSelectedStationMessages(), uiDispatcher);
		/*
		 * Build the live message mirror before the first Compose selection. The
		 * selection path already records the selected member before its legacy focus
		 * callback runs, so waiting for that callback leaves the mirror null and the
		 * table cannot receive either existing or newly arriving messages.
		 */
		rebuildSelectedCallSignInfoMessageBinding();
		selectedStationMessagesFeed.push(selectedCallSignInfoMessageBinding.list());
		connectionStateFeed = new kst4contest.view.feed.ConnectionStateFeed(
				composeMainWindowState.getSurroundings(), uiDispatcher);

		menu.setConnectLabel(buildComposeConnectLabel());

		/*
		 * Subscribed first and seeded afterwards, so a message arriving in between is not
		 * missed — the order RosterListBinding uses for the same reason.
		 */
		composeMemberListener = rows -> uiDispatcher.runOnUi(() -> stations.replaceRows(rows));
		chatcontroller.getLst_chatMemberList().addListener(composeMemberListener);

		composeMainWindowState.setStationSelector((member, source) -> {
			selectStationInCompose(selectedStation, member, source);
			return kotlin.Unit.INSTANCE;
		});
		composeMainWindowState.setStationFiltersChanged(() -> {
			updateComposeFilters();
			return kotlin.Unit.INSTANCE;
		});
		composeMainWindowState.setLayoutSaveRequested(() -> {
			requestLayoutSave();
			return kotlin.Unit.INSTANCE;
		});

		composeChatListener = rows -> uiDispatcher.runOnUi(() -> {
			directed.replaceRows(chatcontroller.toMeMessages());
			publicMessages.replaceRows(rows);
			qsoOfTheOther.replaceRows(chatcontroller.toOtherMessages());
		});
		chatcontroller.getLst_globalChatMessageList().addListener(composeChatListener);

		composeClusterListener = rows -> uiDispatcher.runOnUi(() -> cluster.replaceRows(rows));
		chatcontroller.getLst_clusterMemberList().addListener(composeClusterListener);

		if (chatcontroller.getScoreService() != null) {
			chatcontroller.getScoreService().topCandidates().addListener(candidates -> uiDispatcher.runOnUi(() -> topPriority.refresh()));
		}
		uiDispatcher.runOnUi(() -> topPriority.refresh());

		stations.replaceRows(chatcontroller.getLst_chatMemberList().snapshot());
		directed.replaceRows(chatcontroller.toMeMessages());
		publicMessages.replaceRows(chatcontroller.getLst_globalChatMessageList().snapshot());
		cluster.replaceRows(chatcontroller.getLst_clusterMemberList().snapshot());
		qsoOfTheOther.replaceRows(chatcontroller.toOtherMessages());
		/*
		 * The only push that does not check the feed for null, and deliberately so: the
		 * feed is assigned earlier in this same method, so a guard here could never fire
		 * and would read as a seam that does not exist. The other four sites are called
		 * from listeners that can run before this method does, which is why they check.
		 * If this block is ever moved above the assignment, the guard belongs back.
		 */
		if (selectedCallSignInfoMessageBinding != null) {
			selectedStationMessagesFeed.push(selectedCallSignInfoMessageBinding.list());
		}

		double[] storedSize = chatcontroller.getChatPreferences().getGUIscn_ChatwindowMainSceneSizeHW();
		MainWindowSize size = MainWindowFrame.INSTANCE.startupSize(
				storedSize, ScreenBounds.availableHeight(), ScreenBounds.availableWidth());

		MainWindowHost.show(
				composeMainWindowState,
				new ComposeMenuActions(this),
				buildOperatorProfileTitleSuffixForCompose(),
				chatcontroller.getChatPreferences().isGUI_darkModeActive(),
				SETTINGS_WINDOW_FONT_SIZE_SP,
				paletteStore,
				languageStore,
				(float) size.getWidthDp(),
				(float) size.getHeightDp(),
				(width, height) -> {
					/*
					 * Written to the profile now. It was deliberately dropped while the
					 * JavaFX window still existed, because two windows writing one size
					 * would have left the operator's arrangement to whichever was resized
					 * last. There is only this window left, so the size it is given back on
					 * the next start is the size the operator chose.
					 *
					 * Height first, then width: that is the order ChatPreferences stores
					 * and getScreenAwareMainSceneSize read.
					 */
					chatcontroller.getChatPreferences().setGUIscn_ChatwindowMainSceneSizeHW(
							new double[] { height, width });
					requestLayoutSave();
					return kotlin.Unit.INSTANCE;
				},
				/*
				 * Closing the main window quits the application, asking first — the same
				 * question the File menu's Exit asks. Without this the window would merely
				 * disappear and leave the client running with the chat connected.
				 */
				() -> {
					closeWindowEvent();
					return kotlin.Unit.INSTANCE;
				},
				address -> runOnUi(() -> ExternalDocuments.open(address)),
				() -> {
					runOnUi(() -> showSelectedCallsignOnMap());
					return kotlin.Unit.INSTANCE;
				},
				() -> {
					if (selectedCallSignInfoStageChatMember != null) {
						chatcontroller.airScout_SendAsShowPathPacket(selectedCallSignInfoStageChatMember);
					}
					return kotlin.Unit.INSTANCE;
				},
				member -> selectStationInCompose(selectedStation, member, StationSelectionSource.STATION_TABLE),
				message -> onDirectedMessageClickedInCompose(selectedStation, composeMainWindowState.getChatInput(), message),
				(message, source) -> onMessageSenderClickedInCompose(selectedStation, message, source),
				this::isOwnChatMessage,
				this::chatMessageAgeSeconds,
				() -> {
					runOnUi(() -> {
						updateComposeFilters();
					});
					return kotlin.Unit.INSTANCE;
				},
				() -> {
					runOnUi(() -> showTopPriorityCandidatesWindow());
					return kotlin.Unit.INSTANCE;
				},
				candidate -> {
					if (candidate == null) return kotlin.Unit.INSTANCE;
					ChatMember resolved = resolveChatMemberForCallRawAndCategory(
							candidate.getCallSignRaw(), candidate.getPreferredChatCategory());
					if (resolved != null) {
						selectStationInCompose(selectedStation, resolved, StationSelectionSource.TIMELINE);
						composeMainWindowState.getChatInput().prepareCq(resolved.getCallSign(), false);
						
						// Sync with JavaFX state just in case
						prepareCqTextForSelectedChatMember(resolved, false);
						focusChatMemberAndPrepareCq(resolved, false);
					}
					return kotlin.Unit.INSTANCE;
				});

		updateComposeSurroundings();
	}

	/**
	 * Arranges a sked from the Compose panel, exactly as the JavaFX sked button does.
	 *
	 * @return whether it was arranged; a station without a bearing still gets one, at 0°, as
	 *         the original did
	 */
	private boolean createSkedFromCompose(
			final ChatMember selectedMember,
			final int minutes,
			final Band band,
			final String mode) {

		if (selectedMember == null || band == null) {
			return false;
		}

		long skedTime = System.currentTimeMillis() + minutes * 60_000L;
		double azimuth =
				selectedMember.getQTFdirection() != null ? selectedMember.getQTFdirection() : 0.0;

		ContestSked sked = new ContestSked(
				selectedMember.getCallSignRaw(),
				selectedMember.getCallSign(),
				selectedMember.getChatCategory(),
				azimuth,
				skedTime,
				band);

		chatcontroller.addSked(sked);
		chatcontroller.getScoreService().requestRecompute("sked-created");

		// Arm PM reminders, exactly as the JavaFX Create sked handler does
		if (composeMainWindowState != null) {
			SelectedStationState ssState = composeMainWindowState.getSelectedStation();
			if (ssState.getRemindPm()) {
				List<Integer> offsets = parseMinuteOffsets(ssState.getReminderOffsets());
				chatcontroller.getSkedReminderService().armReminders(
						sked.getTargetChatCallsign(),
						sked.getTargetChatCategory(),
						skedTime,
						offsets
				);
			}
		}

		return true;
	}

	/** The Connect item's label, built exactly as initMenuBar builds it. */
	private String buildComposeConnectLabel() {
		/*
		 * Through the volatile bridge rather than a composition local: this runs on whatever
		 * thread rebuilt the menu state, not inside a composition. The menu picks the new
		 * label up the next time the state is rebuilt.
		 */
		kst4contest.view.i18n.Strings strings = kst4contest.view.i18n.CurrentStrings.get();

		ChatCategory mainCat = chatcontroller.getChatPreferences().getLoginChatCategoryMain();
		if (mainCat == null) {
			return strings.getConnectionConnect();
		}

		String label = strings.connectionConnectTo(
				mainCat.getChatCategoryName(mainCat.getCategoryNumber()));
		if (chatcontroller.getChatPreferences().isLoginToSecondChatEnabled()) {
			ChatCategory secondCat = chatcontroller.getChatPreferences().getLoginChatCategorySecond();
			if (secondCat != null) {
				label += " & " + secondCat.getChatCategoryName(secondCat.getCategoryNumber());
			}
		}
		return label;
	}

	/** The profile name for the title, or null for the root profile. */
	private String buildOperatorProfileTitleSuffixForCompose() {
		OperatorProfileSelection activeProfile = ActiveOperatorProfile.get();
		if (activeProfile == null || activeProfile.getProfile().isRootProfile()) {
			return null;
		}
		return activeProfile.getProfile().getDisplayName();
	}

	/** The score service's ranking, as the Compose priority bar wants it. */
	private java.util.List<kotlin.Pair<ChatMember, Double>> composeTopStations() {
		java.util.List<kotlin.Pair<ChatMember, Double>> out = new ArrayList<>();

		if (chatcontroller.getScoreService() == null) {
			return out;
		}

		for (ScoreService.TopCandidate candidate
				: chatcontroller.getScoreService().topCandidates().snapshot()) {

			/*
			 * A candidate names a callsign, not a station. The first active variant is the one
			 * the JavaFX buttons resolved to as well; a callsign with no variant left the chat
			 * between the ranking and this draw.
			 */
			java.util.List<ChatMember> variants =
					chatcontroller.findActiveChatMembersByRawCall(candidate.getCallSignRaw());
			if (!variants.isEmpty()) {
				out.add(new kotlin.Pair<>(variants.get(0), candidate.getScore()));
			}
		}
		return out;
	}

	/**
	 * Selects a station in the Compose window: the panel, the table and the score service, the
	 * three places the JavaFX selection listener kept in step.
	 */
	private kotlin.Unit selectStationInCompose(
			SelectedStationState panel,
			ChatMember member,
			StationSelectionSource source) {

		panel.select(member, source);
		
		selectedCallSignInfoStageChatMember = member;
		applySelectedStationMessageFilter(panel.getMessageFilter(), member);

		if (member != null && chatcontroller.getScoreService() != null) {
			chatcontroller.getScoreService().setSelectedChatMember(member);
			
			if (composeMainWindowState != null && composeMainWindowState.getChatInput() != null) {
				composeMainWindowState.getChatInput().prepareCq(member.getCallSign(), false);
			}
			
			runOnUi(() -> focusChatMemberAndPrepareCq(member, false));
		}
		return kotlin.Unit.INSTANCE;
	}

	private kotlin.Unit onDirectedMessageClickedInCompose(
			SelectedStationState panel,
			ChatInputState chatInput,
			ChatMessage message) {

		String ownCall = chatcontroller.getChatPreferences().getStn_loginCallSign();
		StationIdentity target = DirectedMessageStationTarget.forMessage(message, ownCall);
		if (target == null) {
			return kotlin.Unit.INSTANCE;
		}

		ChatMember activeTarget = chatcontroller.findActiveChatMember(target.getCallSign(), message.getChatCategory());
		if (activeTarget != null) {
			selectStationInCompose(panel, activeTarget, StationSelectionSource.DIRECTED_MESSAGE);
		} else {
			chatInput.prepareCq(target.getCallSign(), false);
			if (message.getSender() != null && message.getSender().getCallSign().equalsIgnoreCase(ownCall)) {
				rememberAutoPreparedCqTarget(target.getCallSign(), message.getChatCategory());
			}
		}

		return kotlin.Unit.INSTANCE;
	}

	private kotlin.Unit onMessageSenderClickedInCompose(
			SelectedStationState panel,
			ChatMessage message,
			StationSelectionSource source) {
		if (message == null || message.getSender() == null) {
			return kotlin.Unit.INSTANCE;
		}

		ChatMember sender = chatcontroller.findActiveChatMember(
				message.getSender().getCallSign(), message.getChatCategory());
		if (sender != null) {
			selectStationInCompose(panel, sender, source);
		}
		return kotlin.Unit.INSTANCE;
	}

	/** Whether a message came from this station; decides its row colour. */
	private boolean isOwnChatMessage(ChatMessage message) {
		if (message == null || message.getSender() == null) {
			return false;
		}

		String ownCall = chatcontroller.getChatPreferences().getStn_loginCallSign();
		return ownCall != null && ownCall.equals(message.getSender().getCallSign());
	}

	/** How long ago a message arrived, for the fading highlight. */
	private long chatMessageAgeSeconds(ChatMessage message) {
		if (message == null) {
			return Long.MAX_VALUE;
		}

		try {
			long generated = Long.parseLong(message.getMessageGeneratedTime());
			return (System.currentTimeMillis() / 1000L) - generated;
		} catch (RuntimeException notATimestamp) {
			return Long.MAX_VALUE;
		}
	}

	/** Pushes the link state and the window flags into the Compose window. */
	private void updateComposeSurroundings() {
		if (composeMainWindowState == null) {
			return;
		}

		MainWindowSurroundings surroundings = composeMainWindowState.getSurroundings();
		/*
		 * Through the feed, and with the detail. This used to set the state alone, which
		 * left the badge standing beside the default "No ON4KST connection" until the
		 * next indicator update corrected it. A null detail makes the feed substitute
		 * the state's own name, which is what the JavaFX indicator always did.
		 */
		if (connectionStateFeed != null) {
			connectionStateFeed.push(chatcontroller.getOn4KstConnectionState(), null);
		}
		surroundings.setSettingsWindowOpen(SettingsWindow.isOpen());
		surroundings.setMonitorWindowOpen(MonitorWindow.isOpen());
		composeMainWindowState.getMenu().setAwayFromChat(
				chatcontroller.getChatPreferences().isStn_loginAFKState());
	}

	/** Releases the Compose window's feeds; a discarded runtime that still feeds one lives on. */
	private void releaseComposeMainWindow() {
		if (composeMemberListener != null) {
			chatcontroller.getLst_chatMemberList().removeListener(composeMemberListener);
			composeMemberListener = null;
		}
		if (composeChatListener != null) {
			chatcontroller.getLst_globalChatMessageList().removeListener(composeChatListener);
			composeChatListener = null;
		}
		if (composeClusterListener != null) {
			chatcontroller.getLst_clusterMemberList().removeListener(composeClusterListener);
			composeClusterListener = null;
		}
		if (composeMainWindowState != null) {
			composeMainWindowState.release();
			composeMainWindowState = null;
		}
	}

	/**
	 * Opens the cluster and QSO monitor.
	 *
	 * Both tables follow their roster: the listener replaces the rows, and
	 * DataTableState keeps the selection on the row the operator picked rather than on
	 * the position it happened to occupy. The two subscriptions are held in fields and
	 * released by releaseMonitorListeners, because a discarded runtime that still feeds
	 * a closed window keeps itself alive.
	 */
	private void openMonitorWindow() {

		/*
		 * Guarded before anything is built. Without this a second click registers two
		 * more roster listeners and then hands them to a window that refuses to open,
		 * so every inbound message would re-run toOtherMessages once more over up to
		 * thirty thousand stored messages — work whose result is thrown away, on the
		 * thread that draws.
		 */
		if (MonitorWindow.isOpen()) {
			return;
		}

		ColumnWidthStore widths = new ColumnWidthStore() {
			@Override
			public Double width(String tableId, String columnId) {
				java.util.OptionalDouble stored =
						chatcontroller.getChatPreferences().getTableColumnWidth(tableId, columnId);
				return stored.isPresent() ? stored.getAsDouble() : null;
			}

			@Override
			public void setWidth(String tableId, String columnId, double width) {
				chatcontroller.getChatPreferences().setTableColumnWidth(tableId, columnId, width);
			}

			@Override
			public void requestSave() {
				requestLayoutSave();
			}
		};

		DataTableState<ClusterMessage> clusterTable = new DataTableState<>(
				MonitorColumns.INSTANCE.dxCluster(),
				RowKeys.INSTANCE.byReference(),
				"dx-cluster-monitor",
				widths);

		DataTableState<ChatMessage> qsoTable = new DataTableState<>(
				MonitorColumns.INSTANCE.qsoOfTheOther(),
				RowKeys.INSTANCE.byReference(),
				"qso-other-monitor",
				widths);

		/*
		 * Subscribed first and seeded afterwards, so a message arriving in between is
		 * not missed — the order RosterListBinding uses for the same reason.
		 */
		monitorClusterListener = rows -> uiDispatcher.runOnUi(() -> clusterTable.replaceRows(rows));
		chatcontroller.getLst_clusterMemberList().addListener(monitorClusterListener);

		/*
		 * The QSO table shows a filtered view, so the whole filter is re-run rather
		 * than the delivered list being used: toOtherMessages is what decides which
		 * messages belong to other operators.
		 */
		monitorChatListener =
				rows -> uiDispatcher.runOnUi(() -> qsoTable.replaceRows(chatcontroller.toOtherMessages()));
		chatcontroller.getLst_globalChatMessageList().addListener(monitorChatListener);

		clusterTable.replaceRows(chatcontroller.getLst_clusterMemberList().snapshot());
		qsoTable.replaceRows(chatcontroller.toOtherMessages());

		/*
		 * The same preference the JavaFX SplitPane wrote, so a profile gets back the
		 * position its operator dragged before the Compose port (#144).
		 */
		SplitterState monitorSplitter = new SplitterState(
				2,
				chatcontroller.getChatPreferences().getGUIpnl_directedMSGWin_dividerpositionDefault(),
				SplitterState.DEFAULT_MIN_PANE_PX,
				positions -> {
					chatcontroller.getChatPreferences()
							.setGUIpnl_directedMSGWin_dividerpositionDefault(positions);
					requestLayoutSave();
					return kotlin.Unit.INSTANCE;
				});

		MonitorWindow.show(
				clusterTable,
				qsoTable,
				monitorSplitter,
				chatcontroller.getChatPreferences().isGUI_darkModeActive(),
				SETTINGS_WINDOW_FONT_SIZE_SP,
				paletteStore,
				languageStore,
				(float) chatcontroller.getChatPreferences().getGUIclusterAndQSOMonStage_SceneSizeHW()[0],
				(float) chatcontroller.getChatPreferences().getGUIclusterAndQSOMonStage_SceneSizeHW()[1],
				(width, height) -> {
					chatcontroller.getChatPreferences().getGUIclusterAndQSOMonStage_SceneSizeHW()[0] = width;
					chatcontroller.getChatPreferences().getGUIclusterAndQSOMonStage_SceneSizeHW()[1] = height;
					requestLayoutSave();
					return kotlin.Unit.INSTANCE;
				});
	}

	/**
	 * Opens the update window when the server carries a newer release.
	 *
	 * The decision itself lives in UpdateWindowState and is covered by tests; what
	 * stays here is reaching the system browser, which is the host application's
	 * business.
	 */
	private void openUpdateWindowIfAvailable() {

		try {
			UpdateWindowState updateState = new UpdateWindowState(
					chatcontroller.getUpdateInformation(),
					ApplicationConstants.APPLICATION_CURRENT_VERSION,
					ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER);

			if (!updateState.getUpdateAvailable()) {
				return;
			}

			UpdateWindow.show(
					updateState,
					chatcontroller.getChatPreferences().isGUI_darkModeActive(),
					SETTINGS_WINDOW_FONT_SIZE_SP,
					paletteStore,
					languageStore,
					(float) chatcontroller.getChatPreferences().getGUIstage_updateStage_SceneSizeHW()[0],
					(float) chatcontroller.getChatPreferences().getGUIstage_updateStage_SceneSizeHW()[1],
					address -> ExternalDocuments.open(address),
					(width, height) -> {
						chatcontroller.getChatPreferences().getGUIstage_updateStage_SceneSizeHW()[0] = width;
						chatcontroller.getChatPreferences().getGUIstage_updateStage_SceneSizeHW()[1] = height;
						requestLayoutSave();
						return kotlin.Unit.INSTANCE;
					});
		} catch (Exception updateProblem) {
			// The client must start even when the update service is unreachable.
			System.out.println("[KST4ContestApp, ERROR]: Problem on Updateservice! "
					+ updateProblem.getMessage());
			updateProblem.printStackTrace();
		}
	}

	/**
	 * The session state the settings window shows, read from the main window's Compose state
	 * so the Station tab's login fields follow it while the window is open. Before that state
	 * exists the controller's value is the best there is.
	 */
	private On4KstConnectionState settingsConnectionState() {
		if (composeMainWindowState != null) {
			return composeMainWindowState.getSurroundings().getConnectionState();
		}
		On4KstConnectionState current = chatcontroller.getOn4KstConnectionState();
		return current == null ? On4KstConnectionState.DISCONNECTED : current;
	}

	/**
	 * Opens the settings window, or does nothing when it is already open.
	 *
	 * <p>This window carries the Connect button and is therefore the way into the chat,
	 * which is why it is opened once at startup as the JavaFX window was.</p>
	 */
	private void openSettingsWindow() {

		/*
		 * One draft per opening: the tabs stage into it, the buttons apply it, and the window
		 * asks about it when closed with edits pending.
		 */
		SettingsDraft draft = new SettingsDraft();

		SettingsWindow.show(
				SettingsTabsKt.buildSettingsTabs(chatcontroller, this, settingsNotices, paletteStore, languageStore,
						this::settingsConnectionState, draft),
				settingsNotices,
				draft,
				chatcontroller.getChatPreferences().isGUI_darkModeActive(),
				SETTINGS_WINDOW_FONT_SIZE_SP,
				paletteStore,
				languageStore,
				(float) chatcontroller.getChatPreferences().getGUIsettingsStageSceneSizeHW()[0],
				(float) chatcontroller.getChatPreferences().getGUIsettingsStageSceneSizeHW()[1],
				(width, height) -> {
					chatcontroller.getChatPreferences().getGUIsettingsStageSceneSizeHW()[0] = width;
					chatcontroller.getChatPreferences().getGUIsettingsStageSceneSizeHW()[1] = height;
					requestLayoutSave();
					return kotlin.Unit.INSTANCE;
				},
				SettingsTabsKt.settingsButtons(
						chatcontroller,
						this,
						settingsNotices,
						draft,
						this::settingsConnectionState));
	}

	/*
	 * The Compose main menu's actions, lifted out of the JavaFX menu-item handlers that
	 * initMenuBar used to carry. ComposeMenuActions calls these directly now; the enabled
	 * state of the entries lives in MainMenuState, so these methods carry only behaviour,
	 * not the former setDisable() bookkeeping of the deleted MenuItems.
	 */

	/** File -> Connect, using the stored login credentials. */
	void menuActionConnect() {
		String call = chatcontroller.getChatPreferences().getStn_loginCallSign();
		String pass = chatcontroller.getChatPreferences().getStn_loginPassword();

		boolean darkMode = chatcontroller.getChatPreferences().isGUI_darkModeActive();

		if (call == null || call.isBlank() || pass == null || pass.isBlank()) {
			ComposeAlert.show(
					kst4contest.view.i18n.CurrentStrings.get().getAlertCannotConnectTitle(),
					kst4contest.view.i18n.CurrentStrings.get().getAlertCredentialsMissing(),
					kst4contest.view.i18n.CurrentStrings.get().getAlertCredentialsMissingBody(),
					darkMode);
			return;
		}

		try {
			chatcontroller.execute();
		} catch (InterruptedException | IOException e) {
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception", e);
			ComposeAlert.show(
					kst4contest.view.i18n.CurrentStrings.get().getAlertConnectionFailedTitle(),
					null,
					kst4contest.view.i18n.CurrentStrings.get().alertConnectionFailedBody(String.valueOf(e.getMessage())),
					darkMode);
		}
	}

	/** File -> Disconnect (keeps the session, unlike exit). */
	void menuActionDisconnect() {
		chatcontroller.disconnect(ApplicationConstants.DISCSTRING_DISCONNECTONLY);
	}

	/** File -> Exit + disconnect. */
	void menuActionExit() {
		closeWindowEvent();
	}

	/** Options -> Set QRG as name in Chat (main category). */
	void menuActionSetQrgAsNameInChat() {
		ChatMessage sendMe = new ChatMessage();
		sendMe.setMessageDirectedToServer(false);
		sendMe.setMessageText("/SETNAME " + chatcontroller.getChatPreferences().getMYQRGFirstCat().get());
		chatcontroller.getMessageTXBus().add(sendMe);
	}

	/** Options -> toggle AFK state; sends /AWAY or /BACK accordingly. */
	void menuActionToggleAwayState() {
		ChatMessage sendMe = new ChatMessage();
		sendMe.setMessageDirectedToServer(false);

		if (chatcontroller.getChatPreferences().isStn_loginAFKState()) {
			chatcontroller.getChatPreferences().setStn_loginAFKState(false);
			sendMe.setMessageText("/BACK");
		} else {
			chatcontroller.getChatPreferences().setStn_loginAFKState(true);
			sendMe.setMessageText("/AWAY");
		}

		chatcontroller.getMessageTXBus().add(sendMe);
	}

	/**
	 * Options/Windows -> open or close the settings window. Closing goes the way of the
	 * window's own X, so edits not applied yet are asked about rather than dropped.
	 */
	void menuActionToggleSettingsWindow() {
		if (SettingsWindow.isOpen()) {
			SettingsWindow.requestClose();
		} else {
			openSettingsWindow();
		}
	}

	/** Windows -> open or close the cluster / stranger-QSO monitor window. */
	void menuActionToggleMonitorWindow() {
		if (MonitorWindow.isOpen()) {
			MonitorWindow.close();
			releaseMonitorListeners();
		} else {
			openMonitorWindow();
		}
	}

	/** Windows -> show or hide the station map. */
	void menuActionToggleStationMap() {
		toggleStationMapWindow();
	}

	/** Windows -> dark design, for this session only. */
	void menuActionUseDarkDesign() {
		applyTheme(true);
	}

	/** Windows -> default (light) design, for this session only. */
	void menuActionUseDefaultDesign() {
		applyTheme(false);
	}

	/** Info -> donate via Ko-fi. */
	void menuActionOpenDonationPage() {
		ExternalDocuments.open("https://ko-fi.com/praktimarc");
	}

	/** Info -> DARC X08 homepage. */
	void menuActionOpenHomepage() {
		ExternalDocuments.open("http://www.x08.de");
	}

	/** Info -> kst4Contest newsgroup. */
	void menuActionOpenNewsgroup() {
		ExternalDocuments.open("https://groups.google.com/g/kst4contest/about");
	}

	/** Info -> donate for OV3T's aeroplane feed service. */
	void menuActionOpenOv3tDonationPage() {
		ExternalDocuments.open("https://www.paypal.me/ov3t");
	}

	/** Info -> contact the author by mail. */
	void menuActionContactAuthor() {
		ExternalDocuments.open("mailto:praktimarc+kst4contest@gmail.com");
	}

	/** Info -> About dialog. */
	void menuActionShowAbout() {
		ComposeAlert.show(
				"About kst4contest",
				"kst4Contest " + ApplicationConstants.APPLICATION_CURRENT_VERSION
						+ ": ON4KST Chatclient by DO5AMF and DN9APW",
				chatcontroller.getChatPreferences().getProgramVersion(),
				chatcontroller.getChatPreferences().isGUI_darkModeActive());
	}

	@Override
	public void refreshShortcutButtonsFromSettings() {
		/*
		 * Points at the Compose shortcut row, which is the only one left. Until now this
		 * rebuilt the JavaFX button row into flwPane_textSnippets, a pane that has been null
		 * since the window was disconnected — so an edited shortcut did not reach the
		 * operator until the window was rebuilt. MainWindowState.RosterMirror says as much
		 * about its own refresh hook; this is the caller it was waiting for.
		 */
		uiDispatcher.runOnUi(() -> {
			if (composeMainWindowState != null) {
				composeMainWindowState.getShortcuts().refresh();
			}
		});
	}

	/**
	 * No-op since the JavaFX snippet context menus were removed with the never-shown
	 * table. The Compose settings still call this through the shared interface; there is
	 * no JavaFX menu left to rebuild, and the Compose message panes do not expose the
	 * snippet context menu that this used to refresh.
	 */
	@Override
	public void refreshTextSnippetContextMenusFromSettings() {
		/*
		 * The JavaFX context menu this used to rebuild is gone, but the Compose snippet
		 * mirror still needs catching up after an edit — the same hook the shortcut row
		 * was missing.
		 */
		uiDispatcher.runOnUi(() -> {
			if (composeMainWindowState != null) {
				composeMainWindowState.getSnippets().refresh();
			}
		});
	}

	@Override
	public String detectWintestBroadcastAddress() {

		try {
			return detectPreferredWintestBroadcastAddress();
		} catch (Exception detectionProblem) {
			System.out.println("[Main.java, Warning]: Could not auto-detect broadcast: "
					+ detectionProblem.getMessage());
			return null;
		}
	}

	/** Pushes the stored SO2R switches to the main window so it shows or hides the radio 2 controls. */
	@Override
	public void refreshSo2rState() {
		runOnUi(() -> {
			if (composeMainWindowState == null) {
				return;
			}
			kst4contest.model.ChatPreferences preferences = chatcontroller.getChatPreferences();
			MainWindowSurroundings target = composeMainWindowState.getSurroundings();
			target.setSo2rEnabled(preferences.isTrxSynch_so2rEnabled());
			target.setSecondAntennaOffered(preferences.isTrxSynch_so2rEnabled() && preferences.isStn_pstRotator2Enabled());
		});
	}

	/**
	 * No-op since the JavaFX own-QRG text field was removed with the never-shown window.
	 *
	 * <p>The Compose chat input owns that value on both sides: it writes it through
	 * {@code prefs.getMYQRGFirstCat().set(...)} and the MYQRG button reads it back through
	 * {@code ChatInputState.appendOwnQrg}. There is no second field left to keep in step.
	 * The Compose settings still call this through the shared interface, and an empty body
	 * is honest about there being nothing to do.</p>
	 */
	@Override
	public void applyOwnQrgFollower(boolean enabled) {
		// intentionally empty: no JavaFX text field left to follow
	}

	@Override
	public void applyDebugModeToFile(boolean enabled) {
		/*
		 * The preference itself is written by the settings tab. What only the application
		 * can do is open and close the error-log file handler and the recorder, in the
		 * order the JavaFX checkbox kept: on the way up the log opens before the recorder
		 * starts, on the way down the recorder stops before the log closes.
		 */
		if (enabled) {
			setDebugFileLoggingEnabled(true);
			chatcontroller.setMessageHistoryRecordingEnabled(true);
		} else {
			chatcontroller.setMessageHistoryRecordingEnabled(false);
			setDebugFileLoggingEnabled(false);
		}
	}

	/**
	 * The worked-stations rows are a snapshot the tab takes from the roster itself, so
	 * there is nothing for this class to redraw. The JavaFX table needed an explicit
	 * refresh because a TableView does not notice that the objects inside it changed.
	 */
	@Override
	public void refreshWorkedStationsView() {
		// nothing to do: the Compose tab re-reads the roster
	}

	@Override
	public void switchToOperatorProfile(OperatorProfile profile) {
		uiDispatcher.runOnUi(() -> requestOperatorProfileSwitch(profile));
	}

	/**
	 * Starts the ON4KST session. The credentials are already in the preferences: every
	 * field of the Station tab writes through as it is edited, so there is nothing to
	 * collect here first.
	 */
	@Override
	public String connectFromSettings() {

		try {
			chatcontroller.execute();
			return null;
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			return "The connection was interrupted: " + interrupted.getMessage();
		} catch (IOException connectionProblem) {
			return "The ON4KST server could not be reached: " + connectionProblem.getMessage();
		}
	}

	@Override
	public void disconnectOnlyFromSettings() {
		chatcontroller.disconnect(ApplicationConstants.DISCSTRING_DISCONNECTONLY);
	}

	@Override
	public void disconnectAndCloseChatFromSettings() {
		uiDispatcher.runOnUi(this::closeWindowEvent);
	}

	@Override
	public String savePreferencesFromSettings() {

		if (!chatcontroller.getChatPreferences().writePreferencesToXmlFile()) {
			return null;
		}

		if (layoutAutosave != null) {
			layoutAutosave.cancelPending();
		}

		return chatcontroller.getChatPreferences().getStoreAndRestorePreferencesFileName();
	}

	private String detectPreferredWintestBroadcastAddress() {
		String internetRouteBroadcast = detectInternetRouteBroadcastAddress();
		if (internetRouteBroadcast != null && !internetRouteBroadcast.isBlank()) {
			return internetRouteBroadcast;
		}
		return detectFirstUsableBroadcastAddress();
	}

	private String detectInternetRouteBroadcastAddress() {
		java.net.DatagramSocket routeProbe = null;
		try {
			routeProbe = new java.net.DatagramSocket();
			routeProbe.connect(java.net.InetAddress.getByName("8.8.8.8"), 53);

			java.net.InetAddress localAddress = routeProbe.getLocalAddress();
			if (localAddress == null || localAddress.isAnyLocalAddress() || localAddress.isLoopbackAddress()) {
				return null;
			}

			java.net.NetworkInterface networkInterface = java.net.NetworkInterface.getByInetAddress(localAddress);
			if (networkInterface == null || !networkInterface.isUp()) {
				return null;
			}

			for (java.net.InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
				if (!(interfaceAddress.getAddress() instanceof java.net.Inet4Address)) {
					continue;
				}
				if (!localAddress.equals(interfaceAddress.getAddress())) {
					continue;
				}
				if (interfaceAddress.getBroadcast() != null) {
					return interfaceAddress.getBroadcast().getHostAddress();
				}
			}

			for (java.net.InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
				if (interfaceAddress.getBroadcast() != null && interfaceAddress.getAddress() instanceof java.net.Inet4Address) {
					return interfaceAddress.getBroadcast().getHostAddress();
				}
			}
		} catch (Exception ignored) {
			// Fallback to generic detection if internet-route probing fails
		} finally {
			if (routeProbe != null && !routeProbe.isClosed()) {
				routeProbe.close();
			}
		}
		return null;
	}

	private String detectFirstUsableBroadcastAddress() {
		try {
			for (java.net.NetworkInterface networkInterface : java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
				if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual() || networkInterface.isPointToPoint()) {
					continue;
				}
				for (java.net.InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					if (interfaceAddress.getBroadcast() != null && interfaceAddress.getAddress() instanceof java.net.Inet4Address) {
						return interfaceAddress.getBroadcast().getHostAddress();
					}
				}
			}
		} catch (Exception ignored) {
			// Keep configured value if no interface can be detected
		}
		return null;
	}


	/**
	 * Checks whether a value can safely be used as an AirScout routing
	 * identifier.
	 *
	 * Spaces are allowed because AirScout encloses the identifier in quotation
	 * marks. Quotation marks and line breaks would break the protocol message.
	 *
	 * @param identifier entered server or client identifier
	 * @return true if the identifier can be used
	 */
	private boolean isValidAirScoutIdentifier(String identifier) {
		if (identifier == null) {
			return false;
		}

		String normalizedIdentifier = identifier.trim();

		return !normalizedIdentifier.isEmpty()
				&& !normalizedIdentifier.contains("\"")
				&& !normalizedIdentifier.contains("\r")
				&& !normalizedIdentifier.contains("\n");
	}



	/**
	 * Resolves the currently selected UI reachability band. Null override means Auto.
	 *
	 * @param member station row
	 * @return manually selected band or automatic band
	 */
	private Band resolveReachabilityBandForUi(ChatMember member) {
		if (composeMainWindowState != null) {
			return composeMainWindowState.getReachability().resolveBand(member);
		}
		return chatcontroller.getReachabilityService().resolveAutoBand(member);
	}

	/**
	 * Formats the projected priority score for the station table.
	 *
	 * @param member station row
	 * @return score text
	 */
	private String formatPriorityScore(ChatMember member) {
		if (member == null || !Double.isFinite(member.getCurrentPriorityScore())) {
			return "-";
		}
		return String.format(Locale.US, "%.0f", member.getCurrentPriorityScore());
	}

	/**
	 * Parses a numeric table value for custom score sorting.
	 *
	 * @param value table text
	 * @return parsed value or negative infinity for missing values
	 */
	private double parseTableDouble(String value) {
		if (value == null || value.isBlank() || value.equals("-") || value.startsWith("- @")) {
			return Double.NEGATIVE_INFINITY;
		}

		String normalized = value.replace(",", ".").replace("+", "").trim();
		int firstSpace = normalized.indexOf(' ');
		if (firstSpace > 0) {
			normalized = normalized.substring(0, firstSpace);
		}

		try {
			return Double.parseDouble(normalized);
		} catch (NumberFormatException ignored) {
			return Double.NEGATIVE_INFINITY;
		}
	}

	/**
	 * Predicate helper for the tropo filter. Pending or failed calculations remain
	 * visible as requested; only finite negative margins are hidden.
	 *
	 * @param member station row
	 * @return true if station should remain visible under the tropo filter
	 */
	private boolean isReachableViaTropoFilterMatch(ChatMember member) {
		if (member == null || chatcontroller.getReachabilityService() == null) {
			return true;
		}

		Band selectedBand = resolveReachabilityBandForUi(member);
		chatcontroller.getReachabilityService().ensureTropoMarginCalculated(member, selectedBand);

		OptionalDouble marginDb = member.getTropoSsbMarginDb(selectedBand);
		if (marginDb.isEmpty() || !Double.isFinite(marginDb.getAsDouble())) {
			return true;
		}

		return marginDb.getAsDouble() >= 0.0;
	}


	/**
	 * Returns true when a station has any AirScout window now or within maxMinutes.
	 *
	 * @param member station row
	 * @param maxMinutes look-ahead window
	 * @return true if any AS window is available
	 */
	private boolean hasAsWindowInNextMinutes(ChatMember member, int maxMinutes) {
		if (member == null || member.getAirPlaneReflectInfo() == null
				|| member.getAirPlaneReflectInfo().getRisingAirplanes() == null) {
			return false;
		}

		return member.getAirPlaneReflectInfo().getRisingAirplanes().stream()
				.filter(Objects::nonNull)
				.anyMatch(airPlane -> airPlane.getArrivingDurationMinutes() >= 0
						&& airPlane.getArrivingDurationMinutes() <= maxMinutes);
	}

	/**
	 * Returns true when the station is known to be QRV on at least one enabled own band
	 * that has not been worked yet.
	 *
	 * @param member station row
	 * @return true if there is a new-band opportunity
	 */
	private boolean isNewBandOpportunity(ChatMember member) {
		if (member == null || chatcontroller == null) {
			return false;
		}

		List<ChatMember> variants =
				chatcontroller.findActiveChatMembersByRawCall(member.getCallSignRaw());
		if (variants.isEmpty()) {
			variants = List.of(member);
		}

		BandOpportunityResolver.Resolution resolution =
				BandOpportunityResolver.resolve(variants, System.currentTimeMillis());
		EnumSet<Band> enabledBands =
				BandOpportunityResolver.getEnabledStationBands(chatcontroller.getChatPreferences());

		return !resolution.getUnworkedEnabledBands(enabledBands).isEmpty();
	}

}




