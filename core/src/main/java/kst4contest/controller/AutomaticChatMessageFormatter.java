package kst4contest.controller;

/** Adds the fixed marker used by every user-visible automatic chat message. */
final class AutomaticChatMessageFormatter {

    /** Exact marker appended to every user-visible automatic chat message. */
    /* default */ static final String SUFFIX = " *a*";

    private AutomaticChatMessageFormatter() {
    }

    /* default */ static String appendSuffix(final String messageText) {
        return messageText + SUFFIX;
    }
}
