## v2.2.39
* Added six themes — dusk, carbon, mint, plum, paper and latte — one design over four dark palettes and two light
* Added free movement and uniform scaling from the transform gizmo's centre box
* Added planar scale handles that scale the two axes they span
* Added headless UI test runs
* Added more builtin lss
* Improved the rotation gizmo to draw only the near half of each ring, over a faint ball outline
* Improved the planar handles by moving them further out from the centre
* Fixed the transform gizmo's size and picking under an orthographic camera
* Fixed a scene click being broadcast to every interactable instead of the nearest one
* Fixed an option being invisible in the inspector
* Fixed JEI leaking into the published pom as a runtime dependency
* Fixed parallel test shards each running everything, and sizing scenarios differently from a serial run
* Fixed a class-path failure reporting the previous run's result as a pass