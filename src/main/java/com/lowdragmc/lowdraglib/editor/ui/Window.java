package com.lowdragmc.lowdraglib.editor.ui;

import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import org.appliedenergistics.yoga.YogaEdge;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class Window extends UIElement {
    public final TabView tabView;
    @Nullable @Setter
    protected Consumer<UIEvent> onLeftBorderDragging;
    @Nullable @Setter
    protected Consumer<UIEvent> onRightBorderDragging;
    @Nullable @Setter
    protected Consumer<UIEvent> onTopBorderDragging;
    @Nullable @Setter
    protected Consumer<UIEvent> onBottomBorderDragging;

    public Window() {
        this.tabView = new TabView();
        this.tabView.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
        }).setId("tab_view");
        getStyle().backgroundTexture(Sprites.RECT_SOLID);
        getLayout().setPadding(YogaEdge.ALL, 1);

        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);

        tabView.tabContentContainer.layout(layout -> {
            layout.setFlexGrow(1);
        });
        addChild(tabView);
    }

    protected void onDragSourceUpdate(UIEvent event) {
        if (event.dragHandler.draggingObject == YogaEdge.LEFT) {
            if (onLeftBorderDragging != null) {
                onLeftBorderDragging.accept(event);
            }
        } else if (event.dragHandler.draggingObject == YogaEdge.RIGHT) {
            if (onRightBorderDragging != null) {
                onRightBorderDragging.accept(event);
            }
        } else if (event.dragHandler.draggingObject == YogaEdge.TOP) {
            if (onTopBorderDragging != null) {
                onTopBorderDragging.accept(event);
            }
        } else if (event.dragHandler.draggingObject == YogaEdge.BOTTOM) {
            if (onBottomBorderDragging != null) {
                onBottomBorderDragging.accept(event);
            }
        }
    }

    protected void onMouseDown(UIEvent event) {
        if (onLeftBorderDragging != null && isMouseOver(getPositionX(), getPositionY(), 2, getSizeHeight(), event.x, event.y)) {
            startDrag(YogaEdge.LEFT, Icons.ARROW_LEFT_RIGHT).setDragTexture(-7, -4, 13, 7);
        } else if (onRightBorderDragging != null && isMouseOver(getPositionX() + getSizeWidth() - 2, getPositionY(), 2, getSizeHeight(), event.x, event.y)) {
            startDrag(YogaEdge.RIGHT, Icons.ARROW_LEFT_RIGHT).setDragTexture(-7, -4, 13, 7);
        } else if (onTopBorderDragging != null && isMouseOver(getPositionX(), getPositionY(), getSizeWidth(), 2, event.x, event.y)) {
            startDrag(YogaEdge.TOP, Icons.ARROW_UP_DOWN).setDragTexture(-4, -6, 7, 11);
        } else if (onBottomBorderDragging != null && isMouseOver(getPositionX(), getPositionY() + getSizeHeight() - 2, getSizeWidth(), 2, event.x, event.y)) {
            startDrag(YogaEdge.BOTTOM, Icons.ARROW_UP_DOWN).setDragTexture(-4, -6, 7, 11);
        }
    }

    @Override
    public void drawBackgroundAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackgroundAdditional(graphics, mouseX, mouseY, partialTicks);
        if (isHover()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            if (onLeftBorderDragging != null && isMouseOver(getPositionX(), getPositionY(), 2, getSizeHeight(), mouseX, mouseY)) {
                Icons.ARROW_LEFT_RIGHT.draw(graphics, mouseX, mouseY, mouseX - 7, mouseY - 4, 13, 7, partialTicks);
            } else if (onRightBorderDragging != null && isMouseOver(getPositionX() + getSizeWidth() - 2, getPositionY(), 2, getSizeHeight(), mouseX, mouseY)) {
                Icons.ARROW_LEFT_RIGHT.draw(graphics, mouseX, mouseY, mouseX - 7, mouseY - 4, 13, 7, partialTicks);
            } else if (onTopBorderDragging != null && isMouseOver(getPositionX(), getPositionY(), getSizeWidth(), 2, mouseX, mouseY)) {
                Icons.ARROW_UP_DOWN.draw(graphics, mouseX, mouseY, mouseX - 4, mouseY - 6, 7, 11, partialTicks);
            } else if (onBottomBorderDragging != null && isMouseOver(getPositionX(), getPositionY() + getSizeHeight() - 2, getSizeWidth(), 2, mouseX, mouseY)) {
                Icons.ARROW_UP_DOWN.draw(graphics, mouseX, mouseY, mouseX - 4, mouseY - 6, 7, 11, partialTicks);
            }
            graphics.pose().popPose();
        }
    }
}
