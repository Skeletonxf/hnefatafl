use std::fmt::Display;

pub enum Piece {
    Empty,
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
                Piece::Empty => "_",
                Piece::Attacker => "A",
                Piece::Defender => "D",
                Piece::King => "K",
            }
        )
    }
}
