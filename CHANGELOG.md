## v26.2.2.43.a
* Fixed a cached scene drawing nothing once its blocks changed after its first compile, such as an editor scene filled after it was first shown
* Fixed a scene compile cancelled while still running writing into, or freeing, the buffers the next compile builds into
* Fixed UI test worlds not freezing time, weather, mob spawning and fire, their game rules being set by names 26.2 no longer has
* Improved UI test frame waits to allow time for each frame rather than a fixed timeout, so several test clients on one GPU no longer time out
