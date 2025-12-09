package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.client.shader.management.ShaderManager;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.editor.ui.UIEditor;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
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
        var commands = new ArrayList<LiteralArgumentBuilder<CommandSourceStack>>();
        commands.add(createLiteral("ldlib2_client").then(createLiteral("reload_shader")
                .executes(context -> {
                    LDLibShaders.reload();
                    ShaderManager.getInstance().reload();
                    return 1;
                })));
        if (Platform.isDevEnv()) {
            commands.add(createScreenTestCommands());
        }
        return commands;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createScreenTestCommands() {
        var builder = Commands.literal("ldlib2_screen_test");
        if (LDLib2Registries.SCREEN_TESTS == null) {
            return builder;
        }
        for (var uiTest : LDLib2Registries.SCREEN_TESTS) {
            builder = builder.then(createLiteral(uiTest.annotation().name())
                    .executes(context -> {
                        var test = uiTest.value().get();
                        var minecraft = Minecraft.getInstance();
                        var entityPlayer = minecraft.player;
                        if (entityPlayer == null) return 0;
                        var ui = test.createUI(entityPlayer);
                        minecraft.pushGuiLayer(new ModularUIScreen(ui, Component.empty()));
                        return 1;
                    }));
        }
        return builder;
    }
}
