package com.example.coroutineviewmodelunittesting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.ContinuationInterceptor

class SimpleViewModel : ViewModel() {
    private val _sumData = MutableLiveData<Int>()
    val sumData: LiveData<Int>
        get() = _sumData

    private val _sumPostData = MutableLiveData<Int>()
    val sumPostData: LiveData<Int>
        get() = _sumPostData

    fun fetchSumData(addUpToParameterValue: Int) {
        viewModelScope.launch {
            val result = MyAdder().addUpTo(addUpToParameterValue, DefaultDispatcherProvider.io())
            withContext(DefaultDispatcherProvider.main()) {
                _sumData.value = result
            }
        }
    }

    fun fetchSumPostData(addUpToParameterValue: Int, dispatcher: CoroutineDispatcher) {
        viewModelScope.launch(dispatcher) {
            val adder = MyAdder()
            val result2 = adder.addUpTo(addUpToParameterValue, this.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher)
            _sumPostData.postValue(result2)
        }
    }
}