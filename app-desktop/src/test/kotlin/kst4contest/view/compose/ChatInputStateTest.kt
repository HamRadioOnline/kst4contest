package kst4contest.view.compose

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import kst4contest.controller.RadioSlot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The line the operator actually types into. Everything here is the JavaFX behaviour;
 * two places where that behaviour is inconsistent are pinned as they are rather than
 * quietly repaired, because both decide what gets transmitted.
 */
class ChatInputStateTest {

    private val mainCategory = ChatCategory(2)
    private val memberCategory = ChatCategory(3)

    // ---- sending -----------------------------------------------------------

    @Test
    fun `sending queues the resolved text and empties the field`() {
        val actions = FakeActions()
        val state = ChatInputState(actions)
        state.value = TextFieldValue("test de dn9apw")

        state.send()

        assertEquals(1, actions.queued.size)
        assertEquals("test de dn9apw", actions.queued.single().messageText)
        assertEquals("", state.value.text)
    }

    @Test
    fun `the enter key sends the same way the button does`() {
        val actions = FakeActions()
        val state = ChatInputState(actions)
        state.value = TextFieldValue("cq cq")

        state.send()

        assertEquals(1, actions.queued.size)
    }

    /** A variable may legitimately resolve to nothing; then there is no message. */
    @Test
    fun `a text that resolves to nothing is dropped and the field cleared`() {
        val actions = FakeActions(resolved = "   ")
        val state = ChatInputState(actions)
        state.value = TextFieldValue("%qrg%")

        state.send()

        assertTrue(actions.queued.isEmpty())
        assertEquals("", state.value.text)
    }

    @Test
    fun `nothing is queued for an empty field`() {
        val actions = FakeActions(resolved = "")
        val state = ChatInputState(actions)
        state.send()
        assertTrue(actions.queued.isEmpty())
    }

    /** Sending a private message to yourself is a mistake, not a feature. */
    @Test
    fun `a message addressed to the own callsign is refused`() {
        val actions = FakeActions(addressedToSelf = true)
        val state = ChatInputState(actions)
        state.value = TextFieldValue("/cq DN9APW hi")

        state.send()

        assertTrue(actions.queued.isEmpty())
        assertEquals("", state.value.text)
    }

    @Test
    fun `the category comes from the resolver`() {
        val actions = FakeActions(category = memberCategory)
        val state = ChatInputState(actions)
        state.value = TextFieldValue("/cq DL0ABC hi")

        state.send()

        assertSame(memberCategory, actions.queued.single().chatCategory)
    }

    /** If the category cannot be resolved at all, the main chat is where it goes. */
    @Test
    fun `an unresolvable category falls back to the main chat`() {
        val actions = FakeActions(category = null)
        val state = ChatInputState(actions)
        state.value = TextFieldValue("hello")

        state.send()

        assertSame(mainCategory, actions.queued.single().chatCategory)
    }

    @Test
    fun `an outgoing message is never addressed to the server`() {
        val actions = FakeActions()
        val state = ChatInputState(actions)
        state.value = TextFieldValue("hello")

        state.send()

        assertEquals(false, actions.queued.single().isMessageDirectedToServer)
    }

    /** A /cq starts the reply clock, which is what the no-reply tracking runs on. */
    @Test
    fun `sending records the outbound cq and asks for a recompute`() {
        val actions = FakeActions()
        val state = ChatInputState(actions)
        state.value = TextFieldValue("/cq DL0ABC pse 23cm")

        state.send()

        assertEquals(listOf("/cq DL0ABC pse 23cm"), actions.recordedCqs)
        assertEquals(listOf("outbound-tx"), actions.recomputeReasons)
    }

    // ---- which station the text is resolved against ------------------------

    @Test
    fun `the panel selection wins over the table`() {
        val panel = member("DL0ABC")
        val table = member("OK1XYZ")
        val actions = FakeActions(panelSelection = panel, tableSelection = table)
        val state = ChatInputState(actions)
        state.value = TextFieldValue("%call%")

        state.send()

        assertSame(panel, actions.resolvedAgainst.first())
    }

    @Test
    fun `the table selection is used when the panel has none`() {
        val table = member("OK1XYZ")
        val actions = FakeActions(tableSelection = table)
        val state = ChatInputState(actions)
        state.value = TextFieldValue("%call%")

        state.send()

        assertSame(table, actions.resolvedAgainst.first())
    }

    /**
     * Pinned as found, not repaired. On the send path the score service's selection is
     * consulted only AFTER the variables have been resolved, so a station that only it
     * knows about does not reach the resolver — while the shortcut buttons, which use
     * the three-step lookup, do see it. Repairing this would change which station a
     * variable resolves against, and therefore what gets transmitted.
     */
    @Test
    fun `on the send path the score service selection reaches the category but not the variables`() {
        val scored = member("S50ZZZ")
        val actions = FakeActions(scoreSelection = scored)
        val state = ChatInputState(actions)
        state.value = TextFieldValue("%call%")

        state.send()

        assertNull(actions.resolvedAgainst.first(), "the resolver saw a station it did not see in JavaFX")
        assertSame(scored, actions.categoryResolvedAgainst.first())
    }

    // ---- the shortcut buttons ---------------------------------------------

    /** A shortcut button APPENDS, so several can be strung together into one message. */
    @Test
    fun `a shortcut button appends to what is already typed`() {
        val actions = FakeActions(resolved = "pse sked")
        val state = ChatInputState(actions)
        state.value = TextFieldValue("/cq DL0ABC ")

        state.appendShortcut("pse sked")

        assertEquals("/cq DL0ABC pse sked", state.value.text)
    }

    @Test
    fun `a shortcut button resolves its variables before appending`() {
        val actions = FakeActions(resolved = "144.300")
        val state = ChatInputState(actions)

        state.appendShortcut("MYQRGVAR")

        assertEquals("144.300", state.value.text)
        assertEquals("MYQRGVAR ", actions.resolvedTemplates.single())
    }

    @Test
    fun `a shortcut that resolves to nothing changes nothing`() {
        val actions = FakeActions(resolved = "  ")
        val state = ChatInputState(actions)
        state.value = TextFieldValue("keep me")

        state.appendShortcut("%unset%")

        assertEquals("keep me", state.value.text)
    }

    /** The two QRG buttons append the field contents rather than a resolved template. */
    @Test
    fun `the MYQRG button appends the own frequency`() {
        val actions = FakeActions(ownQrgMain = "144.370")
        val state = ChatInputState(actions)
        state.value = TextFieldValue("qrg ")

        state.appendOwnQrg(mainCategory = true)

        assertEquals("qrg 144.370 ", state.value.text)
    }

    @Test
    fun `the SECONDQRG button appends the second frequency`() {
        val actions = FakeActions(ownQrgSecond = "432.200")
        val state = ChatInputState(actions)

        state.appendOwnQrg(mainCategory = false)

        assertEquals("432.200 ", state.value.text)
    }

    @Test
    fun `manual QRG commits keep their radio source explicit`() {
        val actions = FakeActions()
        val state = ChatInputState(actions)

        state.commitOwnQrg(mainCategory = true, "144.300")
        state.commitOwnQrg(mainCategory = false, "1296.100")

        assertEquals(
            listOf(RadioSlot.FIRST to "144.300", RadioSlot.SECOND to "1296.100"),
            actions.manualQrgs,
        )
    }

    // ---- the snippets ------------------------------------------------------

    /**
     * A snippet REPLACES the field, unlike a shortcut button: it builds a whole
     * directed message, so whatever was half typed would only corrupt it.
     */
    @Test
    fun `a snippet replaces the field with a directed message`() {
        val actions = FakeActions(panelSelection = member("DL0ABC"), resolved = "/cq DL0ABC pse 23cm")
        val state = ChatInputState(actions)
        state.value = TextFieldValue("half typed")

        state.insertSnippet(listOf("pse 23cm", "qrz?"), 0)

        assertEquals("/cq DL0ABC pse 23cm", state.value.text)
        assertEquals("/cq DL0ABC pse 23cm", actions.resolvedTemplates.single())
    }

    /** Without a station there is nobody to direct it at, so nothing happens. */
    @Test
    fun `a snippet without a selected station does nothing`() {
        val actions = FakeActions()
        val state = ChatInputState(actions)
        state.value = TextFieldValue("keep me")

        state.insertSnippet(listOf("pse 23cm"), 0)

        assertEquals("keep me", state.value.text)
        assertTrue(actions.resolvedTemplates.isEmpty())
    }

    @Test
    fun `a snippet index nobody configured does nothing`() {
        val actions = FakeActions(panelSelection = member("DL0ABC"))
        val state = ChatInputState(actions)
        state.value = TextFieldValue("keep me")

        state.insertSnippet(listOf("only one"), 5)
        state.insertSnippet(listOf("only one"), -1)

        assertEquals("keep me", state.value.text)
    }

    @Test
    fun `the snippet uses the three step station lookup`() {
        val scored = member("S50ZZZ")
        val actions = FakeActions(scoreSelection = scored, resolved = "/cq S50ZZZ hi")
        val state = ChatInputState(actions)

        state.insertSnippet(listOf("hi"), 0)

        assertSame(scored, actions.resolvedAgainst.single())
    }

    // ---- the context menu -------------------------------------------------

    /**
     * The same snippet roster reaches the field three ways, and all three differ. Ctrl+N
     * replaces the field with a directed message; a right-click APPENDS the snippet with
     * no trailing space and no /cq prefix; a shortcut button appends its own roster's
     * entry followed by a space. Pinned because collapsing any two of them would change
     * what goes out.
     */
    @Test
    fun `a context menu entry appends the resolved snippet without a trailing space`() {
        val actions = FakeActions(panelSelection = member("DL0ABC"), resolved = "pse 23cm")
        val state = ChatInputState(actions)
        state.value = TextFieldValue("/cq DL0ABC ")

        state.appendSnippet("pse 23cm")

        assertEquals("/cq DL0ABC pse 23cm", state.value.text)
        assertEquals("pse 23cm", actions.resolvedTemplates.single(), "a space was added to the template")
    }

    @Test
    fun `a context menu entry needs no selected station`() {
        val actions = FakeActions(resolved = "qrz?")
        val state = ChatInputState(actions)

        state.appendSnippet("qrz?")

        assertEquals("qrz?", state.value.text)
    }

    @Test
    fun `a context menu entry that resolves to nothing changes nothing`() {
        val actions = FakeActions(resolved = "")
        val state = ChatInputState(actions)
        state.value = TextFieldValue("keep me")

        state.appendSnippet("%unset%")

        assertEquals("keep me", state.value.text)
    }

    // ---- the caret and the focus ------------------------------------------

    /**
     * After every insertion the caret sits at the end and the field takes the focus
     * back — the operator is mid sentence, and a caret left at position 0 turns the
     * next keystroke into a corrupted message.
     */
    @Test
    fun `an insertion puts the caret at the end`() {
        val actions = FakeActions(resolved = "pse sked")
        val state = ChatInputState(actions)
        state.value = TextFieldValue("/cq DL0ABC ", selection = TextRange(0))

        state.appendShortcut("pse sked")

        assertEquals(state.value.text.length, state.value.selection.start)
        assertTrue(state.value.selection.collapsed)
    }

    @Test
    fun `an insertion asks for the focus back`() {
        val actions = FakeActions(resolved = "x")
        val state = ChatInputState(actions)
        val before = state.focusRequests

        state.appendShortcut("x")

        assertTrue(state.focusRequests > before)
    }

    @Test
    fun `a refused insertion does not steal the focus`() {
        val actions = FakeActions(resolved = "  ")
        val state = ChatInputState(actions)
        val before = state.focusRequests

        state.appendShortcut("%unset%")

        assertEquals(before, state.focusRequests)
    }

    // ---- clearing ----------------------------------------------------------

    @Test
    fun `clear empties the field without sending`() {
        val actions = FakeActions()
        val state = ChatInputState(actions)
        state.value = TextFieldValue("unsent")

        state.clear()

        assertEquals("", state.value.text)
        assertTrue(actions.queued.isEmpty())
    }

    private fun member(call: String): ChatMember = ChatMember().apply {
        setCallSign(call)
        setChatCategory(memberCategory)
    }

    /** Real behaviour, recorded; no mocking framework needed for a boundary this small. */
    private inner class FakeActions(
        private val panelSelection: ChatMember? = null,
        private val tableSelection: ChatMember? = null,
        private val scoreSelection: ChatMember? = null,
        private val resolved: String? = null,
        private val addressedToSelf: Boolean = false,
        private val category: ChatCategory? = null,
        private val ownQrgMain: String = "",
        private val ownQrgSecond: String = "",
    ) : ChatInputActions {

        val queued = mutableListOf<ChatMessage>()
        val resolvedAgainst = mutableListOf<ChatMember?>()
        val categoryResolvedAgainst = mutableListOf<ChatMember?>()
        val resolvedTemplates = mutableListOf<String>()
        val recordedCqs = mutableListOf<String>()
        val recomputeReasons = mutableListOf<String>()
        val manualQrgs = mutableListOf<Pair<RadioSlot, String>>()

        override fun panelSelection(): ChatMember? = panelSelection
        override fun tableSelection(): ChatMember? = tableSelection
        override fun scoreSelection(): ChatMember? = scoreSelection

        override fun resolveVariables(template: String, member: ChatMember?): String? {
            resolvedAgainst += member
            resolvedTemplates += template
            return resolved ?: template
        }

        override fun isAddressedToOwnCallsign(text: String): Boolean = addressedToSelf

        override fun resolveCategory(text: String, member: ChatMember?): ChatCategory? {
            categoryResolvedAgainst += member
            return category
        }

        override fun mainCategory(): ChatCategory = this@ChatInputStateTest.mainCategory
        override fun recordOutboundCq(text: String) { recordedCqs += text }
        override fun requestRecompute(reason: String) { recomputeReasons += reason }
        override fun queue(message: ChatMessage) { queued += message }
        override fun ownQrgMain(): String = ownQrgMain
        override fun ownQrgSecond(): String = ownQrgSecond
        override fun applyManualQrg(slot: RadioSlot, value: String) { manualQrgs += slot to value }
    }
}
