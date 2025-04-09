package it.smaso.drawingtablet.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import it.smaso.drawingtablet.connection.SocketConnection

@Composable
fun AddressRow(socket: SocketConnection) {
    var address by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }

    Row(
        Modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") }
        )
        Button(
            {
                socket.connect(address)
                connected = socket.isConnected()
            },
            colors = ButtonDefaults.buttonColors(containerColor = if (connected) Color.Green else Color.Red)
        ) { Text("Connect") }
    }
}
