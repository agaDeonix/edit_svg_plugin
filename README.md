# Vector Drawable Editor

An open-source visual editor for Android Vector Drawable XML files in IntelliJ IDEA and Android Studio.

## Features

- Visual preview with fit, 1:1 zoom, panning, and a configurable grid
- Hierarchy for `vector`, `group`, `path`, and `clip-path` elements
- Contextual property editing for the selected element
- Direct path-node editing with mouse and keyboard
- Group, ungroup, reorder, move, and delete operations
- IDE-native undo and redo
- Vector Drawable path optimization

## Requirements

- IntelliJ IDEA or Android Studio compatible with build 242–259
- Java 17

## Build and test

```bash
./gradlew test buildPlugin
```

The installable archive is written to `build/distributions/`.

To launch a sandbox IDE with the plugin installed:

```bash
./gradlew runIde
```

## Install a local build

1. Build the plugin with `./gradlew buildPlugin`.
2. Open **Settings → Plugins** in IntelliJ IDEA or Android Studio.
3. Choose **Install Plugin from Disk** and select the ZIP from `build/distributions/`.

## Contributing

Bug reports and pull requests are welcome. Please run `./gradlew test` before submitting changes.

## License

Licensed under the [Apache License 2.0](LICENSE).
