package it.smaso.drawingtablet

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
                    Button(
                            { socket.getScreenSize()?.let { screenSize = it } },
                            Modifier.padding(insets)
                                .zIndex(2f)
                    ) { Text("Fetch screen size $screenSize") }
                    DrawingView(Modifier.padding(insets), screenSize)
                }
            }
        }
    }
}

@Composable
fun DrawingView(modifier: Modifier, screenSize: ScreenSize?) {
    val brush =
            Brush.createWithColorIntArgb(
                    family = StockBrushes.pressurePenLatest,
                    colorIntArgb = Color.Black.toArgb(),
                    size = 5f,
                    epsilon = 0.1F
            )

    Box {
        if (screenSize != null)
                Box(
                        modifier =
                                Modifier.size(screenSize.width.dp, screenSize.height.dp)
                                        .border(BorderStroke(1.dp, Color.Black))
                                        .zIndex(1f)
                                        .align(Alignment.Center)
                ) { Text("Current screen size: $screenSize") }

        AndroidView(
                modifier = modifier.fillMaxSize().clipToBounds().zIndex(0f),
                factory = { context ->
                    InProgressStrokesView(context).apply {
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
                        setOnTouchListener(StrokeActionTouchListener(brush, this))
                    }
                }
        )
    }
}
