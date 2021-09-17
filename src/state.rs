use easy_ml::matrices::views::MatrixMut;
use easy_ml::matrices::Matrix;

use crate::piece::{Piece, Tile};

use std::convert::TryInto;
use std::fmt::Display;
use std::ops::{Index, IndexMut};

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

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct Play {
    pub from: Position,
    pub to: Position,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
enum Direction {
    Left,
    Right,
    Up,
    Down,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum GameStateUpdate {
    DefenderWin,
    AttackerWin,
    DefenderCapture,
    AttackerCapture,
    Nothing,
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
        self.board.get_reference(position.1, position.0)
    }
}

impl IndexMut<Position> for Board {
    fn index_mut(&mut self, position: Position) -> &mut Self::Output {
        // how did I forget to make get_reference_mut a thing?
        self.board
            .try_get_reference_mut(position.1, position.0)
            .unwrap()
    }
}

impl Board {
    fn swap(&mut self, position1: Position, position2: Position) {
        let tmp = self[position1];
        self[position1] = self[position2];
        self[position2] = tmp;
    }
}

impl Board {
    fn size(&self) -> Position {
        self.board.size()
    }

    fn adjacent(&self, position: Position) -> [Option<Position>; 4] {
        [
            self.left(position),
            self.right(position),
            self.down(position),
            self.up(position),
        ]
    }

    #[rustfmt::skip]
    fn right(&self, position: Position) -> Option<Position> {
        let (x, y) = position;
        if x < self.size().0 - 1 { Some((x + 1, y)) } else { None }
    }

    #[rustfmt::skip]
    fn left(&self, position: Position) -> Option<Position> {
        let (x, y) = position;
        if x > 0 { Some((x - 1, y)) } else { None }
    }

    #[rustfmt::skip]
    fn up(&self, position: Position) -> Option<Position> {
        let (x, y) = position;
        if y > 0 { Some((x, y - 1)) } else { None }
    }

    #[rustfmt::skip]
    fn down(&self, position: Position) -> Option<Position> {
        let (x, y) = position;
        if y < self.size().1 - 1 { Some((x, y + 1)) } else { None }
    }

    fn step(&self, position: Position, direction: Direction) -> Option<Position> {
        match direction {
            Direction::Right => self.right(position),
            Direction::Left => self.left(position),
            Direction::Up => self.up(position),
            Direction::Down => self.down(position),
        }
    }

    fn is_edge(&self, position: Position) -> bool {
        let (w, h) = self.size();
        match position {
            (0, _) | (_, 0) => true,
            (x, y) => x == w - 1 || y == h - 1,
        }
    }

    fn is_corner(&self, position: Position) -> bool {
        let (w, h) = self.size();
        position == (0, 0)
            || position == (0, h - 1)
            || position == (w - 1, 0)
            || position == (w - 1, h - 1)
    }

    fn on(&self, position: Position) -> bool {
        let (w, h) = self.size();
        let (x, y) = position;
        x < w && y < h
    }
}

fn direction(from: Position, to: Position) -> Direction {
    let (x0, y0) = from;
    let (x1, y1) = to;
    if x0 < x1 {
        Direction::Right
    } else if x0 > x1 {
        Direction::Left
    } else if y0 < y1 {
        Direction::Down
    } else if y0 > y1 {
        Direction::Up
    } else {
        panic!("Bad input")
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

    fn next(&self) -> Player {
        match self {
            Player::Defender => Player::Attacker,
            Player::Attacker => Player::Defender,
        }
    }
}

impl Piece {
    fn owner(&self) -> Player {
        match self {
            Piece::Defender | Piece::King => Player::Defender,
            Piece::Attacker => Player::Attacker,
        }
    }
}

impl Tile {
    fn owner(self) -> Option<Player> {
        let piece: Result<Piece, _> = self.try_into();
        piece.ok().map(|p| p.owner())
    }
}

impl GameStateUpdate {
    fn update(&self, event: GameStateUpdate) -> GameStateUpdate {
        match (self, event) {
            (GameStateUpdate::DefenderWin, _) => GameStateUpdate::DefenderWin,
            (GameStateUpdate::AttackerWin, _) => GameStateUpdate::AttackerWin,
            _ => event,
        }
    }
}

impl GameState {
    pub fn make_play(&mut self, play: &Play) -> Result<GameStateUpdate, ()> {
        if !self.board.on(play.from) || !self.board.on(play.to) {
            return Err(());
        }
        let valid = match self.turn {
            Player::Defender => self.is_valid_defender_play(play),
            Player::Attacker => self.is_valid_attacker_play(play),
        };
        if !valid {
            return Err(());
        }
        self.board.swap(play.from, play.to);
        let mut info = self.check_capture(play);
        if self.is_defender_victory() {
            info = info.update(GameStateUpdate::DefenderWin);
        } else {
            self.turn = self.turn.next();
        }
        Ok(info)
    }

    fn is_valid_defender_play(&self, play: &Play) -> bool {
        // Does the piece exist at the starting position?
        let from = self.board[play.from];
        let piece: Result<Piece, _> = from.try_into();
        let piece = match piece {
            Ok(piece) => piece,
            Err(_) => return false,
        };
        // Is this a piece we can even own?
        if !Player::Defender.owns(piece) {
            return false;
        }
        // Can we sit on the final position?
        let to = self.board[play.to];
        let unoccupied = match piece {
            Piece::King => to == Tile::Empty,
            _ => to == Tile::Empty && play.to != self.castle && !self.board.is_corner(play.to),
        };
        if !unoccupied {
            return false;
        }
        self.is_valid_path(play.from, play.to)
    }

    fn is_valid_attacker_play(&self, play: &Play) -> bool {
        // Does the piece exist at the starting position?
        let from = self.board[play.from];
        let piece: Result<Piece, _> = from.try_into();
        let piece = match piece {
            Ok(piece) => piece,
            Err(_) => return false,
        };
        // Is this a piece we can even own?
        if !Player::Attacker.owns(piece) {
            return false;
        }
        // Can we sit on the final position?
        let to = self.board[play.to];
        let unoccupied =
            to == Tile::Empty && play.to != self.castle && !self.board.is_corner(play.to);
        if !unoccupied {
            return false;
        }
        self.is_valid_path(play.from, play.to)
    }

    /// Checks if the path between from and to is unoccpied and a single horizontal or vertical
    /// movement.
    fn is_valid_path(&self, from: Position, to: Position) -> bool {
        fn range(x0: usize, x1: usize) -> std::ops::RangeInclusive<usize> {
            if x0 < x1 {
                x0..=x1
            } else {
                x1..=x0
            }
        }
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
            for y in range(y0, y1) {
                let position = (x0, y);
                if y != y0 {
                    if self.is_occupied(position) {
                        return false;
                    }
                }
            }
        } else {
            for x in range(x0, x1) {
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

    fn check_capture(&mut self, play: &Play) -> GameStateUpdate {
        let mut info = GameStateUpdate::Nothing;
        for &next in self
            .board
            .adjacent(play.to)
            .iter()
            .filter_map(|p| p.as_ref())
        {
            match self.board[play.to] {
                Tile::Attacker => match self.board[next] {
                    Tile::Defender => {
                        // check if other side of defender is an attacker
                        let other_side = self.board.step(next, direction(play.to, next));
                        let capture = match other_side {
                            Some(position) => {
                                let owner = self.board[position].owner();
                                match owner {
                                    Some(Player::Attacker) => true,
                                    // corner squares count towards a capture
                                    _ => self.board.is_edge(position),
                                }
                            }
                            None => false,
                        };
                        if capture {
                            self.board[next] = Tile::Empty;
                            info = info.update(GameStateUpdate::AttackerCapture);
                        }
                    }
                    Tile::King => {
                        // check if all sides of king are attackers
                        let capture = self
                            .board
                            .adjacent(next)
                            .iter()
                            .map(|&side| match side {
                                Some(position) => {
                                    let owner = self.board[position].owner();
                                    match owner {
                                        Some(Player::Attacker) => true,
                                        // corner squares count towards a capture
                                        _ => self.board.is_edge(position),
                                    }
                                }
                                // edge counts towards a capture for the king
                                None => self.board.is_edge(next),
                            })
                            .all(|c| c == true);
                        if capture {
                            self.board[next] = Tile::Empty;
                            info = info.update(GameStateUpdate::AttackerWin);
                        }
                    }
                    _ => (),
                },
                Tile::Defender | Tile::King => {
                    match self.board[next] {
                        Tile::Attacker => {
                            // check if other side of attacker is defender or king
                            let other_side = self.board.step(next, direction(play.to, next));
                            let capture = match other_side {
                                Some(position) => {
                                    let owner = self.board[position].owner();
                                    match owner {
                                        Some(Player::Defender) => true,
                                        // corner squares count towards a capture
                                        _ => self.board.is_edge(position),
                                    }
                                }
                                None => false,
                            };
                            if capture {
                                self.board[next] = Tile::Empty;
                                info = info.update(GameStateUpdate::DefenderCapture);
                            }
                        }
                        _ => (),
                    }
                }
                Tile::Empty => unreachable!(),
            }
        }
        info
    }

    fn is_defender_victory(&self) -> bool {
        let (w, h) = self.board.size();
        self.board[(0, 0)] == Tile::King
            || self.board[(w - 1, 0)] == Tile::King
            || self.board[(0, h - 1)] == Tile::King
            || self.board[(w - 1, h - 1)] == Tile::King
    }
}
