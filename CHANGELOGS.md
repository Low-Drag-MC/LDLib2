# ChangeLogs
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