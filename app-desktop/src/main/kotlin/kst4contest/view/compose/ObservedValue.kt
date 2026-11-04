package kst4contest.view.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kst4contest.observe.ObservableValue
import java.util.function.Consumer

/**
 * An [ObservableValue] from the domain layer, mirrored into Compose state.
 *
 * The domain's values notify their own listeners, none of which Compose can see, so a view
 * drawn straight from one renders once and then silently goes stale. Three of them move on
 * their own while the operator watches: the rotator writes the antenna heading, and the
 * frequency follower writes the operator's own QRG.
 *
 * The JavaFX window did exactly this with attachQtfFollower() and attachOwnQrgFollower(),
 * ending in a TextField; this one ends in Compose state.
 *
 * [release] must be called when the mirror is dropped. A profile switch builds a new window
 * over the same preferences, and a leaked listener keeps the old one alive and writing.
 */
class ObservedValue<T>(private val source: ObservableValue<T>) {

    var value: T? by mutableStateOf(source.get())
        private set

    private val follower = Consumer<T> { updated -> value = updated }

    init {
        source.addListener(follower)
    }

    fun release() {
        source.removeListener(follower)
    }
}

/** Observes a domain value only for the lifetime of the current composition. */
@Composable
internal fun <T> ObservableValue<T>.observeAsState(): State<T?> {
    val state = remember(this) { mutableStateOf(get()) }

    DisposableEffect(this) {
        val follower = Consumer<T> { updated -> state.value = updated }
        addListener(follower)
        // Close the gap between the initial read and listener registration.
        state.value = get()
        onDispose { removeListener(follower) }
    }

    return state
}
