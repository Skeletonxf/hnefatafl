use crate::ffi::results::{FFIResult, FFIResultType, get_type, get_ok, get_error};
use crate::ffi::array::{Array, array_get, array_length, array_destroy};
use crate::state::Play;

/// An array of plays.
pub type PlayArray = Array<Play>;

/// A flattened representation of a Play, consisting of 4 u8s for a total size of 4 bytes
#[repr(C)]
#[derive(Clone, Debug)]
pub struct FlatPlay {
    pub from_x: u8,
    pub from_y: u8,
    pub to_x: u8,
    pub to_y: u8,
}

/// Returns a value from the array, or a dummy all 0s Play if out of bounds
#[no_mangle]
pub extern fn play_array_get(array: *const PlayArray, index: usize) -> FlatPlay {
    match array_get(array, index) {
        Err(error) => {
            eprint!("Error calling play_array_get: {:?}", error);
            FlatPlay {
                from_x: 0,
                from_y: 0,
                to_x: 0,
                to_y: 0,
            }
        },
        Ok(None) => FlatPlay {
            from_x: 0,
            from_y: 0,
            to_x: 0,
            to_y: 0,
        },
        Ok(Some(play)) => FlatPlay {
            from_x: play.from.0 as u8,
            from_y: play.from.1 as u8,
            to_x: play.to.0 as u8,
            to_y: play.to.1 as u8,
        },
    }
}

/// Returns the length of the array
#[no_mangle]
pub extern fn play_array_length(array: *const PlayArray) -> usize {
    // TODO: use FFIResult
    array_length(array)
}

/// Destroys the data owned by the PlayArray
/// The caller is responsible for ensuring there are no aliased references elsewhere in the
/// program
#[no_mangle]
pub unsafe extern fn play_array_destroy(array: *mut PlayArray) {
    array_destroy(array);
}

/// Safety: calling this on an invalid pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn result_play_array_get_type(result: *mut FFIResult<*mut PlayArray, ()>) -> FFIResultType {
    get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_play_array_get_ok(result: *mut FFIResult<*mut PlayArray, ()>) -> *mut PlayArray {
    get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_play_array_get_error(result: *mut FFIResult<*mut PlayArray, ()>) -> () {
    get_error(result)
}
