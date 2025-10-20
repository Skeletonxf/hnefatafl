package io.github.skeletonxf.data

enum class Player {
    Defender,
    Attacker;

    companion object {
        fun from(player: uniffi.hnefatafl.TurnPlayer): Player = when (player) {
            uniffi.hnefatafl.TurnPlayer.DEFENDERS -> Defender
            uniffi.hnefatafl.TurnPlayer.ATTACKERS -> Attacker
        }
    }
}