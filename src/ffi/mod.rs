pub mod array;
pub mod results;
pub mod tile_array;
pub mod play_array;

#[derive(Clone, Debug)]
pub enum FFIError {
    NullPointer,
    Panic,
}
