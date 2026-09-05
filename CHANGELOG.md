## v26.2.2.39
* Added six themes — dusk, carbon, mint, plum, paper and latte — one design over four dark palettes and two light
* Added moving the UI debugger into its own window, and inspecting any window from it
* Added parallel UI test runs across several client processes
* Added headless UI test runs
* Added free movement and uniform scaling from the transform gizmo's centre box
* Added planar scale handles that scale the two axes they span
* Added read-only graph viewing and a copy-to-provider dialog
* Added a reusable ItemLibraryPanel split out of the node graph's item library
* Added ITransform so the scene gizmo can drive anything with a transform
* Added more builtin lss
* Improved the JEI integration to use only JEI's public API, so a JEI update no longer breaks it
* Improved the rotation gizmo with screen and trackball handles, and rings that are easier to grab
* Improved the rotation gizmo to draw only the near half of each ring, over a faint ball outline
* Improved the planar handles by moving them further out from the centre
* Improved OS-level windows with always-on-top and remembered bounds
* Improved the item library to recommend same type ports first
* Improved the two-way ScrollerView to swap the scroll wheel's axes with shift
* Improved the parallel and multi-process test runs to come up on the backend `-PgraphicsBackend` pinned
* Fixed a UI in its own window being clipped to the game window's size
* Fixed a UI test run stopping at NeoForge's mod loading warning screen, which any mod in the runtime can raise
* Fixed the transform gizmo's size and picking under an orthographic camera
* Fixed a scene click being broadcast to every interactable instead of the nearest one
* Fixed an option being invisible in the inspector
* Fixed a recipe slot always reporting itself to JEI as render-only, ignoring its IngredientIO
* Fixed a tooltip in a floating window being kept inside the game window
* Fixed builtin UI resources not being openable
* Fixed JEI leaking into the published pom as a runtime dependency
* Fixed datagen crashing because it reached for a Minecraft instance it never has
* Fixed a class-path failure reporting the previous run's result as a pass
* Fixed a test selection that matched nothing reporting a passing run
