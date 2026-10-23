## v26.1.2.43
* Added `@ManagedCodec`, which saves and syncs a managed field with a codec of your own instead of the accessor registered for its type, from methods a subclass can override
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
