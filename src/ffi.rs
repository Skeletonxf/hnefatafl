use crate::piece::Tile;
use crate::FFIError;

/// An array of tiles.
#[derive(Debug)]
pub struct TileArray {
    // TODO: Might as well make this 2 dimensional indexing by taking a copy of the Matrix?
    data: Vec<Tile>,
}

impl TileArray {
    pub fn new(data: Vec<Tile>) -> *mut Self {
        let boxed = Box::new(TileArray { data });
        // let the caller be responsible for managing this memory now
        Box::into_raw(boxed)
    }
}

/// Returns a value from the array
#[no_mangle]
pub extern fn tile_array_get(array: *const TileArray, index: usize) -> Tile {
    with_array(array, |array| {
        array.data.get(index).cloned().unwrap_or(Tile::Empty)
    }).unwrap_or_else(|error| {
        eprint!("Error calling tile_array_get: {:?}", error);
        Tile::Empty
    })
}

/// Returns the length of the array
#[no_mangle]
pub extern fn tile_array_length(array: *const TileArray) -> usize {
    with_array(array, |array| {
        array.data.len()
    }).unwrap_or_else(|error| {
        eprint!("Error calling tile_array_length: {:?}", error);
        0
    })
}

/// Destroys the data owned by the TileArray
/// The caller is responsible for ensuring there are no aliased references elsewhere in the
/// program
#[no_mangle]
pub unsafe extern fn tile_array_destroy(array: *mut TileArray) {
    if array.is_null() {
        return;
    }
    std::mem::drop(Box::from_raw(array));
}

/// Takes an (optionally) aliased handle to the tile array and performs
/// an operation with an immutable reference to it, returning the
/// result of the operation or an error if there was a failure with the FFI.
fn with_array<F, R>(array: *const TileArray, op: F) -> Result<R, FFIError>
where
    F: FnOnce(&TileArray) -> R + std::panic::UnwindSafe,
{
    if array.is_null() {
        return Err(FFIError::NullPointer)
    }
    std::panic::catch_unwind(|| {
        // SAFETY: We only give out valid pointers, and are trusting that the Kotlin code
        // does not invalidate them.
        let array = unsafe {
            &*array
        };
        op(array)
    }).map_err(|_| FFIError::Panic)
}
