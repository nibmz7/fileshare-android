package com.nibmz7gmail.fileshare.server

import androidx.lifecycle.LiveData
import com.nibmz7gmail.fileshare.model.Event

object ServerLiveData : LiveData<Event<Int>>() {

    fun setStatus(event: Event<Int>) {
        value = event
    }

}