package it.smaso.drawingtablet

import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import it.smaso.drawingtablet.connection.SocketConnection
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

const val TAG = "STROKE_LISTENER"

class StrokeActionTouchListener(
    private val brush: Brush,
    private val inProgressStrokesView: InProgressStrokesView,
) : View.OnTouchListener, View.OnHoverListener {
    var currentPointerId by mutableStateOf<Int?>(null)
    var currentStrokeId by mutableStateOf<InProgressStrokeId?>(null)
    val connection = SocketConnection.getInstance()

    // Updates the cursor status without the click
    override fun onHover(view: View, event: MotionEvent): Boolean {
        runBlocking {
            launch {
                val coords: MainServiceOuterClass.CursorPosition
                if (currentPointerId != null) {
                    coords = MainServiceOuterClass.CursorPosition.newBuilder()
                        .setX(event.getX(currentPointerId!!))
                        .setY(event.getY(currentPointerId!!))
                        .setClicking(false)
                        .build()
                } else {
                    coords = MainServiceOuterClass.CursorPosition.newBuilder()
                        .setX(event.rawX)
                        .setY(event.rawY)
                        .setClicking(false)
                        .build()
                }
                connection.onCursorPositionUpdate(coords)
            }
        }
        return true
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val isUsingPen = recognizePen(event)

        runBlocking {
            launch {
                val coords: MainServiceOuterClass.CursorPosition
                if (currentPointerId != null) {
                    coords = MainServiceOuterClass.CursorPosition.newBuilder()
                        .setX(event.getX(currentPointerId!!))
                        .setY(event.getY(currentPointerId!!))
                        .setClicking(isUsingPen)
                        .build()
                } else {
                    coords = MainServiceOuterClass.CursorPosition.newBuilder()
                        .setX(event.rawX)
                        .setY(event.rawY)
                        .setClicking(isUsingPen)
                        .build()
                }

                connection.onCursorPositionUpdate(coords)
            }
        }

        if (!isUsingPen) return true

        return when (mapEventToAction(event)) {
            StrokeAction.Start -> {
                handleStartStroke(view, event)
                true
            }

            StrokeAction.Update -> {
                handleStrokeUpdate(view, event)
                true
            }

            StrokeAction.Finish -> {
                handleStrokeStop(view, event)
                true
            }

            StrokeAction.Cancel -> {
                handleCancelStroke(view, event)
                true
            }

            StrokeAction.Skip -> {
                true
            }
        }.also { doPostHandlerAction(view, event) }
    }

    private fun recognizePen(event: MotionEvent): Boolean {
        return try {
            event.device.name.lowercase().contains("pen")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if using pen: $e")
            false
        }
    }

    private fun doPostHandlerAction(view: View, event: MotionEvent) {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            view.performClick()
        }
    }

    private fun mapEventToAction(event: MotionEvent): StrokeAction =
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> StrokeAction.Start
            MotionEvent.ACTION_UP -> StrokeAction.Finish
            MotionEvent.ACTION_MOVE -> StrokeAction.Update
            MotionEvent.ACTION_CANCEL -> StrokeAction.Cancel
            else -> StrokeAction.Skip
        }

    private fun handleStartStroke(view: View, event: MotionEvent) {
        view.requestUnbufferedDispatch(event)
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        currentPointerId = pointerId
        currentStrokeId =
            inProgressStrokesView.startStroke(
                event = event,
                pointerId = pointerId,
                brush = brush
            )
    }

    private fun handleStrokeUpdate(view: View, event: MotionEvent) {
        val pointerId = checkNotNull(currentPointerId)
        val strokeId = checkNotNull(currentStrokeId)


        for (pointerIndex in 0 until event.pointerCount) {
            if (event.getPointerId(pointerIndex) != pointerId) continue
            inProgressStrokesView.addToStroke(
                event,
                pointerId,
                strokeId,
                // predictedEvent,
            )
        }
    }

    private fun handleStrokeStop(view: View, event: MotionEvent) {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        check(pointerId == currentPointerId)
        val strokeId = checkNotNull(currentStrokeId)
        inProgressStrokesView.finishStroke(event, pointerId, strokeId)
        view.performClick()
    }

    private fun handleCancelStroke(view: View, event: MotionEvent) {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        check(pointerId == currentPointerId)

        val strokeId = checkNotNull(currentStrokeId)
        inProgressStrokesView.cancelStroke(strokeId, event)
        true
    }
}
