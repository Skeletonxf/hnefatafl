#!/bin/sh
cargo build --release
cargo run --bin uniffi-bindgen generate --library target/release/libhnefatafl.so --language kotlin --out-dir ui/bindings/src/main/kotlin/io/github/skeletonxf/bindings
