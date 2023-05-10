use crate::ffi::results::{FFIResult, FFIError, FFIResultType};
use crate::state::Player;

#[repr(u8)]
#[derive(Clone, Copy, Debug)]
pub enum Winner {
    Defenders = 0,
    Attackers = 1,
    None = 2,
}

impl Winner {
    pub fn from(winner: Option<Player>) -> Winner {
        match winner {
            Some(Player::Attacker) => Winner::Attackers,
            Some(Player::Defender) => Winner::Defenders,
            None => Winner::None,
        }
    }
}

/// Safety: calling this on an invalid pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn result_winner_get_type(result: *mut FFIResult<Winner, ()>) -> FFIResultType {
    FFIResult::get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_winner_get_ok(result: *mut FFIResult<Winner, ()>) -> Winner {
    FFIResult::get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_winner_get_error(result: *mut FFIResult<Winner, ()>) -> () {
    FFIResult::get_error(result)
}

#[repr(u8)]
#[derive(Clone, Copy, Debug)]
pub enum TurnPlayer {
    // Jextract codegen can't cope with two unrelated enums sharing variant names :(
    DefendersTurn = 0,
    AttackersTurn = 1,
}

impl TurnPlayer {
    pub fn from(turn: Player) -> TurnPlayer {
        match turn {
            Player::Attacker => TurnPlayer::AttackersTurn,
            Player::Defender => TurnPlayer::DefendersTurn,
        }
    }
}

/// Safety: calling this on an invalid pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn result_player_get_type(result: *mut FFIResult<TurnPlayer, *mut FFIError>) -> FFIResultType {
    FFIResult::get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_player_get_ok(result: *mut FFIResult<TurnPlayer, *mut FFIError>) -> TurnPlayer {
    FFIResult::get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_player_get_error(result: *mut FFIResult<TurnPlayer, *mut FFIError>) -> *mut FFIError {
    FFIResult::get_error(result)
}
