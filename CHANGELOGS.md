# ChangeLogs

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
