package com.lowdragmc.lowdraglib.gui.widget.custom;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;

/**
 * @author KilaBash
 * @date 2022/12/12
 * @implNote PlayerInventoryWidget
 */
@LDLRegister(name = "player_inventory", group = "widget.custom")
public class PlayerInventoryWidget extends WidgetGroup {
    @Configurable(name = "ldlib.gui.editor.name.slot_background")
    @Getter
    private IGuiTexture slotBackground = SlotWidget.ITEM_SLOT_TEXTURE.copy();

    public PlayerInventoryWidget() {
        super(0, 0, 172, 86);

        for (int col = 0; col < 9; col++) {
            String id = "player_inv_" + col;
            var pos = new Position(5 + col * 18, 5 + 58);
            var slot = new SlotWidget();
            slot.initTemplate();
            slot.setSelfPosition(pos);
            slot.setId(id);
            addWidget(slot);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                var id = "player_inv_" + (col + (row + 1) * 9);
                var pos = new Position(5 + col * 18, 5 + row * 18);
                var slot = new SlotWidget();
                slot.initTemplate();
                slot.setSelfPosition(pos);
                slot.setId(id);
                addWidget(slot);
            }
        }
    }

    @Override
    public void initTemplate() {
    }

    @Override
    public void initWidget() {
        super.initWidget();
        for (int i = 0; i < widgets.size(); i++) {
            if (widgets.get(i) instanceof SlotWidget slotWidget) {
                slotWidget.setContainerSlot(gui.entityPlayer.getInventory(), i);
                slotWidget.setLocationInfo(true, i < 9);
                slotWidget.setBackground(slotBackground);
                if (LDLib.isClient() && Editor.INSTANCE != null) {
                    slotWidget.setCanPutItems(false);
                    slotWidget.setCanTakeItems(false);
                } else {
                    slotWidget.setCanPutItems(true);
                    slotWidget.setCanTakeItems(true);
                }
            }
        }
    }

    @Deprecated
    public void setPlayer(Player entityPlayer) {
        for (int i = 0; i < widgets.size(); i++) {
            if (widgets.get(i) instanceof  SlotWidget slotWidget) {
                slotWidget.setContainerSlot(entityPlayer.getInventory(), i);
                slotWidget.setLocationInfo(true, i < 9);
            }
        }
    }

    @ConfigSetter(field = "slotBackground")
    public void setSlotBackground(IGuiTexture slotBackground) {
        this.slotBackground = slotBackground;
        for (var widget : widgets) {
            if (widget instanceof SlotWidget slotWidget) {
                slotWidget.setBackground(slotBackground);
            }
        }
    }

}
