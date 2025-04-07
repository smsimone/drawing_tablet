package it.smaso.drawingtablet

sealed interface StrokeAction {
    data object Start : StrokeAction
    data object Update : StrokeAction
    data object Finish : StrokeAction
    data object Cancel : StrokeAction
    data object Skip : StrokeAction
}
