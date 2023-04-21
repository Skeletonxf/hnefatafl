use crate::config::Config;
use crate::ffi::results::{FFIResult, FFIResultType, get_type, get_ok, get_error};
use crate::ffi::strings;
use crate::ffi::strings::UTF16Array;
use crate::ffi::handle::MutexHandle;

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
        Config::from(toml).map(|config| ConfigHandle { config: Mutex::new(config) })
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
    if let Err(error) = ConfigHandle::with_handle(handle, |config| {
        println!("Config:\n{:?}", config);
    }) {
        eprint!("Error calling config_handle_debug: {:?}", error);
    }
}

/// Returns a vec of UTF-16 chars of the string key value
#[no_mangle]
pub extern fn config_handle_get_string_key(
    handle: *const ConfigHandle,
    key: ConfigStringKey,
) -> *mut FFIResult<*mut UTF16Array, ()> {
    FFIResult::new(ConfigHandle::with_handle(handle, |config| {
        match key {
            ConfigStringKey::Locale => strings::string_to_utf16(&config.locale)
        }
    }).map_err(|error| {
        eprintln!("Error calling config_handle_locale: {:?}", error);
        ()
    }))
}

/// Sets a UTF-16 string to the string key value
#[no_mangle]
pub unsafe extern fn config_handle_set_string_key(
    handle: *const ConfigHandle,
    key: ConfigStringKey,
    chars: *const u16,
    length: usize,
    // return a boolean for now because JExtract/bindgen can't handle an FFI result where both
    // generic types are (), this problem should go away once proper error types are returned
) -> bool {
    let utf8 = match strings::utf16_to_string(chars, length) {
        Ok(toml_utf8) => toml_utf8,
        Err(error) => {
            eprintln!("Error calling config_handle_set_string_key: {:?}", error);
            return false;
        }
    };
    ConfigHandle::with_handle(handle, |config| {
        match key {
            ConfigStringKey::Locale => {
                config.locale = utf8;
            }
        }
    }).map_err(|error| {
        eprintln!("Error calling config_handle_set_string_key: {:?}", error);
        ()
    }).is_ok()
}

// TODO: Get entire TOML file as string for saving

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
