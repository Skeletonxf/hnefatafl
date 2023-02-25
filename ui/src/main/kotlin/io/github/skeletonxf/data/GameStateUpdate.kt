package io.github.skeletonxf.data

import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.ffi.KEnum

enum class GameStateUpdate : KEnum {
    DefenderWin,
    AttackerWin,
    DefenderCapture,
    AttackerCapture,
    Nothing;

    override fun value(): Byte = when (this) {
        DefenderWin -> bindings_h.DefenderWin().toByte()
        AttackerWin -> bindings_h.AttackerWin().toByte()
        DefenderCapture -> bindings_h.DefenderCapture().toByte()
        AttackerCapture -> bindings_h.AttackerCapture().toByte()
        Nothing -> bindings_h.Nothing().toByte()
    }

    companion object {
        private val variants = GameStateUpdate.values().toList()
        fun valueOf(gameStateUpdate: Byte) = KEnum.valueOf(gameStateUpdate, variants, Nothing)
    }
}