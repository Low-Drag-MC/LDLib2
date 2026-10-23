## v2.2.44
* Added `UIElement#reorderChildren`, which puts an element's children in a new order without detaching them, so focus and running animations survive it
* Added `UIElement#getContentHost`, the element that children added from outside belong in, which for a `ScrollerView` is its inner container
* Added `ModularUI#addFrameCallback` and `removeFrameCallback`, for work that has to run every frame before styles and layout are computed
* Improved `UIElement#getOrCreateMessageRPC` so subclasses can call it, and register the named-message RPC from their constructor on both sides
* Fixed an element whose id changed after it was added to a UI no longer being found by that id
* Fixed labels and progress bars keeping their tick listener after their data source was unbound
* Fixed `runParTest` and `runMpTest` building the same files from several child builds at once, failing in projects with NeoGradle subprojects, and reporting an earlier run's result when a run died before writing its own
* Fixed UI templates printing a blank line to the console
