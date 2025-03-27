package com.lowdragmc.lowdraglib.client;

import com.lowdragmc.lowdraglib.client.shader.Shaders;
import com.lowdragmc.lowdraglib.client.shader.management.ShaderManager;
import com.lowdragmc.lowdraglib.compass.CompassManager;
import com.lowdragmc.lowdraglib.utils.AnnotationDetector;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
                createLiteral("ldlib_client").then(createLiteral("reload_shader")
                        .executes(context -> {
                            Shaders.reload();
                            ShaderManager.getInstance().reload();
                            return 1;
                        })),
                createLiteral("compass").then(createLiteral("dev_mode")
                        .then(Commands.argument("mode", BoolArgumentType.bool())
                                .executes(context -> {
                                    CompassManager.INSTANCE.devMode = BoolArgumentType.getBool(context, "mode");
                                    return 1;
                                }))),
                createTestCommands()
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createTestCommands() {
        var builder = Commands.literal("ldlib_test");
        for (var uiTest : AnnotationDetector.REGISTER_UI_TESTS) {
            builder = builder.then(createLiteral(uiTest.annotation().name())
                    .executes(context -> {
                        var holder = IUIHolder.EMPTY;
                        var test = uiTest.creator().get();

                        var minecraft = Minecraft.getInstance();
                        var entityPlayer = minecraft.player;
                        var uiTemplate = test.createUI(holder, entityPlayer);
                        uiTemplate.initWidgets();
                        ModularUIGuiContainer ModularUIGuiContainer = new ModularUIGuiContainer(uiTemplate, entityPlayer.containerMenu.containerId);
                        minecraft.setScreen(ModularUIGuiContainer);
                        entityPlayer.containerMenu = ModularUIGuiContainer.getMenu();
                        return 1;
                    }));
        }
        return builder;
    }
}
