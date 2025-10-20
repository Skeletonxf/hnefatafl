package io.github.skeletonxf.data


enum class Winner {
    Defenders,
    Attackers,
    None;

    companion object {
        fun from(winner: uniffi.hnefatafl.Winner): Winner = when (winner) {
            uniffi.hnefatafl.Winner.DEFENDERS -> Defenders
            uniffi.hnefatafl.Winner.ATTACKERS -> Attackers
            uniffi.hnefatafl.Winner.NONE -> None
        }
    }
}