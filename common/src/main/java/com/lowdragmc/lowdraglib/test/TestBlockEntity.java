package com.lowdragmc.lowdraglib.test;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.compass.CompassView;
import com.lowdragmc.lowdraglib.gui.factory.BlockEntityUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAsyncAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import com.lowdragmc.lowdraglib.test.sync.TestReadOnlyManaged;
import dev.architectury.injectables.annotations.ExpectPlatform;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
public class TestBlockEntity extends BlockEntity implements IUIHolder.BlockEntityUI, IAsyncAutoSyncBlockEntity, IManaged {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(TestBlockEntity.class);
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @DescSynced
    @Persisted
    @ReadOnlyManaged(onDirtyMethod = "onDirty",
            serializeMethod = "serializeUid",
            deserializeMethod = "deserializeUid")
    public TestReadOnlyManaged testReadOnlyManaged = new TestReadOnlyManaged(this);

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

    @SuppressWarnings("unused")
    private boolean onDirty(TestReadOnlyManaged testManaged) {
        if (testManaged != null) {
            for (IRef ref : testManaged.getSyncStorage().getNonLazyFields()) {
                ref.update();
            }
            return testManaged.getSyncStorage().hasDirtySyncFields() ||
                    testManaged.getSyncStorage().hasDirtyPersistedFields();
        }
        return false;
    }

    @SuppressWarnings("unused")
    private CompoundTag serializeUid(TestReadOnlyManaged coverBehavior) {
        var uid = new CompoundTag();
        uid.putString("id", "testID");
        return uid;
    }

    @SuppressWarnings("unused")
    private TestReadOnlyManaged deserializeUid(CompoundTag uid) {
        return new TestReadOnlyManaged(this);
    }
}
