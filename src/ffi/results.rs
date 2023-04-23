use crate::ffi::handle::ReferenceHandle;
use crate::ffi::strings;
use crate::ffi::array::Array;
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
    Other(Box<dyn Error + std::panic::RefUnwindSafe>),
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
    pub fn new(
        error: Box<dyn Error + std::panic::RefUnwindSafe>,
        context: &'static str,
        other_info: Option<String>
    ) -> FFIError {
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

/// Returns a vec of UTF-16 chars of the error info, reclaiming the memory for the FFIError.
/// Safety: calling this on an invalid or aliased pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn error_consume_info(
    error: *mut FFIError,
) -> *mut Array<u16> {
    let error_description = if error.is_null() {
        "Error pointer was null".to_string()
    } else {
        let owned = Box::from_raw(error);
        let error: &FFIError = owned.as_ref();
        error.to_string()
        // drop error
    };
    strings::string_to_utf16(&error_description)
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

/// Safety: calling this on an invalid pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn result_void_get_type(result: *mut FFIResult<(), *mut FFIError>) -> FFIResultType {
    FFIResult::get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_void_get_ok(result: *mut FFIResult<(), *mut FFIError>) -> () {
    FFIResult::get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_void_get_error(result: *mut FFIResult<(), *mut FFIError>) -> *mut FFIError {
    FFIResult::get_error(result)
}
