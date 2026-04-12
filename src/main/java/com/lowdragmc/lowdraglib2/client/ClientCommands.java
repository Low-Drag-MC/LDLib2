package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.client.shader.management.ShaderManager;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2023/2/9
 * @implNote ClientCommands
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
public class ClientCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> createLiteral(String command) {
        return ClientCommandManager.literal(command);
    }

    public static List<LiteralArgumentBuilder<FabricClientCommandSource>> createClientCommands() {
        var commands = new ArrayList<LiteralArgumentBuilder<FabricClientCommandSource>>();
        commands.add(createLiteral("ldlib2_client").then(createLiteral("reload_shader")
                .executes(context -> {
                    LDLibShaders.reload();
                    ShaderManager.getInstance().reload();
                    return 1;
                })));
        if (LDLib2Registries.SCREEN_TESTS != null) {
            commands.add(createScreenTestCommands());
        }
        return commands;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> createScreenTestCommands() {
        var builder = ClientCommandManager.literal("ldlib2_screen_test")
            .executes(context -> {
                int count = LDLib2Registries.SCREEN_TESTS == null ? -1 : LDLib2Registries.SCREEN_TESTS.values().size();
                context.getSource().sendFeedback(Component.literal("ldlib2_screen_test registered! Test count: " + count));
                return 1;
            });
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
                        minecraft.setScreen(new ModularUIScreen(ui, Component.empty()));
                        return 1;
                    }));
        }
        return builder;
    }
}
