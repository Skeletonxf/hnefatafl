use crate::state::GameState;
use crate::uniffi;

use std::sync::Mutex;

#[derive(Debug)]
#[derive(uniffi::Object)]
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
}
