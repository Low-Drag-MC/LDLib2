package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.BindableUIElement;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaEdge;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemSlot extends BindableUIElement<ItemStack> {
    @Getter
    private ItemStack item = ItemStack.EMPTY;

    public ItemSlot() {
        getLayout().setWidth(18);
        getLayout().setHeight(18);
        getLayout().setPadding(YogaEdge.ALL, 1);
    }

    public ItemSlot setItem(ItemStack item) {
        return setValue(item, true);
    }

    public ItemSlot setItem(ItemStack itemStack, boolean notify) {
        return setValue(itemStack, notify);
    }

    @Override
    public ItemStack getValue() {
        return item;
    }

    @Override
    public ItemSlot setValue(ItemStack value, boolean notify) {
        if (ItemStack.isSameItemSameComponents(value, item)) return this;
        item = value;
        if (notify) notifyListeners();
        return this;
    }

    @Override
    public void drawBackgroundAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (item.isEmpty()) return;
        var contentX = getContentX();
        var contentY = getContentY();
        var contentWidth = getContentWidth();
        var contentHeight = getContentHeight();

        graphics.pose().pushPose();
        graphics.pose().scale(contentWidth / 16f, contentHeight / 16f, 1);
        graphics.pose().translate(contentX * 16 / contentWidth, contentY * 16 / contentHeight, -200);
        DrawerHelper.drawItemStack(graphics, item, 0, 0, -1, null);
        graphics.pose().popPose();
    }
}
