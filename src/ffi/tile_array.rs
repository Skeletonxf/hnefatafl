use crate::ffi::results::{FFIResult, FFIError, FFIResultType};
use crate::ffi::array::Array;
use crate::piece::Tile;

/// An array of tiles.
pub type TileArray = Array<Tile>;

/// Returns a value from the array, or Empty if out of bounds
#[no_mangle]
pub extern fn tile_array_get(array: *const TileArray, index: usize) -> Tile {
    match Array::get(array, index, "tile_array_get") {
        Err(error) => {
            eprint!("Error calling tile_array_get: {}", error);
            Tile::Empty
        },
        Ok(None) => Tile::Empty,
        Ok(Some(tile)) => tile,
    }
}

/// Returns the length of the array
#[no_mangle]
pub extern fn tile_array_length(array: *const TileArray) -> usize {
    // TODO: use FFIResult
    Array::length(array, "tile_array_length")
}

/// Destroys the data owned by the TileArray
/// The caller is responsible for ensuring there are no aliased references elsewhere in the
/// program
#[no_mangle]
pub unsafe extern fn tile_array_destroy(array: *mut TileArray) {
    Array::destroy(array);
}

/// Safety: calling this on an invalid pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn result_tile_array_error_get_type(result: *mut FFIResult<*mut TileArray, *mut FFIError>) -> FFIResultType {
    FFIResult::get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_tile_array_error_get_ok(result: *mut FFIResult<*mut TileArray, *mut FFIError>) -> *mut TileArray {
    FFIResult::get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_tile_array_error_get_error(result: *mut FFIResult<*mut TileArray, *mut FFIError>) -> *mut FFIError {
    FFIResult::get_error(result)
}
