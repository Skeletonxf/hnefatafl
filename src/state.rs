use easy_ml::matrices::views::MatrixMut;
use easy_ml::matrices::Matrix;

use crate::piece::{Piece, Tile};

use std::convert::TryInto;
use std::fmt::Display;
use std::ops::{Index, IndexMut};

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum Player {
    Defender,
    Attacker,
}

type Position = (u8, u8);

#[derive(Clone, Debug)]
pub struct GameState {
    board: Board,
    turn: Player,
    winner: Option<Player>,
    dead: Vec<Piece>,
    turn_count: u32,
    // This is redundant state but extremely useful to have O(1) queries for
    king: Position,
}

#[derive(Clone, Debug)]
pub struct Board {
    board: Matrix<Tile>,
    castle: Position,
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

#[repr(u8)]
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum GameStateUpdate {
    DefenderWin = 0,
    AttackerWin = 1,
    /// Defenders captured a piece.
    DefenderCapture = 2,
    /// Attackers captured a piece.
    AttackerCapture = 3,
    Nothing = 4,
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
        self.board.get_reference(position.1 as usize, position.0 as usize)
    }
}

impl IndexMut<Position> for Board {
    fn index_mut(&mut self, position: Position) -> &mut Self::Output {
        // how did I forget to make get_reference_mut a thing?
        self.board
            .try_get_reference_mut(position.1 as usize, position.0 as usize)
            .unwrap()
    }
}

impl Display for Play {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> Result<(), std::fmt::Error> {
        write!(
            f,
            "({},{}) -> ({},{})",
            self.from.0, self.from.1, self.to.0, self.to.1
        )
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
    fn size(&self) -> (u8, u8) {
        let (w, h) = self.board.size();
        (w as u8, h as u8)
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

impl Direction {
    fn directions() -> [Direction; 4] {
        [
            Direction::Left,
            Direction::Up,
            Direction::Right,
            Direction::Down,
        ]
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
            board: Board {
                board,
                castle: (5, 5),
            },
            turn: Player::Defender,
            winner: None,
            dead: vec![],
            turn_count: 0,
            king: (5, 5),
        }
    }

    pub fn from_setup(
        pieces: Matrix<Tile>,
        turn: Player,
    ) -> Self {
        assert_eq!((11, 11), pieces.size(), "Board must be 11x11");
        let board = pieces;
        let (king, _) = board.row_major_iter().with_index().find(|&(_, tile)| tile == Tile::King)
            .expect("1 king must be present in board");
        GameState {
            board: Board {
                board,
                castle: (5, 5),
            },
            turn,
            winner: None,
            dead: vec![],
            turn_count: 0,
            king: (king.0 as u8, king.1 as u8),
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
        writeln!(f, "Turn: {}", self.turn)?;
        let (w, h) = self.board.size();
        write!(f, "  ")?;
        for x in 0..w {
            write!(f, " {}", x)?;
        }
        writeln!(f)?;
        for x in 0..w {
            if x < 10 {
                write!(f, " ")?;
            }
            write!(f, "{}", x)?;
            for y in 0..h {
                write!(f, " {}", self.board[(x, y)])?;
            }
            writeln!(f)?;
        }
        Ok(())
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
        if self.winner.is_some() {
            return Err(());
        }
        let valid = match self.turn {
            Player::Defender => self.is_valid_defender_play(play),
            Player::Attacker => self.is_valid_attacker_play(play),
        };
        if !valid {
            return Err(());
        }
        if self.king == play.from {
            self.king = play.to;
        }
        self.board.swap(play.from, play.to);
        let mut info = self.check_capture(play);
        if self.is_defender_victory() {
            info = info.update(GameStateUpdate::DefenderWin);
        } else {
            self.turn = self.turn.next();
            if info != GameStateUpdate::DefenderWin &&
                info != GameStateUpdate::AttackerWin &&
                self.available_plays().is_empty()
            {
                // give victory to the player that just stopped the other from being able to make
                // any plays (this could be due to capturing all the Attacker's pieces or either
                // player having pieces but being completely boxed in by the other)
                match self.turn {
                    Player::Defender => info = GameStateUpdate::AttackerWin,
                    Player::Attacker => info = GameStateUpdate::DefenderWin,
                }
            }
        }
        match info {
            GameStateUpdate::DefenderWin => self.winner = Some(Player::Defender),
            GameStateUpdate::AttackerWin => self.winner = Some(Player::Attacker),
            _ => (),
        };
        self.turn_count = match self.turn_count.checked_add(1) {
            Some(count) => count,
            None => {
                eprintln!("Ran out of bits to count the turn with");
                return Err(());
            }
        };
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
        if !self.can_stop_at(piece, play.to) {
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
        if !self.can_stop_at(piece, play.to) {
            return false;
        }
        self.is_valid_path(play.from, play.to)
    }

    /// Is this piece allowed to be moved to this position assuming it has a path to it?
    fn can_stop_at(&self, piece: Piece, position: Position) -> bool {
        let to = self.board[position];
        match piece {
            Piece::Defender | Piece::Attacker => {
                to == Tile::Empty
                    && position != self.board.castle
                    && !self.board.is_corner(position)
            }
            Piece::King => to == Tile::Empty,
        }
    }

    /// Is this piece allowed to pass through this position during a movement?
    fn can_pass_through(&self, piece: Piece, position: Position) -> bool {
        let to = self.board[position];
        match piece {
            Piece::Defender | Piece::Attacker => {
                to == Tile::Empty && !self.board.is_corner(position)
            }
            Piece::King => to == Tile::Empty,
        }
    }

    /// Checks if the path between from and to is unoccpied and a single horizontal or vertical
    /// movement.
    fn is_valid_path(&self, from: Position, to: Position) -> bool {
        fn range(x0: u8, x1: u8) -> std::ops::RangeInclusive<u8> {
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
                                    _ => self.board.is_corner(position),
                                }
                            }
                            None => false,
                        };
                        if capture {
                            if let Some(piece) = self.board[next].try_into().ok() {
                                self.dead.push(piece);
                            }
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
                                        _ => self.board.is_corner(position),
                                    }
                                }
                                // edge counts towards a capture for the king
                                None => true,
                            })
                            .all(|c| c == true);
                        if capture {
                            if let Some(piece) = self.board[next].try_into().ok() {
                                self.dead.push(piece);
                            }
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
                                        _ => self.board.is_corner(position),
                                    }
                                }
                                None => false,
                            };
                            if capture {
                                if let Some(piece) = self.board[next].try_into().ok() {
                                    self.dead.push(piece);
                                }
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

    pub fn available_plays(&self) -> Vec<Play> {
        if self.winner.is_some() {
            return vec![];
        }
        let mut plays = Vec::new();
        let (w, h) = self.board.size();
        for x in 0..w {
            for y in 0..h {
                let tile = self.board[(x, y)];
                let piece: Piece = match tile.try_into() {
                    Ok(piece) => piece,
                    Err(_) => continue,
                };
                match (&self.turn, piece) {
                    (Player::Defender, Piece::Defender | Piece::King)
                    | (Player::Attacker, Piece::Attacker) => {
                        for direction in Direction::directions() {
                            let (mut x1, mut y1) = (x, y);
                            while let Some(position) = self.board.step((x1, y1), direction) {
                                if self.can_stop_at(piece, position) {
                                    plays.push(Play {
                                        from: (x, y),
                                        to: position,
                                    });
                                }
                                if self.can_pass_through(piece, position) {
                                    x1 = position.0;
                                    y1 = position.1;
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                    _ => (),
                }
            }
        }
        plays
    }

    pub fn winner(&self) -> Option<Player> {
        self.winner
    }

    pub fn representation(&self) -> easy_ml::tensors::Tensor<f64, 3> {
        crate::bot::new_input(&self.board)
    }

    pub fn tiles(&self) -> Vec<Tile> {
        self.board.board.column_major_iter().collect()
    }

    pub fn size(&self) -> (u8, u8) {
        self.board.size()
    }

    pub fn turn(&self) -> Player {
        self.turn
    }

    pub fn dead(&self) -> &Vec<Piece> {
        &self.dead
    }

    pub fn turn_count(&self) -> u32 {
        self.turn_count
    }

    pub fn king_position(&self) -> Position {
        self.king
    }
}
