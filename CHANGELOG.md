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
