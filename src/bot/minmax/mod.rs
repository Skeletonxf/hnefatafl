use crate::state::{GameState, Play, Player};
use crate::piece::Piece;

use rayon::prelude::*;

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
    let depth_remaining = 3;
    let mut α = Heuristic(i8::MIN); // min score maximising player found (trying to maximise)
    let mut β = Heuristic(i8::MAX); // max score minimising player found (trying to minimise)
    // Min Max algorithm is the maximising player if the turn in the game state is attackers
    // (because we arbitrarily choose attackers as maximising in the heuristic) and the minimising
    // player if the turn in the game state is the defenders.
    let player = match game_state.turn() {
        Player::Attacker => MinMaxPlayer::Maximising,
        Player::Defender => MinMaxPlayer::Minimising,
    };

    match player {
        MinMaxPlayer::Maximising => {
            //let mut best_value = Heuristic(i8::MIN);
            //let mut best_play = plays[0].clone();
            let dummy = (Heuristic(i8::MIN), plays[0].clone());
            let (_best_value, best_play) = plays
                .par_iter()
                // Attackers want to maximise the heuristic, so it starts at -infinity.
                .fold(|| dummy.clone(),
                |(best_value, best_play), play| {
                    let state = {
                        let mut copy = game_state.clone();
                        copy
                            .make_play(&play)
                            .expect("Using available plays should mean making a play never fails");
                        copy
                    };
                    // because alpha beta search can be done randomly on the children, in principle
                    // any of the first children we look at could be calculated when α was still
                    // infinity and β was still -infinity, so to avoid serialising the algorithm
                    // with a critical section to update α and β we'll try just not writing to them
                    // on this top level function (which will mean we skip less work, but we'll
                    // be doing that work fully in parallel)
                    let value = min_max(state, depth_remaining - 1, α, β, player.next());
                    if value > best_value {
                        (value, play.clone())
                    } else {
                        (best_value, best_play)
                    }
                })
                .reduce(
                    || dummy.clone(),
                    |(best_value, best_play), (value, play)| {
                    if value > best_value {
                        (value, play.clone())
                    } else {
                        (best_value, best_play)
                    }
                });
            Some(best_play)
        },
        MinMaxPlayer::Minimising => {
            // Defenders want to minimise the heuristic, so it starts at infinity.
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

// Returns a heuristic score of this game state, given highest minimum score alpha for maximising
// player (attackers) and lowest maximum score beta for minimising player (defenders) so far, and
// the turn player for this game state.
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
            // Victory for defenders is min score
            Player::Defender => Heuristic(i8::MIN),
            // Victory for attackers is max score
            Player::Attacker => Heuristic(i8::MAX),
        };
    }
    if depth_remaining == 0 {
        // With low depth it's far too easy for the algorithm to be looking at what it can do
        // then considering any move the player might make (which may for example move the king
        // to a single turn away from victory) then looking at what it can do again but not
        // considering that the game state is left in a state where the game can be won outright
        // on the next turn. Hence even at zero depth we should ensure the game isn't left on
        // a turn player victory to avoid the heuristic being misleading.
        let plays = game_state.available_plays();
        let king = game_state.king_position();
        match player {
            // Defenders can only win by moving the king so we can ignore all other plays
            // that don't move it
            MinMaxPlayer::Minimising => {
                for play in plays {
                    if play.from != king {
                        continue;
                    }
                    let state = {
                        let mut copy = game_state.clone();
                        copy
                            .make_play(&play)
                            .expect("Using available plays should mean making a play never fails");
                        copy
                    };
                    if let Some(winner) = state.winner() {
                        if winner == Player::Defender {
                            // Victory available for defenders on their turn is min score
                            return Heuristic(i8::MIN);
                        }
                    }
                }
            },
            // Attackers can almost only win by capturing the king so we could potentially prune
            // these plays a little too?
            MinMaxPlayer::Maximising => {
                for play in plays {
                    let state = {
                        let mut copy = game_state.clone();
                        copy
                            .make_play(&play)
                            .expect("Using available plays should mean making a play never fails");
                        copy
                    };
                    if let Some(winner) = state.winner() {
                        if winner == Player::Attacker {
                            // Victory available for attackers on their turn is max score
                            return Heuristic(i8::MAX);
                        }
                    }
                }
            }
        }
        // otherwise approximate value of this state based on number of pieces alive
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
    // Attackers will be maximising alpha, defenders minimising beta
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
                // We can guarantee at least this score of alpha by choosing the highest
                // scoring play available
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
                // We can guarantee at least this score of beta by choosing the lowest
                // scoring play available
                β = std::cmp::min(β, best_value);
                if best_value <= α {
                    break;
                }
            }
            best_value
        },
    }
}
