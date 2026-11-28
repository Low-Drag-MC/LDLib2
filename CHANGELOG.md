## v2.2.48
* Added multi-selection, marquee drag, batch actions, and wider inline rename to resource views and the asset browser
* Added settable snap increments (`snapTranslate`, `snapScale`, `snapRotate`) to `TransformGizmo`
* Fixed elements with an opacity below 1 clipping overflowing children to their own bounds even when `overflow` is visible (thanks @n1xx1)
* Fixed adaptive sizes on text elements and scroller views flipping and re-laying out every frame
* Fixed a graph panel collapsed from code keeping its toggle open
* Fixed animated scene camera turns swapping yaw and pitch
