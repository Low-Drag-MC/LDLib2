package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.client.shader.Shaders;
import com.lowdragmc.lowdraglib2.client.shader.management.ShaderManager;
import com.lowdragmc.lowdraglib2.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIContainerScreen;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * @author KilaBash
 * @date 2023/2/9
 * @implNote ClientCommands
 */
@OnlyIn(Dist.CLIENT)
public class ClientCommands {

    public static LiteralArgumentBuilder<CommandSourceStack> createLiteral(String command) {
        return Commands.literal(command);
    }

    public static List<LiteralArgumentBuilder<CommandSourceStack>> createClientCommands() {
        return List.of(
                createLiteral("ldlib2_client").then(createLiteral("reload_shader")
                        .executes(context -> {
                            Shaders.reload();
                            ShaderManager.getInstance().reload();
                            return 1;
                        })),
                createTestCommands()
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createTestCommands() {
        var builder = Commands.literal("ldlib_test");
        if (LDLib2Registries.UI_TESTS == null) {
            return builder;
        }
        for (var uiTest : LDLib2Registries.UI_TESTS) {
            builder = builder.then(createLiteral(uiTest.annotation().name())
                    .executes(context -> {
                        var holder = IUIHolder.EMPTY;
                        var test = uiTest.value().get();

                        var minecraft = Minecraft.getInstance();
                        var entityPlayer = minecraft.player;
                        if (entityPlayer == null) return 0;
                        var ui = test.createUI(entityPlayer);
                        var screen = new ModularUIContainerScreen<>(ui, new ModularUIContainerMenu(entityPlayer.containerMenu.containerId), entityPlayer.getInventory(), Component.empty());
                        minecraft.setScreen(screen);
                        entityPlayer.containerMenu = screen.getMenu();
                        return 1;
                    }));
        }
        return builder;
    }
}
