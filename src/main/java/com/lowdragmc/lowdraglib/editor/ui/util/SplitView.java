package com.lowdragmc.lowdraglib.editor.ui.util;

import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.appliedenergistics.yoga.YogaFlexDirection;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public abstract class SplitView extends UIElement{
    private static final Object DRAGGING = new Object();
    public final UIElement first = new UIElement();
    public final UIElement second = new UIElement();
    @Getter @Setter
    private int borderSize = 1;
    @Getter @Setter
    private int minPercentage = 5;
    @Getter @Setter
    private int maxPercentage = 95;

    public SplitView() {
        getLayout().setFlex(1);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);

        addChildren(first, second);
    }

    protected abstract boolean isHoverDragging(float mouseX, float mouseY);

    protected abstract SpriteTexture getDraggingIcon();

    protected abstract void onDragSourceUpdate(UIEvent event);

    public abstract SplitView setPercentage(float percentage);

    public abstract float getPercentage();

    protected void onMouseDown(UIEvent event) {
        if (event.button == 0 && isHoverDragging(event.x, event.y)){
            var icon = getDraggingIcon();
            var width = icon.spriteSize.width;
            var height = icon.spriteSize.height;
            startDrag(DRAGGING, icon).setDragTexture(- width / 2f, -height / 2f, width, height);
        }
    }

    @Override
    public void drawBackgroundAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackgroundAdditional(graphics, mouseX, mouseY, partialTicks);
        if (isHoverDragging(mouseX, mouseY)) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            var icon = getDraggingIcon();
            var width = icon.spriteSize.width;
            var height = icon.spriteSize.height;
            icon.draw(graphics, mouseX, mouseY,
                    mouseX - width / 2f,
                    mouseY - height / 2f,
                    width,
                    height, partialTicks);
            graphics.pose().popPose();
        }
    }

    public static class Horizontal extends SplitView {
        public Horizontal() {
            getLayout().setFlexDirection(YogaFlexDirection.ROW);
            first.getLayout().setWidthPercent(50);
            first.getLayout().setHeightPercent(100);
            second.getLayout().setFlex(1);
            second.getLayout().setHeightPercent(100);
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
            first.addChild(left);
            return this;
        }

        public Horizontal right(UIElement right) {
            second.addChild(right);
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

        @Override
        public float getPercentage() {
            return first.getLayout().getWidth().value;
        }
    }

    public static class Vertical extends SplitView {
        public Vertical() {
            first.getLayout().setWidthPercent(100);
            first.getLayout().setHeightPercent(50);
            second.getLayout().setFlex(1);
            second.getLayout().setWidthPercent(100);
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
            first.addChild(top);
            return this;
        }

        public Vertical bottom(UIElement bottom) {
            second.addChild(bottom);
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

        @Override
        public float getPercentage() {
            return first.getLayout().getHeight().value;
        }
    }
}
