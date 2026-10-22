package com.lowdragmc.lowdraglib2.test.syncdata;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LDLib2.MOD_ID)
public class SyncTestBlockEntityTest {

    private static SyncTestBlockEntity placeSeeded(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, SyncTestBlock.BLOCK);
        SyncTestBlockEntity blockEntity = helper.getBlockEntity(pos);
        blockEntity.anchor = new BlockPos(1, -2, 3);
        blockEntity.label = "Steve";
        blockEntity.codecTags.add("a");
        blockEntity.maybeAnchor = new BlockPos(4, 5, 6);
        blockEntity.counter = 1;
        blockEntity.stack = new ItemStack(Items.DIAMOND, 3);
        blockEntity.positions.add(new BlockPos(7, 8, 9));
        blockEntity.scores.put("a", 1);
        blockEntity.gated = 5;
        blockEntity.lazyValue = 7;
        blockEntity.serverOnly = 99;
        return blockEntity;
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void survivesChunkSave(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var saved = placeSeeded(helper);
        var tag = saved.saveWithFullMetadata(access);
        var managed = tag.getCompound("managed");
        check(helper, managed.get("anchor") instanceof LongTag, "anchor saved with its codec: " + managed.get("anchor"));
        check(helper, managed.get("label") instanceof StringTag label && label.getAsString().equals("Steve"), "label saved: " + managed.get("label"));
        check(helper, !managed.contains("gated") && !managed.contains("lazyValue"), "fields that are only synced were saved: " + managed);

        if (!(BlockEntity.loadStatic(saved.getBlockPos(), saved.getBlockState(), tag, access) instanceof SyncTestBlockEntity loaded)) {
            helper.fail("the block entity did not load back");
            return;
        }
        check(helper, loaded.describeSynced().equals(saved.describeSynced()), "loaded " + loaded.describeSynced() + ", saved " + saved.describeSynced());
        check(helper, loaded.label.equals("Steve"), "label loaded with the codec: " + loaded.label);
        check(helper, loaded.serverOnly == 99, "serverOnly loaded: " + loaded.serverOnly);
        check(helper, loaded.gated == 0 && loaded.lazyValue == 0, "fields that are only synced were loaded: " + loaded.gated + "/" + loaded.lazyValue);

        saved.maybeAnchor = null;
        var reloaded = BlockEntity.loadStatic(saved.getBlockPos(), saved.getBlockState(), saved.saveWithFullMetadata(access), access);
        check(helper, reloaded instanceof SyncTestBlockEntity be && be.maybeAnchor == null, "a null field did not load back as null");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void updateTagCarriesEverySyncedField(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var server = placeSeeded(helper);
        var client = new SyncTestBlockEntity(server.getBlockPos(), server.getBlockState());
        client.loadWithComponents(server.getUpdateTag(access), access);

        check(helper, client.describeSynced().equals(server.describeSynced()), "client " + client.describeSynced() + ", server " + server.describeSynced());
        check(helper, client.label.equals("Steve"), "initial sync delivers the label through the codec: " + client.label);
        check(helper, client.gated == 5 && client.lazyValue == 7, "initial sync skips held-back fields: " + client.gated + "/" + client.lazyValue);
        check(helper, client.serverOnly == 0, "a field that is only persisted was synced: " + client.serverOnly);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
