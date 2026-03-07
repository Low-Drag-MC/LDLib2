package com.lowdragmc.lowdraglib2.configurator;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    default void serialize(@NotNull ValueOutput valueOutput) {
        valueOutput.putBoolean("_enable", isEnable());
        if (isEnable() || !skipDisableSerialize()) {
            IPersistedSerializable.super.serialize(valueOutput);
        }
    }

    @Override
    default void deserialize(@NotNull ValueInput valueOutput) {
        setEnable(valueOutput.getBooleanOr("_enable", false));
        if (isEnable() || !skipDisableSerialize()) {
            IPersistedSerializable.super.deserialize(valueOutput);
        }
    }
}
