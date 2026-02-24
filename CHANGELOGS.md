# ChangeLogs
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
