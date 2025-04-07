package it.smaso.drawingtablet.connection

import android.util.Log
import com.google.protobuf.Empty
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import it.smaso.drawingtablet.MainServiceGrpcKt
import it.smaso.drawingtablet.MainServiceOuterClass
import it.smaso.drawingtablet.connection.model.ScreenSize
import kotlinx.coroutines.runBlocking

const val TAG = "SOCKET_CONNECTION"

class SocketConnection {

    private var channel: ManagedChannel =
        ManagedChannelBuilder.forAddress("192.168.1.50", 50051).usePlaintext().build()
    val stub = MainServiceGrpcKt.MainServiceCoroutineStub(channel)

    companion object {
        @Volatile
        private var instance: SocketConnection? = null

        fun getInstance(): SocketConnection =
            instance
                ?: synchronized(this) {
                    instance ?: SocketConnection().also { instance = it }
                }
    }

    private constructor() {
        channel.getState(true).also { Log.i(TAG, "Current connection status: ${it.name}") }

        suspend {
            Log.d(TAG, "Started job to send cursor updates")

            // stub.onCursorPosition(cursorUpdatesChannel.consumeAsFlow()).also {
            //     Log.d(TAG, "Sent new update to server")
            // }
        }
    }

    fun getScreenSize(): ScreenSize? {
        var screenSize: MainServiceOuterClass.ScreenSize? = null
        runBlocking {
            try {
                screenSize = stub.getScreenSize(Empty.getDefaultInstance())
                Log.d(TAG, "Fetched screen size: $screenSize")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get screen size: ${e.message}")
            }
        }

        return screenSize?.let { ScreenSize.fromGrpcResult(it) }
    }

    suspend fun onCursorPositionUpdate(update: MainServiceOuterClass.CursorPosition) {
        try {
            stub.onCursorPosition(update)
            Log.d(TAG, "Sent new update to the cursor channel")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send cursor update: ${e.message}")
        }
    }
}
