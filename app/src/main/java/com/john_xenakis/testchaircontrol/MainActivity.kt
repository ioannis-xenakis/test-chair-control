package com.john_xenakis.testchaircontrol

import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.john_xenakis.testchaircontrol.ui.theme.TestChairControlTheme

class MainActivity : ComponentActivity() {

    private val aoa by lazy { AoAController(applicationContext) }

    private val vm: ChairViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChairViewModel(aoa) as T
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If the app was launched because an accessory was attached, open it.
        val accessory = intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)
        if (accessory != null) {
            aoa.openFromIntent(accessory)
        }
        enableEdgeToEdge()
        setContent {
            TestChairControlTheme {
                ChairControlScreen(vm)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        aoa.close()
    }
}