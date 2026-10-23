package com.lowdragmc.lowdraglib2.test.gametest.syncdata;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

public final class SyncTestBlockEntityGameTest {
    private static final String SURVIVES_CHUNK_SAVE_PATH = "sync_test_block_entity_survives_chunk_save";
    private static final String UPDATE_TAG_PATH = "sync_test_block_entity_update_tag_carries_every_synced_field";

    private SyncTestBlockEntityGameTest() {
    }

    static void registerFunctions() {
        SyncDataGameTests.registerFunction(SURVIVES_CHUNK_SAVE_PATH, SyncTestBlockEntityGameTest::survivesChunkSave);
        SyncDataGameTests.registerFunction(UPDATE_TAG_PATH, SyncTestBlockEntityGameTest::updateTagCarriesEverySyncedField);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = SyncDataGameTests.defaultTestData(environment, "empty");
        SyncDataGameTests.registerFunctionTest(event, SURVIVES_CHUNK_SAVE_PATH, SyncDataGameTests.functionKey(SURVIVES_CHUNK_SAVE_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, UPDATE_TAG_PATH, SyncDataGameTests.functionKey(UPDATE_TAG_PATH), testData);
    }

    private static SyncTestBlockEntity placeSeeded(GameTestHelper helper) {
        // the "empty" template is a single block, keep the block inside it so the test area cleans it up
        var pos = BlockPos.ZERO;
        helper.setBlock(pos, SyncTestBlock.BLOCK);
        var blockEntity = helper.getBlockEntity(pos, SyncTestBlockEntity.class);
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

    private static void survivesChunkSave(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var saved = placeSeeded(helper);
        var tag = saved.saveWithFullMetadata(access);
        var managed = tag.getCompoundOrEmpty("managed");
        check(helper, managed.get("anchor") instanceof LongTag, "anchor saved with its codec: " + managed.get("anchor"));
        check(helper, managed.get("label") instanceof StringTag(var label) && label.equals("Steve"), "label saved: " + managed.get("label"));
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

    private static void updateTagCarriesEverySyncedField(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var server = placeSeeded(helper);
        var client = new SyncTestBlockEntity(server.getBlockPos(), server.getBlockState());
        client.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, access, server.getUpdateTag(access)));

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
