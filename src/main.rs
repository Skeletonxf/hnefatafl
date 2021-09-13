mod piece;
mod state;

use state::GameState;

fn main() {
    println!("{}", GameState::default());
}
