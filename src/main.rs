mod piece;
mod state;

use state::{GameState, Play};

use structopt::StructOpt;

#[derive(StructOpt, Debug)]
#[structopt(name = "hnefatafl")]
/// Hnefatafl
struct Arguments {
    #[structopt(subcommand)]
    mode: Mode,
}

#[derive(StructOpt, Debug)]
/// Game mode
enum Mode {
    /// Two player game
    TwoPlayer,
}

fn make_play(game: &mut GameState, play: Play) {
    println!("{:?}", play);
    let info = game.make_play(&play).unwrap();
    println!("{}", game);
    println!("{:?}\n", info);
}

fn main() {
    let arguments = Arguments::from_args();
    match arguments.mode {
        Mode::TwoPlayer => two_player(),
    }
}

fn two_player() {
    let mut game = GameState::default();
    println!("{}\n", game);
}
