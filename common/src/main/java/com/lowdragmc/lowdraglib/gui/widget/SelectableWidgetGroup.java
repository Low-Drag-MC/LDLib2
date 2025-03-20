package com.lowdragmc.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class SelectableWidgetGroup extends WidgetGroup implements DraggableScrollableWidgetGroup.ISelected {
    protected boolean isSelected;
    protected IGuiTexture selectedTexture;
    protected Consumer<SelectableWidgetGroup> onSelected;
    protected Consumer<SelectableWidgetGroup> onUnSelected;
    @Setter
    private Object prefab;

    public SelectableWidgetGroup(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public SelectableWidgetGroup(Position position) {
        super(position);
    }

    public SelectableWidgetGroup(Position position, Size size) {
        super(position, size);
    }

    public boolean isSelected() {
        return isSelected;
    }

    public SelectableWidgetGroup setOnSelected(Consumer<SelectableWidgetGroup> onSelected) {
        this.onSelected = onSelected;
        return this;
    }

    public SelectableWidgetGroup setOnUnSelected(Consumer<SelectableWidgetGroup> onUnSelected) {
        this.onUnSelected = onUnSelected;
        return this;
    }

    public SelectableWidgetGroup setSelectedTexture(IGuiTexture selectedTexture) {
        this.selectedTexture = selectedTexture;
        return this;
    }

    public SelectableWidgetGroup setSelectedTexture(int border, int color) {
        this.selectedTexture = new ColorBorderTexture(border, color);
        return this;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        if (isSelected && selectedTexture != null) {
            selectedTexture.draw(graphics, mouseX, mouseY, getPosition().x, getPosition().y, getSize().width, getSize().height);
        }
    }

    @Override
    public boolean allowSelected(double mouseX, double mouseY, int button) {
        return isMouseOverElement(mouseX, mouseY);
    }

    @Override
    public void onSelected() {
        isSelected = true;
        if (onSelected != null) onSelected.accept(this);
    }

    @Override
    public void onUnSelected() {
        isSelected = false;
        if (onUnSelected != null) onUnSelected.accept(this);
    }

    public <T> T getPrefab() {
        return (T) prefab;
    }

}
