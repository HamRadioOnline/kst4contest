package kst4contest.view;

import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatMessage;
import kst4contest.observe.DirectUiDispatcher;
import kst4contest.observe.SimpleRoster;
import kst4contest.view.compose.SelectedMessageFilter;
import kst4contest.view.compose.SelectedStationMessagePredicate;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RosterListBindingTest {

    @Test
    void theMirrorStartsWithTheCurrentContent() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        roster.add("DN9APW");

        RosterListBinding<String> binding = RosterListBinding.mirror(roster, new DirectUiDispatcher());

        assertEquals(List.of("DN9APW"), List.copyOf(binding.list()));
    }

    @Test
    void theMirrorFollowsLaterChanges() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        RosterListBinding<String> binding = RosterListBinding.mirror(roster, new DirectUiDispatcher());

        roster.add("DN9APW");
        roster.add("DO5AMF");

        assertEquals(List.of("DN9APW", "DO5AMF"), List.copyOf(binding.list()));
    }

    @Test
    void theMirrorIsTheSameInstanceThroughout() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        RosterListBinding<String> binding = RosterListBinding.mirror(roster, new DirectUiDispatcher());
        List<String> list = binding.list();

        roster.add("DN9APW");

        assertTrue(list == binding.list(),
                "a caller holds on to the instance it was given; it must not be replaced");
    }

    @Test
    void disposeStopsFollowingAndReleasesTheListener() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        RosterListBinding<String> binding = RosterListBinding.mirror(roster, new DirectUiDispatcher());

        binding.dispose();
        roster.add("DN9APW");

        assertTrue(binding.list().isEmpty(),
                "a disposed binding must not keep the old runtime alive across a profile switch");
    }

    @Test
    void anUnchangedResultNotifiesNobody() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        roster.add("DN9APW");
        RosterListBinding<String> binding = RosterListBinding.mirror(roster, new DirectUiDispatcher());

        AtomicInteger changes = new AtomicInteger();
        binding.onChanged(rows -> changes.incrementAndGet());

        binding.refresh();
        binding.refresh();

        assertEquals(0, changes.get(),
                "an unchanged station list must not make the message pane drop its selection");
    }

    @Test
    void aRealChangeNotifiesOnceWithTheNewContent() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        roster.add("DN9APW");
        RosterListBinding<String> binding = RosterListBinding.mirror(roster, new DirectUiDispatcher());

        List<List<String>> seen = new java.util.ArrayList<>();
        binding.onChanged(seen::add);

        roster.add("DO5AMF");

        // Nothing covered this before: a mirror that never notifies leaves the pane blank.
        assertEquals(1, seen.size(), "a real change must notify exactly once");
        assertEquals(List.of("DN9APW", "DO5AMF"), seen.get(0));
    }

    @Test
    void aDerivedBindingAppliesTheDerivationAndFollowsItsSources() {
        SimpleRoster<String> members = new SimpleRoster<>();
        SimpleRoster<String> filters = new SimpleRoster<>();
        members.add("DO5AMF");
        members.add("DN9APW");

        RosterListBinding<String> binding = RosterListBinding.derived(
                new DirectUiDispatcher(),
                () -> members.snapshot().stream()
                        .filter(call -> filters.snapshot().isEmpty() || filters.snapshot().contains(call))
                        .sorted(Comparator.naturalOrder())
                        .toList(),
                members, filters);

        assertEquals(List.of("DN9APW", "DO5AMF"), List.copyOf(binding.list()));

        filters.add("DO5AMF");
        assertEquals(List.of("DO5AMF"), List.copyOf(binding.list()),
                "a change of a source roster must recompute the derived content");

        members.add("DL1ABC");
        assertEquals(List.of("DO5AMF"), List.copyOf(binding.list()));
    }

    @Test
    void refreshRecomputesWithoutASourceChange() {
        SimpleRoster<String> members = new SimpleRoster<>();
        members.add("DN9APW");
        members.add("DO5AMF");
        String[] wanted = { "DN9APW" };

        RosterListBinding<String> binding = RosterListBinding.derived(
                new DirectUiDispatcher(),
                () -> members.snapshot().stream().filter(call -> call.equals(wanted[0])).toList(),
                members);

        assertEquals(List.of("DN9APW"), List.copyOf(binding.list()));

        wanted[0] = "DO5AMF";
        binding.refresh();

        assertEquals(List.of("DO5AMF"), List.copyOf(binding.list()),
                "a newly set filter must be visible without waiting for the next roster change");
    }

    @Test
    void selectedStationMessagesFollowNewMessagesAndSelectionChanges() {
        ChatMember first = member("DL1FIRST", 2);
        ChatMember second = member("DL2SECOND", 2);
        SimpleRoster<ChatMessage> messages = new SimpleRoster<>();
        AtomicReference<Predicate<ChatMessage>> filter = new AtomicReference<>(
                SelectedStationMessagePredicate.forSelection(
                        SelectedMessageFilter.PUBLIC, first, "DL0OWN"));

        RosterListBinding<ChatMessage> binding = RosterListBinding.derived(
                new DirectUiDispatcher(),
                () -> messages.snapshot().stream().filter(filter.get()).toList(),
                messages);

        ChatMessage unrelated = publicMessage(second);
        messages.add(unrelated);
        assertTrue(binding.list().isEmpty());

        ChatMessage matching = publicMessage(first);
        messages.add(matching);
        assertEquals(List.of(matching), List.copyOf(binding.list()));

        filter.set(SelectedStationMessagePredicate.forSelection(
                SelectedMessageFilter.PUBLIC, second, "DL0OWN"));
        binding.refresh();
        assertEquals(List.of(unrelated), List.copyOf(binding.list()),
                "the former station's messages must disappear immediately");

        ChatMessage next = publicMessage(second);
        messages.add(next);
        assertEquals(List.of(unrelated, next), List.copyOf(binding.list()));
    }

    @Test
    void aDisposedDerivedBindingDetachesFromEverySource() {
        SimpleRoster<String> members = new SimpleRoster<>();
        SimpleRoster<String> filters = new SimpleRoster<>();

        RosterListBinding<String> binding = RosterListBinding.derived(
                new DirectUiDispatcher(), members::snapshot, members, filters);

        binding.dispose();
        members.add("DN9APW");
        filters.add("DN9APW");

        assertTrue(binding.list().isEmpty());
    }

    private static ChatMember member(String callSign, int category) {
        ChatMember member = new ChatMember();
        member.setCallSign(callSign);
        member.setChatCategory(new ChatCategory(category));
        return member;
    }

    private static ChatMessage publicMessage(ChatMember sender) {
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(member("ALL", sender.getChatCategory().getCategoryNumber()));
        message.setChatCategory(sender.getChatCategory());
        message.setMessageText("test");
        return message;
    }
}
