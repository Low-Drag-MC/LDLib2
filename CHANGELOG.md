## v26.3.2.45.a
* Added improved transparency to scenes: while the game uses it, a `WorldSceneRenderer` draws its translucent blocks and features through the order-independent transparency phases, as the level does. `setFollowImprovedTransparency(false)` keeps a scene blending classically, as do orthographic scenes and scenes drawn without depth
