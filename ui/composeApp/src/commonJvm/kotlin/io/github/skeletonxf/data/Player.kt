package io.github.skeletonxf.data

import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.ffi.KEnum

enum class Player : KEnum {
    Defender,
    Attacker;

    override fun value(): Byte = when (this) {
        Defender -> bindings_h.DefendersTurn().toByte()
        Attacker -> bindings_h.AttackersTurn().toByte()
    }

    companion object {
        private val variants = values().toList()
        fun valueOf(player: Byte) = KEnum.valueOf(player, variants, Defender)

        fun from(player: uniffi.hnefatafl.TurnPlayer): Player = when (player) {
            uniffi.hnefatafl.TurnPlayer.DEFENDERS -> Defender
            uniffi.hnefatafl.TurnPlayer.ATTACKERS -> Attacker
        }
    }
}