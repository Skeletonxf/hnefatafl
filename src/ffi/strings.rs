use crate::ffi::results::{FFIError, FFIResult, FFIResultType};
use crate::ffi::array::Array;
use widestring::{Utf16Str, Utf16String};

/// An array of UTF-16 characters.
pub type UTF16Array = Array<u16>;

pub unsafe fn utf16_to_string(
    chars: *const u16, length: usize, context: &'static str,
) -> Result<String, FFIError> {
    if chars.is_null() {
        return if length == 0 {
            // can't allocate 0 length char arrays easily on the Java side so treat null pointer as
            // empty string
            Ok("".to_string())
        } else {
            Err(
                FFIError::from_null_pointer(
                    context,
                    Some(format!("non zero length input {} but null char pointer", length))
                )
            )
        };
    }
    std::panic::catch_unwind(|| {
        let slice = unsafe { std::slice::from_raw_parts(chars, length) };
        let utf16 = Utf16Str::from_slice_unchecked(slice);
        let utf8: String = utf16.into();
        utf8
    }).map_err(|panic| FFIError::from_panic(panic, context))
}

pub fn string_to_utf16(string: &str) -> *mut UTF16Array {
    let utf16 = Utf16String::from_str(string);
    Array::new(utf16.into_vec())
}

/// Destroys the data owned by the pointer
/// The caller is responsible for ensuring there are no aliased references elsewhere in the
/// program
#[no_mangle]
pub unsafe extern fn utf16_destroy(handle: *mut Vec<u16>) {
    if handle.is_null() {
        return;
    }
    std::mem::drop(Box::from_raw(handle));
}

/// Returns the length of the array
#[no_mangle]
pub extern fn utf16_array_length(array: *const UTF16Array) -> usize {
    // TODO: use FFIResult
    Array::length(array, "utf16_array_length")
}

/// Copies over all values from the array into the buffer
/// Safety: The caller is responsible for ensuring the buffer is at least as many bytes long as
/// the array and not aliased anywhere.
#[no_mangle]
pub unsafe extern fn utf16_array_copy_to(array: *const UTF16Array, buffer: *mut u16) {
    if let Err(error) = Array::copy_to(array, buffer, "utf16_array_copy_to") {
        eprintln!("Error calling utf16_array_copy_to: {}", error);
    }
}

/// Destroys the data owned by the UTF16Array
/// The caller is responsible for ensuring there are no aliased references elsewhere in the
/// program
#[no_mangle]
pub unsafe extern fn utf16_array_destroy(array: *mut UTF16Array) {
    Array::destroy(array);
}

/// Safety: calling this on an invalid pointer is undefined behavior
#[no_mangle]
pub unsafe extern fn result_utf16_array_get_type(result: *mut FFIResult<*mut UTF16Array, ()>) -> FFIResultType {
    FFIResult::get_type(result)
}

/// Safety: calling this on an invalid pointer or an Err variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_utf16_array_get_ok(result: *mut FFIResult<*mut UTF16Array, ()>) -> *mut UTF16Array {
    FFIResult::get_ok(result)
}

/// Safety: calling this on an invalid pointer or an Ok variant is undefined behavior
#[no_mangle]
pub unsafe extern fn result_utf16_array_get_error(result: *mut FFIResult<*mut UTF16Array, ()>) -> () {
    FFIResult::get_error(result)
}
