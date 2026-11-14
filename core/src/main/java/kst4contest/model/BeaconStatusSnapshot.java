package kst4contest.model;

import java.util.Objects;

/** Immutable, display-neutral status of both beacon categories. */
public final class BeaconStatusSnapshot {
    public enum AggregateState {
        OFF,
        PARTIAL,
        ON,
        FAILED
    }

    public enum InactiveReason {
        NOT_ACTIVATED_THIS_SESSION,
        USER_DISABLED,
        PRIMARY_QRG_UNAVAILABLE,
        SECONDARY_QRG_UNAVAILABLE,
        AUTOMATIC_QRG_CHANGE
    }

    /** Immutable status of one beacon category. */
    public static final class CategoryStatus {
        private final boolean available;
        private final boolean enabled;
        private final boolean failed;
        private final InactiveReason inactiveReason;

        public CategoryStatus(
                boolean available,
                boolean enabled,
                boolean failed,
                InactiveReason inactiveReason
        ) {
            this.available = available;
            this.enabled = enabled;
            this.failed = failed;
            this.inactiveReason = Objects.requireNonNull(inactiveReason);
        }

        public boolean isAvailable() {
            return available;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isFailed() {
            return failed;
        }

        public InactiveReason getInactiveReason() {
            return inactiveReason;
        }
    }

    private final AggregateState aggregateState;
    private final CategoryStatus main;
    private final CategoryStatus second;

    public BeaconStatusSnapshot(
            AggregateState aggregateState,
            CategoryStatus main,
            CategoryStatus second
    ) {
        this.aggregateState = Objects.requireNonNull(aggregateState);
        this.main = Objects.requireNonNull(main);
        this.second = Objects.requireNonNull(second);
    }

    public AggregateState getAggregateState() {
        return aggregateState;
    }

    public CategoryStatus getMain() {
        return main;
    }

    public CategoryStatus getSecond() {
        return second;
    }
}
