package com.example.coroutineviewmodelunittesting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    /**
     * Verifies that [delay] is properly observed and not shortened to 0ms or skipped.
     *
     * Verifies using [runBlocking] with [MyAdder.addUpTo] works as expected and doesn't stall for infinity. This
     * is because we haven't introduced [Dispatchers.Main] and don't need to manipulate it's scheduler for posted
     * runnables.
     */
    @ExperimentalTime
    @Test
    fun `testing with delay enabled`() {
        val adder = MyAdder()
        var result = -1

        val duration = runBlocking {
            measureTime {
                result = adder.addUpTo(100)
            }
        }
        assert(result > 0)
        println("Duration = $duration")
        assert(duration.inMilliseconds > 9000)
    }

    /**
     * Verifies that [delay] is shortened to 0ms/skipped because we used [runBlockingTest]
     */
    @ExperimentalCoroutinesApi
    @ExperimentalTime
    @Test
    fun `testing with delay disabled`() {
        val adder = MyAdder()
        var result = -1

        val duration = measureTime {
            runBlockingTest {
                result = adder.addUpTo(100)
            }
        }
        assert(result > 0)
        println("Duration = $duration")
        assert(duration.inMilliseconds < 9000)
    }

    /**
     * Verifies [MyAdder.addUpToDeferredAsync] will stall indefinitely due to using [DefaultDispatcherProvider.testDispatcher]
     * for it's [Dispatchers.Main] by way of [Dispatchers.setMain]
     */
    @Test(timeout = 20000, expected = Exception::class)
    @ExperimentalCoroutinesApi
    @ExperimentalTime
    fun `testing with deferred`() {
        Dispatchers.setMain(DefaultDispatcherProvider.testDispatcher)
        val adder = MyAdder()
        var result = Integer.MIN_VALUE

        runBlocking {
            measureTime { result = adder.addUpToDeferredAsync(100).await() }
        }

        // We're expecting this method to timeout and throw an exception. If for reason it doesn't then
        // this test should fail. We'll make the test fail by asserting the result has changed.
        assert(result != Int.MIN_VALUE)
    }

    /**
     * Verifies [MyAdder.addUpToDeferredAsync] will stall indefinitely due it using [DefaultDispatcherProvider]
     * which in `test` flavor uses [TestCoroutineDispatcher] for all dispatchers. Using [DefaultDispatcherProvider.io],
     * [DefaultDispatcherProvider.main], [DefaultDispatcherProvider.io], [DefaultDispatcherProvider.unconfined]
     * should all fail
     */
    @Test(timeout = 20000, expected = Exception::class)
    @ExperimentalCoroutinesApi
    @ExperimentalTime
    fun `testing with deferred on dispatcher io`() {
        val adder = MyAdder(CustomCoroutineScope(DefaultDispatcherProvider.io()))
        var result = Int.MIN_VALUE

        runBlocking {
            measureTime { result = adder.addUpToDeferredAsync(100).await() }
        }
        // We're expecting this method to timeout and throw an exception. If for reason it doesn't then
        // this test should fail. We'll make the test fail by asserting the result has changed.
        assert(result != Int.MIN_VALUE)
    }

    /**
     * Verifies [MyAdder.addUpToDeferredAsync] will stall indefinitely due it using [DefaultDispatcherProvider]
     * which in `test` flavor uses [TestCoroutineDispatcher] for all dispatchers. Using [DefaultDispatcherProvider.io],
     * [DefaultDispatcherProvider.main], [DefaultDispatcherProvider.io], [DefaultDispatcherProvider.unconfined]
     * should all fail
     */
    @Test(timeout = 20000, expected = Exception::class)
    @ExperimentalCoroutinesApi
    @ExperimentalTime
    fun `testing with deferred on dispatcher default`() {
        val adder = MyAdder(CustomCoroutineScope(DefaultDispatcherProvider.default()))
        var result = Int.MIN_VALUE

        runBlocking {
            measureTime { result = adder.addUpToDeferredAsync(100).await() }
        }
        // We're expecting this method to timeout and throw an exception. If for reason it doesn't then
        // this test should fail. We'll make the test fail by asserting the result has changed.
        assert(result != Int.MIN_VALUE)
    }

    /**
     * There is currently a bug with [runBlockingTest] when changing contexts to other dispatchers. See
     * https://github.com/Kotlin/kotlinx.coroutines/issues/1204 for details.
     */
    @ExperimentalCoroutinesApi
    @Test(expected = IllegalStateException::class)
    fun `testing with dispatchers with runBlockingTest`() {
        val adder = MyAdder()
        var result = -1
        runBlockingTest {
            result = adder.addUpTo(100, Dispatchers.Main)
        }
        assert(result > 0)
    }

    /**
     * Verifies that by using [TestCoroutineDispatcher] to execute [runBlockingTest] we can avoid the [IllegalStateException]
     * that occurs with [testing with dispatchers with runBlockingTest]
     */
    @ExperimentalCoroutinesApi
    @Test
    fun `testing with dispatchers with runBlockingTest FIXED`() {
        val adder = MyAdder()
        var result = -1
        DefaultDispatcherProvider.testDispatcher.runBlockingTest {
            result = adder.addUpTo(100, DefaultDispatcherProvider.main())
        }
        assert(result > 0)
    }

    /**
     * Verifies that calling [MyAdder.addUpTo] with a dispatcher will cause it to block indefinitely. As discussed
     * before, this is due to it using `delay` and the scheduler not getting advanced. However, this test tries to advance
     * the system time using [DefaultDispatcherProvider.testDispatcher.advanceUntilIdle] but since we never return
     * from [MyAdder.addUpTo] we don't get the change to advance the system time.
     */
    @ExperimentalCoroutinesApi
    @Test(timeout = 20000, expected = Exception::class)
    fun `testing with runBlocking with TestCoroutineDispatcher`() {
        Dispatchers.setMain(DefaultDispatcherProvider.testDispatcher)
        val adder = MyAdder()
        var result = Int.MIN_VALUE
        runBlocking(Dispatchers.Main) {
            result = adder.addUpTo(100, DefaultDispatcherProvider.io())
            // This is required else the `delay` will not advance
            DefaultDispatcherProvider.testDispatcher.advanceUntilIdle()
        }

        assert(result != Int.MIN_VALUE)
    }

    /**
     * Fixes [testing with runBlocking with TestCoroutineDispatcher] by using [launch] to call [MyAdder.addUpTo]
     * as this prevents waiting for it's coroutine to complete. This allows [DefaultDispatcherProvider.testDispatcher.advanceUntilIdle]
     * to do its magic.
     *
     * Helpful link about why `delay` doesn't work in runBlocking: https://github.com/Kotlin/kotlinx.coroutines/issues/1066
     * If you're using a TestCoroutineDispatcher, it has its own way of posting runnables (which is what delay does).
     * When using runBlockingTest it will auto advance time; however, if you're using runBlocking, you need to advance
     * the time yourself.
     */
    @Test
    fun `testing with runBlocking with TestCoroutineDispatcher FIXED`() {
        Dispatchers.setMain(DefaultDispatcherProvider.testDispatcher)
        val adder = MyAdder()
        var result = -1
        runBlocking(Dispatchers.Main) {
            launch {
                result = adder.addUpTo(100, DefaultDispatcherProvider.io())
            }
            // This is required else the `delay` will not advance
            DefaultDispatcherProvider.testDispatcher.advanceUntilIdle()
        }

        assert(result > 0)
    }
}
