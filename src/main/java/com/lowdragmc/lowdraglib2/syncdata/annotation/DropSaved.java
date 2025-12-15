package com.lowdragmc.lowdraglib2.syncdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sometimes, you want to store the field values into the drop item while breaking the block.
 * This annotation is used to mark a field to be saved to the drop item. However, it also require additional code work before using it.
 * <pre>{@code
 * public class MyBlock extends Block {
 *     @Override
 *     public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
 *         if (!level.isClientSide) {
 *             if (level.getBlockEntity(pos) instanceof IPersistManagedHolder persistManagedHolder) {
 *                 // you can use other DataComponents if you want.
 *                 Optional.ofNullable(stack.get(DataComponents.CUSTOM_DATA)).ifPresent(customData -> {
 *                     persistManagedHolder.loadManagedPersistentData(customData.copyTag());
 *                 });
 *             }
 *         }
 *     }
 *
 *     @Override
 *     protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
 *         var opt = Optional.ofNullable(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY));
 *         if (opt.isPresent() && opt.get() instanceof IPersistManagedHolder persistManagedHolder) {
 *             var drop = new ItemStack(this);
 *             var tag = new CompoundTag();
 *             persistManagedHolder.saveManagedPersistentData(tag, true);
 *             drop.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
 *             // you can move this part to LootTable if you want.
 *             return List.of(drop);
 *         }
 *         return super.getDrops(state, params);
 *     }
 *
 *     @Override
 *     public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
 *         // if you want to clone an item with drop data, don't forget it
 *         if (level.getBlockEntity(pos) instanceof IPersistManagedHolder persistManagedHolder) {
 *             var clone = new ItemStack(this);
 *             var tag = new CompoundTag();
 *             persistManagedHolder.saveManagedPersistentData(tag, true);
 *             clone.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
 *             return clone;
 *         }
 *         return super.getCloneItemStack(state, target, level, pos, player);
 *     }
 * }
 *
 * public class MyBlockEntity extends BlockEntity implements ISyncPersistRPCBlockEntity {
 *     @Persisted
 *     private int intValue = 10;
 *     @Persisted
 *     @DropSaved
 *     private ItemStack itemStack = ItemStack.EMPTY;
 * }
 * }</pre>
 * After the above setup, the value of `itemStack` in the MyBlockEntity will be stored in the itemstack while breaking and clone.
 * And the value stored in the itemstack will be resumed after placement.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DropSaved {
}
