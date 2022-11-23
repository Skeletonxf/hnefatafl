mod bot;
mod piece;
mod state;

use state::GameState;

#[derive(Debug)]
pub struct GameStateHandle {
    state: GameState,
}

impl GameStateHandle {
    fn new() -> Self {
        GameStateHandle {
            state: GameState::default(),
        }
    }
}

#[no_mangle]
pub extern fn game_state_handle_new() -> *mut GameStateHandle {
    let boxed = Box::new(GameStateHandle::new());
    // let the caller be responsible for managing this memory now
    Box::into_raw(boxed)
}

#[no_mangle]
pub extern fn game_state_handle_destroy(handle: *mut GameStateHandle) {
    if handle.is_null() {
        return;
    }
    std::mem::drop(unsafe {
        Box::from_raw(handle)
    });
}

#[no_mangle]
pub extern fn game_state_handle_debug(handle: *mut GameStateHandle) {
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

fn with_handle<F, R>(handle: *mut GameStateHandle, op: F) -> Result<R, FFIError>
where
    F: FnOnce(&mut GameStateHandle) -> R + std::panic::UnwindSafe,
{
    if handle.is_null() {
        return Err(FFIError::NullPointer)
    }
    std::panic::catch_unwind(|| {
        let handle = unsafe {
            &mut *handle
        };
        op(handle)
    }).map_err(|_| FFIError::Panic)
}

#[no_mangle]
pub extern "C" fn hello_from_rust() {
    println!("Hello from Rust!");
}
