package io.github.skeletonxf.data

enum class GameStateUpdate {
    DefenderWin,
    AttackerWin,
    DefenderCapture,
    AttackerCapture,
    Nothing;

    fun value(): Byte = when (this) {
        DefenderWin -> 0
        AttackerWin -> 1
        DefenderCapture -> 2
        AttackerCapture -> 3
        Nothing -> 4
    }

    companion object {
        fun valueOf(gameStateUpdate: Byte) = when (gameStateUpdate) {
            DefenderWin.value() -> DefenderWin
            AttackerWin.value() -> AttackerWin
            DefenderCapture.value() -> DefenderCapture
            AttackerCapture.value() -> AttackerCapture
            Nothing.value() -> Nothing
            else -> Nothing
        }
    }
}