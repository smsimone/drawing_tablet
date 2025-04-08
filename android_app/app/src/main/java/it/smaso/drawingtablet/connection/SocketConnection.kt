package it.smaso.drawingtablet.connection

import android.util.Log
import com.google.protobuf.Empty
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import it.smaso.drawingtablet.MainServiceGrpcKt
import it.smaso.drawingtablet.MainServiceOuterClass
import it.smaso.drawingtablet.connection.model.ScreenSize
import kotlinx.coroutines.runBlocking

const val TAG = "SOCKET_CONNECTION"

class SocketConnection private constructor() {
    private var channel: ManagedChannel? = null
        set(value) {
            value?.let {
                stub = MainServiceGrpcKt.MainServiceCoroutineStub(it)
            }
            field = value
        }

    private var stub: MainServiceGrpcKt.MainServiceCoroutineStub? = null

    companion object {
        @Volatile
        private var instance: SocketConnection? = null

        fun getInstance(): SocketConnection =
            instance
                ?: synchronized(this) {
                    instance ?: SocketConnection().also { instance = it }
                }
    }

    fun connect(address: String) {
        channel = ManagedChannelBuilder.forAddress(address, 50051).usePlaintext().build()
        channel?.let { it ->
            it.getState(true).also { Log.i(TAG, "Current connection status: ${it.name}") }
        }
    }

    fun isConnected(): Boolean {
        val state = channel?.getState(true) ?: return false
        return state != ConnectivityState.SHUTDOWN
    }

    fun getScreenSize(): ScreenSize? {
        assert(stub != null)

        var screenSize: MainServiceOuterClass.ScreenSize? = null
        runBlocking {
            try {
                screenSize = stub!!.getScreenSize(Empty.getDefaultInstance())
                Log.d(TAG, "Fetched screen size: $screenSize")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get screen size: ${e.message}")
            }
        }

        return screenSize?.let { ScreenSize.fromGrpcResult(it) }
    }

    suspend fun onCursorPositionUpdate(update: MainServiceOuterClass.CursorPosition) {
        assert(stub != null)
        try {
            stub!!.onCursorPosition(update)
            Log.d(TAG, "Sent new update to the cursor channel")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send cursor update: ${e.message}")
        }
    }
}
