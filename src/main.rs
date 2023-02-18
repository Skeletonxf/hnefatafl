mod bot;
mod piece;
mod state;

use state::{GameState, GameStateUpdate, Play, Player};

use std::num::ParseIntError;
use std::str::FromStr;

use clap::{Parser, Subcommand};

#[derive(Parser, Debug)]
#[command(name = "hnefatafl")]
/// Hnefatafl
struct Arguments {
    #[command(subcommand)]
    mode: Mode,
}

#[derive(Subcommand, Debug)]
/// Game mode
enum Mode {
    /// Two player game
    TwoPlayer,
}

impl Play {
    fn new(play: Move) -> Self {
        Play {
            from: (play.from.x.into(), play.from.y.into()),
            to: (play.to.x.into(), play.to.y.into()),
        }
    }
}

fn make_play(game: &mut GameState, play: Play) {
    match game.make_play(&play) {
        Ok(info) => {
            println!("{}", game);
            match info {
                GameStateUpdate::DefenderWin => println!("White wins!"),
                GameStateUpdate::AttackerWin => println!("Red wins!"),
                GameStateUpdate::DefenderCapture => println!("Capture!"),
                GameStateUpdate::AttackerCapture => println!("Capture!"),
                GameStateUpdate::Nothing => (),
            }
            println!("");
        }
        Err(_) => println!("Invalid move"),
    }
}

fn main() {
    let arguments = Arguments::parse();
    match arguments.mode {
        Mode::TwoPlayer => two_player(),
    }
}

#[derive(Debug, PartialEq)]
enum ParseMoveError {
    ParseIntError(ParseIntError),
    ParsePositionError,
    ParseMoveError,
}

#[derive(Debug, PartialEq)]
struct Move {
    from: Position,
    to: Position,
}

#[derive(Debug, PartialEq)]
struct Position {
    x: u8,
    y: u8,
}

impl From<ParseIntError> for ParseMoveError {
    fn from(error: ParseIntError) -> Self {
        ParseMoveError::ParseIntError(error)
    }
}

impl FromStr for Position {
    type Err = ParseMoveError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let coords: Vec<&str> = s
            .trim_matches(|p| p == '(' || p == ')')
            .split(',')
            .collect();

        if coords.len() != 2 {
            return Err(ParseMoveError::ParsePositionError);
        }

        Ok(Position {
            x: coords[0].trim().parse()?,
            y: coords[1].trim().parse()?,
        })
    }
}

impl FromStr for Move {
    type Err = ParseMoveError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let (from, to) = s
            .split_once("->")
            .or_else(|| s.split_once("to"))
            .ok_or(ParseMoveError::ParseMoveError)?;

        Ok(Move {
            from: from.trim().parse()?,
            to: to.trim().parse()?,
        })
    }
}

fn two_player() {
    let mut game = GameState::default();
    let mut rl = rustyline::Editor::<()>::new();
    println!("{}\n", game);
    println!("Enter 'enumerate' to list available moves");
    loop {
        //println!("NN encoding: {:?}", game.representation());
        let input = match rl.readline("Enter move: ") {
            Ok(s) => s,
            Err(_) => return,
        };
        if input.trim() == "enumerate" {
            println!("Available moves:\n");
            let plays = game.available_plays();
            let total = plays.len();
            for (i, play) in plays.iter().enumerate() {
                let formatted = play.to_string();
                print!("{} ", formatted);
                for _ in 0.."(10,10) -> (10,11)".len() - formatted.len() {
                    print!(" ");
                }
                if i % 4 == 3 || i == total {
                    println!();
                }
            }
            continue;
        }
        let player_move = input.trim().parse::<Move>();
        match player_move {
            Ok(m) => make_play(&mut game, Play::new(m)),
            Err(_) => {
                println!("Did not understand input, expected input in the form (0, 0) -> (5, 0)")
            }
        };
        if let Some(winner) = game.winner() {
            match winner {
                Player::Attacker => println!("The King was captured!"),
                Player::Defender => println!("The King escapes!"),
            };
            return;
        }
    }
}
