package com.lowdragmc.lowdraglib2.gui.ui

import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.StyleAnimation
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.StyleAnimationDsl
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.at
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class StyleAnimationDslTest {
    @Test
    fun `supports modern style animation dsl keyframes`() {
        val animation = StyleAnimation.of(null)
        val dsl = StyleAnimationDsl(animation)
        val sub = dsl.apply {
            duration(0.25f)
            delay(0.05f)
            style(
                PropertyRegistry.OPACITY,
                at(0f, 1f),
                at(1f, 0.6f),
            )
            to(PropertyRegistry.OPACITY, 0.6f)
        }.start()
        assertNotNull(sub)
        sub.unsubscribe()
    }

    @Test
    fun `supports fromTo shortcut`() {
        val animation = StyleAnimation.of(null)
        val dsl = StyleAnimationDsl(animation)
        val sub = dsl.apply {
            fromTo(PropertyRegistry.OPACITY, 1f, 0f)
            lss("opacity", "1")
            to(PropertyRegistry.OPACITY, 1f)
        }.start()
        assertNotNull(sub)
        sub.unsubscribe()
    }

    @Test
    fun `ui element animation extension is available for compile`() {
        val configured: UIElement.() -> UIElement = {
            animationDsl(start = false) {
                duration(0.2f)
                to(PropertyRegistry.OPACITY, 1f)
            }
            this
        }
        assertNotNull(configured)
    }

    @Test
    fun `builder animation block is available for compile`() {
        val block: Element<UIElement>.() -> Unit = {
            animation(start = false) {
                duration(0.2f)
                to(PropertyRegistry.OPACITY, 1f)
            }
        }
        assertNotNull(block)
    }
}
