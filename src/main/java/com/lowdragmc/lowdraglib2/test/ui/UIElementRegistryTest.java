package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;

public class UIElementRegistryTest {

    @GameTest(template = "empty")
    public static void uiRegistryTest(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start UI Registry Test");
        for (var holder : LDLib2Registries.UI_ELEMENTS.values()) {
            var element = holder.value().get();
            element.deserializeNBT(helper.getLevel().registryAccess(), new CompoundTag());
        }
        LDLib2.LOGGER.info("End UI Registry Test");
        helper.succeed();
    }

}
