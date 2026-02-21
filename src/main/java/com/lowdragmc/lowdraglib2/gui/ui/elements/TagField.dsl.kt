package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import net.minecraft.nbt.Tag
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * Specification for TagField element
 */
open class TagFieldSpec<T : TagField>(
    var tagValidator: Predicate<Tag>? = null,
    var tagResponder: Consumer<Tag>? = null,
    var value: Tag? = null,
    var textFieldConfig: (TextField.() -> Unit)? = null,
) : ElementSpec<T>() {
    /**
     * Convenience alias for tagResponder
     */
    fun onTagChanged(handler: Consumer<Tag>) = apply {
        this.tagResponder = handler
    }

    /**
     * Kotlin lambda-friendly tag change listener
     */
    fun onTagChanged(handler: (Tag) -> Unit) = apply {
        this.tagResponder = Consumer { handler(it) }
    }

    /**
     * Only allow CompoundTag
     */
    fun compoundTagOnly() = apply {
        // Will be applied in applyTagFieldProperties
    }

    /**
     * Only allow ListTag
     */
    fun listOnly() = apply {
        // Will be applied in applyTagFieldProperties
    }

    /**
     * Allow any tag type
     */
    fun anyTag() = apply {
        // Will be applied in applyTagFieldProperties
    }
}

/**
 * TagField element builder
 */
open class TagFieldElement<T : TagField>(
    element: T,
    spec: (TagFieldSpec<T>.() -> Unit)? = null,
) : UIContainer<T, TagFieldSpec<T>>(element, spec) {
    override fun makeSpec(): TagFieldSpec<T>? {
        return spec?.let { TagFieldSpec<T>().apply(it) }
    }

    override fun build(spec: TagFieldSpec<T>?): T {
        val e = super.build(spec)
        applyTagFieldProperties(spec, e)
        return e
    }

    protected fun applyTagFieldProperties(spec: TagFieldSpec<T>?, element: TagField) {
        spec?.tagValidator?.let { element.setTagValidator(it) }
        spec?.tagResponder?.let { element.setTagResponder(it) }
        spec?.value?.let { element.setValue(it, false) }
        spec?.textFieldConfig?.let { element.textField.apply(it) }
    }
}

/**
 * Top Level - Create a standalone TagField element
 */
fun tagField(spec: (TagFieldSpec<TagField>.() -> Unit)? = null,
             init: TagFieldElement<TagField>.() -> Unit = {}): TagField {
    return TagFieldElement(TagField(), spec).apply(init).build()
}

/**
 * Internal Builder - Add TagField as a child to a container
 */
fun UIContainer<*, *>.tagField(spec: (TagFieldSpec<TagField>.() -> Unit)? = null,
                                init: TagFieldElement<TagField>.() -> Unit = {}) =
    add(TagFieldElement(TagField(), spec), init)

/**
 * DSL converter - Convert existing TagField to DSL builder
 */
fun <T : TagField> T.dsl(spec: (TagFieldSpec<T>.() -> Unit)? = null,
                         init: TagFieldElement<T>.() -> Unit = {}): TagFieldElement<T> {
    return TagFieldElement(this, spec).apply(init)
}

/**
 * Extension: Configure as CompoundTag-only
 */
fun <T : TagField> TagFieldElement<T>.asCompoundTag(): TagFieldElement<T> = apply {
    element.setCompoundTagOnly()
}

/**
 * Extension: Configure as ListTag-only
 */
fun <T : TagField> TagFieldElement<T>.asListTag(): TagFieldElement<T> = apply {
    element.setListOnly()
}

/**
 * Extension: Configure to accept any tag
 */
fun <T : TagField> TagFieldElement<T>.asAnyTag(): TagFieldElement<T> = apply {
    element.setAny()
}
