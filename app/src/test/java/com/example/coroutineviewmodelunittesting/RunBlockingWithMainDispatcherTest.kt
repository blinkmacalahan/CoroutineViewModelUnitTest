package com.example.coroutineviewmodelunittesting

import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class RunBlockingWithMainDispatcherTest {

    /**
     * Verifies that [MyAdder.addUpTo] will never complete due to it being called on [Dispatchers.Main] inside a
     * [runBlocking] coroutine.
     *
     * [MyAdder.addUpTo] never succeeds in this test due to the nature of`idleMainLooper` and that it only execute
     * [Runnable]s that are immediately ready aka executed with `post`. Any Runnable which is scheduled at a future
     * system time won't be executed because the system clock isn't advanced by `idleMainLooper`
     *
     * To make this test pass, we put a timeout on the unit test for 20 seconds. Since our method will never return,
     * an [Exception] is thrown.
     */
    @Test(timeout = 20000, expected = Exception::class)
    fun `testing with idleMainLooper`() {
        val adder = MyAdder()
        var result = -1
        runBlocking {
            launch(Dispatchers.Main) {
                result = adder.addUpTo(2)
            }
            for (i in 0 until 2) {
                ShadowLooper.idleMainLooper()
                delay(100)
            }
        }

        assert(result > 0)
    }

    /**
     * This test succeeds because `runMainLooperToNextTask` will advance the clock to the next future Runnable and
     * execute it. Since `addUpTo` uses `delay` (and `delay` internally is just a Runnable posted for the future), in
     * order to properly unit test `addUpTo` using `runBlocking`, we need to advance the system time ourselves.
     *
     * [MyAdder.addUpTo] makes multiple calls to [delay] which in turn creates
     * several Runnables queued at various system times. An interesting problem [MyAdder.addUpTo] also creates, is
     * the [delay]s created by this method aren't queued or execute all at once. It won't schedule the next [delay] until the
     * previous one completes.
     * Therefore, (afaik) there is no method we can call on the [ShadowLooper] to help us get through all of them since
     * it doesn't know about the ones to be schedule in the future (since they haven't been created).
     * Instead, we can process each queued [delay] one at a time and continue to do this until all delays have been
     * added. This is done by watching the [Scheduler] and processing the runnables on the main loop until the [Scheduler]
     * is empty.
     */
    @Test
    fun `testing with runMainLooperToNextTask`() {
        val adder = MyAdder()
        var result = -1
        runBlocking {
            launch(Dispatchers.Main) {
                result = adder.addUpTo(2)
            }

            while (shadowOf(Looper.getMainLooper()).scheduler.size() > 0) {
                ShadowLooper.runMainLooperToNextTask()
            }
        }

        assert(result > 0)
    }

    /**
     * Similar to [testing with runMainLooperToNextTask]; however, `runMainLooperOneTask` will only run EXACTLY one
     * queued runnable. Since [MyAdder.addUpTo] doesn't queue multiple delays/runnables for the same future time, it
     * doesn't make a difference if we use [ShadowLooper.runMainLooperToNextTask] or [ShadowLooper.runMainLooperOneTask]
     *
     * As [testing with runMainLooperToNextTask] describes though, we still need to iterate of the [Scheduler]
     * in order to execute all [delay] which will occur.
     */
    @Test
    fun `testing with runMainLooperOneTask`() {
        val adder = MyAdder()
        var result = -1
        runBlocking {
            launch(Dispatchers.Main) {
                result = adder.addUpTo(2)
            }

            while (shadowOf(Looper.getMainLooper()).scheduler.size() > 0) {
                ShadowLooper.runMainLooperOneTask()
            }
        }

        assert(result > 0)
    }

    /**
     * Verifies that using [ShadowLooper.runMainLooperOneTask] while calling [MyAdder.addUpToWithCallback] fails to work properly.
     * This is because [MyAdder.addUpToWithCallback] queues 2 runnables for the same future system time.
     * Since [ShadowLooper.runMainLooperOneTask] will execute 1 runnable, the other doesn't execute which prevents the `callback`
     * for executing.
     */
    @Test
    fun `test runnables queued at same time with runMainLooperOneTask`() {
        val adder = MyAdder()
        var callbackCompleted = false
        adder.addUpToWithCallback(100, object : (Int) -> Unit {
            override fun invoke(sum: Int) {
                callbackCompleted = true
            }
        }, 100L)
        ShadowLooper.runMainLooperOneTask()
        assert(callbackCompleted == false)
    }

    /**
     * Verifies that using [ShadowLooper.runMainLooperToNextTask] properly tests [MyAdder.addUpToWithCallback].
     *
     * [MyAdder.addUpToWithCallback] queues 2 runnables for the same future and since
     * [ShadowLooper.runMainLooperToNextTask] execute all [Runnable]s scheduled at the same future system time,
     * both runnables used by the method will execute and the `callback` will be executed.
     */
    @Test
    fun `test runnables queued at same time with runMainLooperToNextTask`() {
        val adder = MyAdder()
        var result = -1
        adder.addUpToWithCallback(100, object : (Int) -> Unit {
            override fun invoke(sum: Int) {
                result = sum
            }
        }, 100L)
        ShadowLooper.runMainLooperToNextTask()
        assert(result > 0)
    }
}