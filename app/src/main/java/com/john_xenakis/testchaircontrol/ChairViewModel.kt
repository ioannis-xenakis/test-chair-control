package com.john_xenakis.testchaircontrol

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ChairViewModel(
    private val usb: UsbController
) : ViewModel() {
    var inputText by mutableStateOf("")
        private set

    var receivedText by mutableStateOf("")
        private set

    var status by mutableStateOf("Idle")
        private set

    init {
        usb.onStatusChanged = { msg ->
            viewModelScope.launch(Dispatchers.Main) {
                status = msg
            }
        }
        usb.onTextReceived = { msg ->
            viewModelScope.launch(Dispatchers.Main) {
                receivedText = msg
            }
        }
    }

    fun onInputChange(newText: String) {
        inputText = newText
    }

    fun connect() {
        status = "Connecting..."
        usb.findAndRequestPermission()
    }

    fun send() {
        val text = inputText.trim()
        if (text.isEmpty()) return
        usb.sendText(text)
        inputText = ""
    }

    override fun onCleared() {
        usb.close()
        super.onCleared()
    }
}