// Only want these allowed inside function bodies but no way to configure the lint for that :(
#![allow(mixed_script_confusables)]

use uniffi;

mod bot;
mod config;
mod piece;
mod state;
mod uffi;

uniffi::setup_scaffolding!();
