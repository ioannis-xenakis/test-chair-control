package com.john_xenakis.testchaircontrol

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChairControlScreen(vm: ChairViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Status: ${vm.status}")

        Button(onClick = vm::connect) {
            Text("Connect USB")
        }

        OutlinedTextField(
            value = vm.inputText,
            onValueChange = vm::onInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Text to send") },
            singleLine = true
        )

        Button(
            onClick = vm::send,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send")
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp)
                .fillMaxWidth())

        Text(
            text = "Received from PC:",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = vm.receivedText,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}