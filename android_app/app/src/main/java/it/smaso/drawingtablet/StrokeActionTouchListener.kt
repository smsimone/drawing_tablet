package it.smaso.drawingtablet

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
) : View.OnTouchListener {
    var currentPointerId by mutableStateOf<Int?>(null)
    var currentStrokeId by mutableStateOf<InProgressStrokeId?>(null)
    val connection = SocketConnection.getInstance()


    @Suppress("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val isUsingPen = recognizePen(event)


        runBlocking {
            launch {
                val coords: MainServiceOuterClass.Coordinates
                if (currentPointerId != null) {
                    coords = MainServiceOuterClass.Coordinates.newBuilder()
                        .setX(event.getX(currentPointerId!!))
                        .setY(event.getY(currentPointerId!!))
                        .build()
                } else {
                    coords = MainServiceOuterClass.Coordinates.newBuilder()
                        .setX(event.rawX)
                        .setY(event.rawY)
                        .build()
                }

                connection.onCursorPositionUpdate(
                    MainServiceOuterClass.CursorPosition.newBuilder()
                        .setClicking(isUsingPen)
                        .setCoordinates(coords)
                        .build()
                )
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

    private fun recognizePen(event: MotionEvent): Boolean =
        event.device.name.lowercase().contains("pen")

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
