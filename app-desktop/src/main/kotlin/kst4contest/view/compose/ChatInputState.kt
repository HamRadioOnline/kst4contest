package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kst4contest.controller.RadioSlot
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage

/**
 * Everything the send line needs from the rest of the application.
 *
 * An interface rather than a handful of lambdas because there are twelve of them and
 * they belong together; the implementation is the runtime, the test implements it with
 * recording doubles.
 */
interface ChatInputActions {

    /** The station shown in the info panel, set by clicking one. */
    fun panelSelection(): ChatMember?

    fun tableSelection(): ChatMember?

    fun scoreSelection(): ChatMember?

    /** Replaces %variables% in a template; may legitimately return blank. */
    fun resolveVariables(template: String, member: ChatMember?): String?

    /** Whether an outgoing private message targets the local station. */
    fun isAddressedToOwnCallsign(text: String): Boolean

    /** Which chat the message belongs in; the /cq target in the text outranks the selection. */
    fun resolveCategory(text: String, member: ChatMember?): ChatCategory?

    fun mainCategory(): ChatCategory?

    fun recordOutboundCq(text: String)

    fun requestRecompute(reason: String)

    /** Hands the message to the TX queue; the existing mechanism sends it. */
    fun queue(message: ChatMessage)

    fun ownQrgMain(): String

    fun ownQrgSecond(): String

    fun applyManualQrg(slot: RadioSlot, value: String) {}
}

/**
 * The line the operator types into, and the buttons around it.
 *
 * Three things go into this field, and they behave differently on purpose. A shortcut
 * button APPENDS, so several can be strung together into one message. A snippet
 * REPLACES, because it builds a whole directed message and a half-typed remainder would
 * only corrupt it. Typing is typing.
 */
class ChatInputState(private val actions: ChatInputActions) {

    /** Text and caret together, because every insertion also moves the caret. */
    var value: TextFieldValue by mutableStateOf(TextFieldValue(""))

    /**
     * Bumped whenever the field should take the focus back. The Compose counterpart of
     * requestFocus() + selectEnd(): the operator is mid sentence, and a field that lost
     * the focus to a button turns the next keystroke into nothing.
     */
    var focusRequests: Int by mutableStateOf(0)
        private set

    /**
     * Resolves, checks and queues the message, then empties the field.
     *
     * The order is the original's, including one inconsistency that is kept because
     * changing it would change what gets transmitted: the score service's selection is
     * consulted only after the variables have been resolved, so a station only it knows
     * about reaches the category decision but not the resolver.
     */
    fun send() {
        var member = actions.panelSelection() ?: actions.tableSelection()

        val resolved = actions.resolveVariables(value.text, member)

        // A variable may legitimately resolve to an empty string; then there is nothing
        // to send, and the field is emptied anyway so the operator sees it was consumed.
        if (resolved.isNullOrBlank()) {
            clear()
            return
        }

        if (actions.isAddressedToOwnCallsign(resolved)) {
            clear()
            return
        }

        if (member == null) member = actions.scoreSelection()

        val category = actions.resolveCategory(resolved, member) ?: actions.mainCategory()

        val message = ChatMessage()
        message.chatCategory = category
        message.messageText = resolved
        message.isMessageDirectedToServer = false

        // A /cq starts the reply clock the no-reply tracking runs on.
        actions.recordOutboundCq(resolved)
        actions.requestRecompute("outbound-tx")

        actions.queue(message)
        clear()
    }

    fun clear() {
        value = TextFieldValue("")
    }

    /**
     * Appends a shortcut's resolved text. A template that resolves to nothing leaves the
     * field — and the focus — alone.
     */
    fun appendShortcut(template: String) {
        val resolved = actions.resolveVariables("$template ", effectiveStation())
        if (resolved.isNullOrBlank()) return
        appendAndFocus(resolved)
    }

    /**
     * Appends a snippet chosen from a right-click menu, resolved, with no trailing space
     * and no /cq prefix.
     *
     * The same roster reaches the field three ways and all three differ: Ctrl+1..Ctrl+0
     * replaces the field with a whole directed message, a right-click appends just the
     * snippet, and a shortcut button appends its own roster's entry followed by a space.
     * Collapsing any two of them would change what goes out over the air.
     */
    fun appendSnippet(snippet: String) {
        val resolved = actions.resolveVariables(snippet, effectiveStation())
        if (resolved.isNullOrBlank()) return
        appendAndFocus(resolved)
    }

    /**
     * Appends the operator's own frequency for one of the two chats. These two buttons
     * take the value straight from its field rather than through the resolver, which is
     * why they are not ordinary shortcuts.
     */
    fun appendOwnQrg(mainCategory: Boolean) {
        val qrg = if (mainCategory) actions.ownQrgMain() else actions.ownQrgSecond()
        appendAndFocus("$qrg ")
    }

    fun commitOwnQrg(mainCategory: Boolean, value: String) {
        actions.applyManualQrg(if (mainCategory) RadioSlot.FIRST else RadioSlot.SECOND, value)
    }

    /**
     * Replaces the field with "/cq CALL <snippet>", resolved.
     *
     * Without a selected station there is nobody to direct it at, so nothing happens —
     * silently, as before, because this runs from Ctrl+1..Ctrl+0 while the operator is
     * typing and a dialog would be worse than nothing.
     */
    fun insertSnippet(snippets: List<String>, index: Int) {
        val station = effectiveStation() ?: return
        val callSign = station.callSign ?: return
        if (index < 0 || index >= snippets.size) return

        val resolved = actions.resolveVariables("/cq $callSign ${snippets[index]}", station)
        if (resolved.isNullOrBlank()) return

        setAndFocus(resolved)
    }

    private var lastAutoPreparedSendText: String = ""
    
    /**
     * Replaces the field with "/cq CALL ", if the field is empty or contains a previous auto-generated CQ.
     */
    fun prepareCq(callSign: String, forceOverwrite: Boolean = false) {
        if (callSign.isBlank()) return
        val preparedText = "/cq ${callSign.trim()} "
        
        val canOverwrite = value.text.isEmpty() || value.text == lastAutoPreparedSendText
        if (!forceOverwrite && !canOverwrite) {
            return
        }
        
        lastAutoPreparedSendText = preparedText
        setAndFocus(preparedText)
    }

    /** The three-step lookup the shortcuts and snippets use. */
    private fun effectiveStation(): ChatMember? =
        actions.panelSelection() ?: actions.tableSelection() ?: actions.scoreSelection()

    private fun appendAndFocus(text: String) = setAndFocus(value.text + text)

    private fun setAndFocus(text: String) {
        value = TextFieldValue(text = text, selection = TextRange(text.length))
        focusRequests++
    }
}
