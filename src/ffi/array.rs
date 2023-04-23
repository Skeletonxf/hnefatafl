use crate::ffi::FFIError;
use crate::ffi::handle::ReferenceHandle;

/// An array of something
#[derive(Debug)]
pub struct Array<T> {
    data: Vec<T>,
}

impl<T> AsRef<Vec<T>> for Array<T> {
    fn as_ref(&self) -> &Vec<T> {
        &self.data
    }
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
    Array::with_handle(array, |data| {
        data.get(index).cloned()
    })
}

/// Copies over all values from the array into the buffer
/// Safety: The caller is responsible for ensuring the buffer is at least as many bytes long as
/// the array and not aliased anywhere.
pub unsafe fn array_copy_to<T>(array: *const Array<T>, buffer: *mut T) -> Result<(), FFIError>
where
    T: Clone,
    T: std::panic::RefUnwindSafe,
{
    if buffer.is_null() {
        return Err(FFIError::NullPointer);
    }
    Array::with_handle(array, |data| {
        let buffer = std::slice::from_raw_parts_mut(buffer, data.len());
        buffer.clone_from_slice(&data)
    })
}

/// Returns the length of the array
pub fn array_length<T>(array: *const Array<T>) -> usize
where
    T: std::panic::RefUnwindSafe,
{
    Array::with_handle(array, |data| {
        data.len()
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
