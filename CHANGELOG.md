## v26.1.2.38
* Added moving the UI debugger into its own window, and inspecting any window from it
* Added parallel UI test runs across several client processes
* Added read-only graph viewing and a copy-to-provider dialog
* Added a reusable ItemLibraryPanel split out of the node graph's item library
* Added ITransform so the scene gizmo can drive anything with a transform
* Improved the JEI integration to use only JEI's public API, so a JEI update no longer breaks it
* Improved the rotation gizmo with screen and trackball handles, and rings that are easier to grab
* Improved OS-level windows with always-on-top and remembered bounds
* Improved the item library to recommend same type ports first
* Improved the two-way ScrollerView to swap the scroll wheel's axes with shift
* Updated JEI to 29.34.0.90 and NeoForge to 26.1.2.100
* Fixed a recipe slot always reporting itself to JEI as render-only, ignoring its IngredientIO
* Fixed a tooltip in a floating window being kept inside the game window
* Fixed builtin UI resources not being openable
* Fixed a test selection that matched nothing reporting a passing run
