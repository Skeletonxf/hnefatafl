mod bot;
mod piece;
mod state;

use std::sync::Mutex;

use state::GameState;

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

#[derive(Clone, Debug)]
enum FFIError {
    NullPointer,
    Panic,
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

#[no_mangle]
pub extern "C" fn hello_from_rust() {
    println!("Hello from Rust!");
}
