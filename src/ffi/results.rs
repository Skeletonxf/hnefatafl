use crate::FFIError;

/// A wrapper around a result
pub struct FFIResult<T, E> {
    result: Result<T, E>
}

impl<T, E> FFIResult<T, E> {
    pub fn new(result: Result<T, E>) -> *mut Self {
        let boxed = Box::new(FFIResult { result });
        // let the caller be responsible for managing this memory now
        Box::into_raw(boxed)
    }
}

pub unsafe fn get_ok<T, E>(result: *mut FFIResult<T, E>) -> T
where
    E: std::fmt::Debug,
{
    if result.is_null() {
        panic!("Result was null");
    }
    let owned = Box::from_raw(result);
    owned.result.unwrap()
}

pub unsafe fn is_ok<T, E>(result: *const FFIResult<T, E>) -> bool
where
    T: std::panic::RefUnwindSafe,
    E: std::panic::RefUnwindSafe,
{
    with_result(result, |result| {
        result.result.is_ok()
    }).unwrap_or(false)
}

/// Returns the Err value, dropping the result
///
/// SAFETY: It is undefined behavior to call this if the result type is Ok
pub unsafe fn get_error<T, E>(result: *mut FFIResult<T, E>) -> E
where
    T: std::fmt::Debug,
{
    if result.is_null() {
        panic!("Result was null");
    }
    let owned = Box::from_raw(result);
    owned.result.unwrap_err()
}

/// Takes an (optionally) aliased handle to the result and performs
/// an operation with an immutable reference to it, returning the
/// result of the operation or an error if there was a failure with the FFI.
fn with_result<F, R, T, E>(result: *const FFIResult<T, E>, op: F) -> Result<R, FFIError>
where
    F: FnOnce(&FFIResult<T, E>) -> R + std::panic::UnwindSafe,
    T: std::panic::RefUnwindSafe,
    E: std::panic::RefUnwindSafe,
{
    if result.is_null() {
        return Err(FFIError::NullPointer)
    }
    std::panic::catch_unwind(|| {
        // SAFETY: We only give out valid pointers, and are trusting that the Kotlin code
        // does not invalidate them.
        let result = unsafe {
            &*result
        };
        op(result)
    }).map_err(|_| FFIError::Panic)
}
