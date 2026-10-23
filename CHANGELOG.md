## v26.3.2.43
* Added `@ManagedCodec`, which saves and syncs a managed field with a codec of your own instead of the accessor registered for its type, from methods a subclass can override
* Added `SceneLighting` and `Scene#setLighting`, for choosing how a scene lights its entities and block entities
* Added highlighting where files dragged in from outside the game would be dropped
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
* Fixed block entities and entities behind glass or water in a scene not showing
* Fixed name tags, flames, thrown items and the like in a scene not turning to the scene's camera
* Fixed a scene lighting its entities and block entities by whatever was drawn before it; they are now lit as in the world by default
* Fixed a cached scene's blocks vanishing for good once it was recompiled, and each recompile leaking the GPU buffers it replaced
* Fixed a graph or UI editor opened from the resource panel landing behind the Scene tab, and opening a resource that is already open giving it a second editor instead of bringing its tab forward
* Fixed UI editor undo not bringing back a removed tab's content, and not reverting a property change when another kind of edit was made in between
* Fixed the asset browser's search running on after it was closed, and a file in a folder no provider covers being read twice, with graphs there failing to save
* Fixed a shortcut key being kept from the game whenever a view merely listened for its command, rather than only when one acted on it
* Fixed the file dialog failing to jump to a path it shows itself, and navigating outside its root folder
* Fixed a submenu flipped over its parent menu drawing under that menu's entries
* Fixed tree lists leaving a removed child's row behind when folded, unselecting rows that stay through a re-diff, and not following a flattened root's top rows
* Fixed a tree list row's arrow selecting its row when clicked and turning it a third time when double-clicked
* Fixed unbinding a data source or an observer leaving its tick listener behind (thanks @Darkona)
* Fixed an empty fluid slot showing no tooltip (thanks @Darkona)
* Fixed scene gizmos and overlays being covered by particles
* Fixed a crash when loading an ingredient slot from XML (thanks @vladisfaer)
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
