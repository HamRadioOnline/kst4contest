package kst4contest.model;

/**
 * Object for the description of the activity of a Thread to show these information in a View.
 * <br/><br/>
 * If state is critical, there could be used a further information field for the stacktrace
 */
public class ThreadStateMessage {
    String threadNickName;
    String threadDescription;
    boolean running;
    String runningInformationTextDescription;
    String runningInformation;

    boolean criticalState;
    String criticalStateFurtherInfo;
    BeaconStatusSnapshot beaconStatusSnapshot;



    public ThreadStateMessage(String threadNickName, boolean running, String runningInformation, boolean criticalState) {

        this.threadNickName = threadNickName;
        this.running = running;
        this.criticalState = criticalState;
        this.runningInformation = runningInformation;

    }

    /**
     * This triggers the message for "Sked armed"
     *
     * @return
     */
    public String getRunningInformationTextDescription() {

        // If a custom description was set (e.g. for UI indicator buttons), prefer it.
        if (runningInformationTextDescription != null && !runningInformationTextDescription.isBlank()) {
            return runningInformationTextDescription;
        }

        // Fallback (legacy behavior)
        if (isRunning()) {
            return "on";
        } else if (!isRunning() && isCriticalState()) {
            return "FAILED";
        } else {
            return "off";
        }
    }

    public void setRunningInformationTextDescription(String runningInformationTextDescription) {
        this.runningInformationTextDescription = runningInformationTextDescription;
    }

    public String getThreadNickName() {
        return threadNickName;
    }

    public void setThreadNickName(String threadNickName) {
        this.threadNickName = threadNickName;
    }

    public String getThreadDescription() {
        return threadDescription;
    }

    public void setThreadDescription(String threadDescription) {
        this.threadDescription = threadDescription;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public String getRunningInformation() {
        return runningInformation;
    }

    public void setRunningInformation(String runningInformation) {
        this.runningInformation = runningInformation;
    }

    public boolean isCriticalState() {
        return criticalState;
    }

    public void setCriticalState(boolean criticalState) {
        this.criticalState = criticalState;
    }

    public String getCriticalStateFurtherInfo() {
        return criticalStateFurtherInfo;
    }

    public void setCriticalStateFurtherInfo(String criticalStateFurtherInfo) {
        this.criticalStateFurtherInfo = criticalStateFurtherInfo;
    }

    public BeaconStatusSnapshot getBeaconStatusSnapshot() {
        return beaconStatusSnapshot;
    }

    public void setBeaconStatusSnapshot(BeaconStatusSnapshot beaconStatusSnapshot) {
        this.beaconStatusSnapshot = beaconStatusSnapshot;
    }
}

