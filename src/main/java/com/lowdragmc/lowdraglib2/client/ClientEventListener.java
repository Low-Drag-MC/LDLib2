package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.EditorResourceEvent;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.OreSprites;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
@EventBusSubscriber(modid = LDLib2.MOD_ID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ClientEventListener {

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
                mui.setScreenAndInit(containerScreen);
                event.addListener(mui.getWidget());
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
//
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
