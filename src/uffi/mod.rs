use crate::bot::minmax::min_max_play;
use crate::piece::{Piece, Tile};
use crate::state::{GameState, GameStateUpdate, Play, Player};
use crate::uniffi;

use std::fmt;
use std::sync::Mutex;

mod config;

/// A handle to the game state behind a mutex to allow calling from Kotlin without issue
#[derive(Debug, uniffi::Object)]
pub struct GameStateHandle {
    state: Mutex<GameState>,
}

#[uniffi::export]
impl GameStateHandle {
    #[uniffi::constructor]
    fn new() -> Self {
        GameStateHandle {
            state: Mutex::new(GameState::default()),
        }
    }

    fn debug(&self) -> String {
        self.state
            .lock()
            .expect("Poisoned mutex in GameStateHandle debug")
            .to_string()
    }

    /// Returns the tiles in row major order
    fn tiles(&self) -> Vec<Tile> {
        self.state
            .lock()
            .expect("Poisoned mutex in GameStateHandle tiles")
            .tiles()
    }

    /// Returns the length of one side of the square grid
    fn grid_size(&self) -> u8 {
        self.state
            .lock()
            .expect("Poisoned mutex in GameStateHandle grid_size")
            .size()
            .0
    }

    /// Returns the available plays
    fn available_plays(&self) -> Vec<FlatPlay> {
        self.state
            .lock()
            .expect("Poisoned mutex in GameStateHandle available_plays")
            .available_plays()
            .into_iter()
            .map(|play| play.into())
            .collect()
    }

    /// Makes a play, if legal
    fn make_play(&self, play: FlatPlay) -> Result<GameStateUpdate, InvalidPlayError> {
        let play: Play = play.into();
        self.state
            .lock()
            .expect("Poisoned mutex in GameStateHandle make_play")
            .make_play(&play)
            .map_err(|_| InvalidPlayError::Illegal)
    }

    /// Makes a play with the bot, if legal
    fn make_bot_play(&self) -> Result<GameStateUpdate, PlayError> {
        let mut state = self
            .state
            .lock()
            .expect("Poisoned mutex in GameStateHandle make_bot_play");
        if let Some(play) = min_max_play(&state) {
            state
                .make_play(&play)
                .map_err(|_| PlayError::Illegal(InvalidPlayError::Illegal))
        } else {
            Err(PlayError::None(NoPlayError::None))
        }
    }

    /// Returns the winner, if any
    fn winner(&self) -> Winner {
        self.state
            .lock()
            .expect("Poisoned mutex in GameStateHandle winner")
            .winner()
            .into()
    }

    /// Returns the player that is making the current turn
    fn current_player(&self) -> TurnPlayer {
        self.state
            .lock()
            .expect("Poisoned mutex in GameStateHandle current_player")
            .turn()
            .into()
    }

    /// Returns the turn count. Starts at 0 with Defenders going first, odd turn counts are
    /// Attackers' turns.
    fn turn_count(&self) -> u32 {
        self.state
            .lock()
            .expect("Poisoned mutex in GameStateHandle current_player")
            .turn_count()
    }

    /// Returns the dead pieces
    fn dead(&self) -> Vec<Dead> {
        self.state
            .lock()
            .expect("Poisoned mutex in GameStateHandle current_player")
            .dead()
            .into_iter()
            .map(|piece| piece.into())
            .collect()
    }
}

/// A flattened representation of a Play, consisting of 4 u8s for a total size of 4 bytes
#[repr(C)]
#[derive(Clone, Debug, uniffi::Record)]
pub struct FlatPlay {
    pub from_x: u8,
    pub from_y: u8,
    pub to_x: u8,
    pub to_y: u8,
}

impl From<Play> for FlatPlay {
    fn from(value: Play) -> Self {
        FlatPlay {
            from_x: value.from.0,
            from_y: value.from.1,
            to_x: value.to.0,
            to_y: value.to.1,
        }
    }
}

impl From<FlatPlay> for Play {
    fn from(value: FlatPlay) -> Self {
        Play {
            from: (value.from_x, value.from_y),
            to: (value.to_x, value.to_y),
        }
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq, uniffi::Enum)]
enum InvalidPlayError {
    Illegal,
}

impl fmt::Display for InvalidPlayError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "Play is illegal")
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq, uniffi::Enum)]
enum NoPlayError {
    None,
}

impl fmt::Display for NoPlayError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "No play is available to make")
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq, uniffi::Enum)]
enum PlayError {
    Illegal(InvalidPlayError),
    None(NoPlayError),
}

impl fmt::Display for PlayError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            PlayError::Illegal(error) => error.fmt(f),
            PlayError::None(error) => error.fmt(f),
        }
    }
}

#[repr(u8)]
#[derive(Clone, Copy, Debug, uniffi::Enum)]
pub enum Winner {
    Defenders = 0,
    Attackers = 1,
    None = 2,
}

impl From<Option<Player>> for Winner {
    fn from(value: Option<Player>) -> Self {
        match value {
            Some(Player::Attacker) => Winner::Attackers,
            Some(Player::Defender) => Winner::Defenders,
            None => Winner::None,
        }
    }
}

#[repr(u8)]
#[derive(Clone, Copy, Debug, uniffi::Enum)]
pub enum TurnPlayer {
    Defenders = 0,
    Attackers = 1,
}

impl From<Player> for TurnPlayer {
    fn from(value: Player) -> Self {
        match value {
            Player::Attacker => TurnPlayer::Attackers,
            Player::Defender => TurnPlayer::Defenders,
        }
    }
}

#[repr(u8)]
#[derive(Clone, Copy, Debug, Eq, PartialEq, uniffi::Enum)]
pub enum Dead {
    Attacker = 0,
    Defender = 1,
    King = 2,
}

impl From<&Piece> for Dead {
    fn from(value: &Piece) -> Self {
        match value {
            Piece::Attacker => Dead::Attacker,
            Piece::Defender => Dead::Defender,
            Piece::King => Dead::King,
        }
    }
}
