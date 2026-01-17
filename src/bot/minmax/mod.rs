use crate::piece::Piece;
use crate::state::{GameState, Play, Player};

use rayon::prelude::*;

static STARTING_DEPTH: u8 = 3;

pub fn min_max_play(game_state: &GameState) -> Option<Play> {
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
    let depth_remaining = STARTING_DEPTH;
    let α = Heuristic(i8::MIN); // min score maximising player found (trying to maximise)
    let β = Heuristic(i8::MAX); // max score minimising player found (trying to minimise)
    // Min Max algorithm is the maximising player if the turn in the game state is attackers
    // (because we arbitrarily choose attackers as maximising in the heuristic) and the minimising
    // player if the turn in the game state is the defenders.
    let player = match game_state.turn() {
        Player::Attacker => MinMaxPlayer::Maximising,
        Player::Defender => MinMaxPlayer::Minimising,
    };

    match player {
        MinMaxPlayer::Maximising => {
            // Attackers want to maximise the heuristic, so it starts at -infinity.
            let dummy = (α, plays[0].clone());
            let (_best_value, best_play) = plays
                .par_iter()
                .fold(|| dummy.clone(),
                |(best_value, best_play), play| {
                    let state = {
                        let mut copy = game_state.clone();
                        copy
                            .make_play(&play)
                            .expect("Using available plays should mean making a play never fails");
                        copy
                    };
                    // To avoid serialising the algorithm with a critical section we won't write
                    // to α or β for the top level iteration. Children will still be able to cull
                    // work via α and β optimisations. This will mean we might do more work overall
                    // but it can happen more in parallel.
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
            let dummy = (β, plays[0].clone());
            let (_best_value, best_play) = plays
                .par_iter()
                .fold(|| dummy.clone(),
                |(best_value, best_play), play| {
                    let state = {
                        let mut copy = game_state.clone();
                        copy
                            .make_play(&play)
                            .expect("Using available plays should mean making a play never fails");
                        copy
                    };
                    // To avoid serialising the algorithm with a critical section we won't write
                    // to α or β for the top level iteration. Children will still be able to cull
                    // work via α and β optimisations. This will mean we might do more work overall
                    // but it can happen more in parallel.
                    let value = min_max(state, depth_remaining - 1, α, β, player.next());
                    if value < best_value {
                        (value, play.clone())
                    } else {
                        (best_value, best_play)
                    }
                })
                .reduce(
                    || dummy.clone(),
                    |(best_value, best_play), (value, play)| {
                    if value < best_value {
                        (value, play.clone())
                    } else {
                        (best_value, best_play)
                    }
                });
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
    // Winning sooner is better than winning later, we don't want the bot to ignore a 'free' win
    // because the opponent can't actually deny it one or two turns later.
    // If it can win at maximum depth remaining we want the penalty to be 0 as this is
    // the best possible move the bot could take.
    let victory_delay_penalty = ((STARTING_DEPTH - 1) - depth_remaining) as i8;
    if let Some(winner) = game_state.winner() {
        return match winner {
            // Victory for defenders is min score
            Player::Defender => Heuristic(i8::MIN + victory_delay_penalty),
            // Victory for attackers is max score
            Player::Attacker => Heuristic(i8::MAX - victory_delay_penalty),
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
            // Defenders can almost always only win by moving the king so we can
            // ignore all other plays that don't move it
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
                            return Heuristic(i8::MIN + victory_delay_penalty);
                        }
                    }
                }
            },
            MinMaxPlayer::Maximising => {
                let possible_king_capture_positions = {
                    let (x, y) = king;
                    let (length_w, length_h) = game_state.size();
                    let mut positions = Vec::with_capacity(4);
                    if x > 1 {
                        positions.push((x - 1, y));
                    }
                    if y > 1 {
                        positions.push((x, y - 1));
                    }
                    if x < length_w - 1 {
                        positions.push((x + 1, y));
                    }
                    if y < length_h - 1 {
                        positions.push((x, y + 1));
                    }
                    positions
                };
                for play in plays {
                    if !possible_king_capture_positions.contains(&play.to) {
                        // In rare cases it is possible that a non-capturing
                        // move could win the game due to how we have implemented
                        // resolving a player left with no remaining moves as a
                        // loss (which attackers are more likely to be able to
                        // force), but this is unlikely and also not in the immediate
                        // future because we're checking at a depth of 0 here, so
                        // the performance gain of not checking most plays at no
                        // depth is worth the accuracy penalty.
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
                        if winner == Player::Attacker {
                            // Victory available for attackers on their turn is max score
                            return Heuristic(i8::MAX - victory_delay_penalty);
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
        }
    };
}

// Although the minmax algorithm is randomised because it will break ties differently on randomised
// order of moves, it should be deterministic in terms of always taking the 'best' move according
// to the heuristics
#[test]
fn defenders_minmax_takes_the_winning_move() {
    use crate::state::GameStateUpdate;
    use easy_ml::matrices::Matrix;
    #[rustfmt::skip]
    let board = {
        use crate::piece::Tile::Empty as E;
        use crate::piece::Tile::Attacker as A;
        use crate::piece::Tile::Defender as D;
        use crate::piece::Tile::King as K;
        Matrix::from_flat_row_major((11, 11), vec![
            E, E, E, E, E, K, E, E, E, E, E,
            E, E, E, E, E, A, E, E, A, E, E,
            E, E, E, E, E, E, E, E, E, E, E,
            A, E, E, E, E, E, E, E, E, E, A,
            E, E, E, E, D, E, E, E, E, E, E,
            E, E, E, E, D, E, D, D, E, E, A,
            A, E, E, E, E, D, A, E, E, E, E,
            A, E, E, E, E, D, E, E, E, E, A,
            E, E, E, E, E, E, E, E, E, E, E,
            E, E, E, E, E, E, E, E, E, A, E,
            E, E, E, E, A, A, E, A, E, E, E,
        ])
    };
    let mut game_state = GameState::from_setup(board, Player::Defender);
    println!("Game state before defender's turn: {}", game_state);
    let best_play = min_max_play(&game_state).expect("Defenders should have a play to make");
    let result = game_state.make_play(&best_play);
    println!("Game state after play: {}", game_state);
    assert_eq!(Ok(GameStateUpdate::DefenderWin), result);
    assert_eq!(Some(Player::Defender), game_state.winner());
}

// Although the minmax algorithm is randomised because it will break ties differently on randomised
// order of moves, it should be deterministic in terms of always taking the 'best' move according
// to the heuristics
#[test]
fn attackers_minmax_takes_the_winning_move() {
    use crate::state::GameStateUpdate;
    use easy_ml::matrices::Matrix;
    #[rustfmt::skip]
    let board = {
        use crate::piece::Tile::Empty as E;
        use crate::piece::Tile::Attacker as A;
        use crate::piece::Tile::Defender as D;
        use crate::piece::Tile::King as K;
        Matrix::from_flat_row_major((11, 11), vec![
            E, E, A, E, E, K, A, E, E, E, E,
            E, E, E, E, E, A, E, E, A, E, E,
            E, E, E, E, E, E, E, E, E, E, E,
            A, E, E, E, E, E, E, E, E, E, A,
            E, E, E, E, D, E, E, E, E, E, E,
            E, E, E, E, D, E, D, D, E, E, A,
            A, E, E, E, E, D, A, E, E, E, E,
            A, E, E, E, E, D, E, E, E, E, A,
            E, E, E, E, E, E, E, E, E, E, E,
            E, E, E, E, E, E, E, E, E, A, E,
            E, E, E, E, A, A, E, A, E, E, E,
        ])
    };
    let mut game_state = GameState::from_setup(board, Player::Attacker);
    println!("Game state before attacker's turn: {}", game_state);
    let best_play = min_max_play(&game_state).expect("Attackers should have a play to make");
    let result = game_state.make_play(&best_play);
    println!("Game state after play: {}", game_state);
    assert_eq!(Ok(GameStateUpdate::AttackerWin), result);
    assert_eq!(Some(Player::Attacker), game_state.winner());
}
