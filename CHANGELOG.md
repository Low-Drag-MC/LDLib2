## v2.2.41
* Added auto layout to the graph's contextual menu, with layered, grid and force-directed algorithms, and placemats arranged either as one box or from the inside out
* Added selectable wire route styles — curved, octilinear, orthogonal and straight
* Added a Get/Set choice when a variable is dropped on a graph that can write one
* Added canAuthorLiteral, so the item library defaults to the supported types a literal can be authored of
* Added authored tooltips to port builders, matching option builders
* Added an lss sprite wrap mode, kebab-case enum values and rect corner segments
* Improved graph snapping to take a node by whichever of its four edges is nearest, and to line it up with the edges and centres of nearby elements behind a guide
* Improved the graph view to remember its snapping and wire style per graph type
* Improved the resource view's tab strip to be resizable, wrapping, reorderable by drag and placeable on any of the four sides, remembered per editor
* Fixed a client being unable to join a server that does not have LDLib (thanks @TcatHeBlueCreper)
* Fixed dragging several elements at once changing the spacing between them
* Fixed a ResourceProvider crash
* Fixed the item stack configurator crashing on a n**u**ll stack, which the block state one already survives
