// Only want these allowed inside function bodies but no way to configure the lint for that :(
#![allow(mixed_script_confusables)]

use uniffi;

uniffi::setup_scaffolding!();

mod bot;
mod config;
mod piece;
mod state;
mod ffi;
