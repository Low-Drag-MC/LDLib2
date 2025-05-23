package com.lowdragmc.lowdraglib.graphprocessor.widget;

import com.lowdragmc.lowdraglib.gui.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

public class DraggablePanelWidget extends WidgetGroup {
    @Getter
    protected WidgetGroup title;
    @Getter
    protected WidgetGroup content;
    @Setter
    @Getter
    protected Supplier<String> titleSupplier;
    @Setter
    @Getter
    protected boolean isDraggable = true;

    // runtime
    protected boolean isPressed = false;
    protected long lastClickTick;
    protected double lastMouseX, lastMouseY;
    protected Position lastPosition;

    public DraggablePanelWidget(String title, int x, int y, int width, int height) {
        super(x, y, width, height);
        setBackground(ColorPattern.WHITE.borderTexture(1).setTopRadius(5));
        titleSupplier = () -> title;
    }

    public void updateSize(Size size) {
        super.setSize(size);
        loadWidgets();
    }

    @Override
    public void initWidget() {
        super.initWidget();
        loadWidgets();
    }

    protected void loadWidgets() {
        clearAllWidgets();
        addWidget(this.title = new WidgetGroup(0, 0, getSizeWidth(), 15));
        this.title.setBackground(ColorPattern.BLACK.rectTexture().setTopRadius(5));
        addWidget(this.content = new WidgetGroup(0, 15, getSizeWidth(), getSizeHeight() - 15));
        this.content.setBackground(ColorPattern.DARK_GRAY.rectTexture());
        this.title.clearAllWidgets();
        this.title.setSize(getSizeWidth(), 15);
        this.title.addWidget(new ImageWidget(0, 14, getSizeWidth(), 1, ColorPattern.WHITE.rectTexture()));
        this.title.addWidget(new ImageWidget(5, 2, getSizeWidth() - 10, 11,
                new TextTexture().setSupplier(titleSupplier).setWidth(getSizeWidth() - 10).setType(TextTexture.TextType.LEFT)));
    }


    public boolean isCollapsed() {
        return !content.isVisible();
    }

    public void setCollapsed(boolean collapsed) {
        content.setVisible(!collapsed);
        setSize(getSizeWidth(), collapsed ? 15 : content.getSizeHeight() + 15);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (button == 0 && title.isMouseOverElement(mouseX, mouseY)) {
            if (lastClickTick != 0 && gui.getTickCount() - lastClickTick < 10) {
                // double click to collapse
                setCollapsed(!isCollapsed());
                return true;
            }
            lastClickTick = gui.getTickCount();

            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            isPressed = true;
            lastPosition = getSelfPosition();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isPressed && isDraggable) {
            setSelfPosition(lastPosition.add((int) (mouseX - lastMouseX), (int) (mouseY - lastMouseY)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isPressed = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
