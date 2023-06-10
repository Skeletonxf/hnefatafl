extern crate cbindgen;

use cbindgen::Language;
use std::env;

fn main() {
    let crate_dir = env::var("CARGO_MANIFEST_DIR").unwrap();

    match cbindgen::Builder::new()
        .with_crate(crate_dir)
        .with_language(Language::C)
        .generate()
    {
        Ok(bindings) => {
            bindings.write_to_file("bindings.h");
        }
        Err(error) => eprintln!("Error: {}", error),
    }
}
