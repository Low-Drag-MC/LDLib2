package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.editor.ui.UIEditor;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.integration.kjs.ui.LDKJSMenuTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public final class LDMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, LDLib2.MOD_ID);

    public static final Supplier<MenuType<ModularUIContainerMenu>> PLAYER_UI = MENUS.register("player_ui",
            () -> IMenuTypeExtension.create(PlayerUIMenuType::create));

    public static final Supplier<MenuType<ModularUIContainerMenu>> HELD_ITEM_UI = MENUS.register("held_item_ui",
            () -> IMenuTypeExtension.create(HeldItemUIMenuType::create));

    public static final Supplier<MenuType<ModularUIContainerMenu>> BLOCK_UI = MENUS.register("block_ui",
            () -> IMenuTypeExtension.create(BlockUIMenuType::create));

    public static void init(IEventBus eventBus) {
        PlayerUIMenuType.register(UIEditor.UI_ID, player -> new PlayerUIMenuType.PlayerUIHolder() {
            @Override
            public @Nonnull ResourceLocation getUIId() {
                return UIEditor.UI_ID;
            }

            @Override
            public @Nonnull ModularUI createUI(@Nonnull Player player) {
                if (player.level().isClientSide) {
                    return new ModularUI(UI.of(EditorWindow.open(UIEditor.UI_ID, UIEditor::new)))
                            .shouldCloseOnEsc(false)
                            .shouldCloseOnKeyInventory(false);
                }
                return new ModularUI(UI.empty());
            }
        });

        if (LDLib2.isKubejsLoaded()) {
            LDKJSMenuTypes.init();
        }
        MENUS.register(eventBus);
    }
}
