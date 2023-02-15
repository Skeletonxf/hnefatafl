use easy_ml::tensors::Tensor;

use crate::piece::Tile;
use crate::state::Board;

/// Encodes a Board of Tiles into a 3 dimensional Tensor where the third dimension encodes
/// each type of piece.
pub fn new_input(board: &Board) -> Tensor<f64, 3> {
    let mut representation: Tensor<f64, 3> = Tensor::empty([("piece", 3), ("y", 11), ("x", 11)], 0.0);

    let mut attackers = representation.select_mut([("piece", 0)]);
    for ([y, x], value) in attackers.iter_reference_mut().with_index() {
        if board[(x, y)] == Tile::Attacker {
            *value = 1.0;
        }
    }

    let mut defenders = representation.select_mut([("piece", 1)]);
    for ([y, x], value) in defenders.iter_reference_mut().with_index() {
        if board[(x, y)] == Tile::Defender {
            *value = 1.0;
        }
    }

    let mut king = representation.select_mut([("piece", 2)]);
    for ([y, x], value) in king.iter_reference_mut().with_index() {
        if board[(x, y)] == Tile::King {
            *value = 10.0;
        }
    }

    representation
}
