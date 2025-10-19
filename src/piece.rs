use std::cmp::PartialEq;
use std::convert::TryFrom;
use std::fmt::Display;

#[repr(u8)]
#[derive(Clone, Copy, Debug, Eq, PartialEq, uniffi::Enum)]
pub enum Tile {
    Empty = 0,
    Attacker = 1,
    Defender = 2,
    King = 3,
}

impl Display for Tile {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> Result<(), std::fmt::Error> {
        write!(
            f,
            "{}",
            match self {
                Tile::Empty => "_",
                Tile::Attacker => "A",
                Tile::Defender => "D",
                Tile::King => "K",
            }
        )
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum Piece {
    Attacker,
    Defender,
    King,
}

impl Display for Piece {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> Result<(), std::fmt::Error> {
        write!(
            f,
            "{}",
            match self {
                Piece::Attacker => "A",
                Piece::Defender => "D",
                Piece::King => "K",
            }
        )
    }
}

impl PartialEq<Tile> for Piece {
    fn eq(&self, tile: &Tile) -> bool {
        match (tile, self) {
            (Tile::Attacker, Piece::Attacker)
            | (Tile::Defender, Piece::Defender)
            | (Tile::King, Piece::King) => true,
            _ => false,
        }
    }
}

impl PartialEq<Piece> for Tile {
    fn eq(&self, piece: &Piece) -> bool {
        piece.eq(self)
    }
}

impl From<Piece> for Tile {
    fn from(piece: Piece) -> Tile {
        match piece {
            Piece::Attacker => Tile::Attacker,
            Piece::Defender => Tile::Defender,
            Piece::King => Tile::King,
        }
    }
}

impl TryFrom<Tile> for Piece {
    type Error = ();
    fn try_from(tile: Tile) -> Result<Self, Self::Error> {
        match tile {
            Tile::Attacker => Ok(Piece::Attacker),
            Tile::Defender => Ok(Piece::Defender),
            Tile::King => Ok(Piece::King),
            Tile::Empty => Err(()),
        }
    }
}
