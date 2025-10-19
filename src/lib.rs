// Only want these allowed inside function bodies but no way to configure the lint for that :(
#![allow(mixed_script_confusables)]

use backtrace::Backtrace;
use std::cell::RefCell;
use std::sync::Once;
use uniffi;

mod bot;
mod config;
mod ffi;
mod piece;
mod state;
mod uffi;

uniffi::setup_scaffolding!();

thread_local! {
    static BACKTRACE: RefCell<Option<Backtrace>> = RefCell::new(None);
}

static SET_HOOK: Once = Once::new();

fn initalize() {
    SET_HOOK.call_once(|| {
        std::panic::set_hook(Box::new(|_| {
            let trace = Backtrace::new();
            BACKTRACE.with(move |b| b.borrow_mut().replace(trace));
        }));
    });
}
