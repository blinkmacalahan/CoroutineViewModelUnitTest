package com.example.coroutineviewmodelunittesting

import android.os.Process
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread

private val CPU_COUNT = Runtime.getRuntime().availableProcessors()

/**
 * Maximum parallel running tasks at one time.  Definition Stolen from Android's AsyncTask
 */
private val MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1

/**
 * Dispatcher for Background Coroutines tasks on Android
 */
val BackgroundDispatcher = ForkJoinPool(
    MAXIMUM_POOL_SIZE,
    { pool ->
        BackgroundForkJoinWorkerThread(pool)
    },
    null,
    false
).asCoroutineDispatcher()

private class BackgroundForkJoinWorkerThread(pool: ForkJoinPool) : ForkJoinWorkerThread(pool) {
    /**
     * Initializes internal state after construction but before
     * processing any tasks. If you override this method, you must
     * invoke `super.onStart()` at the beginning of the method.
     * Initialization requires care: Most fields must have legal
     * default values, to ensure that attempted accesses from other
     * threads work correctly even before this thread starts
     * processing tasks.
     */
    override fun onStart() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        super.onStart()
    }
}