use crate::ffi::handle::ReferenceHandle;

/// A wrapper around a result
pub struct FFIResult<T, E> {
    result: Result<T, E>
}

impl<T, E> AsRef<Result<T, E>> for FFIResult<T, E> {
    fn as_ref(&self) -> &Result<T, E> {
        &self.result
    }
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
    FFIResult::with_handle(result, |result| {
        match result {
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
