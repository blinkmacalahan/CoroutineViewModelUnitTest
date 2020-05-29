package com.example.coroutineviewmodelunittesting

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class SimpleViewModelTest {

    @ExperimentalCoroutinesApi
    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that only constructing SimpleViewModel will not populate the sumData value`() {
        Dispatchers.setMain(DefaultDispatcherProvider.testDispatcher)
        val simpleViewModel = SimpleViewModel()
        simpleViewModel.fetchSumData(100)
        assert(simpleViewModel.sumData.value == null)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `test that runBlockTest with setting TestCoroutineDispatcher as DispatcherMain fails`() {
        Dispatchers.setMain(DefaultDispatcherProvider.testDispatcher)
        val simpleViewModel = SimpleViewModel()
        runBlockingTest {
            simpleViewModel.fetchSumData(100)
        }
        assert(simpleViewModel.sumData.value == null)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `test that using the TestCoroutineDispatcher and its runBlockingTest works`() {
        val simpleViewModel = SimpleViewModel()
        DefaultDispatcherProvider.testDispatcher.runBlockingTest {
            simpleViewModel.fetchSumData(100)
        }
        assert(simpleViewModel.sumData.value == 4950)
        DefaultDispatcherProvider.testDispatcher.cleanupTestCoroutines()
    }

    /**
     * Verifies that using [LiveData.postValue] will cause the value to not be set after the coroutine completes
     */
    @Test
    fun `test live data post value fails`() {
        val simpleViewModel = SimpleViewModel()

        runBlocking {
            simpleViewModel.fetchSumPostData(100, Dispatchers.IO)
            // This delay is completely dependent the value passed to fetchSumPostData
            delay(12000)
        }

        assert(simpleViewModel.sumPostData.value == null)
    }

    /**
     * Fixes [test live data post value fails] by advancing the system clock using [ShadowLooper.runMainLooperToNextTask]
     * in order for the [LiveData.postValue]'s runnable to be executed and the result `value` set.
     */
    @Test
    fun `test live data post value`() {
        val simpleViewModel = SimpleViewModel()
        runBlocking {
            simpleViewModel.fetchSumPostData(100, Dispatchers.IO)
            // This delay is completely dependent the value passed to fetchSumPostData
            delay(12000)
        }

        // We MUST call this in order for the [LiveData.postValue] to get processed and have its resulting `value`
        // set.
        ShadowLooper.runMainLooperToNextTask()
        assert(simpleViewModel.sumPostData.value == 4950)
    }

    /**
     * Similar to [test live data post value] but uses [DefaultDispatcherProvider.testDispatcher.runBlockingTest]
     * to speed up the test since [delay] are skipped/posted immediately
     */
    @ExperimentalCoroutinesApi
    @Test
    fun `test live data post value with runBlockingTest`() {
        val simpleViewModel = SimpleViewModel()
        DefaultDispatcherProvider.testDispatcher.runBlockingTest {
            simpleViewModel.fetchSumPostData(100, DefaultDispatcherProvider.io())
        }

        // We MUST call this in order for the [LiveData.postValue] to get processed and have its resulting `value`
        // set.
        ShadowLooper.runMainLooperToNextTask()
        assert(simpleViewModel.sumPostData.value == 4950)
    }
}

@RunWith(RobolectricTestRunner::class)
class SimpleViewModelTestAgain {

    @Rule @JvmField var activityActivityTestRule = InstantTaskExecutorRule()

    /**
     * Fixes [SimpleViewModelTest.test live data post value fails] in a different way than
     * [SimpleViewModelTest.test live data post value]. Rather than using [ShadowLooper.runMainLooperToNextTask] to
     * advance the system clock in order to process the [LiveData.postValue] synchronously, we use [InstantTaskExecutorRule].
     */
    @Test
    fun `test live data post value succeeds with InstantTaskExecutorRule`() {
        val simpleViewModel = SimpleViewModel()

        runBlocking {
            simpleViewModel.fetchSumPostData(100, Dispatchers.IO)
            // This delay is completely dependent the value passed to fetchSumPostData
            delay(12000)
        }

        assert(simpleViewModel.sumPostData.value == 4950)
    }
}