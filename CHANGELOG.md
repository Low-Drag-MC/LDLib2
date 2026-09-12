## v2.2.40
* Added a configurable keymap framework for the editor, with rebindable chords, key contexts and a settings page
* Added a node option API for keeping an option out of the inspector, the opposite of showInInspectorOnly
* Improved the graph toolbar tooltips to show the chord each action is bound to
* Fixed minimizing an editor window without an id crashing the game
* Fixed every editor sharing one recent projects list instead of keeping its own project types
* Fixed a search box offering only the value already chosen until a character was typed
* Fixed a focused text field letting the keys it types bubble on to the container's shortcuts
* Fixed a loaded constant keeping the type it was saved under instead of following its pin's declared type
* Fixed a freshly opened graph drawing its wires at the layer's old offset until a node was dragged
* Fixed the headless test harness disabling the early window on machines that have a display
