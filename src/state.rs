use easy_ml::matrices::Matrix;

use crate::piece::{Piece, Tile};
use std::fmt::Display;

use std::ops::Index;

pub enum Player {
    Defender,
    Attacker,
}

type Position = (usize, usize);

pub struct GameState {
    board: Board,
    turn: Player,
    castle: Position,
}

pub struct Board {
    board: Matrix<Tile>,
}

struct Play {
    piece: Piece,
    from: Position,
    to: Position,
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

impl Index<Position> for Board {
    type Output = Tile;

    fn index(&self, position: Position) -> &Self::Output {
        self.board.get_reference(position.0, position.1)
    }
}

impl Board {
    fn size(&self) -> Position {
        self.board.size()
    }
}

impl GameState {
    fn new() -> Self {
        #[rustfmt::skip]
        let board = {
            use crate::piece::Tile::Empty as E;
            use crate::piece::Tile::Attacker as A;
            use crate::piece::Tile::Defender as D;
            use crate::piece::Tile::King as K;
            Matrix::from_flat_row_major((11, 11), vec![
                E, E, E, A, A, A, A, A, E, E, E,
                E, E, E, E, E, A, E, E, E, E, E,
                E, E, E, E, E, E, E, E, E, E, E,
                A, E, E, E, E, D, E, E, E, E, A,
                A, E, E, E, D, D, D, E, E, E, A,
                A, A, E, D, D, K, D, D, E, A, A,
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
            castle: (5, 5),
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

impl Player {
    fn owns(&self, piece: Piece) -> bool {
        match self {
            Player::Defender => piece != Piece::Attacker,
            Player::Attacker => piece == Piece::Attacker,
        }
    }
}

impl GameState {
    fn is_valid_defender_play(&self, play: Play) -> bool {
        // Is this a piece we can even own?
        if !Player::Defender.owns(play.piece) {
            return false;
        }
        // Does the piece exist at the starting position?
        let from = self.board[play.from];
        if from != play.piece {
            return false;
        }
        // Can we sit on the final position?
        let to = self.board[play.to];
        let unoccupied = match play.piece {
            Piece::King => to == Tile::Empty,
            _ => to == Tile::Empty && play.to != self.castle,
        };
        if !unoccupied {
            return false;
        }
        self.is_valid_path(play.from, play.to)
    }

    fn is_valid_attacker_play(&self, play: Play) -> bool {
        // Is this a piece we can even own?
        if !Player::Attacker.owns(play.piece) {
            return false;
        }
        // Does the piece exist at the starting position?
        let from = self.board[play.from];
        if from != play.piece {
            return false;
        }
        // Can we sit on the final position?
        let to = self.board[play.to];
        let unoccupied = to == Tile::Empty && play.to != self.castle;
        if !unoccupied {
            return false;
        }
        self.is_valid_path(play.from, play.to)
    }

    /// Checks if the path between from and to is unoccpied and a single horizontal or vertical
    /// movement.
    fn is_valid_path(&self, from: Position, to: Position) -> bool {
        if from == to {
            return false;
        }
        let (x0, y0) = from;
        let (x1, y1) = to;
        if x0 != x1 && y0 != y1 {
            // movement must be up, down, left or right
            return false;
        }
        if x0 == x1 {
            for y in y0..=y1 {
                let position = (x0, y);
                if y != y0 {
                    if self.is_occupied(position) {
                        return false;
                    }
                }
            }
        } else {
            for x in x0..=x1 {
                let position = (x, y0);
                if x != x0 {
                    if self.is_occupied(position) {
                        return false;
                    }
                }
            }
        }
        true
    }

    fn is_occupied(&self, position: Position) -> bool {
        self.board[position] != Tile::Empty
    }

    fn is_in_range(&self, position: Position) -> bool {
        position < self.board.size()
    }
}
