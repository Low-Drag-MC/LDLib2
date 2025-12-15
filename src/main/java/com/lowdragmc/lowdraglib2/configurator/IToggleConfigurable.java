package com.lowdragmc.lowdraglib2.configurator;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * Toggle Configurable is a configurable that can be toggled on and off.
 * By default, the object will not be serialized when it is disabled. To change this behavior, override the {@link #skipDisableSerialize()} method.
 */
public interface IToggleConfigurable extends IConfigurable, IPersistedSerializable {

    boolean isEnable();

    void setEnable(boolean enable);

    @Override
    @OnlyIn(Dist.CLIENT)
    default void buildConfigurator(ConfiguratorGroup father) {
        father.setCanCollapse(isEnable());
        father.lineContainer.addChildAt(new Toggle()
                .setOn(isEnable(),false)
                .setOnToggleChanged(isOn -> {
                    setEnable(isOn);
                    father.setCollapse(!isOn);
                    father.setCanCollapse(isOn);
                    father.notifyChanges();
                }).setText("").addEventListener(UIEvents.MOUSE_DOWN, e -> {
                    if (e.button == 0) {
                        e.stopPropagation();
                    }
                }), 1)
                .addEventListener(UIEvents.TICK, e -> {
                    var canCollapse = father.isCanCollapse();
                    var isEnable = isEnable();
                    if (canCollapse != isEnable) {
                        setEnable(isEnable);
                        if (!isEnable && !father.isCollapse()) {
                            father.setCollapse(true);
                        }
                    }
                });
        IConfigurable.super.buildConfigurator(father);
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
        data.putBoolean("_enable", isEnable());
        return data;
    }

    @Override
    default void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        setEnable(tag.getBoolean("_enable"));
        if (isEnable() || !skipDisableSerialize()) {
            IPersistedSerializable.super.deserializeNBT(provider, tag);
        }
    }
}
