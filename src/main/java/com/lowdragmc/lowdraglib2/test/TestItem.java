package com.lowdragmc.lowdraglib2.test;

import com.lowdragmc.lowdraglib2.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.AlignContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2022/05/24
 * @implNote TestItem
 */
@ParametersAreNonnullByDefault
public class TestItem extends BlockItem implements IItemRendererProvider, HeldItemUIMenuType.HeldItemUI {

    public static TestItem ITEM;

    public TestItem(Item.Properties properties) {
        super(TestBlock.BLOCK, properties);
        ITEM = this;
    }

    @Override
    @Nullable
    public IRenderer getRenderer(ItemStack stack) {
        return TestBlock.BLOCK.getRenderer(TestBlock.BLOCK.defaultBlockState());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer) {
            HeldItemUIMenuType.openUI(serverPlayer, usedHand);
        }
        return InteractionResult.SUCCESS;
    }


    @Override
    public ModularUI createUI(HeldItemUIMenuType.HeldItemUIHolder holder) {
        var root = new UIElement().layout(layout -> layout
                .width(100)
                .height(100)
                .paddingAll(4)
                .gapAll(2)
                .justifyContent(AlignContent.CENTER)
        ).style(style -> style.backgroundTexture(Sprites.BORDER));
        root.addChild(new Label().setText("Test Item UI"));
        root.addChild(new TextField());
        return new ModularUI(UI.of(root), holder.player);
    }
}
