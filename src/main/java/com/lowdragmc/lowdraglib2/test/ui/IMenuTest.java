package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IMenuTest extends PlayerUIMenuType.PlayerUIHolder, ILDLRegister<IMenuTest, Supplier<IMenuTest>> {
    @Override
    default ResourceLocation getUIId() {
        return LDLib2.id(name());
    }
}
