package com.lowdragmc.lowdraglib.test;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.compass.CompassView;
import com.lowdragmc.lowdraglib.gui.factory.BlockEntityUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author KilaBash
 * @date 2022/05/24
 * @implNote TODO
 */
public class TestBlockEntity extends BlockEntity implements IUIHolder.BlockEntity {

    public TestBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(TYPE(), pWorldPosition, pBlockState);
    }

    @ExpectPlatform
    public static BlockEntityType<?> TYPE() {
        throw new AssertionError();
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

}
