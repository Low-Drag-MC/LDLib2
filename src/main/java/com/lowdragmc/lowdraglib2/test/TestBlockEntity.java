package com.lowdragmc.lowdraglib2.test;

import com.lowdragmc.lowdraglib2.CommonProxy;
import com.lowdragmc.lowdraglib2.gui.factory_outdated.BlockEntityUIFactory;
import com.lowdragmc.lowdraglib2.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib2.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib2.syncdata.IBlockEntityManaged;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.UpdateListener;
import com.lowdragmc.lowdraglib2.syncdata.blockentity.IManagedBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author KilaBash
 * @date 2022/05/24
 * @implNote TODO
 */
public class TestBlockEntity extends BlockEntity implements IUIHolder.BlockEntityUI, IBlockEntityManaged, IManagedBlockEntity {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(TestBlockEntity.class);
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    @Persisted
    @DescSynced
    @UpdateListener(methodName = "onIntValueChanged")
    private int intValue = 10;

    public TestBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(CommonProxy.TEST_BE_TYPE.get(), pWorldPosition, pBlockState);
    }

    private void onIntValueChanged(int oldValue, int newValue) {

    }

    public void use(Player player) {
        if (!getLevel().isClientSide) {
            BlockEntityUIFactory.INSTANCE.openUI(this, (ServerPlayer) player);
        }
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(this, entityPlayer);
//        return new ModularUI(this, entityPlayer).widget(new UIEditor(LDLib.location));
    }

    @Override
    public IManagedStorage getRootStorage() {
        return getSyncStorage();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onChanged() {
        markAsDirty();
    }

}
