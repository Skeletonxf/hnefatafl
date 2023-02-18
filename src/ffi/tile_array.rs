use crate::ffi::results::{FFIResult, FFIResultType, get_type, get_ok, get_error};
use crate::ffi::array::{Array, array_get, array_length, array_destroy};
use crate::piece::Tile;

/// An array of tiles.
pub type TileArray = Array<Tile>;

/// Returns a value from the array, or Empty if out of bounds
#[no_mangle]
pub extern fn tile_array_get(array: *const TileArray, index: usize) -> Tile {
    match array_get(array, index) {
        Err(error) => {
            eprint!("Error calling tile_array_get: {:?}", error);
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
    array_length(array)
}

/// Destroys the data owned by the TileArray
/// The caller is responsible for ensuring there are no aliased references elsewhere in the
/// program
#[no_mangle]
pub unsafe extern fn tile_array_destroy(array: *mut TileArray) {
    array_destroy(array);
}

/// Safety: calling this on an invalid pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn result_tile_array_get_type(result: *mut FFIResult<*mut TileArray, ()>) -> FFIResultType {
    get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_tile_array_get_ok(result: *mut FFIResult<*mut TileArray, ()>) -> *mut TileArray {
    get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_tile_array_get_error(result: *mut FFIResult<*mut TileArray, ()>) -> () {
    get_error(result)
}
