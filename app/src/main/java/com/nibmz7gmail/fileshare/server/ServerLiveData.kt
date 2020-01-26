package com.nibmz7gmail.fileshare.server

import androidx.lifecycle.LiveData

object ServerLiveData : LiveData<Result<Int>>() {

    fun setStatus(result: Result<Int>) {
        value = result
    }

}