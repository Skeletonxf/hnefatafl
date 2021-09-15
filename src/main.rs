mod piece;
mod state;

use state::{GameState, Play};

fn make_play(game: &mut GameState, play: Play) {
    println!("{:?}", play);
    game.make_play(&play).unwrap();
    println!("{}\n", game);
}

fn main() {
    let mut game = GameState::default();
    println!("{}\n", game);
    make_play(
        &mut game,
        Play {
            from: (3, 5),
            to: (3, 2),
        },
    );
    make_play(
        &mut game,
        Play {
            from: (3, 0),
            to: (3, 1),
        },
    );
    make_play(
        &mut game,
        Play {
            from: (4, 4),
            to: (4, 1),
        },
    );
    make_play(
        &mut game,
        Play {
            from: (0, 3),
            to: (3, 3),
        },
    );
}
