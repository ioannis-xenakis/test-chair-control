package com.john_xenakis.testchaircontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.john_xenakis.testchaircontrol.ui.theme.TestChairControlTheme

class MainActivity : ComponentActivity() {

    private lateinit var usb: UsbController

    private val vm: ChairViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                usb = UsbController(applicationContext)
                return ChairViewModel(usb) as T
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usb.register()
        enableEdgeToEdge()
        setContent {
            TestChairControlTheme {
                ChairControlScreen(vm)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        usb.unregister()
    }
}