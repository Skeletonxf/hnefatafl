use crate::config::Config;
use crate::ffi::FFIError;
use crate::ffi::results::{FFIResult, FFIResultType, get_type, get_ok, get_error};
use crate::ffi::strings;
use crate::ffi::strings::UTF16Array;

#[derive(Debug)]
pub struct ConfigHandle {
    config: Config,
}

impl ConfigHandle {
    fn from(toml: &str) -> Result<ConfigHandle, toml::de::Error> {
        Config::from(toml).map(|config| ConfigHandle { config })
    }
}

/// Creates a new ConfigHandle from a UTF-16 toml file
#[no_mangle]
pub unsafe extern fn config_handle_new(
    chars: *const u16, length: usize
) -> *mut FFIResult<*mut ConfigHandle, ()> {
    let toml_utf8 = match strings::utf16_to_string(chars, length) {
        Ok(toml_utf8) => toml_utf8,
        Err(error) => {
            eprintln!("Error calling config_handle_new: {:?}", error);
            return FFIResult::new(Err(()));
        }
    };
    FFIResult::new(match std::panic::catch_unwind(|| {
        ConfigHandle::from(&toml_utf8)
    }) {
        Err(panic) => {
            eprintln!("Error calling config_handle_new, panic: {:?}", panic);
            Err(())
        },
        Ok(Ok(handle)) => {
            let boxed = Box::new(handle);
            // let the caller be responsible for managing this memory now
            Ok(Box::into_raw(boxed))
        },
        Ok(Err(parse_error)) => {
            eprintln!("Error calling config_handle_new, parse error: {:?}", parse_error);
            Err(())
        }
    })
}

/// Destroys the data owned by the pointer
/// The caller is responsible for ensuring there are no aliased references elsewhere in the
/// program
#[no_mangle]
pub unsafe extern fn config_handle_destroy(handle: *mut ConfigHandle) {
    if handle.is_null() {
        return;
    }
    std::mem::drop(Box::from_raw(handle));
}

/// Prints the config handle
#[no_mangle]
pub extern fn config_handle_debug(handle: *const ConfigHandle) {
    if let Err(error) = with_handle(handle, |handle| {
        println!("Config handle:\n{:?}", handle);
    }) {
        eprint!("Error calling config_handle_debug: {:?}", error);
    }
}

/// Returns a vec of UTF-16 chars of the locale value
#[no_mangle]
pub extern fn config_handle_locale(
    handle: *const ConfigHandle
) -> *mut FFIResult<*mut UTF16Array, ()> {
    FFIResult::new(with_handle(handle, |handle| {
        strings::string_to_utf16(&handle.config.locale)
    }).map_err(|error| {
        eprintln!("Error calling config_handle_locale: {:?}", error);
        ()
    }))
}

fn with_handle<F, R>(handle: *const ConfigHandle, op: F) -> Result<R, FFIError>
where
    F: FnOnce(&ConfigHandle) -> R + std::panic::UnwindSafe,
{
    if handle.is_null() {
        return Err(FFIError::NullPointer)
    }
    std::panic::catch_unwind(|| {
        // SAFETY: We only give out valid pointers, and are trusting that the Kotlin code
        // does not invalidate them.
        let handle = unsafe {
            &*handle
        };
        op(handle)
    }).map_err(|_| FFIError::Panic)
}

/// Safety: calling this on an invalid pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn result_config_handle_get_type(result: *mut FFIResult<*const ConfigHandle, ()>) -> FFIResultType {
    get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_config_handle_get_ok(result: *mut FFIResult<*const ConfigHandle, ()>) -> *const ConfigHandle {
    get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_config_handle_get_error(result: *mut FFIResult<*const ConfigHandle, ()>) -> () {
    get_error(result)
}
