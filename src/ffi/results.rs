use crate::ffi::FFIError;

/// A wrapper around a result
pub struct FFIResult<T, E> {
    result: Result<T, E>
}

#[repr(u8)]
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum FFIResultType {
    Ok = 0,
    Err = 1,
    Null = 2,
}

impl<T, E> FFIResult<T, E> {
    pub fn new(result: Result<T, E>) -> *mut Self {
        let boxed = Box::new(FFIResult { result });
        // let the caller be responsible for managing this memory now
        Box::into_raw(boxed)
    }
}

/// Gets the type of the result. In almost all scenarios this should be Ok or Err, but if
/// the pointer is null, invalid or otherwise corrupted, Null may be returned instead.
pub unsafe fn get_type<T, E>(result: *const FFIResult<T, E>) -> FFIResultType
where
    T: std::panic::RefUnwindSafe,
    E: std::panic::RefUnwindSafe,
{
    with_result(result, |result| {
        match result.result {
            Ok(_) => FFIResultType::Ok,
            Err(_) => FFIResultType::Err,
        }
    }).unwrap_or(FFIResultType::Null)
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
