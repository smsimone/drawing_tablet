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
import it.smaso.drawingtablet.connection.model.ScreenSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val TAG = "STROKE_LISTENER"

class StrokeActionTouchListener(
    private val brush: Brush,
    private val inProgressStrokesView: InProgressStrokesView,
) : View.OnTouchListener, View.OnHoverListener {
    var currentPointerId by mutableStateOf<Int?>(null)
    var currentStrokeId by mutableStateOf<InProgressStrokeId?>(null)
    val connection = SocketConnection.getInstance()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var targetScreenSize: ScreenSize? = null

    fun setScreenSize(size: ScreenSize) {
        this.targetScreenSize = size
    }

    private fun currentScreenSize() = ScreenSize(
        inProgressStrokesView.height, inProgressStrokesView.width
    )

    private fun sendUpdate(event: MotionEvent, clicking: Boolean) {
        if (targetScreenSize == null) return

        scope.launch(Dispatchers.Main.immediate) {
            var currentX =
                if (currentPointerId == null) event.rawX else event.getX(currentPointerId!!)

            var currentY =
                if (currentPointerId == null) event.rawY else event.getY(currentPointerId!!)

            Log.i(TAG, "Mapping from ($currentX, $currentY)")
            val currentSize = currentScreenSize()


            currentX = (targetScreenSize!!.width.toFloat() / currentSize.width.toFloat()) * currentX
            currentY =
                (targetScreenSize!!.height.toFloat() / currentSize.height.toFloat()) * currentY
            Log.i(TAG, "Mapped to ($currentX, $currentY)")

            val coordinates = MainServiceOuterClass.CursorPosition.newBuilder()
                .setX(currentX)
                .setY(currentY)
                .setClicking(clicking)
                .build()
            connection.onCursorPositionUpdate(coordinates)
        }
    }

    // Updates the cursor status without the click
    override fun onHover(view: View, event: MotionEvent): Boolean {
        sendUpdate(event, false)
        return true
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val isUsingPen = recognizePen(event)
        sendUpdate(event, isUsingPen)
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
