use crate::state::{GameState, Play, Player};
use crate::piece::Piece;

pub fn min_max_play(game_state: GameState) -> Option<Play> {
    let mut plays = game_state.available_plays();
    if plays.is_empty() {
        return None;
    }
    {
        use rand::prelude::*;
        let mut rng = rand::thread_rng();
        // Shuffle the top level moves so we don't have a bias towards the top left which would
        // otherwise always be the first available moves we consider and then pick if we're unable
        // to find any moves that are better than others
        // This isn't necessary for recursive calls because we only pick the top iteration for
        // making a move, the rest are just lookahead and their order of evaluation should have
        // no impact on our behaviour.
        plays.shuffle(&mut rng);
    }
    let depth_remaining = 2;
    let mut α = Heuristic(i8::MIN);
    let mut β = Heuristic(i8::MAX);
    let player = match game_state.turn() {
        Player::Attacker => MinMaxPlayer::Maximising,
        Player::Defender => MinMaxPlayer::Minimising,
    };

    match player {
        MinMaxPlayer::Maximising => {
            let mut best_value = Heuristic(i8::MIN);
            let mut best_play = plays[0].clone();
            for play in plays {
                let state = {
                    let mut copy = game_state.clone();
                    copy
                        .make_play(&play)
                        .expect("Using available plays should mean making a play never fails");
                    copy
                };
                let value = min_max(state, depth_remaining - 1, α, β, player.next());
                if value > best_value {
                    best_value = value;
                    best_play = play;
                }
                α = std::cmp::max(α, best_value);
                if best_value >= β {
                    break;
                }
            }
            Some(best_play)
        },
        MinMaxPlayer::Minimising => {
            let mut best_value = Heuristic(i8::MAX);
            let mut best_play = plays[0].clone();
            for play in plays {
                let state = {
                    let mut copy = game_state.clone();
                    copy
                        .make_play(&play)
                        .expect("Using available plays should mean making a play never fails");
                    copy
                };
                let value = min_max(state, depth_remaining - 1, α, β, player.next());
                if value < best_value {
                    best_value = value;
                    best_play = play;
                }
                // FIXME: This has a slight bias towards earlier moves, we should randomise
                // selecting the move to take if the best value is tied.
                β = std::cmp::min(β, best_value);
                if best_value <= α {
                    break;
                }
            }
            Some(best_play)
        },
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq, Ord, PartialOrd)]
struct Heuristic(i8);

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
enum MinMaxPlayer {
    /// Attackers maximise
    Maximising,
    /// Defenders minimise
    Minimising,
}

impl MinMaxPlayer {
    fn next(self) -> MinMaxPlayer {
        match self {
            MinMaxPlayer::Maximising => MinMaxPlayer::Minimising,
            MinMaxPlayer::Minimising => MinMaxPlayer::Maximising,
        }
    }
}

fn min_max(
    game_state: GameState,
    depth_remaining: u8,
    alpha: Heuristic,
    beta: Heuristic,
    player: MinMaxPlayer,
) -> Heuristic {
    let plays = game_state.available_plays();
    if let Some(winner) = game_state.winner() {
        return match winner {
            Player::Defender => Heuristic(i8::MIN),
            Player::Attacker => Heuristic(i8::MAX),
        };
    }
    if depth_remaining == 0 {
        // approximate value of this state based on number of pieces alive
        // this is very approximate, as there are more attackers than defenders so piece loss
        // might not be of equal value, but need to start with something
        let dead = game_state.dead();
        let dead_attackers = dead.iter().filter(|&&piece| piece == Piece::Attacker).count();
        let dead_defenders = dead.iter().filter(|&&piece| piece == Piece::Defender).count();
        return Heuristic((dead_defenders as i8) - (dead_attackers as i8));
    }
    if plays.is_empty() {
        panic!("Plays can't be empty if there is no winner");
    }
    // From wikipedia: Alpha represents the minimum amount of points the player the algorithm
    // wants to win can have (the maximizing player), while beta is the maximum amount of points
    // the algorithm wants to lose can have (the minimizing player).
    let mut α = alpha;
    let mut β = beta;
    return match player {
        MinMaxPlayer::Maximising => {
            let mut best_value = Heuristic(i8::MIN);
            for play in plays {
                let state = {
                    let mut copy = game_state.clone();
                    copy
                        .make_play(&play)
                        .expect("Using available plays should mean making a play never fails");
                    copy
                };
                best_value = std::cmp::max(
                    best_value,
                    min_max(state, depth_remaining - 1, α, β, player.next())
                );
                α = std::cmp::max(α, best_value);
                if best_value >= β {
                    break;
                }
            }
            best_value
        },
        MinMaxPlayer::Minimising => {
            let mut best_value = Heuristic(i8::MAX);
            for play in plays {
                let state = {
                    let mut copy = game_state.clone();
                    copy
                        .make_play(&play)
                        .expect("Using available plays should mean making a play never fails");
                    copy
                };
                best_value = std::cmp::min(
                    best_value,
                    min_max(state, depth_remaining - 1, α, β, player.next())
                );
                β = std::cmp::min(β, best_value);
                if best_value <= α {
                    break;
                }
            }
            best_value
        },
    }
}
