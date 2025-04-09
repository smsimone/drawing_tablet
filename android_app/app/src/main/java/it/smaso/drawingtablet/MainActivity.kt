package it.smaso.drawingtablet

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.Stroke
import it.smaso.drawingtablet.components.AddressRow
import it.smaso.drawingtablet.connection.SocketConnection
import it.smaso.drawingtablet.connection.model.ScreenSize
import it.smaso.drawingtablet.ui.theme.DrawingTabletTheme

class MainActivity : ComponentActivity() {
    var screenSize by mutableStateOf<ScreenSize?>(null)
    val socket = SocketConnection.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrawingTabletTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
                    Box(modifier = Modifier.padding(insets)) {
                        DrawingView(socket)
                    }
                }
            }
        }
    }
}

@Composable
fun DrawingView(socket: SocketConnection) {
    var listener by mutableStateOf<StrokeActionTouchListener?>(null)

    val brush =
        Brush.createWithColorIntArgb(
            family = StockBrushes.pressurePenLatest,
            colorIntArgb = Color.Black.toArgb(),
            size = 5f,
            epsilon = 0.1F
        )

    Column {
        Row {
            AddressRow(socket)
            IconButton({
                socket.getScreenSize()?.let { listener?.setScreenSize(it) }
            }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh the current display size")
            }
        }
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .padding(5.dp)
                .border(1.dp, Color.Black)
                .zIndex(0f),
            factory = { context ->
                InProgressStrokesView(context).apply {
                    if (listener == null) listener = StrokeActionTouchListener(brush, this)
                    listener.let {
                        setOnTouchListener(it)
                        setOnHoverListener(it)
                    }

                    layoutParams =
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        )

                    addFinishedStrokesListener(
                        object : InProgressStrokesFinishedListener {
                            override fun onStrokesFinished(
                                strokes: Map<InProgressStrokeId, Stroke>
                            ) {
                                removeFinishedStrokes(strokes.keys)
                            }
                        }
                    )

                }
            }
        )
    }
}
