package kst4contest.controller;

import java.util.OptionalLong;

import kst4contest.model.BeaconStatusSnapshot;
import kst4contest.model.BeaconStatusSnapshot.AggregateState;
import kst4contest.model.BeaconStatusSnapshot.CategoryStatus;
import kst4contest.model.BeaconStatusSnapshot.InactiveReason;
import kst4contest.model.ChatPreferences;

/** Owns session-only beacon activation, QRG references, reasons, and send failures. */
final class BeaconSessionState {
    static final long MAX_AUTOMATIC_QRG_DELTA_HZ = 5_000L;

    enum Channel {
        MAIN,
        SECOND
    }

    private final ChannelState main = new ChannelState();
    private final ChannelState second = new ChannelState();

    void resetSession(ChatPreferences preferences) {
        synchronized (this) {
            main.reset();
            second.reset();
        }
        project(preferences);
    }

    boolean setEnabled(Channel channel, boolean enabled, ChatPreferences preferences) {
        boolean accepted;
        synchronized (this) {
            ChannelState target = state(channel);
            if (!enabled) {
                target.disable(InactiveReason.USER_DISABLED);
                accepted = true;
            } else {
                InactiveReason invalidReason = missingRequiredQrg(channel, preferences);
                if (invalidReason != null) {
                    target.disable(invalidReason);
                    accepted = false;
                } else {
                    target.enabled = true;
                    target.failed = false;
                    OptionalLong reference = ownQrg(channel, preferences);
                    target.referenceHz = reference.isPresent() ? reference.getAsLong() : null;
                    accepted = true;
                }
            }
        }
        project(preferences);
        return accepted;
    }

    void onManualQrgChanged(Channel channel, String value, ChatPreferences preferences) {
        if (channel == Channel.MAIN) {
            preferences.getMYQRGFirstCat().set(value);
        } else {
            preferences.getMYQRGSecondCat().set(value);
        }
        OptionalLong parsed = BeaconFrequencyParser.parseHz(value);
        synchronized (this) {
            ChannelState target = state(channel);
            if (target.enabled) {
                target.referenceHz = parsed.isPresent() ? parsed.getAsLong() : null;
            }
            revalidateEnabled(preferences);
        }
        project(preferences);
    }

    void onExternalQrgChanged(Channel channel, long frequencyHz, ChatPreferences preferences) {
        boolean valid = frequencyHz > 0L && ownQrg(channel, preferences).isPresent();
        synchronized (this) {
            ChannelState target = state(channel);
            if (target.enabled) {
                if (!valid) {
                    target.referenceHz = null;
                } else if (target.referenceHz == null) {
                    target.referenceHz = frequencyHz;
                } else if (absoluteDifference(target.referenceHz, frequencyHz)
                        > MAX_AUTOMATIC_QRG_DELTA_HZ) {
                    target.disable(InactiveReason.AUTOMATIC_QRG_CHANGE);
                }
            }
            revalidateEnabled(preferences);
        }
        project(preferences);
    }

    void onTemplateChanged(Channel channel, ChatPreferences preferences) {
        synchronized (this) {
            ChannelState target = state(channel);
            if (target.enabled) {
                InactiveReason reason = missingRequiredQrg(channel, preferences);
                if (reason != null) {
                    target.disable(reason);
                }
            }
        }
        project(preferences);
    }

    synchronized boolean isEnabled(Channel channel) {
        return state(channel).enabled;
    }

    synchronized void recordSendFailure(Channel channel, boolean failed) {
        ChannelState target = state(channel);
        target.failed = target.enabled && failed;
    }

    synchronized BeaconStatusSnapshot snapshot(ChatPreferences preferences) {
        boolean secondAvailable = preferences.isLoginToSecondChatEnabled();
        boolean mainEnabled = main.enabled;
        boolean secondEnabled = secondAvailable && second.enabled;
        boolean failed = (mainEnabled && main.failed) || (secondEnabled && second.failed);
        int available = secondAvailable ? 2 : 1;
        int enabled = (mainEnabled ? 1 : 0) + (secondEnabled ? 1 : 0);

        AggregateState aggregate;
        if (failed) {
            aggregate = AggregateState.FAILED;
        } else if (enabled == 0) {
            aggregate = AggregateState.OFF;
        } else if (enabled == available) {
            aggregate = AggregateState.ON;
        } else {
            aggregate = AggregateState.PARTIAL;
        }

        return new BeaconStatusSnapshot(
                aggregate,
                new CategoryStatus(true, mainEnabled, main.failed, main.reason),
                new CategoryStatus(secondAvailable, secondEnabled, second.failed, second.reason)
        );
    }

    private void revalidateEnabled(ChatPreferences preferences) {
        revalidate(Channel.MAIN, preferences);
        revalidate(Channel.SECOND, preferences);
    }

    private void revalidate(Channel channel, ChatPreferences preferences) {
        ChannelState target = state(channel);
        if (!target.enabled) {
            return;
        }
        InactiveReason reason = missingRequiredQrg(channel, preferences);
        if (reason != null) {
            target.disable(reason);
        }
    }

    private static InactiveReason missingRequiredQrg(
            Channel channel,
            ChatPreferences preferences
    ) {
        String template = channel == Channel.MAIN
                ? preferences.getBcn_beaconTextMainCat()
                : preferences.getBcn_beaconTextSecondCat();
        String safeTemplate = template == null ? "" : template;
        if ((safeTemplate.contains("MYQRG") || safeTemplate.contains("MYQRGSHORT"))
                && BeaconFrequencyParser.parseHz(preferences.getMYQRGFirstCat().get()).isEmpty()) {
            return InactiveReason.PRIMARY_QRG_UNAVAILABLE;
        }
        if (safeTemplate.contains("SECONDQRG")
                && BeaconFrequencyParser.parseHz(preferences.getMYQRGSecondCat().get()).isEmpty()) {
            return InactiveReason.SECONDARY_QRG_UNAVAILABLE;
        }
        return null;
    }

    private static OptionalLong ownQrg(Channel channel, ChatPreferences preferences) {
        String value = channel == Channel.MAIN
                ? preferences.getMYQRGFirstCat().get()
                : preferences.getMYQRGSecondCat().get();
        return BeaconFrequencyParser.parseHz(value);
    }

    private static long absoluteDifference(long first, long second) {
        return first >= second ? first - second : second - first;
    }

    private void project(ChatPreferences preferences) {
        boolean mainEnabled;
        boolean secondEnabled;
        synchronized (this) {
            mainEnabled = main.enabled;
            secondEnabled = second.enabled;
        }
        if (preferences.isBcn_beaconsEnabledMainCat() != mainEnabled) {
            preferences.setBcn_beaconsEnabledMainCat(mainEnabled);
        }
        if (preferences.isBcn_beaconsEnabledSecondCat() != secondEnabled) {
            preferences.setBcn_beaconsEnabledSecondCat(secondEnabled);
        }
    }

    private ChannelState state(Channel channel) {
        return channel == Channel.MAIN ? main : second;
    }

    private static final class ChannelState {
        private boolean enabled;
        private boolean failed;
        private Long referenceHz;
        private InactiveReason reason = InactiveReason.NOT_ACTIVATED_THIS_SESSION;

        private void reset() {
            enabled = false;
            failed = false;
            referenceHz = null;
            reason = InactiveReason.NOT_ACTIVATED_THIS_SESSION;
        }

        private void disable(InactiveReason inactiveReason) {
            enabled = false;
            failed = false;
            referenceHz = null;
            reason = inactiveReason;
        }
    }
}
