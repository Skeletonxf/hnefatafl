use crate::ffi::results::{FFIResult, FFIResultType, get_type, get_ok, get_error};
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
    get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_winner_get_ok(result: *mut FFIResult<Winner, ()>) -> Winner {
    get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_winner_get_error(result: *mut FFIResult<Winner, ()>) -> () {
    get_error(result)
}
