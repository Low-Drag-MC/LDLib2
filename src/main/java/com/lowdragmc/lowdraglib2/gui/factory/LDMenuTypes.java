package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.editor.UIEditor;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public final class LDMenuTypes {

    public static MenuType<ModularUIContainerMenu> PLAYER_UI;
    public static MenuType<ModularUIContainerMenu> HELD_ITEM_UI;
    public static MenuType<ModularUIContainerMenu> BLOCK_UI;

    public static void init() {
        PLAYER_UI = register("player_ui", new ExtendedScreenHandlerType<>(PlayerUIMenuType::create, net.minecraft.resources.ResourceLocation.STREAM_CODEC));
        HELD_ITEM_UI = register("held_item_ui", new ExtendedScreenHandlerType<>(HeldItemUIMenuType::create, HeldItemUIMenuType.STREAM_CODEC));
        BLOCK_UI = register("block_ui", new ExtendedScreenHandlerType<>(BlockUIMenuType::create, BlockUIMenuType.STREAM_CODEC));

        PlayerUIMenuType.register(UIEditor.WINDOW_ID, ignored -> player -> {
            if (player.level().isClientSide) {
                return new ModularUI(UI.of(EditorWindow.open(UIEditor.WINDOW_ID, UIEditor::new)))
                        .shouldCloseOnEsc(false)
                        .shouldCloseOnKeyInventory(false);
            }
            return new ModularUI(UI.empty());
        });

        // KubeJS integration removed as per Phase 0
    }

    private static <T extends MenuType<?>> T register(String name, T type) {
        return Registry.register(BuiltInRegistries.MENU, LDLib2.id(name), type);
    }
}
