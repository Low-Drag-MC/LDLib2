package com.lowdragmc.lowdraglib2.test;

import com.lowdragmc.lowdraglib2.CommonProxy;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.syncdata.IBlockEntityManaged;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.UpdateListener;
import com.lowdragmc.lowdraglib2.syncdata.blockentity.IAsyncAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.blockentity.IAsyncAutoSyncPersistBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.blockentity.IAutoPersistBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaJustify;


public class TestBlockEntity extends BlockEntity implements IAsyncAutoSyncPersistBlockEntity {
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    @UpdateListener(methodName = "onIntValueChanged")
    private int intValue = 10;
    @Persisted
    @DescSynced
    private ItemStack itemStack = ItemStack.EMPTY;

    public TestBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(CommonProxy.TEST_BE_TYPE.get(), pWorldPosition, pBlockState);
    }

    private void onIntValueChanged(int oldValue, int newValue) {
        LDLib2.LOGGER.info("Int value changed from {} to {}", oldValue, newValue);
    }

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        var root = new UIElement().layout(layout -> layout
                .setPadding(YogaEdge.ALL, 4)
                .setGap(YogaGutter.ALL, 2)
                .setJustifyContent(YogaJustify.CENTER)
        ).style(style -> style.backgroundTexture(Sprites.BORDER));
        root.addChild(new Label().setText("Test Block UI"));
        root.addChild(new TextField());
        root.addChild(new Button().setText("Change Random Value").setOnServerClick(e -> {
            intValue = (int) (Math.random() * 100);
            itemStack = new ItemStack(BuiltInRegistries.ITEM.getRandom(LDLib2.RANDOM).orElse(Items.APPLE.builtInRegistryHolder()));
        }));
        root.addChild(new ItemSlot().bindDataSource(SupplierDataSource.of(() -> itemStack)));
        root.addChild(new Label().bindDataSource(SupplierDataSource.of(() -> Component.literal(String.valueOf(intValue)))));
        return new ModularUI(UI.of(root), holder.player);
    }
}
