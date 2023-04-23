use crate::ffi::results::FFIError;

use std::sync::Mutex;
use std::panic::RefUnwindSafe;

pub(crate) trait MutexHandle<T>: AsRef<Mutex<T>> + RefUnwindSafe {
    /// Takes an (optionally) aliased handle to the inner type, unlocks the mutex and performs
    /// an operation with a non aliased mutable reference to the game state, returning the
    /// result of the operation or an error if there was a failure with the FFI.
    fn with_handle<F, R>(
        handle: *const Self,
        context: &'static str,
        op: F
    ) -> Result<R, FFIError>
    where
        F: FnOnce(&mut T) -> R + std::panic::UnwindSafe,
    {
        if handle.is_null() {
            return Err(FFIError::from_null_pointer(context, None));
        }
        std::panic::catch_unwind(|| {
            // SAFETY: We only give out valid pointers, and are trusting that the Kotlin code
            // does not invalidate them.
            let handle = unsafe {
                &*handle
            };
            // Since the Kotlin side can freely alias as much as it likes, we have the aliased handle
            // contain a Mutex so we can ensure no aliasing for the actual wrapped type
            let mut guard = match handle.as_ref().lock() {
                Ok(guard) => guard,
                Err(poison_error) => {
                    eprintln!("Poisoned mutex: {}", poison_error);
                    poison_error.into_inner()
                },
            };
            op(&mut guard)
            // drop mutex guard
        }).map_err(|panic| FFIError::from_panic(panic, context))
    }
}

impl <T, H: AsRef<Mutex<T>> + RefUnwindSafe> MutexHandle<T> for H {}

pub(crate) trait ReferenceHandle<T>: AsRef<T> + RefUnwindSafe {
    /// Takes an (optionally) aliased handle to the inner type and performs
    /// an operation with an immutable reference to it, returning the
    /// result of the operation or an error if there was a failure with the FFI.
    fn with_handle<F, R>(
        reference: *const Self,
        context: &'static str,
        op: F
    ) -> Result<R, FFIError>
    where
        F: FnOnce(&T) -> R + std::panic::UnwindSafe,
    {
        if reference.is_null() {
            return Err(FFIError::from_null_pointer(context, None));
        }
        std::panic::catch_unwind(|| {
            // SAFETY: We only give out valid pointers, and are trusting that the Kotlin code
            // does not invalidate them.
            let reference = unsafe {
                &*reference
            };
            op(reference.as_ref())
        }).map_err(|panic| FFIError::from_panic(panic, context))
    }
}

impl <T, H: AsRef<T> + RefUnwindSafe> ReferenceHandle<T> for H {}
