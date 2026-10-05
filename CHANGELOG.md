## v26.2.2.42
* Added popping an editor window out into its own OS window
* Added global search and using a folder as a resource provider in the asset browser, and search and sort in resource grids
* Added project types deciding how their files open and adding their own entries to the asset browser's menu
* Added dropping another view's drag payload on an asset browser folder, for an editor to write it there as a file
* Added undo and redo for adding, removing, pasting and moving elements in the UI editor
* Added debugging a running UI editor simulation with the UI debugger, by F12 inside it or from the target picker
* Added Ctrl+A to select all in the graph view
* Added window dialogs remembering the size they are resized to, and fitting themselves to the screen
* Added translating an enum's selector entries by `<enum class>.<CONSTANT>` keys
* Added configurators remembering the field they edit, taking menu entries from the groups around them, and drawing a value as overridden
* Improved the file dialog with a path to jump to and the system folder picker (thanks @spawner1145)
* Improved shortcut commands to bubble from the focused element up to the view that handles them, so copy, paste and undo still reach a view whose child has the focus (thanks @spawner1145)
* Improved the gizmo so a right-click cancels a drag, and the trackball no longer turns as soon as it is pressed
* Improved the asset browser to stop rebuilding for other folders' providers or twice per change, and to find a resource's entry without reading every one listed before it
* Improved UI test runs to play in a game directory of their own, emptied first, so a developer's run/ neither affects them nor collects what they leave behind
* Fixed a disabled panel, such as a read-only inspector, still letting its fields, pickers, dialogs, paste and reordering change values, while foldouts, scrolling and selection keep working
* Fixed renaming a resource to a name its provider cannot store deleting the resource, and a removed file resource being read back when one was made again under its path
* Fixed the asset browser losing track of a folder spelled differently from its root, of providers added or removed elsewhere, and of the path of a resource dragged out of a folder with no provider
* Fixed graph and UI editor tabs showing the file's type suffix and not following a rename made elsewhere or undone
* Fixed a node defined inside another node's definition taking the outer node's later ports and options
* Fixed an option that changes a node's ports or options leaving the inspector with the old ones
* Fixed a graph's own variable declaration type being discarded when its variables were read back
* Fixed menus staying 120 wide, with longer entries cut off or spilling out
* Fixed lines drawn into a scene smaller than the window coming out thinner than their width
* Fixed pasting into a text field taking only text copied inside the game, not from other applications
