package com.example.coroutineviewmodelunittesting

import android.os.Handler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MyAdder(val customCoroutineScope: CustomCoroutineScope = CustomCoroutineScope(DefaultDispatcherProvider.main())) {
    private val handler = Handler()
    private var runnable: Runnable? = null

    inner class SumRunnable(
        private val value: Int,
        private val callback: (result: Int) -> Unit,
        callbackDelay: Long
    ) : Runnable {

        private val handler = Handler()
        private var result: Int = 0

        init {
            handler.postDelayed(this, callbackDelay)
            handler.postDelayed(Runnable {
                callback.invoke(result)
            }, callbackDelay)
        }

        override fun run() {
            var sum = 0
            for (i in 0 until value) {
                sum += i
            }
            result = sum
        }
    }

    fun addUpToWithCallback(value: Int, callback: (result: Int) -> Unit, callbackDelay: Long) {
        if (runnable != null) {
            handler.removeCallbacks(runnable)
        }
        runnable = SumRunnable(value, callback, callbackDelay)
    }

    suspend fun addUpTo(value: Int): Int {
        var sum = 0
        for (i in 0 until value) {
            delay(100)
            sum += i
        }
        return sum
    }

    suspend fun addUpTo(value: Int, dispatcher: CoroutineDispatcher): Int {
        return withContext(dispatcher) {
            addUpTo(value)
        }
    }

    fun addUpToDeferredAsync(value: Int): Deferred<Int> = customCoroutineScope.scope.async {
        addUpTo(value)
    }
}

class CustomCoroutineScope(dispatcher: CoroutineDispatcher = Dispatchers.Main) {
    val job = SupervisorJob()
    val scope = CoroutineScope(job + dispatcher)
}