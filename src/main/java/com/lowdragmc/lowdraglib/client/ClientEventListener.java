package com.lowdragmc.lowdraglib.client;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.integration.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.integration.rei.ModularDisplay;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.emi.emi.screen.RecipeScreen;
import me.shedaniel.rei.api.client.gui.screen.DisplayScreen;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

/**
 * @author KilaBash
 * @date 2022/5/12
 * @implNote EventListener
 */
@EventBusSubscriber(modid = LDLib.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ClientEventListener {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        List<LiteralArgumentBuilder<CommandSourceStack>> commands = ClientCommands.createClientCommands();
        commands.forEach(dispatcher::register);
    }

    @SubscribeEvent
    public static void onScreenClosed(ScreenEvent.Closing event) {
        if (LDLib.isReiLoaded()) {
            if (event.getScreen() instanceof DisplayScreen && !ModularDisplay.CACHE_OPENED.isEmpty()) {
                synchronized (ModularDisplay.CACHE_OPENED) {
                    ModularDisplay.CACHE_OPENED.forEach(modular -> modular.modularUI.triggerCloseListeners());
                    ModularDisplay.CACHE_OPENED.clear();
                }
            }
        }
        if (LDLib.isEmiLoaded()) {
            if (event.getScreen() instanceof RecipeScreen && !ModularEmiRecipe.CACHE_OPENED.isEmpty()) {
                synchronized (ModularEmiRecipe.CACHE_OPENED) {
                    ModularEmiRecipe.CACHE_OPENED.forEach(modular -> modular.modularUI.triggerCloseListeners());
                    ModularEmiRecipe.CACHE_OPENED.clear();
                }
            }
        }
    }
}
