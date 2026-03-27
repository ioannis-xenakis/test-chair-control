package com.john_xenakis.testchaircontrol

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val ACTION_USB_PERMISSION = "com.john_xenakis.testchaircontrol.USB_PERMISSION"
private const val REPORT_SIZE = 32

class UsbController(private val context: Context) {
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var connection: UsbDeviceConnection? = null
    private var endpointOut: UsbEndpoint? = null
    private var endpointIn: UsbEndpoint? = null
    private var usbInterface: UsbInterface? = null

    var onStatusChanged: ((String) -> Unit)? = null
    var onTextReceived: ((String) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var readJob: Job? = null

    private val permissionIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == ACTION_USB_PERMISSION) {
                val device = intent.getParcelableExtra<android.hardware.usb.UsbDevice>(UsbManager.EXTRA_DEVICE)
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)

                if (granted && device != null) {
                    openHidDevice(device)
                } else {
                    onStatusChanged?.invoke("USB permission denied")
                }
            }
        }
    }

    fun register() {
        val filter = IntentFilter(ACTION_USB_PERMISSION)

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(
                usbReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            // Older Android versions do not require the flag
            context.registerReceiver(
                usbReceiver,
                filter
            )
        }
    }

    fun unregister() {
        runCatching { context.unregisterReceiver(usbReceiver) }
    }

    fun findAndRequestPermission() {
        val device = usbManager.deviceList.values.firstOrNull {
            it.vendorId == 1234 && it.productId == 5678
        }

        if (device == null) {
            onStatusChanged?.invoke("No device found")
            return
        }

        if (usbManager.hasPermission(device)) {
            openHidDevice(device)
        } else {
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    fun openHidDevice(device: UsbDevice) {
        close()

        val hidInterface = (0 until device.interfaceCount)
            .map{ device.getInterface(it) }
            .firstOrNull{ it.interfaceClass == UsbConstants.USB_CLASS_HID }

        if (hidInterface == null) {
            onStatusChanged?.invoke("No HID interface found")
            return
        }

        val conn = usbManager.openDevice(device)
        if (conn == null) {
            onStatusChanged?.invoke("Failed to open device")
            return
        }

        if (!conn.claimInterface(hidInterface, true)) {
            conn.close()
            onStatusChanged?.invoke("Failed to claim interface")
            return
        }

        usbInterface = hidInterface
        connection = conn

        endpointOut = ( 0 until hidInterface.endpointCount )
            .map { hidInterface.getEndpoint(it) }
            .firstOrNull {
                it.direction == UsbConstants.USB_DIR_OUT &&
                        it.type == UsbConstants.USB_ENDPOINT_XFER_INT
            }

        endpointIn = ( 0 until hidInterface.endpointCount )
            .map { hidInterface.getEndpoint(it) }
            .firstOrNull{
                it.direction == UsbConstants.USB_DIR_IN &&
                        it.type == UsbConstants.USB_ENDPOINT_XFER_INT
            }

        onStatusChanged?.invoke("Connected")
        startReading()
    }

    fun sendCommand(report: ByteArray) {
        val conn = connection ?: run {
            onStatusChanged?.invoke("Not connected")
            return
        }
        val out = endpointOut ?: run {
            onStatusChanged?.invoke("No OUT endpoint")
            return
        }

        val result = conn.bulkTransfer(out, report, report.size, 1000)
        if (result <= 0) onStatusChanged?.invoke("Failed to send command")
    }

    fun sendText(text: String) {
        val conn = connection ?: run {
            onStatusChanged?.invoke("Not connected")
            return
        }
        val out = endpointOut ?: run {
            onStatusChanged?.invoke("No OUT endpoint")
            return
        }

        val bytes = text.toByteArray(Charsets.UTF_8)
        val report = ByteArray(REPORT_SIZE)
        val len = minOf(bytes.size, REPORT_SIZE)
        System.arraycopy(bytes, 0, report, 0, len)
        val result = conn.bulkTransfer(out, report, report.size, 1000)
        if (result <= 0) onStatusChanged?.invoke("Failed to send text")
    }

    fun startReading() {
        val conn = connection ?: return
        val epIn = endpointIn ?: return

        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(REPORT_SIZE)
            while (isActive) {
                val result = conn.bulkTransfer(epIn, buffer, buffer.size, 1000)
                if (result > 0) {
                    val text = buffer
                        .copyOf(result)
                        .toString(Charsets.UTF_8)
                        .trimEnd('\u0000')
                    if (text.isNotEmpty()) {
                        onTextReceived?.invoke(text)
                    }
                }
            }
        }
    }

    fun close() {
        readJob?.cancel()
        readJob = null

        runCatching { connection?.releaseInterface(usbInterface) }
        runCatching { connection?.close() }

        connection = null
        endpointOut = null
        endpointIn = null
        usbInterface = null
        onStatusChanged?.invoke("Disconnected")
    }
}