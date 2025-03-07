package com.lowdragmc.lowdraglib.test;

import com.lowdragmc.lowdraglib.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * @author KilaBash
 * @date 2022/05/24
 * @implNote TestItem
 */
public class TestItem extends BlockItem implements IItemRendererProvider, IUIHolder.ItemUI {

    public static final TestItem ITEM = new TestItem();

    private TestItem() {
        super(TestBlock.BLOCK, new Properties());
    }

    @Override
    public IRenderer getRenderer(ItemStack stack) {
        return TestBlock.BLOCK.getRenderer(TestBlock.BLOCK.defaultBlockState());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
            HeldItemUIFactory.INSTANCE.openUI(serverPlayer, context.getHand());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ModularUI createUI(Player entityPlayer, HeldItemUIFactory.HeldItemHolder holder) {
        return null;
    }
}
