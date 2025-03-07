package com.lowdragmc.lowdraglib.test;

import com.lowdragmc.lowdraglib.CommonProxy;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.compass.CompassView;
import com.lowdragmc.lowdraglib.gui.factory.BlockEntityUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.syncdata.IBlockEntityManaged;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.syncdata.storage.FieldManagedStorage;
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
public class TestBlockEntity extends BlockEntity implements IUIHolder.BlockEntity, IBlockEntityManaged {
    public final static ManagedFieldHolder FIELD_HOLDER = new ManagedFieldHolder(TestBlockEntity.class);
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    public TestBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(CommonProxy.TEST_BE_TYPE.get(), pWorldPosition, pBlockState);
    }

    public void use(Player player) {
        if (!getLevel().isClientSide) {
            BlockEntityUIFactory.INSTANCE.openUI(this, (ServerPlayer) player);
        }
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(this, entityPlayer)
                .widget(new CompassView(LDLib.MOD_ID));
//        return new ModularUI(this, entityPlayer).widget(new UIEditor(LDLib.location));
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return FIELD_HOLDER;
    }

}
