package com.john_xenakis.testchaircontrol

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ChairViewModel(
    private val aoa: AoAController
) : ViewModel() {
    var inputText by mutableStateOf("")
        private set

    var receivedText by mutableStateOf("")
        private set

    var status by mutableStateOf("Idle")
        private set

    init {
        aoa.onStatusChanged = { msg ->
            viewModelScope.launch(Dispatchers.Main) {
                status = msg
            }
        }
        aoa.onTextReceived = { text ->
            viewModelScope.launch(Dispatchers.Main) {
                receivedText = text
            }
        }
    }

    fun onInputChange(newText: String) {
        inputText = newText
    }

    fun connect() {
        status = "Connecting..."
        aoa.tryOpenExistingAccessory()
    }

    fun send() {
        val text = inputText.trim()
        if (text.isEmpty()) return
        aoa.sendText(text)
        inputText = ""
    }

    override fun onCleared() {
        aoa.close()
        super.onCleared()
    }
}