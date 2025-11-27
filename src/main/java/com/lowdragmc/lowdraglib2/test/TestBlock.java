package com.lowdragmc.lowdraglib2.test;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaJustify;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2022/05/24
 * @implNote TestBlock
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestBlock extends Block implements EntityBlock, IBlockRendererProvider, BlockUIMenuType.BlockUI {

    public static final TestBlock BLOCK = new TestBlock();
    private TestBlock() {
        super(Properties.of().noOcclusion().destroyTime(2));
        this.registerDefaultState(this.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(BlockStateProperties.FACING);
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(BlockStateProperties.FACING, context.getNearestLookingDirection());
    }

    @Nullable
    @Override
    @ParametersAreNonnullByDefault
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new TestBlockEntity(pPos, pState);
    }

    IRenderer renderer = new IModelRenderer(LDLib2.id("block/cube"));

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (player instanceof ServerPlayer serverPlayer){
            BlockUIMenuType.openUI(serverPlayer, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public IRenderer getRenderer(BlockState state) {
        return renderer;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        if (holder.player.level().getBlockEntity(holder.pos) instanceof TestBlockEntity testBlockEntity) {
            return testBlockEntity.createUI(holder);
        }
        var root = new UIElement().layout(layout -> layout
                .setWidth(100)
                .setHeight(100)
                .setPadding(YogaEdge.ALL, 4)
                .setGap(YogaGutter.ALL, 2)
                .setJustifyContent(YogaJustify.CENTER)
        ).style(style -> style.backgroundTexture(Sprites.BORDER));
        root.addChild(new Label().setText("Test Block UI"));
        root.addChild(new TextField());
        return new ModularUI(UI.of(root), holder.player);
    }
}
