use crate::ffi::handle::ReferenceHandle;
use backtrace::Backtrace;
use std::error::Error;

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

#[derive(Debug)]
pub struct FFIError {
    error_type: FFIErrorType,
    context: &'static str,
    other_info: Option<String>,
}

#[derive(Debug)]
pub enum FFIErrorType {
    NullPointer,
    Panic(Option<Backtrace>),
    Other(Box<dyn Error>),
}

impl std::fmt::Display for FFIError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "FFIError {:?}", self.error_type)?;
        match &self.other_info {
            None => write!(f, ": {}", self.context),
            Some(string) => write!(f, ": {} - {}", self.context, string),
        }
    }
}

impl Error for FFIError {}

impl FFIError {
    pub fn new(error: Box<dyn Error>, context: &'static str, other_info: Option<String>) -> FFIError {
        FFIError {
            error_type: FFIErrorType::Other(error),
            context,
            other_info,
        }
    }

    pub fn from_panic(panic: Box<dyn std::any::Any + Send>, context: &'static str) -> FFIError {
        let backtrace = crate::BACKTRACE.with(|b| b.borrow_mut().take());
        FFIError {
            error_type: FFIErrorType::Panic(backtrace),
            context,
            other_info: None, // TODO: Try to populate this from the panic value
        }
    }

    pub fn from_null_pointer(context: &'static str, other_info: Option<String>) -> FFIError {
        FFIError {
            error_type: FFIErrorType::NullPointer,
            context,
            other_info,
        }
    }

    pub fn leak(self: FFIError) -> *mut FFIError {
        let boxed = Box::new(self);
        // let the caller be responsible for managing this memory now
        Box::into_raw(boxed)
    }
}

impl<T, E> FFIResult<T, E> {
    pub fn new(result: Result<T, E>) -> *mut Self {
        let boxed = Box::new(FFIResult { result });
        // let the caller be responsible for managing this memory now
        Box::into_raw(boxed)
    }

    /// Gets the type of the result. In almost all scenarios this should be Ok or Err, but if
    /// the pointer is null, invalid or otherwise corrupted, Null may be returned instead.
    pub unsafe fn get_type(result: *const FFIResult<T, E>) -> FFIResultType
    where
        T: std::panic::RefUnwindSafe,
        E: std::panic::RefUnwindSafe,
    {
        FFIResult::with_handle(result, "get_type", |result| {
            match result {
                Ok(_) => FFIResultType::Ok,
                Err(_) => FFIResultType::Err,
            }
        }).unwrap_or(FFIResultType::Null)
    }

    /// Returns the Ok value, dropping the result
    ///
    /// SAFETY: It is undefined behavior to call this if the result type is Err
    pub unsafe fn get_ok(result: *mut FFIResult<T, E>) -> T
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
    pub unsafe fn get_error(result: *mut FFIResult<T, E>) -> E
    where
        T: std::fmt::Debug,
    {
        if result.is_null() {
            panic!("Result was null");
        }
        let owned = Box::from_raw(result);
        owned.result.unwrap_err()
    }
}
