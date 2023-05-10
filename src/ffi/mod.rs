use crate::ffi::results::{FFIResult, FFIError, FFIResultType};
use crate::state::{GameState, GameStateUpdate, Play};
use crate::ffi::tile_array::TileArray;
use crate::ffi::play_array::PlayArray;
use crate::ffi::player::{Winner, TurnPlayer};
use crate::ffi::handle::MutexHandle;

use std::sync::Mutex;

pub mod array;
pub mod config;
pub mod results;
pub mod tile_array;
pub mod play_array;
pub mod player;
pub mod strings;
pub(crate) mod handle;

#[derive(Debug)]
pub struct GameStateHandle {
    state: Mutex<GameState>,
}

impl GameStateHandle {
    fn new() -> Self {
        GameStateHandle {
            state: Mutex::new(GameState::default()),
        }
    }
}

impl AsRef<Mutex<GameState>> for GameStateHandle {
    fn as_ref(&self) -> &Mutex<GameState> {
        &self.state
    }
}

/// Creates a new GameStateHandle
#[no_mangle]
pub extern fn game_state_handle_new() -> *mut GameStateHandle {
    crate::initalize();
    let boxed = Box::new(GameStateHandle::new());
    // let the caller be responsible for managing this memory now
    Box::into_raw(boxed)
}

/// Destroys the data owned by the pointer
/// The caller is responsible for ensuring there are no aliased references elsewhere in the
/// program
#[no_mangle]
pub unsafe extern fn game_state_handle_destroy(handle: *mut GameStateHandle) {
    if handle.is_null() {
        return;
    }
    std::mem::drop(Box::from_raw(handle));
}

/// Prints the game state
#[no_mangle]
pub extern fn game_state_handle_debug(handle: *const GameStateHandle) {
    if let Err(error) = GameStateHandle::with_handle(handle, "game_state_handle_debug", |handle| {
        println!("Game state handle:\n{:?}", handle);
    }) {
        eprint!("Error calling game_state_handle_debug: {:?}", error);
    }
}

/// Returns the tiles in row major order
#[no_mangle]
pub extern fn game_state_handle_tiles(
    handle: *const GameStateHandle
) -> *mut FFIResult<*mut TileArray, *mut FFIError> {
    FFIResult::new(
        GameStateHandle::with_handle(handle, "game_state_handle_tiles", |handle| {
            TileArray::new(handle.tiles())
        })
            .map_err(|error| error.leak())
    )
}

/// Returns the length of one side of the square grid
#[no_mangle]
pub extern fn game_state_handle_grid_size(handle: *const GameStateHandle) -> u8 {
    // TODO: use FFIResult
    GameStateHandle::with_handle(handle, "game_state_handle_grid_size", |handle| {
        handle.size().0
    }).unwrap_or_else(|error| {
        eprint!("Error calling game_state_handle_grid_size: {:?}", error);
        0
    })
}

/// Returns the available plays
#[no_mangle]
pub extern fn game_state_available_plays(handle: *const GameStateHandle) -> *mut FFIResult<*mut PlayArray, ()> {
    FFIResult::new(GameStateHandle::with_handle(handle, "game_state_available_plays", |handle| {
        PlayArray::new(handle.available_plays())
    }).map_err(|error| {
        eprint!("Error calling game_state_handle_tiles: {:?}", error);
        ()
    }))
}

/// Makes a play, if legal
#[no_mangle]
pub extern fn game_state_handle_make_play(
    handle: *const GameStateHandle,
    from_x: u8,
    from_y: u8,
    to_x: u8,
    to_y: u8,
) -> *mut FFIResult<GameStateUpdate, ()> {
    FFIResult::new(
        match GameStateHandle::with_handle(handle, "game_state_handle_make_play", |handle| {
            handle.make_play(&Play {
                from: (from_x, from_y),
                to: (to_x, to_y),
            })
        }) {
            Ok(Ok(game_state_update)) => Ok(game_state_update),
            Ok(Err(_)) => Err(()),
            Err(error) => {
                eprint!("Error calling game_state_handle_make_play: {:?}", error);
                Err(())
            }
        }
    )
}

/// Returns the winner, if any
#[no_mangle]
pub extern fn game_state_handle_winner(handle: *const GameStateHandle) -> *mut FFIResult<Winner, *mut FFIError> {
    FFIResult::new(GameStateHandle::with_handle(handle, "game_state_handle_winner", |handle| {
        Winner::from(handle.winner())
    }).map_err(|error| error.leak()))
}

/// Returns the player that is making the current turn
#[no_mangle]
pub extern fn game_state_current_player(handle: *const GameStateHandle) -> *mut FFIResult<TurnPlayer, *mut FFIError> {
    FFIResult::new(GameStateHandle::with_handle(handle, "game_state_current_player", |handle| {
        TurnPlayer::from(handle.turn())
    }).map_err(|error| error.leak()))
}

/// Returns the turn count. Starts at 0 with Defenders going first, odd turn counts are Attackers'
/// turns.
#[no_mangle]
pub extern fn game_state_handle_turn_count(handle: *const GameStateHandle) -> *mut FFIResult<u32, ()> {
    FFIResult::new(GameStateHandle::with_handle(handle, "game_state_handle_turn_count", |handle| {
        handle.turn_count()
    }).map_err(|error| {
        eprint!("Error calling game_state_handle_turn_count: {:?}", error);
        ()
    }))
}

/// Returns the dead pieces in row major order (no Empty tiles will actually be in the array, but
/// the TileArray will still return Empty if indexed out of bounds)
///
/// Running out of aliases for the enum variants, so adding a PieceArray type would be problematic
#[no_mangle]
pub extern fn game_state_handle_dead(handle: *const GameStateHandle) -> *mut FFIResult<*mut TileArray, *mut FFIError> {
    FFIResult::new(GameStateHandle::with_handle(handle, "game_state_handle_dead", |handle| {
        TileArray::new(handle.dead().iter().map(|&piece| piece.into()).collect())
    }).map_err(|error| error.leak()))
}

/// Safety: calling this on an invalid pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn result_game_state_update_get_type(result: *mut FFIResult<GameStateUpdate, ()>) -> FFIResultType {
    FFIResult::get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_game_state_update_get_ok(result: *mut FFIResult<GameStateUpdate, ()>) -> GameStateUpdate {
    FFIResult::get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_game_state_update_get_error(result: *mut FFIResult<GameStateUpdate, ()>) -> () {
    FFIResult::get_error(result)
}

/// Safety: calling this on an invalid pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn result_u32_get_type(result: *mut FFIResult<u32, ()>) -> FFIResultType {
    FFIResult::get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_u32_get_ok(result: *mut FFIResult<u32, ()>) -> u32 {
    FFIResult::get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_u32_get_error(result: *mut FFIResult<u32, ()>) -> () {
    FFIResult::get_error(result)
}
