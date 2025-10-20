package io.github.skeletonxf.data

import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.ffi.KEnum

enum class Winner : KEnum {
    Defenders,
    Attackers,
    None;

    override fun value(): Byte = when (this) {
        Defenders -> bindings_h.Defenders().toByte()
        Attackers -> bindings_h.Attackers().toByte()
        None -> bindings_h.None().toByte()
    }

    companion object {
        private val variants = entries
        fun valueOf(winner: Byte) = KEnum.valueOf(winner, variants, None)

        fun from(winner: uniffi.hnefatafl.Winner): Winner = when (winner) {
            uniffi.hnefatafl.Winner.DEFENDERS -> Defenders
            uniffi.hnefatafl.Winner.ATTACKERS -> Attackers
            uniffi.hnefatafl.Winner.NONE -> None
        }
    }
}