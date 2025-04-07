package it.smaso.drawingtablet.connection.model

import it.smaso.drawingtablet.MainServiceOuterClass

data class ScreenSize(val height: Int, val width: Int) {
    companion object {
        fun fromGrpcResult(size: MainServiceOuterClass.ScreenSize): ScreenSize {
            return ScreenSize(size.height, size.width)
        }
    }

    override fun toString(): String {
        return "(${height}, ${width})"
    }
}
