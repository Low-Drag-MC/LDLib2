package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.font.LDFontManager;
import com.lowdragmc.lowdraglib2.editor.resource.EditorResourceEvent;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.OreSprites;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import com.lowdragmc.lowdraglib2.client.font.LDFontStatsOverlay;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

/**
 * @author KilaBash
 * @date 2022/5/12
 * @implNote EventListener
 */
@EventBusSubscriber(modid = LDLib2.MOD_ID, value = Dist.CLIENT)
public class ClientEventListener {

    /**
     * The two things about the text renderer that can only be noticed by looking: the font related video
     * settings, which vanilla gives mods no event for, and rasterized glyph sizes going unused, which is time
     * based by nature. Both free textures, so both belong between frames rather than inside one.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LDFontManager.INSTANCE.refreshVanillaFontOptions();
        LDFontManager.INSTANCE.evictStaleRasterSizes();
    }

    /**
     * TEMPORARY: the statistics overlay is a HUD layer, and HUD layers are drawn before the open screen rather
     * than over it, so on a screen it would sit behind the very interface it is reporting on. Drawing it again
     * here puts it on top. See {@link LDFontStatsOverlay}.
     */
    @SubscribeEvent
    public static void onScreenRendered(ScreenEvent.Render.Post event) {
        if (Platform.isDevEnv()) {
            LDFontStatsOverlay.INSTANCE.render(event.getGuiGraphics(), Minecraft.getInstance().getDeltaTracker());
        }
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        List<LiteralArgumentBuilder<CommandSourceStack>> commands = ClientCommands.createClientCommands();
        commands.forEach(dispatcher::register);
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(ScreenEvent.Init.Pre event) {
        var screen = event.getScreen();
        if (screen instanceof AbstractContainerScreen<?> containerScreen && containerScreen.getMenu() instanceof IModularUIHolder holder) {
            var mui = holder.getModularUI();
            if (mui != null) {
                ModularUIClientAccess.setScreenAndInit(mui, containerScreen);
                event.addListener(ModularUIClientAccess.getWidget(mui));
            }
        }
    }

    @SubscribeEvent
    public static void onLoadBuiltinEditorResource(EditorResourceEvent.LoadBuiltin event) {
        if (event.resourceInstance.resource == TexturesResource.INSTANCE) {
            Sprites.init((ResourceInstance<IGuiTexture>) event.resourceInstance);
            MCSprites.init((ResourceInstance<IGuiTexture>) event.resourceInstance);
            OreSprites.init((ResourceInstance<IGuiTexture>) event.resourceInstance);
        }
    }

//    @SubscribeEvent
//    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
//        // memoize and delay, to make sure ui is generated after the world loading
//        var muiCache = Suppliers.memoize(() -> ModularUI.of(UI.of(
//                new UIElement().layout(l -> l.widthPercent(100).heightPercent(100).paddingAll(10).gapAll(4))
//                        .addChildren(
//                                new UIElement()
//                                        .layout(l -> l.width(50).height(50).paddingAll(5))
//                                        .style(s -> s.background(Sprites.BORDER1_RT1))
//                                        .addChild(new UIElement()
//                                                .layout(l -> l.widthPercent(100).heightPercent(100))
//                                                .style(s -> s.background(new ItemStackTexture(Items.DIAMOND)))
//                                        ),
//                                new ProgressBar().bindDataSource(SupplierDataSource.of(() -> Optional.ofNullable(Minecraft.getInstance().player)
//                                                .map(p -> p.getHealth() / p.getMaxHealth()).orElse(1f)))
//                                        .label(l -> l.setText("health"))
//                                        .layout(l -> l.width(100))
//                        )
//        )));
//        event.registerAboveAll(LDLib2.id("test_hud"), (ModularHudLayer) muiCache::get);
//    }
}
