package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Verifies the fixed suffix contract for automatic chat messages. */
class AutomaticChatMessageFormatterTest {

    @Test
    void appendsTheExactAutomaticChatSuffix() {
        assertEquals(
                "CQ test *a*",
                AutomaticChatMessageFormatter.appendSuffix("CQ test"),
                "the suffix must include its leading space"
        );
    }

    @Test
    void appendsTheSuffixEvenWhenTheTextAlreadyEndsWithIt() {
        assertEquals(
                "CQ test *a* *a*",
                AutomaticChatMessageFormatter.appendSuffix("CQ test *a*"),
                "an existing suffix must not be deduplicated"
        );
    }
}
