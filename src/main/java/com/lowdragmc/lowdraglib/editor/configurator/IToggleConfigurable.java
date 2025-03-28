package com.lowdragmc.lowdraglib.editor.configurator;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

/**
 * Toggle Configurable is a configurable that can be toggled on and off.
 * By default, the object will not be serialized when it is disabled. To change this behavior, override the {@link #skipDisableSerialize()} method.
 */
public interface IToggleConfigurable extends IConfigurable, IPersistedSerializable {

    boolean isEnable();

    void setEnable(boolean enable);

    @Override
    default void buildConfigurator(ConfiguratorGroup father) {
        IConfigurable.super.buildConfigurator(father);
        if (!isEnable()) {
            father.setCanCollapse(false);
            var name = father.getNameWidget();
            if (name != null) {
                name.setTextColor(ColorPattern.GRAY.color);
            }
        } else {
            father.setCanCollapse(true);
            var name = father.getNameWidget();
            if (name != null) {
                name.setTextColor(ColorPattern.WHITE.color);
            }
        }
        father.addWidget(new SwitchWidget(father.getLeftWidth() + 12, 2, 10, 10, (cd, pressed) -> {
            setEnable(pressed);
            if (!isEnable()) {
                father.setCanCollapse(false);
                father.setCollapse(true);
                var name = father.getNameWidget();
                if (name != null) {
                    name.setTextColor(ColorPattern.GRAY.color);
                }
            } else {
                father.setCanCollapse(true);
                var name = father.getNameWidget();
                if (name != null) {
                    name.setTextColor(ColorPattern.WHITE.color);
                }
            }
        })
                .setPressed(isEnable())
                .setTexture(new ColorBorderTexture(-1, -1).setRadius(5),
                        new GuiTextureGroup(new ColorBorderTexture(-1, -1).setRadius(5),
                                new ColorRectTexture(-1).setRadius(5).scale(0.5f)))
                .setHoverTooltips("ldlib.gui.editor.toggle_configurable.tooltip"));
    }

    /**
     * If true, the object will not be serialized when it is disabled.
     */
    default boolean skipDisableSerialize() {
        return true;
    }

    @Override
    default CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        CompoundTag data;
        if (!isEnable() && skipDisableSerialize()) {
            data = new CompoundTag();
        } else {
            data = IPersistedSerializable.super.serializeNBT(provider);
        }
        data.putBoolean("enable", isEnable());
        return data;
    }

    @Override
    default void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        setEnable(tag.getBoolean("enable"));
        if (isEnable() || !skipDisableSerialize()) {
            IPersistedSerializable.super.deserializeNBT(provider, tag);
        }
    }
}
