use easy_ml::matrices::Matrix;

use crate::piece::Piece;
use std::fmt::Display;

pub enum Player {
    Defender,
    Attacker,
}

pub struct GameState {
    board: Board,
    turn: Player,
}

pub struct Board {
    board: Matrix<Piece>,
}

impl Display for Player {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> Result<(), std::fmt::Error> {
        write!(
            f,
            "{}",
            match self {
                Player::Defender => "White",
                Player::Attacker => "Red",
            }
        )
    }
}

impl Display for Board {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> Result<(), std::fmt::Error> {
        write!(f, "{}", self.board)
    }
}

impl GameState {
    fn new() -> Self {
        #[rustfmt::skip]
        let board = {
            use crate::piece::Piece::Empty as E;
            use crate::piece::Piece::Attacker as A;
            use crate::piece::Piece::Defender as D;
            use crate::piece::Piece::King as K;
            Matrix::from_flat_row_major((11, 11), vec![
                E, E, E, A, A, A, A, A, E, E, E,
                E, E, E, E, E, A, E, E, E, E, E,
                E, E, E, E, E, E, E, E, E, E, E,
                A, E, E, E, E, D, E, E, E, E, A,
                A, E, E, E, D, D, D, E, E, E, A,
                A, E, E, D, D, K, D, D, E, E, A,
                A, E, E, E, D, D, D, E, E, E, A,
                A, E, E, E, E, D, E, E, E, E, A,
                E, E, E, E, E, E, E, E, E, E, E,
                E, E, E, E, E, A, E, E, E, E, E,
                E, E, E, A, A, A, A, A, E, E, E,
            ])
        };
        GameState {
            board: Board { board },
            turn: Player::Defender,
        }
    }
}

impl Default for GameState {
    fn default() -> Self {
        GameState::new()
    }
}

impl Display for GameState {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> Result<(), std::fmt::Error> {
        write!(f, "Turn: {}\n{}", self.turn, self.board)
    }
}
