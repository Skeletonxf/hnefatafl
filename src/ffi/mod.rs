use crate::ffi::results::{FFIResult, FFIResultType, get_type, get_ok, get_error};
use crate::state::{GameState, GameStateUpdate, Play};
use crate::ffi::tile_array::TileArray;
use crate::ffi::play_array::PlayArray;
use crate::ffi::player::{Winner, TurnPlayer};

use std::sync::Mutex;

pub mod array;
pub mod results;
pub mod tile_array;
pub mod play_array;
pub mod player;

#[derive(Clone, Debug)]
pub enum FFIError {
    NullPointer,
    Panic,
}

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

/// Creates a new GameStateHandle
#[no_mangle]
pub extern fn game_state_handle_new() -> *mut GameStateHandle {
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
    if let Err(error) = with_handle(handle, |handle| {
        println!("Game state handle:\n{:?}", handle);
    }) {
        eprint!("Error calling game_state_handle_debug: {:?}", error);
    }
}

/// Returns the tiles in row major order
#[no_mangle]
pub extern fn game_state_handle_tiles(handle: *const GameStateHandle) -> *mut FFIResult<*mut TileArray, ()> {
    FFIResult::new(with_handle(handle, |handle| {
        TileArray::new(handle.tiles())
    }).map_err(|error| {
        eprint!("Error calling game_state_handle_tiles: {:?}", error);
        ()
    }))
}

/// Returns the length of one side of the grid
#[no_mangle]
pub extern fn game_state_handle_grid_size(handle: *const GameStateHandle) -> u8 {
    // TODO: use FFIResult
    with_handle(handle, |handle| {
        handle.size().0
    }).unwrap_or_else(|error| {
        eprint!("Error calling game_state_handle_grid_size: {:?}", error);
        0
    })
}

/// Returns the available plays
#[no_mangle]
pub extern fn game_state_available_plays(handle: *const GameStateHandle) -> *mut FFIResult<*mut PlayArray, ()> {
    FFIResult::new(with_handle(handle, |handle| {
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
        match with_handle(handle, |handle| {
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
pub extern fn game_state_handle_winner(handle: *const GameStateHandle) -> *mut FFIResult<Winner, ()> {
    FFIResult::new(with_handle(handle, |handle| {
        Winner::from(handle.winner())
    }).map_err(|error| {
        eprint!("Error calling game_state_handle_winner: {:?}", error);
        ()
    }))
}

/// Returns the player that is making the current turn
#[no_mangle]
pub extern fn game_state_current_player(handle: *const GameStateHandle) -> *mut FFIResult<TurnPlayer, ()> {
    FFIResult::new(with_handle(handle, |handle| {
        TurnPlayer::from(handle.turn())
    }).map_err(|error| {
        eprint!("Error calling game_state_current_player: {:?}", error);
        ()
    }))
}

/// Returns the turn count. Starts at 0 with Defenders going first, odd turn counts are Attackers'
/// turns.
#[no_mangle]
pub extern fn game_state_handle_turn_count(handle: *const GameStateHandle) -> *mut FFIResult<u32, ()> {
    FFIResult::new(with_handle(handle, |handle| {
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
pub extern fn game_state_handle_dead(handle: *const GameStateHandle) -> *mut FFIResult<*mut TileArray, ()> {
    FFIResult::new(with_handle(handle, |handle| {
        TileArray::new(handle.dead().iter().map(|&piece| piece.into()).collect())
    }).map_err(|error| {
        eprint!("Error calling game_state_handle_dead: {:?}", error);
        ()
    }))
}


/// Takes an (optionally) aliased handle to the game state, unlocks the mutex and performs
/// and operation with a non aliased mutable reference to the game state, returning the
/// result of the operation or an error if there was a failure with the FFI.
fn with_handle<F, R>(handle: *const GameStateHandle, op: F) -> Result<R, FFIError>
where
    F: FnOnce(&mut GameState) -> R + std::panic::UnwindSafe,
{
    if handle.is_null() {
        return Err(FFIError::NullPointer)
    }
    std::panic::catch_unwind(|| {
        // SAFETY: We only give out valid pointers, and are trusting that the Kotlin code
        // does not invalidate them.
        let handle = unsafe {
            &*handle
        };
        // Since the Kotlin side can freely alias as much as it likes, we put the aliased handle
        // around a Mutex so we can ensure no aliasing for the actual game state
        let mut guard = match handle.state.lock() {
            Ok(guard) => guard,
            Err(poison_error) => {
                eprintln!("Poisoned mutex: {}", poison_error);
                poison_error.into_inner()
            },
        };
        op(&mut guard)
        // drop mutex guard
    }).map_err(|_| FFIError::Panic)
}

/// Safety: calling this on an invalid pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn result_game_state_update_get_type(result: *mut FFIResult<GameStateUpdate, ()>) -> FFIResultType {
    get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_game_state_update_get_ok(result: *mut FFIResult<GameStateUpdate, ()>) -> GameStateUpdate {
    get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_game_state_update_get_error(result: *mut FFIResult<GameStateUpdate, ()>) -> () {
    get_error(result)
}

/// Safety: calling this on an invalid pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn result_u32_get_type(result: *mut FFIResult<u32, ()>) -> FFIResultType {
    get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_u32_get_ok(result: *mut FFIResult<u32, ()>) -> u32 {
    get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_u32_get_error(result: *mut FFIResult<u32, ()>) -> () {
    get_error(result)
}
