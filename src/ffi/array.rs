use crate::ffi::FFIError;

/// An array of something
#[derive(Debug)]
pub struct Array<T> {
    data: Vec<T>,
}

impl<T> Array<T> {
    pub fn new(data: Vec<T>) -> *mut Self {
        let boxed = Box::new(Array { data });
        // let the caller be responsible for managing this memory now
        Box::into_raw(boxed)
    }
}

/// Returns a value from the array
pub fn array_get<T>(array: *const Array<T>, index: usize) -> Result<Option<T>, FFIError>
where
    T: Clone,
    T: std::panic::RefUnwindSafe,
{
    with_array(array, |array| {
        array.data.get(index).cloned()
    })
}

/// Returns the length of the array
pub fn array_length<T>(array: *const Array<T>) -> usize
where
    T: std::panic::RefUnwindSafe,
{
    with_array(array, |array| {
        array.data.len()
    }).unwrap_or_else(|error| {
        eprint!("Error calling array_length: {:?}", error);
        0
    })
}

/// Destroys the data owned by the Array
/// The caller is responsible for ensuring there are no aliased references elsewhere in the
/// program
pub unsafe fn array_destroy<T>(array: *mut Array<T>) {
    if array.is_null() {
        return;
    }
    std::mem::drop(Box::from_raw(array));
}

/// Takes an (optionally) aliased handle to the array and performs
/// an operation with an immutable reference to it, returning the
/// result of the operation or an error if there was a failure with the FFI.
fn with_array<T, F, R>(array: *const Array<T>, op: F) -> Result<R, FFIError>
where
    T: std::panic::RefUnwindSafe,
    F: FnOnce(&Array<T>) -> R + std::panic::UnwindSafe,
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
