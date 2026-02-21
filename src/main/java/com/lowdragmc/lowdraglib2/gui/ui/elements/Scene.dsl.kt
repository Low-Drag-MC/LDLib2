package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.client.scene.ISceneBlockRenderHook
import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.math.Size
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import org.joml.Vector3f
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * Specification for Scene element
 */
open class SceneSpec<T : Scene>(
    var world: Level? = null,
    var useFBORenderer: Boolean? = null,
    var fboSize: Size? = null,
    var renderedBlocks: Collection<BlockPos>? = null,
    var renderHook: ISceneBlockRenderHook? = null,
    var autoCamera: Boolean? = null,
    var center: Vector3f? = null,
    var rotationPitch: Float? = null,
    var rotationYaw: Float? = null,
    var zoom: Float? = null,
    var orthoRange: Float? = null,
    var renderFacing: Boolean? = null,
    var renderSelect: Boolean? = null,
    var draggable: Boolean? = null,
    var scalable: Boolean? = null,
    var intractable: Boolean? = null,
    var showHoverBlockTips: Boolean? = null,
    var useCache: Boolean? = null,
    var useOrtho: Boolean? = null,
    var autoReleased: Boolean? = null,
    var tickWorld: Boolean? = null,
    var onSelected: BiConsumer<BlockPos, Direction>? = null,
    var beforeWorldRender: Consumer<Scene>? = null,
    var afterWorldRender: Consumer<Scene>? = null,
) : ElementSpec<T>() {
    /**
     * Set world level
     */
    fun world(level: Level, useFBO: Boolean = false, fboSize: Size? = null) = apply {
        this.world = level
        this.useFBORenderer = useFBO
        this.fboSize = fboSize
    }

    /**
     * Set rendered blocks
     */
    fun blocks(vararg positions: BlockPos, hook: ISceneBlockRenderHook? = null) = apply {
        this.renderedBlocks = positions.toList()
        this.renderHook = hook
    }

    /**
     * Set camera position
     */
    fun camera(center: Vector3f, yaw: Float = -135f, pitch: Float = 25f, zoom: Float = 5f) = apply {
        this.center = center
        this.rotationYaw = yaw
        this.rotationPitch = pitch
        this.zoom = zoom
    }

    /**
     * Enable orthographic projection
     */
    fun ortho(range: Float = 1f) = apply {
        this.useOrtho = true
        this.orthoRange = range
    }

    /**
     * Enable cache buffer for better performance
     */
    fun cached() = apply {
        this.useCache = true
    }

    /**
     * Enable user interaction (drag, zoom)
     */
    fun interactive() = apply {
        this.draggable = true
        this.scalable = true
        this.intractable = true
    }

    /**
     * Enable hover tooltips for blocks
     */
    fun withTooltips() = apply {
        this.showHoverBlockTips = true
    }

    /**
     * Set selection callback
     */
    fun onSelect(handler: BiConsumer<BlockPos, Direction>) = apply {
        this.onSelected = handler
    }

    /**
     * Set selection callback (Kotlin lambda)
     */
    fun onSelect(handler: (BlockPos, Direction) -> Unit) = apply {
        this.onSelected = BiConsumer { pos, dir -> handler(pos, dir) }
    }

    /**
     * Set before render callback
     */
    fun beforeRender(handler: Consumer<Scene>) = apply {
        this.beforeWorldRender = handler
    }

    /**
     * Set before render callback (Kotlin lambda)
     */
    fun beforeRender(handler: (Scene) -> Unit) = apply {
        this.beforeWorldRender = Consumer { handler(it) }
    }

    /**
     * Set after render callback
     */
    fun afterRender(handler: Consumer<Scene>) = apply {
        this.afterWorldRender = handler
    }

    /**
     * Set after render callback (Kotlin lambda)
     */
    fun afterRender(handler: (Scene) -> Unit) = apply {
        this.afterWorldRender = Consumer { handler(it) }
    }
}

/**
 * Scene element builder
 */
open class SceneElement<T : Scene>(
    element: T,
    spec: (SceneSpec<T>.() -> Unit)? = null,
) : UIContainer<T, SceneSpec<T>>(element, spec) {
    override fun makeSpec(): SceneSpec<T>? {
        return spec?.let { SceneSpec<T>().apply(it) }
    }

    override fun build(spec: SceneSpec<T>?): T {
        val e = super.build(spec)
        applySceneProperties(spec, e)
        return e
    }

    protected fun applySceneProperties(spec: SceneSpec<T>?, element: Scene) {
        // Apply flags first
        spec?.renderFacing?.let { element.renderFacing = it }
        spec?.renderSelect?.let { element.renderSelect = it }
        spec?.draggable?.let { element.draggable = it }
        spec?.scalable?.let { element.scalable = it }
        spec?.intractable?.let { element.intractable = it }
        spec?.showHoverBlockTips?.let { element.showHoverBlockTips = it }
        spec?.autoReleased?.let { element.autoReleased = it }
        spec?.tickWorld?.let { element.tickWorld = it }

        // Apply callbacks
        spec?.onSelected?.let { element.onSelected = it }
        spec?.beforeWorldRender?.let { element.setBeforeWorldRender(it) }
        spec?.afterWorldRender?.let { element.setAfterWorldRender(it) }

        // Create scene if world is specified
        if (spec?.world != null) {
            element.createScene(spec.world!!, spec.useFBORenderer ?: false, spec.fboSize)

            // Apply cache and ortho settings after scene creation
            spec.useCache?.let { element.useCacheBuffer(it) }
            spec.useOrtho?.let { element.useOrtho(it) }
            spec.orthoRange?.let { element.setOrthoRange(it) }

            // Set rendered blocks if specified
            if (spec.renderedBlocks != null) {
                element.setRenderedCore(spec.renderedBlocks!!, spec.renderHook, spec.autoCamera ?: true)
            }

            // Apply camera settings after blocks are set
            spec.center?.let { element.setCenter(it) }
            spec.zoom?.let { element.setZoom(it) }
            if (spec.rotationYaw != null || spec.rotationPitch != null) {
                element.setCameraYawAndPitch(
                    spec.rotationYaw ?: element.rotationYaw,
                    spec.rotationPitch ?: element.rotationPitch
                )
            }
        }
    }
}

/**
 * Top Level - Create a standalone Scene element
 */
fun scene(spec: (SceneSpec<Scene>.() -> Unit)? = null,
          init: SceneElement<Scene>.() -> Unit = {}): Scene {
    return SceneElement(Scene(), spec).apply(init).build()
}

/**
 * Top Level - Create Scene with world
 */
fun scene(world: Level,
          spec: (SceneSpec<Scene>.() -> Unit)? = null,
          init: SceneElement<Scene>.() -> Unit = {}): Scene {
    val element = Scene()
    element.createScene(world)
    return SceneElement(element, spec).apply(init).build()
}

/**
 * Internal Builder - Add Scene as a child to a container
 */
fun UIContainer<*, *>.scene(spec: (SceneSpec<Scene>.() -> Unit)? = null,
                             init: SceneElement<Scene>.() -> Unit = {}) =
    add(SceneElement(Scene(), spec), init)

/**
 * Internal Builder - Add Scene with world as a child to a container
 */
fun UIContainer<*, *>.scene(world: Level,
                             spec: (SceneSpec<Scene>.() -> Unit)? = null,
                             init: SceneElement<Scene>.() -> Unit = {}) =
    add(SceneElement(Scene().apply { createScene(world) }, spec), init)

/**
 * DSL converter - Convert existing Scene to DSL builder
 */
fun <T : Scene> T.dsl(spec: (SceneSpec<T>.() -> Unit)? = null,
                      init: SceneElement<T>.() -> Unit = {}): SceneElement<T> {
    return SceneElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Create scene with world
 */
fun <T : Scene> SceneElement<T>.createWorld(world: Level, useFBO: Boolean = false, fboSize: Size? = null): SceneElement<T> = apply {
    element.createScene(world, useFBO, fboSize)
}

/**
 * Extension: Set rendered blocks
 */
fun <T : Scene> SceneElement<T>.withBlocks(blocks: Collection<BlockPos>, hook: ISceneBlockRenderHook? = null, autoCamera: Boolean = true): SceneElement<T> = apply {
    element.setRenderedCore(blocks, hook, autoCamera)
}

/**
 * Extension: Set camera center
 */
fun <T : Scene> SceneElement<T>.withCenter(center: Vector3f): SceneElement<T> = apply {
    element.setCenter(center)
}

/**
 * Extension: Set zoom level
 */
fun <T : Scene> SceneElement<T>.withZoom(zoom: Float): SceneElement<T> = apply {
    element.setZoom(zoom)
}

/**
 * Extension: Set camera rotation
 */
fun <T : Scene> SceneElement<T>.withRotation(yaw: Float, pitch: Float): SceneElement<T> = apply {
    element.setCameraYawAndPitch(yaw, pitch)
}

/**
 * Extension: Animated camera rotation
 */
fun <T : Scene> SceneElement<T>.animateRotation(yaw: Float, pitch: Float, duration: Int): SceneElement<T> = apply {
    element.setCameraYawAndPitchAnima(yaw, pitch, duration)
}

/**
 * Extension: Enable cache buffer
 */
fun <T : Scene> SceneElement<T>.useCache(enable: Boolean = true): SceneElement<T> = apply {
    element.useCacheBuffer(enable)
}

/**
 * Extension: Enable orthographic projection
 */
fun <T : Scene> SceneElement<T>.useOrtho(enable: Boolean = true, range: Float = 1f): SceneElement<T> = apply {
    element.useOrtho(enable)
    if (enable) element.setOrthoRange(range)
}

/**
 * Extension: Enable interaction
 */
fun <T : Scene> SceneElement<T>.interactive(draggable: Boolean = true, scalable: Boolean = true): SceneElement<T> = apply {
    element.draggable = draggable
    element.scalable = scalable
    element.intractable = draggable || scalable
}

/**
 * Extension: Enable hover tooltips
 */
fun <T : Scene> SceneElement<T>.withTooltips(enable: Boolean = true): SceneElement<T> = apply {
    element.showHoverBlockTips = enable
}

/**
 * Extension: Set selection callback
 */
fun <T : Scene> SceneElement<T>.onSelect(handler: (BlockPos, Direction) -> Unit): SceneElement<T> = apply {
    element.onSelected = BiConsumer { pos, dir -> handler(pos, dir) }
}

/**
 * Extension: Set before render callback
 */
fun <T : Scene> SceneElement<T>.beforeRender(handler: (Scene) -> Unit): SceneElement<T> = apply {
    element.setBeforeWorldRender(Consumer { handler(it) })
}

/**
 * Extension: Set after render callback
 */
fun <T : Scene> SceneElement<T>.afterRender(handler: (Scene) -> Unit): SceneElement<T> = apply {
    element.setAfterWorldRender(Consumer { handler(it) })
}

/**
 * Extension: Release renderer resources
 */
fun <T : Scene> SceneElement<T>.releaseResources(): SceneElement<T> = apply {
    element.releaseRendererResource()
}

/**
 * Extension: Force recompile cache
 */
fun <T : Scene> SceneElement<T>.recompileCache(): SceneElement<T> = apply {
    element.needCompileCache()
}

/**
 * Extension: Access dummy world
 */
fun <T : Scene> SceneElement<T>.dummyWorld(config: TrackedDummyWorld.() -> Unit): SceneElement<T> = apply {
    element.dummyWorld?.apply(config)
}

