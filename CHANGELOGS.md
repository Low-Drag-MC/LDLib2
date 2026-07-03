# ChangeLogs
## v2.2.27
* Improved draw lines smoothness
* Improved LDShaderInstance APIs
* Fixed TextField selection with font size/bold
* Improved model loading
* Improve qol of styles
* Improved progressbar layout

## v2.2.26
* Fixed incorrect rpc method calling
* Added RPCMethod annotation support for interface

## v2.2.25.a
* Added config to disable layout restore
* Fixed splitwindow crash

## v2.2.25
* Fixed node preview rebuilt
* Fixed editor split window restore

## v2.2.24
* Fixed DirectArray sync

## v2.2.23
* Fixed z-index draw
* Fixed GraphView keydown event doesn’t use
* Improved IDataConsumer + IObserbale apis. + Added xei shift pause scroll

## v2.2.22
* Fixed JEI recipe slot size

## v2.2.21
* Improved GraphPanel qoe
* Fixed Menu API
* Fixed ScrollDataSource (#48 thanks @DaningSnow0517)
* Fixed model loading issue (#49 thanks @Arcomit)
* Fixed GraphModel deserialize clean nodes cache
* Fixed ae2-jei pattern import (help with @DaningSnow0517)

## v2.2.20
* Added ResourceManager fallback while server loading
* Added zh_cn.lang (#47, thanks @Arcomit, @Moflop)

## v2.2.19
* Improved ngt APIs
* Fixed SearchComponent dialog
* Optimize UI rendering hot paths and reduce runtime allocations (#44, thanks @Bogdan)
* Fixed RectTexture Performance

## v2.2.18
* Added port tooltips + Added connection port ui
* Added vertical port container + Preview
* Added more ngt APIs
* Fixed block node preview
* Fixed block node preview
* Fixed locale number parser
* Added GraphLogger
* Added Project default save path

## v2.2.17
* Fixed the editor window to restore the stylesheet
* Removed from using `org.apache.commons.compress.utils.Lists`, (some jre doesn't support it)
* Improved ItemLibrary for node hierarchy

## v2.2.16
* Moved EditorResourceEvent to ModEventBus
* Improved ore styles
* Added BlockStateAccessor

## v2.2.15
* Fixed renderer loading process
* Fixed sync issue while server is unsafe

## v2.2.14
* Fixed editor layout recovery
* Fixed ItemLibrary searching issue
* Improved ItemLibrary dialog scissor
* Fixed EMI integration

## v2.2.13
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

## v2.2.12
* Improved ngt to support custom serialization / configurator during option/port definition
* Improved WorldSceneRenderer to support sync compilation
* Improved stylesheet manager to support merged multiple lss files
* Added scene editor styles
* Fixed INBTSerializable Read-only stream accessor

## v2.2.11
* Improved ngt (node graph toolkit) to support custom configurator and field/owner during option definition.
* Improved configurable api + store inspect status
* Added cache editor layout for reusing
* Improved ui editor view, GNE stylesheeTs
* Added node width resize + snap mode + collapse

## v2.2.10
* Improved editor project api
* Added ContextNode and BlockNode support
* Added a built-in Ore UI Stylesheet

## v2.2.9
* Fixed kjs onMessage duplicated methods
* Fixed EditorWindow restore gui scale
* Added lss support for the VanillaSpriteTexture
* Added StructuredTagEditor
* Added subgraph system to the graph toolkit

## v2.2.8
* Added Map-Like support for ldlib2 sync / serialization
* Fixed selector dialog incorrect position
* Fixed xei drag-place feature to respect element transform

## v2.2.7.a
* Fixed crash while switching variable types

## v2.2.7
* Added a mixin to trigger UI injection in player menus (thanks @Rimevel)
* Added default value for some graph type (primitive, item, fluid, etc)
* Fixed graph toolkit dragging logic
* Fixed graph toolkit rendering
* Fixed graph toolkit deserialization
* Fixed wire rendering
* Fixed TextArea (CodeEditor as well) crash while calling getFont from the server
* Improved graph panel layout
* Improved RPCEvent to support s->c
* Added message system for simple rpc events
* Improved graph type icon
* Fixed dialog position incorrect while transforming
* Temp Fixed for graph dirty check

## v2.2.6
* Fixed crash while changing the type of variables
* Fixed Blackboard clear
* Added variable rename

## v2.2.5
* Fixed HUD overlay default size
* Improved transform2d to support percent
* Added VanillaSpriteTexture
* Improved Graph Toolkit

## v2.2.4.a
* Fixed Dummyworld RegistryAccess for EMI async Thread loading

## v2.2.4
* Fixed animation issue
* Added animation dsl support

## v2.2.3
* Improved xei supports for item/fluid slot
* Improved configurator for resources
* Improved dsl for data binding
* Fixed IManagedObjectAccessor crash
* Fixed style system bugs: broken selector, mutable EMPTY stylesheet
* Added local stylesheet support
* Improved stylesheet resolve performance
* Added new property: `Color` to control self tinted color
* Added sugar syntax for using builtin class in stylesheets(lss)
    * all `__xxx__` can be checked like `:xxx` in lss, similar to the css syntax.
    * for example: `:hover` ==> `.__hovered__`
* Improved UI Debugger to with two more feature tabs: `computed` and `local lss`

## v2.2.2
* better xml support
* fixed progress bar direction
* fix kjs unable to register ui events in the startup script
* graph toolkit improvement

## v2.2.1.a
* Fixed xei compat crash on the server

## v2.2.1
* Fix Editor Resource List Mode

## v2.2.0
* Fix unable to access `assets/` resources on the server side
* Replace `Yoga` Layout with `Taffy` Layout
    * all yoga apis are kept, will be removed since `26.1`
    * `Taffy` is a better layout engine, it is as efficient as `Yoga`, and support more features (e.g. `grid` layout).
* `Node Graph Toolkit` Incubation (https://youtu.be/A7WXmbkIVRo)
    * we implemented the basic features of the node graph toolkit by following the unity GT 0.4.exp
    * it is still under incubation, so the api may change in the future, besides, the editor is not fully supported yet
    * it will be available soon
* Added the `UI Debugger` (F3) to support advanced UI debugging similar to the browser inspector
* Integrated Kotlin STD Library, DSL for UI creation
    * DSL for UI creation, layout, style, event, rpc, binding, etc. Enjoy kotlin sugar!!
    * we added Kotlin STD as a dependency. it doesn't means the ldlib2 will be written in kotlin. the core framework is still written in Java.
    * we plan to gradually migrate the application of UI to Kotlin DSL in the future. (e.g. Graph Toolkit), builtin standard UI Components will still be written in Java.
* Added `HUD(Layer)` supports to display ldlib2 UI as an HUD layer.

## v2.1.9
* Improved ItemSlot API (Thanks @DancingSnow0517)
* Added TagKey + EntityType search configurator
* Fixed scene delta drag to respect the transform

## v2.1.8.a
* Fixed xei drag mouse normal transform

## v2.1.8
* Added Stream (also StreamCodec) support for PersistedParser
* Added flatten parameter for PersistedParser
* Added @ConditionalSynced

## v2.1.7.b
* Fixed EMI compat issue

## v2.1.7.a
* Added parallel style updates
* Fixed id deserialization

## v2.1.7
* Improved performance a lot:
    * batch rendering
    * batch style updates
    * rendering cull
* Improve animation API
* Added QoL features
* Refactor mouse events to respect the transform
* Fixed some minor bugs

## v2.1.6
* Fixed codec bug for enhancement
* Fixed vanilla-like slot interaction conditions

## v2.1.6.a
* Fixed file resource path parser

## v2.1.5.a
* fixed writing direct var of a CollectionAccessor

## v2.1.5
* avoid using frozon registry if the provider is accessible
* better binding strategy
* better file resource parser
* change license to LGPLv3

## v2.1.4
* Added more ui examples
* Added UI xml support
* Shader refactor
* Fixed the inventory slot bug
* Fixed resource provider location

## v2.1.3
* Fixed TransformGizmo rotation behavior
* Added game tests
* UI features:
    * Added overflow clip
    * Added opacity
    * Added `:not()` for stylesheet
    * Added Transition / Animation
    * Refactor `IGUITexture` APIs
    * Minor fixes

## v2.1.2.a (hotfix)
* Fixed Creative Mode Tab crash for production

## v2.1.2 (hotfix)
* Fixed Infinite Loop while loading texture resources

## v2.1.1
* Fixed FrozenRegistryAccess lacks of client-side only RegistryAccess
* Removed test code
* Added KeyBindings for Editor (Thanks @hi4444)

## v2.1.0 (beta release)
* Refactor UI System
    * modern UI layout system
    * modern UI event system
    * data binding system (support data synchronization and rpc event between server <-> remote)
    * stylesheet system
    * massive plug-and-play components
    * in-game UI visual editor
    * kjs support
    * completed document and usage examples
* Remove outdated system
    * widget ui
    * compass
    * node graph
* Many bug fixes
* Many new features and qol
* Documents and examples
* Test code

## v2.0.4
* UI Sync Framework
* Fixed fallback pack resource loading

## v2.0.2
* Move file assets from the `assets` to the `ldlib2` folder
* Fixed cross-OS platform file separator char

## v2.0.1
Added DrawEdges method
Updated Mesh texture
Capture plugin crash
