## v26.2.2.41
* Added a configurable keymap framework for the editor, with rebindable chords, key contexts and a settings page
* Added auto layout to the graph's contextual menu, with layered, grid and force-directed algorithms, and placemats arranged either as one box or from the inside out
* Added selectable wire route styles — curved, octilinear, orthogonal and straight
* Added a Get/Set choice when a variable is dropped on a graph that can write one
* Added canAuthorLiteral, so the item library defaults to the supported types a literal can be authored of
* Added authored tooltips to port builders, matching option builders
* Added a node option API for keeping an option out of the inspector, the opposite of showInInspectorOnly
* Added an lss sprite wrap mode, kebab-case enum values and rect corner segments
* Improved graph snapping to take a node by whichever of its four edges is nearest, and to line it up with the edges and centres of nearby elements behind a guide
* Improved the graph view to remember its snapping and wire style per graph type
* Improved the resource view's tab strip to be resizable, wrapping, reorderable by drag and placeable on any of the four sides, remembered per editor
* Improved the graph toolbar tooltips to show the chord each action is bound to
* Improved a read-only graph to still offer the wire style picker, which decides how the graph is drawn rather than what it holds
* Fixed the game crashing when a UI element was clipped away to nothing, which 26.2's render pass rejects where the old one drew nothing
* Fixed a contextual menu with two separators losing the one in its middle
* Fixed a code editor letting the keys it acts on bubble on to the container's shortcuts, and swallowing Ctrl+Tab to type an indent
* Fixed a client being unable to join a server that does not have LDLib (thanks @TcatHeBlueCreper)
* Fixed dragging several elements at once changing the spacing between them
* Fixed a ResourceProvider crash
* Fixed the item stack configurator crashing on a null stack, which the block state one already survives
* Fixed minimizing an editor window without an id crashing the game
* Fixed every editor sharing one recent projects list instead of keeping its own project types
* Fixed a search box offering only the value already chosen until a character was typed
* Fixed a focused text field letting the keys it types bubble on to the container's shortcuts
* Fixed a loaded constant keeping the type it was saved under instead of following its pin's declared type
* Fixed a freshly opened graph drawing its wires at the layer's old offset until a node was dragged
* Fixed the headless test harness disabling the early window on machines that have a display
