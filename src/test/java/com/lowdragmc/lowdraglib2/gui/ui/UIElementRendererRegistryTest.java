package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.joml.Matrix3x2f;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UIElementRendererRegistryTest {
    @AfterEach
    void tearDown() {
        UIElementRendererRegistry.clear();
    }

    @Test
    void usesNearestRegisteredSuperclassRenderer() {
        var parentRenderer = new TrackingRenderer();
        var childRenderer = new TrackingRenderer();

        UIElementRendererRegistry.register(ParentElement.class, parentRenderer);
        UIElementRendererRegistry.register(ChildElement.class, childRenderer);

        assertSame(childRenderer, UIElementRendererRegistry.findRenderer(new ChildElement()));
        assertSame(parentRenderer, UIElementRendererRegistry.findRenderer(new GrandChildElement()));
    }

    @Test
    void defaultRendererDelegatesToUiElementDrawHooks() {
        var element = new OverridingElement();

        new TrackingRenderer().drawBackgroundAdditional(element, new StubContext());

        assertTrue(element.additionalDrawn);
    }

    private static class ParentElement extends UIElement {
    }

    private static class ChildElement extends ParentElement {
    }

    private static class GrandChildElement extends ParentElement {
    }

    private static class OverridingElement extends UIElement {
        private boolean additionalDrawn;

        @Override
        protected void drawBackgroundAdditional(IGUIContext context) {
            additionalDrawn = true;
        }
    }

    private static class TrackingRenderer implements UIElementRenderer<UIElement> {
        @Override
        public void drawBackgroundAdditional(UIElement element, IGUIContext context) {
            UIElementRenderer.super.drawBackgroundAdditional(element, context);
        }
    }

    private static class StubContext implements IGUIContext {
        @Override
        public Matrix3x2f currentPose() {
            return new Matrix3x2f();
        }

        @Override
        public void pushTransform(Transform2D transform, UIElement element) {
        }

        @Override
        public void popTransform(Transform2D transform) {
        }

        @Override
        public boolean isInsideScissor(float minX, float minY, float width, float height) {
            return true;
        }

        @Override
        public void drawTexture(IGuiTexture texture, float x, float y, float width, float height) {
        }

        @Override
        public void enableScissor(float x, float y, float width, float height) {
        }

        @Override
        public void disableScissor() {
        }

        @Override
        public void pushVisualLayer(UIElement element) {
        }

        @Override
        public void popVisualLayer() {
        }

        @Override
        public int getElementColor() {
            return -1;
        }

        @Override
        public void setElementColor(int color) {
        }

        @Override
        public void resetElementColor() {
        }
    }
}
