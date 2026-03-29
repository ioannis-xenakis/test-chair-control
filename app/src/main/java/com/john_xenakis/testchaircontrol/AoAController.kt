package com.john_xenakis.testchaircontrol

import android.content.Context
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream

class AoAController(private val context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var accessory: UsbAccessory? = null
    private var input: FileInputStream? = null
    private var output: FileOutputStream? = null

    var onStatusChanged: ((String) -> Unit)? = null
    var onTextReceived: ((String) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readJob: Job? = null

    fun openFromIntent(accessory: UsbAccessory?) {
        if (accessory == null) {
            onStatusChanged?.invoke("No accessory in intent")
            return
        }
        openAccessory(accessory)
    }

    fun tryOpenExistingAccessory() {
        val acc = usbManager.accessoryList?.firstOrNull()
        if (acc == null) {
            onStatusChanged?.invoke("No accessory connected")
            return
        }
        openAccessory(acc)
    }

    private fun openAccessory(acc: UsbAccessory) {
        close()

        val conn = usbManager.openAccessory(acc)
        if (conn == null) {
            onStatusChanged?.invoke("Failed to open accessory")
            return
        }

        accessory = acc
        input = FileInputStream(conn.fileDescriptor)
        output = FileOutputStream(conn.fileDescriptor)

        onStatusChanged?.invoke("Accessory connected")
        startReading()
    }

    fun sendText(text: String) {
        val out = output ?: run {
            onStatusChanged?.invoke("Not connected")
            return
        }
        val bytes = (text + "\n").toByteArray(Charsets.UTF_8)
        runCatching { out.write(bytes) }.onFailure {
            onStatusChanged?.invoke("Send failed: ${it.message}")
        }
    }

    private fun startReading() {
        val inp = input ?: return

        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(1024)
            val sb = StringBuilder()

            while (isActive) {
                val read = runCatching { inp.read(buffer) }.getOrElse { -1 }
                if (read <= 0) break

                val chunk = buffer.copyOf(read).toString(Charsets.UTF_8)
                sb.append(chunk)

                var idx: Int
                while (true) {
                    idx = sb.indexOf("\n")
                    if (idx < 0) break
                    val line = sb.substring(0, idx).trimEnd('\r')
                    sb.delete(0, idx + 1)
                    if (line.isNotEmpty()) {
                        onTextReceived?.invoke(line)
                    }
                }
            }

            onStatusChanged?.invoke("Accessory disconnected")
        }
    }

    fun close() {
        readJob?.cancel()
        readJob = null
        runCatching { input?.close() }
        runCatching { output?.close() }
        input = null
        output = null
        accessory = null
    }
}
