package com.lowdragmc.lowdraglib.gui.editor.data.resource;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.lowdraglib.gui.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib.gui.editor.ui.resource.TexturesResourceContainer;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.io.File;

import static com.lowdragmc.lowdraglib.gui.editor.data.resource.TexturesResource.RESOURCE_NAME;
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
        super(new File(LDLib.getLDLibDir(), "assets/resources/textures"));
        addBuiltinResource("empty", IGuiTexture.EMPTY);
    }

    @Override
    public void buildDefault() {
        addBuiltinResource("border background", ResourceBorderTexture.BORDERED_BACKGROUND);
        addBuiltinResource("button", ResourceBorderTexture.BUTTON_COMMON);
        addBuiltinResource("slot", SlotWidget.ITEM_SLOT_TEXTURE.copy());
        addBuiltinResource("fluid slot", TankWidget.FLUID_SLOT_TEXTURE.copy());
        addBuiltinResource("tab", TABS_LEFT.getSubTexture(0, 0, 0.5f, 1f / 3));
        addBuiltinResource("tab pressed", TABS_LEFT.getSubTexture(0.5f, 0, 0.5f, 1f / 3));
        for (var wrapper : AnnotationDetector.REGISTER_TEXTURES) {
            addBuiltinResource("ldlib.gui.editor.register.texture." + wrapper.annotation().name(), wrapper.creator().get());
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
    public Tag serialize(IGuiTexture value) {
        return IGuiTexture.serializeWrapper(value);
    }

    @Override
    public IGuiTexture deserialize(Tag nbt) {
        if (nbt instanceof CompoundTag tag) {
            return IGuiTexture.deserializeWrapper(tag);
        }
        return null;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        getBuiltinResources().clear();
        addBuiltinResource("empty", IGuiTexture.EMPTY);
        for (String key : nbt.getAllKeys()) {
            addBuiltinResource(key, deserialize(nbt.get(key)));
        }
        for (IGuiTexture texture : getBuiltinResources().values()) {
            texture.setUIResource(this);
        }
    }

}
