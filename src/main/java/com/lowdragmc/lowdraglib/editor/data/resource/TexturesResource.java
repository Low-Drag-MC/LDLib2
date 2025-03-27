package com.lowdragmc.lowdraglib.editor.data.resource;

import com.lowdragmc.lowdraglib.LDLibRegistries;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib.editor.ui.resource.TexturesResourceContainer;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import static com.lowdragmc.lowdraglib.editor.data.resource.TexturesResource.RESOURCE_NAME;
import static com.lowdragmc.lowdraglib.gui.widget.TabContainer.TABS_LEFT;

/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote TextureResource
 */
@LDLRegister(name = RESOURCE_NAME, group = "resource")
public class TexturesResource extends Resource<IGuiTexture> {

    public final static String RESOURCE_NAME = "ldlib.gui.editor.group.textures";

    public TexturesResource() {
        data.put("empty", IGuiTexture.EMPTY);
    }

    @Override
    public void buildDefault() {
        data.put("border background", ResourceBorderTexture.BORDERED_BACKGROUND);
        data.put("button", ResourceBorderTexture.BUTTON_COMMON);
        data.put("slot", SlotWidget.ITEM_SLOT_TEXTURE.copy());
        data.put("fluid slot", TankWidget.FLUID_SLOT_TEXTURE.copy());
        data.put("tab", TABS_LEFT.getSubTexture(0, 0, 0.5f, 1f / 3));
        data.put("tab pressed", TABS_LEFT.getSubTexture(0.5f, 0, 0.5f, 1f / 3));
        for (var holder : LDLibRegistries.GUI_TEXTURES) {
            data.put("%s.%s".formatted(LDLibRegistries.GUI_TEXTURES.getRegistryName(), holder.annotation().name()), holder.value().get());
        }
    }

    @Override
    public String name() {
        return RESOURCE_NAME;
    }

    @Override
    public ResourceContainer<IGuiTexture, ImageWidget> createContainer(ResourcePanel panel) {
        return new TexturesResourceContainer(this, panel);
    }

    @Override
    public Tag serialize(IGuiTexture value, HolderLookup.Provider provider) {
        return IGuiTexture.CODEC.encodeStart(NbtOps.INSTANCE, value).result().orElse(null);
    }

    @Override
    public IGuiTexture deserialize(Tag nbt, HolderLookup.Provider provider) {
        return IGuiTexture.CODEC.parse(NbtOps.INSTANCE, nbt).result().orElse(IGuiTexture.MISSING_TEXTURE);
    }

    @Override
    public void deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        data.clear();
        data.put("empty", IGuiTexture.EMPTY);
        for (String key : nbt.getAllKeys()) {
            data.put(key, deserialize(nbt.get(key), provider));
        }
        for (IGuiTexture texture : data.values()) {
            texture.setUIResource(this);
        }
    }

}
