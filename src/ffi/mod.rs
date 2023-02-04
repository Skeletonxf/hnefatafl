pub mod results;
pub mod tile_array;

#[derive(Clone, Debug)]
pub enum FFIError {
    NullPointer,
    Panic,
}
