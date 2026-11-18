package kst4contest.view;

import kst4contest.view.compose.MainMenuActions;

/**
 * The Compose menu bar's actions, delegated to methods on {@link Kst4ContestApplication}.
 *
 * <p>These used to fire the {@code MenuItem}s of the old JavaFX menu bar,
 * which ran side by side with the Compose bar so the two could be compared. The JavaFX bar is
 * gone (part 3), so the handlers now live in {@code menuAction…} methods of the application and
 * this class calls those directly. It stays a thin bridge: no behaviour of its own, one call per
 * menu entry.</p>
 */
public final class ComposeMenuActions implements MainMenuActions {

    private final Kst4ContestApplication application;

    public ComposeMenuActions(final Kst4ContestApplication application) {
        this.application = application;
    }

    @Override
    public void connect() {
        application.runOnUi(application::menuActionConnect);
    }

    @Override
    public void disconnect() {
        application.runOnUi(application::menuActionDisconnect);
    }

    @Override
    public void switchOperatorProfile() {
        application.runOnUi(application::showOperatorProfileSwitchDialogFromCompose);
    }

    @Override
    public void exitApplication() {
        application.runOnUi(application::menuActionExit);
    }

    @Override
    public void setQrgAsNameInChat() {
        application.runOnUi(application::menuActionSetQrgAsNameInChat);
    }

    @Override
    public void toggleAwayState() {
        application.runOnUi(application::menuActionToggleAwayState);
    }

    @Override
    public void toggleSettingsWindow() {
        application.runOnUi(application::menuActionToggleSettingsWindow);
    }

    @Override
    public void openSetupWizard() {
        application.runOnUi(application::menuActionOpenSetupWizard);
    }

    @Override
    public void toggleMonitorWindow() {
        application.runOnUi(application::menuActionToggleMonitorWindow);
    }

    @Override
    public void toggleStationMap() {
        application.runOnUi(application::menuActionToggleStationMap);
    }

    @Override
    public void useDarkDesign() {
        application.runOnUi(application::menuActionUseDarkDesign);
    }

    @Override
    public void useDefaultDesign() {
        application.runOnUi(application::menuActionUseDefaultDesign);
    }

    @Override
    public void openDonationPage() {
        application.runOnUi(application::menuActionOpenDonationPage);
    }

    @Override
    public void openHomepage() {
        application.runOnUi(application::menuActionOpenHomepage);
    }

    @Override
    public void openNewsgroup() {
        application.runOnUi(application::menuActionOpenNewsgroup);
    }

    @Override
    public void openChangelog() {
        /*
         * The JavaFX item and its handler were both commented out, so there is nothing to do.
         * The method stays because the interface is shared and an empty body is honest about
         * there being no such feature.
         */
    }

    @Override
    public void openOv3tDonationPage() {
        application.runOnUi(application::menuActionOpenOv3tDonationPage);
    }

    @Override
    public void contactAuthor() {
        application.runOnUi(application::menuActionContactAuthor);
    }

    @Override
    public void showAbout() {
        application.runOnUi(application::menuActionShowAbout);
    }
}
