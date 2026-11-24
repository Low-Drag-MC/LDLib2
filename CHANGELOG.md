## v26.3.2.47
* Added resource groups: `Resources.builder().group(...)` puts several resource types behind one tab of the resource view, which opens a reorderable panel of its types
* Added a HUD preview to the UI editor, at the game screen's size over a live copy of the game, and an "Add a New HUD" preset with named screen regions
* Added reading resources safely from worker threads, and `ResourceInstance#release` and `keep` to let go of what was read while still handing it out to whatever holds it; `getBuiltinProviders` and `getCustomProviders` now return read-only maps
* Added `GizmoView`, so a transform gizmo can be drawn and dragged outside a scene editor, and `TransformGizmo#setHandleFilter` to offer only some of its handles
* Added `StructuredTagEditor#setExpanded` and `isExpanded` to fold entries from code; a new entry now opens into view, selected and ready to be named
* Added nodes of the source node's group first in a wire drag's recommendations, which also no longer open scrolled down
* Added `ScenarioOptions#skipUnless`, so a UI test scenario can report itself skipped with a reason instead of failing
* Fixed resource panels rescanning their folder every tick (now once a second), and resource files being read a byte at a time
* Fixed a failed save shown as a success with the editor closing anyway, and Save As on a folder closing without saving
* Fixed exiting an editor leaving its project open and the editor behind
* Fixed an editor closed by Esc, the server or another screen keeping its own gui scale, and a closed one still taken as shown
* Fixed copying a builtin resource named with a colon crashing the game
* Fixed dropdown dialogs of selectors and search fields staying narrow in a zoomed-in graph, long previews running under the dropdown arrow, and dialogs at the screen edge laying out again every frame
* Fixed narrow text fields showing the end of their text and keeping a scroll they no longer need, number fields taking a drag while inactive, and fields staying marked as error after a value set from code
* Fixed gizmo plane and ring drags flipping the target once the cursor passed the plane's horizon, and the gizmo bar losing its tool when a key picked another
* Fixed closing the selected tab selecting an arbitrary one instead of the one selected before it
* Fixed a scroller's thumb staying put when its range changed
* Fixed UI test worlds not freezing their time of day, weather and mob spawning, so a long run reached night
