use crate::ffi::results::FFIError;
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

    /// Copies over all values from the array into the buffer
    /// Safety: The caller is responsible for ensuring the buffer is at least as many bytes long as
    /// the array and not aliased anywhere.
    pub unsafe fn copy_to(
        array: *const Array<T>,
        buffer: *mut T,
        context: &'static str,
    ) -> Result<(), FFIError>
    where
        T: Clone,
        T: std::panic::RefUnwindSafe,
    {
        if buffer.is_null() {
            return Err(FFIError::from_null_pointer(context, None));
        }
        Array::with_handle(array, context, |data| {
            let buffer = std::slice::from_raw_parts_mut(buffer, data.len());
            buffer.clone_from_slice(&data)
        })
    }

    /// Returns a value from the array
    pub fn get(
        array: *const Array<T>,
        index: usize,
        context: &'static str,
    ) -> Result<Option<T>, FFIError>
    where
        T: Clone,
        T: std::panic::RefUnwindSafe,
    {
        Array::with_handle(array, context, |data| {
            data.get(index).cloned()
        })
    }

    /// Returns the length of the array
    pub fn length(
        array: *const Array<T>,
        context: &'static str,
    ) -> usize
    where
        T: std::panic::RefUnwindSafe,
    {
        Array::with_handle(array, context, |data| {
            data.len()
        }).unwrap_or_else(|error| {
            eprint!("Error calling array_length: {}", error);
            0
        })
    }

    /// Destroys the data owned by the Array
    /// The caller is responsible for ensuring there are no aliased references elsewhere in the
    /// program
    pub unsafe fn destroy(array: *mut Array<T>) {
        if array.is_null() {
            return;
        }
        std::mem::drop(Box::from_raw(array));
    }
}
