package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import org.appliedenergistics.yoga.YogaFlexDirection;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public abstract class SplitView extends UIElement {
    private static final Object DRAGGING = new Object();
    public final UIElement first = new UIElement();
    public final UIElement second = new UIElement();
    @Getter @Setter
    private float borderSize = 2;
    @Getter @Setter
    private float minPercentage = 5;
    @Getter @Setter
    private float maxPercentage = 95;

    public SplitView() {
        getLayout().setFlex(1);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);

        first.addClass("__split_view_first__");
        second.addClass("__split_view_second__");

        addChildren(first, second);
    }

    protected abstract boolean isHoverDragging(float mouseX, float mouseY);

    protected abstract SpriteTexture getDraggingIcon();

    protected abstract void onDragSourceUpdate(UIEvent event);

    public abstract SplitView setPercentage(float percentage);

    public SplitView first(UIElement first) {
        this.first.clearAllChildren();
        this.first.addChild(first);
        return this;
    }

    public SplitView second(UIElement second) {
        this.second.clearAllChildren();
        this.second.addChild(second);
        return this;
    }

    protected void onMouseDown(UIEvent event) {
        // use int mouse coordinates to avoid issues with floating point precision
        if (event.button == 0 && isHoverDragging((int) event.x, (int) event.y)){
            var icon = getDraggingIcon();
            var width = icon.spriteSize.width;
            var height = icon.spriteSize.height;
            startDrag(DRAGGING, icon).setDragTexture(- width / 2f, -height / 2f, width, height);
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        if (isHoverDragging(guiContext.localMouseX, guiContext.localMouseY)) {
            guiContext.postRendering(ctx -> {
                var icon = getDraggingIcon();
                var width = icon.spriteSize.width;
                var height = icon.spriteSize.height;
                ctx.drawTexture(icon,
                        ctx.localMouseX - width / 2f,
                        ctx.localMouseY - height / 2f,
                        width,
                        height);
            });
        }
    }

    @KJSBindings("SplitViewHorizontal")
    @LDLRegister(name = "split-view-horizontal", group = "container", registry = "ldlib2:ui_element")
    public static class Horizontal extends SplitView {
        public Horizontal() {
            getLayout().setFlexDirection(YogaFlexDirection.ROW);
            first.getLayout().setWidthPercent(50);
            first.getLayout().setHeightPercent(100);
            second.getLayout().setFlex(1);
            second.getLayout().setHeightPercent(100);
            internalSetup();
        }

        @Override
        protected boolean isHoverDragging(float mouseX, float mouseY) {
            return isMouseOver(getPositionX() + first.getSizeWidth() - getBorderSize(), getPositionY(), getBorderSize(), getSizeHeight(), mouseX, mouseY);
        }

        @Override
        protected SpriteTexture getDraggingIcon() {
            return Icons.ARROW_LEFT_RIGHT;
        }

        public Horizontal left(UIElement left) {
            first(left);
            return this;
        }

        public Horizontal right(UIElement right) {
            second(right);
            return this;
        }


        @Override
        protected void onDragSourceUpdate(UIEvent event) {
            if (event.target != this || event.dragHandler.getDraggingObject() != DRAGGING) {
                return; // only handle drag events for this window
            }
            var width = getSizeWidth();
            if (width <= 0) {
                return; // prevent division by zero
            }
            setPercentage((event.x - getPositionX()) / width * 100);
        }

        @Override
        public Horizontal setPercentage(float percentage) {
            first.layout(layout -> layout.setWidthPercent(Mth.clamp(percentage, getMinPercentage(), getMaxPercentage())));
            return this;
        }

    }

    @KJSBindings("SplitViewVertical")
    @LDLRegister(name = "split-view-vertical", group = "container", registry = "ldlib2:ui_element")
    public static class Vertical extends SplitView {
        public Vertical() {
            first.getLayout().setWidthPercent(100);
            first.getLayout().setHeightPercent(50);
            second.getLayout().setFlex(1);
            second.getLayout().setWidthPercent(100);
            internalSetup();
        }

        @Override
        protected boolean isHoverDragging(float mouseX, float mouseY) {
            return isMouseOver(getPositionX(), getPositionY() + first.getSizeHeight() - getBorderSize(), getSizeWidth(), getBorderSize(), mouseX, mouseY);
        }

        @Override
        protected SpriteTexture getDraggingIcon() {
            return Icons.ARROW_UP_DOWN;
        }

        public Vertical top(UIElement top) {
            first(top);
            return this;
        }

        public Vertical bottom(UIElement bottom) {
            second(bottom);
            return this;
        }

        @Override
        protected void onDragSourceUpdate(UIEvent event) {
            if (event.target != this || event.dragHandler.getDraggingObject() != DRAGGING) {
                return; // only handle drag events for this window
            }
            var height = getSizeHeight();
            if (height <= 0) {
                return; // prevent division by zero
            }
            setPercentage((event.y - getPositionY()) / height * 100);
        }

        @Override
        public Vertical setPercentage(float percentage) {
            first.layout(layout -> layout.setHeightPercent(Mth.clamp(percentage, getMinPercentage(), getMaxPercentage())));
            return this;
        }

    }
}
