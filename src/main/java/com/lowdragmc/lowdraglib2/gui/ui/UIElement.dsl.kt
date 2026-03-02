package com.lowdragmc.lowdraglib2.gui.ui

import com.lowdragmc.lowdraglib2.gui.sync.bindings.*
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyLayoutStyleDsl
import com.lowdragmc.lowdraglib2.gui.ui.style.BasicStyle
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.StyleAnimationDsl
import dev.vfyjxf.taffy.style.AlignItems
import dev.vfyjxf.taffy.style.FlexDirection
import org.apache.commons.lang3.function.Consumers
import org.apache.commons.lang3.function.Suppliers
import kotlin.reflect.KMutableProperty0

/**
 * DSL Marker to prevent implicit receivers from outer scopes
 */
@DslMarker
annotation class UIDslMarker

// region Class DSL

@UIDslMarker
class ClassPatchDsl(val element: UIElement) {
    var value: Set<String>
        get() = element.classes
        set(v) {
            element.setClasses(*v.toTypedArray())
        }

    fun set(vararg v: String) {
        value = v.toSet()
    }

    operator fun plusAssign(v: String) {
        element.addClasses(v)
    }

    operator fun minusAssign(v: String) {
        element.removeClass(v)
    }

    operator fun String.unaryPlus() {
        element.addClasses(this)
    }

    operator fun String.unaryMinus() {
        element.removeClass(this)
    }
}
// endregion

// region Style DSL
// ----------------------------
// Pipeline helpers (INLINE/DEFAULT/IMPORTANT)
// ----------------------------
fun <T> T.inline(block: T.() -> Unit) where T : Style{
    Style.inlinePipeline(this) { s -> s.apply(block) }
}

fun <T> T.important(block: T.() -> Unit) where T : Style {
    Style.importantPipeline(this) { s -> s.apply(block) }
}

fun <T> T.default(block: T.() -> Unit) where T : Style {
    Style.defaultPipeline(this) { s -> s.apply(block) }
}
// endregion

// region events
class PendingEventOps{
    val add = mutableListOf<UIEventListener>()
    val remove = mutableListOf<UIEventListener>()
}

@UIDslMarker
class EventsDsl internal constructor(
    private val pendingEvents: MutableMap<String, PendingEventOps>,
) {
    private fun ops(type: String): PendingEventOps =
        pendingEvents.getOrPut(type) { PendingEventOps() }

    private fun add(type: String, listener: UIEventListener) {
        ops(type).add += listener
        ops(type).remove -= listener
    }

    private fun remove(type: String, listener: UIEventListener) {
        ops(type).remove += listener
        ops(type).add -= listener
    }

    infix fun String.on(handler: (UIEvent) -> Unit): UIEventListener {
        val listener = UIEventListener { e -> handler(e) }
        add(this, listener)
        return listener
    }

    fun on(eventType: String, listener: UIEventListener): UIEventListener {
        add(eventType, listener)
        return listener
    }

    operator fun String.minusAssign(listener: UIEventListener) {
        remove(this, listener)
    }

    operator fun String.plusAssign(listener: UIEventListener) {
        add(this, listener)
    }
}
// endregion

/**
 * Base interface for all UI builder elements in the DSL
 */
@UIDslMarker
interface UIBuilder<T : UIElement> {
    fun build(): T // Returns the actual LDLib2 UI element
}

/**
 * Represents a specification for constructing an element with customizable properties such as
 * identifier, class modification logic, layout options, and styles.
 */
open class ElementSpec<T : UIElement>(
    var id: String? = null,
    var cls: (ClassPatchDsl.() -> Unit)? = null,
    var layout: (TaffyLayoutStyleDsl.() -> Unit)? = null,
    var style: (BasicStyle.() -> Unit)? = null,
    var focusable: Boolean? = null,
    var visible: Boolean? = null,
    var active: Boolean? = null,
)

/**
 * Container for UI builder elements with children
 */
abstract class UIContainer<T : UIElement, S: ElementSpec<T>>(
    val element: T,
    val spec: (S.() -> Unit)? = null,
) : UIBuilder<T> {
    protected var captureEvents: MutableMap<String, PendingEventOps>? = null
    protected var bubbleEvents: MutableMap<String, PendingEventOps>? = null
    protected var serverCaptureEvents: MutableMap<String, PendingEventOps>? = null
    protected var serverBubbleEvents: MutableMap<String, PendingEventOps>? = null
    protected var onBuild: MutableList<(T) -> Unit>? = null

    protected abstract fun makeSpec(): S?

    override fun build(): T {
        val ele = build(makeSpec());
        onBuild?.forEach { it(ele) }
        return ele
    }

    protected open fun build(spec:S?): T {
        spec?.id?.let((element::setId))
        spec?.layout?.let(TaffyLayoutStyleDsl(element.layout)::apply)
        spec?.style?.let(element.style::apply)
        spec?.cls?.let(ClassPatchDsl(element)::apply)
        spec?.focusable?.let(element::setFocusable)
        spec?.visible?.let(element::setVisible)
        spec?.active?.let(element::setActive)
        applyEvents(element)
        return element
    }

    /**
     * Add a child element to this container
     */
    open fun addChild(child: UIBuilder<*>) {
        element.addChild(child.build())
    }

    fun onBuild(factory: (T) -> Unit) = apply {
        if (onBuild == null) onBuild = mutableListOf()
        onBuild?.add(factory)
    }

    fun <T, D> UIContainer<T, *>.dataSource(dataProvider: IDataProvider<D>) where T : UIElement, T : IDataConsumer<D>{
        (element as? IDataConsumer<D>)?.bindDataSource(dataProvider)
    }

    fun <T, D> UIContainer<T, *>.dataSource(dataProvider: () -> D) where T : UIElement, T : IDataConsumer<D>{
        (element as? IDataConsumer<D>)?.bindDataSource(SupplierDataSource.of(dataProvider))
    }

    fun <T, D> UIContainer<T, *>.observer(observer: IObserver<D>) where T : UIElement, T : IObservable<D>{
        (element as? IObservable<D>)?.bindObserver(observer)
    }

    fun <T, D, K> UIContainer<T, *>.bindUIData(data: K) where T : UIElement, T : IObservable<D>, T : IDataConsumer<D>, K : IDataProvider<D>, K : IObserver<D>{
        dataSource(data)
        observer(data)
    }

    fun <T, D> UIContainer<T, *>.bindUIData(getter: () -> D,
                                            setter: (D) -> Unit) where T : UIElement, T : IObservable<D>, T : IDataConsumer<D>{
        dataSource(getter)
        observer(setter)
    }

    fun <T, D> UIContainer<T, *>.bindUIData(prop: KMutableProperty0<D>) where T : UIElement, T : IObservable<D>, T : IDataConsumer<D>{
        bindUIData(prop::get, prop::set)
    }

    fun <T, D> UIContainer<T, *>.bind(binding: IBinding<D>) where T : UIElement, T : IBindable<D>{
        (element as? IBindable<D>)?.bind(binding)
    }

    inline fun <T, reified D> UIContainer<T, *>.bind(noinline getter: () -> D,
                                                     noinline setter: (D) -> Unit,
                                                     initialValue: D? = null) where T : UIElement, T : IBindable<D> {
        (element as? IBindable<D>)?.bind(bindings(getter, setter, initialValue).build())
    }

    inline fun <T, reified D> UIContainer<T, *>.bind(prop: KMutableProperty0<D>,
                                                     initialValue: D? = null) where T : UIElement, T : IBindable<D> {
        if (initialValue != null) prop.set(initialValue)

        (element as? IBindable<D>)?.bind(
            bindings(
                getter = { prop.get() },
                setter = { prop.set(it) },
                initialValue = initialValue
            ).syncType(D::class.java).build()
        )
    }

    inline fun <T, reified D> UIContainer<T, *>.bindS2C(noinline getter: () -> D, initialValue: D? = null) where T : UIElement, T : IBindable<D>{
        (element as? IBindable<D>)?.bind(bindingsS2C(getter, initialValue).build())
    }

    inline fun <T, reified D> UIContainer<T, *>.bindC2S(noinline setter: (D) -> Unit, initialValue: D? = null) where T : UIElement, T : IBindable<D>{
        (element as? IBindable<D>)?.bind(bindingsC2S(setter, initialValue).build())
    }

    /**
     * Cache event listener add/remove operations, apply them at build() time.
     */
    fun events(capture: Boolean = false, block: EventsDsl.(T) -> Unit) = apply {
        val map = if (capture) {
            captureEvents ?: mutableMapOf<String, PendingEventOps>().also { captureEvents = it }
        } else {
            bubbleEvents ?: mutableMapOf<String, PendingEventOps>().also { bubbleEvents = it }
        }
        EventsDsl(map).block(element)
    }

    /**
     * Cache server event listener add/remove operations, apply them at build() time.
     */
    fun serverEvents(capture: Boolean = false, block: EventsDsl.(T) -> Unit) = apply {
        val map = if (capture) {
            serverCaptureEvents ?: mutableMapOf<String, PendingEventOps>().also { serverCaptureEvents = it }
        } else {
            serverBubbleEvents ?: mutableMapOf<String, PendingEventOps>().also { serverBubbleEvents = it }
        }
        EventsDsl(map).block(element)
    }

    /**
     * Apply layout configuration to a LDLib2 element
     */
    protected fun applyId(spec:S?, element: UIElement) {

    }

    /**
     * Apply layout configuration to a LDLib2 element
     */
    protected fun applyLayout(spec:S?, element: UIElement) {

    }

    /**
     * Apply style configuration to a LDLib2 element
     */
    protected fun applyStyle(spec:S?, element: UIElement) {
    }

    /**
     * Apply Classes to a LDLib2 element
     */
    protected fun applyClasses(spec:S?, element: UIElement) {
    }

    protected fun applyEvents(element: UIElement) {
        captureEvents?.forEach { (type, ops) ->
            ops.add.forEach { element.addEventListener(type, it, true) }
            ops.remove.forEach { element.removeEventListener(type, it, true) }
        }
        bubbleEvents?.forEach { (type, ops) ->
            ops.add.forEach { element.addEventListener(type, it) }
            ops.remove.forEach { element.removeEventListener(type, it) }
        }
        serverCaptureEvents?.forEach { (type, ops) ->
            ops.add.forEach { element.addServerEventListener(type, it, true) }
            ops.remove.forEach { element.removeServerEventListener(type, it, true) }
        }
        serverBubbleEvents?.forEach { (type, ops) ->
            ops.add.forEach { element.addServerEventListener(type, it) }
            ops.remove.forEach { element.removeServerEventListener(type, it) }
        }
    }

    inline fun <T : UIElement, B : UIBuilder<T>> UIContainer<*, *>.add(builder: B, init: B.() -> Unit = {}): B {
        builder.apply(init)
        addChild(builder)
        return builder
    }

    fun api(block: T.() -> Unit) {
        element.block()
    }

    fun animation(start: Boolean = true, block: StyleAnimationDsl.() -> Unit) = apply {
        element.animationDsl(start, block)
    }
}

/**
 * Basic UIElement builder
 */
open class Element<T : UIElement>(
    element: T,
    spec: (ElementSpec<T>.() -> Unit)? = null
) : UIContainer<T, ElementSpec<T>>(element, spec) {
    override fun makeSpec(): ElementSpec<T>? {
        return spec?.let { ElementSpec<T>().apply(it) }
    }
}

/**
 * Root UI builder function
 */
fun element(spec: (ElementSpec<UIElement>.() -> Unit)? = null,
            init: Element<UIElement>.() -> Unit): UIElement {
    return Element(UIElement(), spec).apply(init).build()
}

fun UIContainer<*, *>.element(spec: (ElementSpec<UIElement>.() -> Unit)? = null, init: Element<UIElement>.() -> Unit = {}) =
    add(Element(UIElement(), spec), init)

fun UIContainer<*, *>.row(spec: (ElementSpec<UIElement>.() -> Unit)? = null, init: Element<UIElement>.() -> Unit = {}) =
    add(Element(UIElement().layoutDsl {
        flexDirection(FlexDirection.ROW)
        alignItems(AlignItems.FLEX_START)
    }, spec), init)

fun UIContainer<*, *>.column(spec: (ElementSpec<UIElement>.() -> Unit)? = null, init: Element<UIElement>.() -> Unit = {}) =
    add(Element(UIElement().layoutDsl {
        alignItems(AlignItems.FLEX_START)
    }, spec), init)

fun UIContainer<*, *>.inventorySlots(spec: (ElementSpec<InventorySlots>.() -> Unit)? = null,
                                     init: Element<InventorySlots>.() -> Unit = {}) =
    add(Element(InventorySlots(), spec), init)

fun <T : UIElement> UIContainer<*, *>.dsl(factory: () -> T, spec: (ElementSpec<T>.() -> Unit)? = null, init: Element<T>.() -> Unit = {}): Element<T> {
    val child = Element(factory(), spec).apply(init)
    addChild(child)
    return child
}

fun <T : UIElement> T.dsl(spec: (ElementSpec<T>.() -> Unit)?, init: Element<T>.() -> Unit = {}): Element<T> {
    return Element(this, spec).apply(init)
}

fun <T : UIElement> T.layoutDsl(init: TaffyLayoutStyleDsl.() -> Unit = {}): T {
    TaffyLayoutStyleDsl(this.layout).apply(init)
    return this
}

fun <T : UIElement> T.styleDsl(init: BasicStyle.() -> Unit = {}): T {
    this.style.apply(init)
    return this
}

fun <T : UIElement> T.clsDsl(init: ClassPatchDsl.() -> Unit = {}): T {
    ClassPatchDsl(this).apply(init)
    return this
}

fun <T : UIElement> T.animationDsl(start: Boolean = true, init: StyleAnimationDsl.() -> Unit = {}): T {
    animation { styleAnimation ->
        StyleAnimationDsl(styleAnimation).apply(init)
        if (start) {
            styleAnimation.start()
        }
    }
    return this
}

inline fun <reified T> bindings(
    noinline getter: () -> T,
    noinline setter: (T) -> Unit,
    initialValue: T? = null,
): DataBindingBuilder<T> {
    return DataBindingBuilder.create(getter, setter).syncType(T::class.java).initialValue(initialValue)
}

inline fun <reified T> bindingsS2C(
    noinline getter: () -> T,
    initialValue: T? = null,
): DataBindingBuilder<T> {
    return DataBindingBuilder.create(getter, Consumers.nop())
        .syncType(T::class.java)
        .c2sStrategy(SyncStrategy.NONE)
        .initialValue(initialValue)
}

inline fun <reified T> bindingsC2S(
    noinline setter: (T) -> Unit,
    initialValue: T? = null,
): DataBindingBuilder<T> {
    return DataBindingBuilder.create(Suppliers.nul<T>(), setter)
        .syncType(T::class.java)
        .s2cStrategy(SyncStrategy.NONE)
        .initialValue(initialValue)
}
