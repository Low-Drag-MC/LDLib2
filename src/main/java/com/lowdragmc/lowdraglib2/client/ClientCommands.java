package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.font.LDFontStatsOverlay;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2023/2/9
 * @implNote ClientCommands
 */
public class ClientCommands {

    public static LiteralArgumentBuilder<CommandSourceStack> createLiteral(String command) {
        return Commands.literal(command);
    }

    public static List<LiteralArgumentBuilder<CommandSourceStack>> createClientCommands() {
        var commands = new ArrayList<LiteralArgumentBuilder<CommandSourceStack>>();
        if (Platform.isDevEnv()) {
            commands.add(createFontCommands());
        }
        if (LDLib2ClientRegistries.SCREEN_TESTS != null && !LDLib2ClientRegistries.SCREEN_TESTS.values().isEmpty()) {
            commands.add(createScreenTestCommands());
        }
        return commands;
    }

    /**
     * Development only helpers for eyeballing the text renderer. Not registered outside a dev environment:
     * the settings they poke live in the client config, which is where users are meant to change them.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> createFontCommands() {
        return createLiteral("ldlib2_font")
                .then(createLiteral("mode")
                        .executes(context -> {
                            var modes = LDLibClientConfig.FontRenderMode.values();
                            var next = modes[(LDLibClientConfig.fontRenderMode().ordinal() + 1) % modes.length];
                            LDLibClientConfig.setFontRenderMode(next);
                            reinitCurrentScreen();
                            context.getSource().sendSuccess(
                                    () -> Component.literal("LDLib text: " + next), false);
                            return 1;
                        }))
                .then(createLiteral("stats")
                        .executes(context -> {
                            LDFontStatsOverlay.toggle();
                            context.getSource().sendSuccess(
                                    () -> Component.literal(LDFontStatsOverlay.describe()), false);
                            return 1;
                        }));
    }

    /**
     * Rebuilds the open screen so text is measured again with the renderer that is now active.
     */
    private static void reinitCurrentScreen() {
        var minecraft = Minecraft.getInstance();
        // 26.2 moved the current screen off Minecraft and onto Gui
        var screen = minecraft.gui.screen();
        if (screen != null) {
            screen.resize(screen.width, screen.height);
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createScreenTestCommands() {
        var builder = Commands.literal("ldlib2_screen_test");
        if (LDLib2ClientRegistries.SCREEN_TESTS == null) {
            return builder;
        }
        for (var uiTest : LDLib2ClientRegistries.SCREEN_TESTS) {
            builder = builder.then(createLiteral(uiTest.annotation().name())
                    .executes(context -> {
                        var test = uiTest.value().get();
                        var minecraft = Minecraft.getInstance();
                        var entityPlayer = minecraft.player;
                        if (entityPlayer == null) return 0;
                        var ui = test.createUI(entityPlayer);
                        minecraft.setScreenAndShow(new ModularUIScreen(ui, Component.empty()));
                        return 1;
                    }));
        }
        return builder;
    }
}
