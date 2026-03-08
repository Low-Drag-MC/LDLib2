package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.GuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.GuiTextureRendererRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class GuiTextureRendererRegistryTest {
    @AfterEach
    void tearDown() {
        GuiTextureRendererRegistry.clear();
    }

    @Test
    void usesExactRegisteredRendererFirst() {
        var parentRenderer = new TrackingRenderer();
        var childRenderer = new TrackingRenderer();

        GuiTextureRendererRegistry.register(ParentTexture.class, parentRenderer);
        GuiTextureRendererRegistry.register(ChildTexture.class, childRenderer);

        assertSame(childRenderer, GuiTextureRendererRegistry.findRenderer(new ChildTexture()));
    }

    @Test
    void usesNearestRegisteredSuperclassRenderer() {
        var parentRenderer = new TrackingRenderer();

        GuiTextureRendererRegistry.register(ParentTexture.class, parentRenderer);

        assertSame(parentRenderer, GuiTextureRendererRegistry.findRenderer(new ChildTexture()));
    }

    @Test
    void fallsBackToDefaultRendererWhenNothingRegistered() {
        assertSame(GuiTextureRendererRegistry.defaultRenderer(),
                GuiTextureRendererRegistry.findRenderer(new UnregisteredTexture()));
    }

    private static class ParentTexture implements IGuiTexture {
    }

    private static class ChildTexture extends ParentTexture {
    }

    private static class UnregisteredTexture implements IGuiTexture {
    }

    private static class TrackingRenderer implements GuiTextureRenderer<IGuiTexture> {
        @Override
        public void draw(IGuiTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
        }
    }
}
