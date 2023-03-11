# Hnefatafl

A work in progress [Tafl](https://en.wikipedia.org/wiki/Tafl_games) game implementation.

## License

This project is licensed under the terms of the GNU Affero General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.

## Building the Jetpack Compose GUI app

[jextract](https://github.com/openjdk/jextract) and Java 19 or later must be installed. The path to the jextract needs to be provided in `ui/local.properties`. A normal `cargo build` will generate the `bindings.h` file from the Rust sources. Once this is generated, building the Jetpack Compose app will generate the Java glue code using the Foreign Function & Memory API of Project Panama. Afterwards the JVM app can be ran normally, and it will use the shared object from the built cdylib Rust code.

```
Rust <-> autogenerated C header <-> autogenerated Java bindings <-> Kotlin
 |               |                                  |                |
 |       Generated by cbindgen                      |                |
 |               |                                  |                |
 ------------------- Built by cargo                 |                |
                                 Generated by the jextract tool      |
                                                    |                |
                                 Built by gradle ---------------------
```

[For more details on the language bindings, see my article](https://skeletonxf.github.io/buildingABridge.html)

## Screenshots

![Main menu](/screenshots/main-menu.png?raw=true "Main menu")
![New game](/screenshots/new-game.png?raw=true "New game")
