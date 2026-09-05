# ChangeLogs
## v26.2.2.39
* Added six themes — dusk, carbon, mint, plum, paper and latte — one design over four dark palettes and two light
* Added free movement and uniform scaling from the transform gizmo's centre box
* Added planar scale handles that scale the two axes they span
* Added headless UI test runs
* Added more builtin lss
* Improved the rotation gizmo to draw only the near half of each ring, over a faint ball outline
* Improved the planar handles by moving them further out from the centre
* Improved the parallel and multi-process test runs to come up on the backend `-PgraphicsBackend` pinned
* Fixed a UI in its own window being clipped to the game window's size
* Fixed a UI test run stopping at NeoForge's mod loading warning screen, which any mod in the runtime can raise
* Fixed the transform gizmo's size and picking under an orthographic camera
* Fixed a scene click being broadcast to every interactable instead of the nearest one
* Fixed an option being invisible in the inspector
* Fixed JEI leaking into the published pom as a runtime dependency
* Fixed datagen crashing because it reached for a Minecraft instance it never has
* Fixed a class-path failure reporting the previous run's result as a pass

## v26.2.2.38
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
* Fixed a recipe slot always reporting itself to JEI as render-only, ignoring its IngredientIO
* Fixed a tooltip in a floating window being kept inside the game window
* Fixed builtin UI resources not being openable
* Fixed a test selection that matched nothing reporting a passing run

## v26.2.2.37
* Fixed the resource selector dialog resetting the GUI scale to auto
* Added wire reroute points to the node graph toolkit
* Added MultiPlayer test for dedicated server + clients.
* Fixed cross modular animations
* Improved editor asset browser qol

## v26.2.2.36
* Fixed missing update packet
* Moved JEI calls to use APIs
* Improved view container APIs

## v26.2.2.35
* Improved Auto Tests in the background without taking focus or the physical mouse
* Added DataBindingBuilder hooks
* Improved graphview api
* Routed UIElement modifier checks through a swappable key state source
* Added level of detail and an adaptive grid to the graph view
* Added an in-client UI test harness
* Added hosting a ModularUI in its own OS-level window
* Added dock pane maximize, tab context menus and floating editor views
* Added UI test scenarios for pane maximize and floating windows
* Cached directory listings and made the file tree follow the file system
* Moved the asset browser grid onto the single-pass directory listing
* Rendered scenes into the surface being drawn on rather than the game window
* Added capturing a floating window's own framebuffer in the UI test harness
* Added project icon
* Improved Clipped UI elements against the render target's pixel
* Fixed Closed the gui renderers a floating window and a visual layer leaked

## v26.2.2.33
* Refactored resource file paths to a game relative form
* Added direct file resolution for resource paths without a provider
* Added slider ui element
* Improved the resource container with a bottom bar and reusable cells
* Added an asset browser to the resource view
* Refactored HDR color support
* Improved Menu to keep open when clicking a toggle entry
* Added external file drop to import resources
* Added sorting and resource type filtering to the asset browser
* Improved graph view default max scale
* Added opening projects from the asset browser
* Added recent projects and remembering the asset browser folder per projects
* Improved xei tooltips display
* Improved LocalSlot to support unlimited stack
* Fixed immediately appending tooltip

## v26.2.2.31
* Added smooth font rendering
* Fixed style resolve crash
* Improved resource dialog searching
* Improved ItemLibrary qol
* Improved FileDialog
* Minor Fixes

## v26.2.2.29
* Fixed EnumAccessor weekmap
* Improved the TreeList to support reordering dragging
* Improved ngt qol
* Fixed camera movement
* Fixed BlockLibrary name
* Improved transform gizmo
* Cached dialogAnchor Pos to remove dialog
* Added fallback missport for ngt deserialization and improved save api
* Bumped up jei compat

## v26.2.2.28
* Improved ngt qol

## v26.2.2.27.a
* Fixed vanilla tooltip rendering missing

## v26.2.2.27
* Improved draw lines smoothness
* Improved LDShaderInstance APIs
* Fixed TextField selection with font size/bold
* Improved model loading
* Improve qol of styles
* Improved progressbar layout

## v26.2.2.26
* Fixed incorrect rpc method calling
* Added RPCMethod annotation support for interface
* Fixed tooltips rendering issue

## v26.2.2.25
* Fixed node preview rebuilt
* Fixed editor split window restore
* Added config to disable layout restore
* Fixed splitwindow crash

## v26.1.2.24
* Fixed DirectArray sync

## v26.1.2.23
* Fixed z-index draw
* Fixed GraphView keydown event doesn’t use
* Improved IDataConsumer + IObserbale apis. + Added xei shift pause scroll

## v26.1.2.22
* Improved itemslot/fluidslot drawing function overridable
* Fixed JEI recipe slot size

## v26.1.2.21
* Fixed Menu API
* Fixed ScrollDataSource (#48 thanks @DaningSnow0517)
* Fixed model loading issue (#49 thanks @Arcomit)
* Fixed GraphModel deserialize clean nodes cache
* Fixed ae2-jei pattern import (help with @DaningSnow0517)

## v26.1.2.20
* Added ResourceManager fallback while server loading
* Added zh_cn.lang (#47, thanks @Arcomit, @Moflop)
* Improved GraphPanel qoe

## v26.1.2.19
* Improved ngt APIs
* Fixed SearchComponent dialog
* Optimize UI rendering hot paths and reduce runtime allocations (#44, thanks @Bogdan)
* Fixed RectTexture Performance
* Fixed FluidSlot pickup (#46, thanks @xinxinsuried)
* Added scene camera context

## v26.1.2.18
* Added port tooltips + Added connection port ui
* Added vertical port container + Preview
* Added more ngt APIs
* Fixed block node preview
* Fixed block node preview
* Fixed locale number parser
* Added GraphLogger
* Added Project default save path

## v26.1.2.17
* Fixed the editor window to restore the stylesheet
* Removed from using `org.apache.commons.compress.utils.Lists`, (some jre doesn't support it)
* Improved ItemLibrary for node hierarchy

## v26.1.2.16
* Fixed renderer loading process
* Fixed sync issue while server is unsafe
* Moved EditorResourceEvent to ModEventBus
* Improved ore styles
* Added BlockStateAccessor

## v26.1.2.14
* Fixed editor layout recovery
* Fixed ItemLibrary searching issue
* Improved ItemLibrary dialog scissor

## v26.1.2.13
* Added Scene custom clip-context support
* Added scene xei lookup
* Fixed slot xei api crash
* Fixed ingredientManager invalid if ldlib jei register late
* Fixed ui adaptive size
* Fixed ReadOnlyRef update sync
* Improved serialization to support stream buffer tag
* Improved map collect accessor to support no arg Constructor class instance
* Improved registry search to support I18n
* Improved itemstack selection from inventory

## v26.1.2.12.a
* Fixed ModularHudLayer screen size to respect scale

## v26.1.2.12
* Improved ngt to support custom serialization / configurator during option/port definition
* Improved WorldSceneRenderer to support sync compilation
* Improved stylesheet manager to support merged multiple lss files
* Added scene editor styles

## v26.1.2.11
* Improved ngt (node graph toolkit) to support custom configurator and field/owner during option definition.
* Improved configurable api + store inspect status
* Added cache editor layout for reusing
* Improved ui editor view, GNE stylesheeTs
* Added node width resize + snap mode + collapse

## v26.1.2.10
* Improved editor project api
* Added ContextNode and BlockNode support
* Added a built-in Ore UI Stylesheet

## v2.2.9
* Fixed kjs onMessage duplicated methods
* Fixed EditorWindow restore gui scale
* Added lss support for the VanillaSpriteTexture
* Added StructuredTagEditor
* Added subgraph system to the graph toolkit
* Added JEI support