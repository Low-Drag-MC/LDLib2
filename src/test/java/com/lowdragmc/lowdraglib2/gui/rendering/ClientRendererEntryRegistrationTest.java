package com.lowdragmc.lowdraglib2.gui.rendering;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.GuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.GuiTextureRendererRegistry;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.GuiTextureRendererBootstrap;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.UIElementRendererRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.UIElementRendererBootstrap;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.RegisteredUIElementRenderer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class ClientRendererEntryRegistrationTest {
    @AfterEach
    void tearDown() {
        GuiTextureRendererRegistry.clear();
        UIElementRendererRegistry.clear();
    }

    @Test
    void appliesGuiTextureRendererEntries() {
        var renderer = new TrackingTextureRenderer();
        GuiTextureRendererBootstrap.applyEntries(List.of(renderer));

        assertSame(renderer, GuiTextureRendererRegistry.findRenderer(new TestTexture()));
    }

    @Test
    void appliesUiElementRendererEntries() {
        var renderer = new TrackingElementRenderer();
        UIElementRendererBootstrap.applyEntries(List.of(renderer));

        assertSame(renderer, UIElementRendererRegistry.findRenderer(new TestElement()));
    }

    private static final class TestTexture implements IGuiTexture {
    }

    private static final class TestElement extends UIElement {
    }

    private static final class TrackingTextureRenderer implements RegisteredGuiTextureRenderer<TestTexture, TrackingTextureRenderer> {
        @Override
        public Class<TestTexture> type() {
            return TestTexture.class;
        }

        @Override
        public void draw(TestTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
        }
    }

    private static final class TrackingElementRenderer implements RegisteredUIElementRenderer<TestElement, TrackingElementRenderer> {
        @Override
        public Class<TestElement> type() {
            return TestElement.class;
        }

        @Override
        public void drawBackgroundAdditional(TestElement element, IGUIContext context) {
        }
    }
}
