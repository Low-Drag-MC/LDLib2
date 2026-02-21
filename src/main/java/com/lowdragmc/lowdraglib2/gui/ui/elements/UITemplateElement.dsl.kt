package com.lowdragmc.lowdraglib2.gui.ui.elements

import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath
import com.lowdragmc.lowdraglib2.gui.ui.ElementSpec
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer

/**
 * Specification for UITemplateElement
 */
open class UITemplateElementSpec<T : UITemplateElement>(
    var templatePath: IResourcePath? = null,
) : ElementSpec<T>() {
    /**
     * Set template path from string
     */
    fun template(path: String) = apply {
        this.templatePath = IResourcePath.parse(path)
    }

    /**
     * Set template path
     */
    fun template(path: IResourcePath) = apply {
        this.templatePath = path
    }
}

/**
 * UITemplateElement builder
 */
open class UITemplateElementElement<T : UITemplateElement>(
    element: T,
    spec: (UITemplateElementSpec<T>.() -> Unit)? = null,
) : UIContainer<T, UITemplateElementSpec<T>>(element, spec) {
    override fun makeSpec(): UITemplateElementSpec<T>? {
        return spec?.let { UITemplateElementSpec<T>().apply(it) }
    }

    override fun build(spec: UITemplateElementSpec<T>?): T {
        val e = super.build(spec)
        applyUITemplateElementProperties(spec, e)
        return e
    }

    protected fun applyUITemplateElementProperties(spec: UITemplateElementSpec<T>?, element: UITemplateElement) {
        spec?.templatePath?.let { element.setTemplate(it) }
    }
}

/**
 * Top Level - Create a standalone UITemplateElement
 */
fun uiTemplate(templatePath: String? = null,
               spec: (UITemplateElementSpec<UITemplateElement>.() -> Unit)? = null,
               init: UITemplateElementElement<UITemplateElement>.() -> Unit = {}): UITemplateElement {
    val path = templatePath?.let { IResourcePath.parse(it) }
    return UITemplateElementElement(UITemplateElement(path), spec).apply(init).build()
}

/**
 * Top Level - Create a standalone UITemplateElement with IResourcePath
 */
fun uiTemplate(templatePath: IResourcePath? = null,
               spec: (UITemplateElementSpec<UITemplateElement>.() -> Unit)? = null,
               init: UITemplateElementElement<UITemplateElement>.() -> Unit = {}): UITemplateElement {
    return UITemplateElementElement(UITemplateElement(templatePath), spec).apply(init).build()
}

/**
 * Internal Builder - Add UITemplateElement as a child to a container
 */
fun UIContainer<*, *>.uiTemplate(templatePath: String? = null,
                                  spec: (UITemplateElementSpec<UITemplateElement>.() -> Unit)? = null,
                                  init: UITemplateElementElement<UITemplateElement>.() -> Unit = {}): UITemplateElementElement<UITemplateElement> {
    val path = templatePath?.let { IResourcePath.parse(it) }
    return add(UITemplateElementElement(UITemplateElement(path), spec), init)
}

/**
 * Internal Builder - Add UITemplateElement as a child to a container with IResourcePath
 */
fun UIContainer<*, *>.uiTemplate(templatePath: IResourcePath? = null,
                                  spec: (UITemplateElementSpec<UITemplateElement>.() -> Unit)? = null,
                                  init: UITemplateElementElement<UITemplateElement>.() -> Unit = {}): UITemplateElementElement<UITemplateElement> {
    return add(UITemplateElementElement(UITemplateElement(templatePath), spec), init)
}

/**
 * DSL converter - Convert existing UITemplateElement to DSL builder
 */
fun <T : UITemplateElement> T.dsl(spec: (UITemplateElementSpec<T>.() -> Unit)? = null,
                                  init: UITemplateElementElement<T>.() -> Unit = {}): UITemplateElementElement<T> {
    return UITemplateElementElement(this, spec).apply(init)
}

// ===========================
// Convenience Extension Methods
// ===========================

/**
 * Extension: Set template from string path
 */
fun <T : UITemplateElement> UITemplateElementElement<T>.withTemplate(path: String): UITemplateElementElement<T> = apply {
    IResourcePath.parse(path)?.let { element.setTemplate(it) }
}

/**
 * Extension: Set template from IResourcePath
 */
fun <T : UITemplateElement> UITemplateElementElement<T>.withTemplate(path: IResourcePath): UITemplateElementElement<T> = apply {
    element.setTemplate(path)
}

/**
 * Extension: Get template path
 */
fun <T : UITemplateElement> UITemplateElementElement<T>.getTemplatePath(): IResourcePath? {
    return element.path
}

/**
 * Extension: Reload template
 */
fun <T : UITemplateElement> UITemplateElementElement<T>.reloadTemplate(): UITemplateElementElement<T> = apply {
    element.path?.let { element.setTemplate(it) }
}
