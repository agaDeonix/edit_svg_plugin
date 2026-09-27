# Vector Drawable Editor UX Design

## Product goal

Turn the current proof-of-concept into a safe, JetBrains-native editor for Android developers who need to inspect and make small changes to existing VectorDrawable resources. The editor prioritizes lossless XML handling and quick edits over becoming a general-purpose drawing application.

## First release

The editor uses a large canvas, a compact toolbar, and a right-hand inspector. It provides Select and Edit Nodes modes, zoom around the pointer, panning, fit and 100% controls, an adaptive grid, cursor coordinates, hover and selected-node states, keyboard nudging, and one IDE undo step per completed edit.

The visual editor remains alongside the platform XML editor. A combined Visual/Split/Code host is desirable, but must use the IntelliJ `TextEditor` rather than embedding a plain text area. If platform API compatibility across builds 242–259 cannot be guaranteed, the release keeps the existing native editor tabs and labels the visual editor clearly.

## Data and compatibility

The path parser accepts the complete SVG/VectorDrawable command set `M/L/H/V/C/S/Q/T/A/Z`, relative variants, repeated argument groups, exponent notation, and adjacent signed numbers. Parsed commands retain enough information for correct rendering. Smooth commands resolve their reflected control points; elliptical arcs are rendered through a deterministic conversion to cubic curves.

Groups form a tree with accumulated rotation, pivot, scale, and translation transforms. Paths in invertible transforms may be edited in local coordinates. Unsupported or unsafe geometry remains visible and selectable but is marked view-only. Opening or viewing a file never rewrites its XML.

## Editing and inspector

The path list exposes names, fill swatches, and editability. The inspector is divided into Path, Fill, Stroke, and Viewport sections. Inputs validate before writing; blank optional values remove their XML attributes. Resource and theme expressions are preserved verbatim when they cannot be resolved for preview.

During a drag the canvas edits an in-memory working copy. PSI is updated once when the gesture completes. Escape cancels the gesture. External XML edits refresh the model without causing a write/reload loop or losing selection when the same XML path still exists.

## Error handling and quality

One malformed path does not hide the rest of the drawable. The UI shows parse diagnostics, read-only reasons, and empty states. Rendering, hit testing, coordinate transforms, and parser behavior are independent of IntelliJ APIs and covered by unit tests. PSI operations and editor synchronization receive platform tests where practical.

## Deferred roadmap

Transform handles, snapping and rulers, creating/deleting nodes, a pen tool, full editing of smooth/arc commands, Android resource resolution, gradients, and visual optimizer diff are intentionally deferred.

