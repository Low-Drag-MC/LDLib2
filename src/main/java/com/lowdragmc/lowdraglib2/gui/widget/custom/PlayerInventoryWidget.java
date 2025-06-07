package com.lowdragmc.lowdraglib2.gui.widget.custom;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.editor_outdated.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib2.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib2.math.Position;
import lombok.Getter;
import lombok.Setter;

/**
 * @author KilaBash
 * @date 2022/12/12
 * @implNote PlayerInventoryWidget
 */
@LDLRegister(name = "player_inventory", group = "widget.custom", registry = "ldlib2:widget")
public class PlayerInventoryWidget extends WidgetGroup {
    @Configurable(name = "ldlib.gui.editor.name.slot_background")
    @Getter
    private IGuiTexture slotBackground = SlotWidget.ITEM_SLOT_TEXTURE.copy();
    @Getter
    @Setter
    @Configurable(name = "ldlib.gui.editor.name.allow_custom_background",
            tips = "ldlib.gui.editor.name.allow_custom_background.tips")
    private boolean allowCustomBackground = false;

    public PlayerInventoryWidget() {
        super(0, 0, 172, 86);

        for (int col = 0; col < 9; col++) {
            String id = "player_inv_" + col;
            var pos = Position.of(5 + col * 18, 5 + 58);
            var slot = new SlotWidget();
            slot.initTemplate();
            slot.setSelfPosition(pos);
            slot.setId(id);
            addWidget(slot);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                var id = "player_inv_" + (col + (row + 1) * 9);
                var pos = Position.of(5 + col * 18, 5 + row * 18);
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
                if (!allowCustomBackground) {
                    slotWidget.setBackground(slotBackground);
                }
                if (LDLib2.isClient() && Editor.INSTANCE != null) {
                    slotWidget.setCanPutItems(false);
                    slotWidget.setCanTakeItems(false);
                } else {
                    slotWidget.setCanPutItems(true);
                    slotWidget.setCanTakeItems(true);
                }
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
