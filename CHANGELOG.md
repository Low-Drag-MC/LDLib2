## v26.3.2.45
* Added `MarkStrategies`, a registry of cheap ways to tell whether values of a class changed, and `IContentVersion` for values that count their own changes
* Added `UIRecording`, scripted UI videos with a smooth pointer, click and key overlays, captions and zooms, run with `-PldRecord`, on OpenGL and Vulkan alike
* Added a gui scale of their own to OS windows, picked by right-clicking a floated tab or from View > Window Size
* Added `UIElement#reorderChildren`, which puts an element's children in a new order without detaching them, so focus and running animations survive it
* Added `UIElement#getContentHost`, the element that children added from outside belong in, which for a `ScrollerView` is its inner container
* Added `ModularUI#addFrameCallback` and `removeFrameCallback`, for work that has to run every frame before styles and layout are computed
* Improved async sync to send one packet per chunk each period, and to leave the changes of chunks nobody watches unencoded until someone does, while still saving them
* Improved async sync to call `setChanged` once per block entity per period instead of once per changed field
* Improved change detection of managed fields, comparing NeoForge's item, fluid and resource stack handlers slot by slot instead of serializing them every period, comparing collections and maps of strings, numbers, enums and registry entries directly, and encoding codec marked fields once instead of twice when they change
* Improved attaching and detaching async logics to constant time, so unloading many block entities at once no longer stalls the server
* Improved `UIElement#getOrCreateMessageRPC` so subclasses can call it, and register the named-message RPC from their constructor on both sides
* Improved a cancelled scene compile to stop at the next block rather than at the end of its block group
* Improved the editor's "minimized" toast to update the one already showing instead of queueing another
* Improved UI test frame waits to allow time for each frame rather than a fixed timeout, so several test clients on one GPU no longer time out
* Fixed translucent and masked elements in an OS window of another gui size being squeezed or stretched to the game window's
* Fixed a scene compile cancelled while still running writing into, or freeing, the buffers the next compile builds into
* Fixed null elements of managed lists and sets loading back as `_NULL_` or as a copy of the element before them
* Fixed item and fluid stack marks sharing component maps with the live stacks
* Fixed sync holders without RPC never leaving the async thread
* Fixed closing an OS window leaving its UI alive, which leaked it and skipped the editor's teardown
* Fixed an editor popped out from code being pulled back into the game window by its settings
* Fixed the UI editor losing JEI after docking back from its own window
* Fixed adaptive scrollers laid out empty never growing, which hid floating windows' tab strips
* Fixed picking a context menu entry leaving focus on the closed menu, so shortcuts did nothing afterwards
* Fixed an element whose id changed after it was added to a UI no longer being found by that id
* Fixed labels and progress bars keeping their tick listener after their data source was unbound
* Fixed `runParTest` and `runMpTest` building the same files from several child builds at once, failing in projects with subprojects, and reporting an earlier run's result when a run died before writing its own
* Fixed UI templates printing a blank line to the console
* Fixed UI test worlds not freezing time, weather, mob spawning and fire, their game rules being set by names that no longer exist
* Fixed recipe viewers being kept on the exclusion areas of a UI's first layout, however its elements moved afterwards
