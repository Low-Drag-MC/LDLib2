package com.lowdragmc.lowdraglib2.test;


import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.syncdata.holder.IPersistManagedHolder;
import dev.vfyjxf.taffy.style.AlignContent;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

/**
 * @author KilaBash
 * @date 2022/05/24
 * @implNote TestBlock
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestBlock extends Block implements EntityBlock, IBlockRendererProvider, BlockUIMenuType.BlockUI {
    public static TestBlock BLOCK;

    public TestBlock(BlockBehaviour.Properties properties) {
        super(properties.noOcclusion().destroyTime(2));
        this.registerDefaultState(this.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        BLOCK = this;
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

//    IRenderer renderer = new IModelRenderer(LDLib2.id("block/cube"));

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
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
        return null;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        if (holder.player.level().getBlockEntity(holder.pos) instanceof TestBlockEntity testBlockEntity) {
            return testBlockEntity.createUI(holder);
        }
        var root = new UIElement().layout(layout -> layout
                .width(100)
                .height(100)
                .paddingAll(4)
                .gapAll(2)
                .justifyContent(AlignContent.CENTER)
        ).style(style -> style.backgroundTexture(Sprites.BORDER));
        root.addChild(new Label().setText("Test Block UI"));
        root.addChild(new TextField());
        return new ModularUI(UI.of(root), holder.player);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof IPersistManagedHolder persistManagedHolder) {
                Optional.ofNullable(stack.get(DataComponents.CUSTOM_DATA)).ifPresent(customData -> {
                    try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
                        var input = TagValueInput.create(reporter, level.registryAccess(), customData.copyTag());
                        persistManagedHolder.loadManagedPersistentData(input);
                    }
                });
            }
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var opt = Optional.ofNullable(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY));
        if (opt.isPresent() && opt.get() instanceof IPersistManagedHolder persistManagedHolder && opt.get().getLevel() instanceof Level level) {
            var drop = new ItemStack(this);
            try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
                var output = TagValueOutput.createWithContext(reporter, level.registryAccess());
                persistManagedHolder.saveManagedPersistentData(output, true);
                drop.set(DataComponents.CUSTOM_DATA, CustomData.of(output.buildResult()));
            }
            return List.of(drop);
        }
        return super.getDrops(state, params);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        if (level.getBlockEntity(pos) instanceof IPersistManagedHolder persistManagedHolder) {
            var clone = new ItemStack(this);
            if (includeData) {
                try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
                    var output = TagValueOutput.createWithContext(reporter, level.registryAccess());
                    persistManagedHolder.saveManagedPersistentData(output, true);
                    clone.set(DataComponents.CUSTOM_DATA, CustomData.of(output.buildResult()));
                }
            }
            return clone;
        }
        return super.getCloneItemStack(level, pos, state, includeData, player);
    }
}
