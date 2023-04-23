use crate::config::Config;
use crate::ffi::results::{FFIResult, FFIError, FFIResultType};
use crate::ffi::strings;
use crate::ffi::strings::UTF16Array;
use crate::ffi::handle::MutexHandle;
use std::convert::{TryFrom, TryInto};

use std::sync::Mutex;

#[derive(Debug)]
pub struct ConfigHandle {
    config: Mutex<Config>,
}

#[repr(u8)]
pub enum ConfigStringKey {
    Locale = 0
}

impl ConfigHandle {
    fn from(toml: &str) -> Result<ConfigHandle, toml::de::Error> {
        Config::try_from(toml).map(|config| ConfigHandle { config: Mutex::new(config) })
    }
}

impl AsRef<Mutex<Config>> for ConfigHandle {
    fn as_ref(&self) -> &Mutex<Config> {
        &self.config
    }
}

/// Creates a new ConfigHandle from a UTF-16 toml file
#[no_mangle]
pub unsafe extern fn config_handle_new(
    chars: *const u16, length: usize
) -> *mut FFIResult<*mut ConfigHandle, *mut FFIError> {
    let context = "config_handle_new";
    let toml_utf8 = match strings::utf16_to_string(chars, length, context) {
        Ok(toml_utf8) => toml_utf8,
        Err(error) => return FFIResult::new(Err(error.leak())),
    };
    FFIResult::new(match std::panic::catch_unwind(|| {
        ConfigHandle::from(&toml_utf8)
    }) {
        Err(panic) => Err(FFIError::from_panic(panic, context).leak()),
        Ok(Ok(handle)) => {
            let boxed = Box::new(handle);
            // let the caller be responsible for managing this memory now
            Ok(Box::into_raw(boxed))
        },
        Ok(Err(parse_error)) => Err(FFIError::new(Box::new(parse_error), context, None).leak()),
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

/// Returns a vec of UTF-16 chars of the string key value
#[no_mangle]
pub extern fn config_handle_get_string_key(
    handle: *const ConfigHandle,
    key: ConfigStringKey,
) -> *mut FFIResult<*mut UTF16Array, *mut FFIError> {
    FFIResult::new(ConfigHandle::with_handle(handle, "config_handle_get_string_key", |config| {
        match key {
            ConfigStringKey::Locale => strings::string_to_utf16(&config.locale)
        }
    }).map_err(|error| error.leak()))
}

/// Sets a UTF-16 string to the string key value
#[no_mangle]
pub unsafe extern fn config_handle_set_string_key(
    handle: *const ConfigHandle,
    key: ConfigStringKey,
    chars: *const u16,
    length: usize,
) -> *mut FFIResult<(), *mut FFIError> {
    let utf8 = match strings::utf16_to_string(chars, length, "config_handle_set_string_key") {
        Ok(toml_utf8) => toml_utf8,
        Err(error) => return FFIResult::new(Err(error.leak())),
    };
    FFIResult::new(ConfigHandle::with_handle(handle, "config_handle_set_string_key", |config| {
        match key {
            ConfigStringKey::Locale => {
                config.locale = utf8;
            }
        }
    }).map_err(|error| error.leak()))
}

/// Returns a vec of UTF-16 chars of the entire TOML data
#[no_mangle]
pub extern fn config_handle_get_file(
    handle: *const ConfigHandle,
) -> *mut FFIResult<*mut UTF16Array, *mut FFIError> {
    let context = "config_handle_get_file";
    match ConfigHandle::with_handle(handle, context, |config| {
        let c: &Config = config;
        c.try_into().map(|utf8: String| strings::string_to_utf16(&utf8))
    }) {
        Err(error) => FFIResult::new(Err(error.leak())),
        Ok(result) => FFIResult::new(
            result.map_err(
                |error| FFIError::new(
                    Box::new(error),
                    context,
                    Some("Unable to convert data to TOML".to_string())
                ).leak()
            )
        ),
    }
}

/// Safety: calling this on an invalid pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn result_config_handle_get_type(result: *mut FFIResult<*const ConfigHandle, *mut FFIError>) -> FFIResultType {
    FFIResult::get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_config_handle_get_ok(result: *mut FFIResult<*const ConfigHandle, *mut FFIError>) -> *const ConfigHandle {
    FFIResult::get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_config_handle_get_error(result: *mut FFIResult<*const ConfigHandle, *mut FFIError>) -> *mut FFIError {
    FFIResult::get_error(result)
}
