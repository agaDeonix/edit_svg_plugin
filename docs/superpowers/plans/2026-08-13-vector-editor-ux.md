# Vector Drawable Editor UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a safe, JetBrains-native visual editor for inspecting and making precise edits to existing Android VectorDrawable resources.

**Architecture:** Keep parsing, geometry, viewport state, and hit testing free of IntelliJ APIs. Build the Swing editor from focused toolbar, canvas, outline, and inspector components backed by a single editor state; commit completed edits through PSI write commands.

**Tech Stack:** Kotlin 2.0, JVM 17, IntelliJ Platform 2024.2 API, Swing/JB UI, JUnit 5.

## Global Constraints

- Preserve plugin ID `com.editsvg.vector-editor` and compatibility range `242` through `259.*`.
- Viewing a drawable must never rewrite XML.
- Preserve Android resource and theme expressions that cannot be rendered.
- Use one IDE undo command for one completed user gesture.
- Do not add third-party runtime dependencies.

---

### Task 1: Lossless path parsing and rendering

**Files:** Modify `PathData.kt`, `PathDataParser.kt`, `PathGeometry.kt`; extend `PathDataParserTest.kt`; create `PathGeometryTest.kt`.

**Interfaces:** `PathDataParser.parse(String): PathData`; `PathData.toPath2D(): Path2D.Double`; new `SmoothCubicTo`, `SmoothQuadTo`, and `ArcTo` command types.

- [x] Add failing tests for `S/s`, `T/t`, `A/a`, repeated move/arc groups, exponent notation, and adjacent negative numbers.
- [x] Run focused parser tests and confirm failures are caused by unsupported commands.
- [x] Implement command models and parsing into absolute endpoint coordinates without changing existing serialization semantics.
- [x] Add failing geometry tests for reflected smooth controls and arc endpoints/bounds.
- [x] Implement smooth reflection and SVG elliptical-arc-to-cubic conversion in the renderer.
- [x] Run all core tests.

### Task 2: Viewport and hit-testing model

**Files:** Create `CanvasViewport.kt`, `CanvasInteraction.kt`, and corresponding tests.

**Interfaces:** `CanvasViewport.fit`, `zoomAt`, `pan`, `worldToScreen`, `screenToWorld`; `CanvasInteraction.hitHandle`.

- [x] Add failing tests proving zoom keeps the world point under the pointer, fit centers the viewport, and hit testing uses screen-space radius.
- [x] Implement immutable viewport calculations with bounded zoom.
- [x] Implement nearest-handle hit testing with deterministic tie-breaking.
- [x] Run focused and full core tests.

### Task 3: Canvas UX and editor state

**Files:** Refactor `VectorEditorPanel.kt`; create focused `VectorCanvasPanel.kt` and `VectorEditorToolbar.kt`.

**Interfaces:** Toolbar callbacks select/edit mode, fit, and 100%; canvas callbacks commit one `PathData` or select a handle.

- [x] Add unit tests for any state transitions extractable from Swing.
- [x] Replace embedded canvas with the viewport model, wheel zoom, space/middle-button pan, fit, 100%, adaptive grid, hover, selected handle, control lines, cursor coordinates, and keyboard nudging.
- [x] Keep drag changes local, commit only on release, and cancel with Escape.
- [x] Use theme-aware JB colors and standard controls; show explicit empty/error/read-only messages.
- [x] Run tests and compile the plugin.

### Task 4: Inspector and safe PSI writes

**Files:** Modify `VectorInspectorPanel.kt`, `VectorPsiWriter.kt`, `VectorModel.kt`, and add platform/unit tests where supported.

**Interfaces:** `updatePathAttributes` accepts nullable updates that distinguish keep/set/remove; validation prevents invalid numeric writes.

- [x] Add failing tests for validation and attribute removal in a testable value model.
- [x] Implement typed inspector state, inline validation, Enter/apply and Escape/reset behavior.
- [ ] Add sections and coordinate display; preserve resource expressions and render an unresolved-value warning.
- [x] Make blank optional fields remove attributes in a single write action.
- [x] Run tests and compile the plugin.

### Task 5: Group transforms and resilient parsing

**Files:** Modify `VectorModel.kt`, `VectorPsiParser.kt`, rendering code; create transform/parser tests.

**Interfaces:** tree nodes represent group/path/clip-path; each drawable path exposes accumulated `AffineTransform` and an editability reason.

- [x] Add failing tests for nested group transform order.
- [x] Parse and apply nested group transforms.
- [ ] Invert transforms before committing editable handle coordinates; mark non-invertible or smooth/arc paths view-only.
- [x] Mark transformed and smooth/arc paths view-only to prevent unsafe writes.
- [x] Run full tests and plugin compilation.

### Task 6: Platform integration and release verification

**Files:** Modify editor provider/file editor as necessary; add integration tests supported by the platform fixture.

- [ ] Preserve native XML editing using IntelliJ `TextEditor`; implement Visual/Split/Code only if APIs remain binary-compatible across the configured IDE range, otherwise retain native sibling tabs.
- [ ] Persist editor mode, selection identity, viewport, and splitter proportion in `FileEditorState`.
- [ ] Verify external XML edits, Undo/Redo, read-only files, light/dark themes, HiDPI, and small windows manually.
- [ ] Run `./gradlew test`, `./gradlew buildPlugin`, and `./gradlew verifyPlugin`.
- [ ] Review the final diff against the design and record any deferred items explicitly.
