package kst4contest.view.compose

import java.awt.EventQueue
import java.lang.reflect.InvocationTargetException

/*
 * An ImageComposeScene must live on the AWT event thread, the thread the application renders
 * on. Compose Multiplatform 1.8.2 sends the global snapshot notifications there
 * (GlobalSnapshotManager), and doing so takes two locks in the opposite order from a render
 * on any other thread:
 *
 *  - the render holds the scene's FlushCoroutineDispatcher runLock while a task it runs
 *    (here LazyListState.scrollToItem) measures and wants the SnapshotStateObserver's
 *    observedScopeMapsLock;
 *  - the event thread holds observedScopeMapsLock in drainChanges while a derived state it
 *    re-reads wakes a snapshotFlow collector of that scene, whose dispatch wants runLock.
 *
 * Both waited forever in CI runs 40252077018, 40252089738 and 40299943329; the thread dump of
 * the last one shows both stacks. On one thread the two monitors are simply re-entered, which
 * is why the application never hangs this way.
 */

/**
 * Runs [block] on the AWT event thread, waits for it and returns its result. Whatever
 * [block] throws is rethrown here unwrapped, so a failed assertion still reads as one.
 */
internal fun <T> onEventThread(block: () -> T): T {
    if (EventQueue.isDispatchThread()) return block()

    var result: Result<T>? = null
    try {
        EventQueue.invokeAndWait { result = runCatching(block) }
    } catch (wrapped: InvocationTargetException) {
        throw wrapped.cause ?: wrapped
    }
    return result!!.getOrThrow()
}
