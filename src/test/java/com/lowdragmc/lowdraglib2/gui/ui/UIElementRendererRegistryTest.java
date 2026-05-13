package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import org.jetbrains.annotations.NotNull;
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
    void delegatingRendererCanInvokeNearestParentRenderer() {
        var child = new ChildElement();
        var parentRenderer = new TrackingRenderer();
        var childRenderer = new DelegatingTrackingRenderer();

        UIElementRendererRegistry.register(ParentElement.class, parentRenderer);
        UIElementRendererRegistry.register(ChildElement.class, childRenderer);

        childRenderer.drawBackgroundAdditional(child, new StubContext());

        assertTrue(parentRenderer.additionalCalled);
        assertTrue(childRenderer.additionalCalled);
    }

    @Test
    void defaultRendererDelegatesToUiElementDrawHooks() {
        var element = new OverridingElement();

        new TrackingRenderer().drawBackgroundAdditional(element, new StubContext());

        assertTrue(element.additionalDrawn);
    }

    @Test
    void defaultRendererDrawContentsInvokesUiElementAdditionalHook() {
        var element = new OverridingElement();

        new TrackingRenderer().drawContents(element, new StubContext());

        assertTrue(element.additionalDrawn);
    }

    @Test
    void rendererOwnsOverlayDrawing() {
        var element = new UIElement();
        var context = new StubContext();
        var renderer = new TrackingRenderer() {
            @Override
            public void drawBackgroundOverlay(UIElement element, IGUIContext context) {
                context.drawTexture(IGuiTexture.MISSING_TEXTURE, 0, 0, 1, 1);
            }
        };

        renderer.drawBackgroundOverlay(element, context);

        assertSame(IGuiTexture.MISSING_TEXTURE, context.lastTexture);
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
        protected void drawBackgroundAdditional(@NotNull IGUIContext context) {
            additionalDrawn = true;
        }
    }

    private static class TrackingRenderer implements UIElementRenderer<UIElement> {
        private boolean additionalCalled;

        @Override
        public void drawBackgroundAdditional(UIElement element, IGUIContext context) {
            additionalCalled = true;
            UIElementRenderer.super.drawBackgroundAdditional(element, context);
        }

        @Override
        public void drawContents(UIElement element, IGUIContext context) {
            UIElementRenderer.super.drawContents(element, context);
        }
    }

    private static class DelegatingTrackingRenderer extends DelegatingUIElementRenderer<ChildElement, DelegatingTrackingRenderer> {
        private boolean additionalCalled;

        @Override
        public Class<ChildElement> type() {
            return ChildElement.class;
        }

        @Override
        public void drawBackgroundAdditional(ChildElement element, IGUIContext context) {
            drawParentBackgroundAdditional(element, context);
            additionalCalled = true;
        }
    }

    private static class StubContext implements IGUIContext {
        private IGuiTexture lastTexture;

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
            lastTexture = texture;
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
