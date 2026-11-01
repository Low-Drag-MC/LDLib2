package com.lowdragmc.lowdraglib2.test.gametest.syncdata;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class PersistedCustomDataGameTest {
    private static final String CUSTOM_DATA_SURVIVES_SAVE_AND_LOAD_PATH = "persisted_custom_data_custom_data_survives_save_and_load";
    private static final String CUSTOM_DATA_SURVIVES_WITHOUT_MANAGED_FIELDS_PATH = "persisted_custom_data_custom_data_survives_without_managed_fields";

    static void registerFunctions() {
        SyncDataGameTests.registerFunction(CUSTOM_DATA_SURVIVES_SAVE_AND_LOAD_PATH, PersistedCustomDataGameTest::customDataSurvivesSaveAndLoad);
        SyncDataGameTests.registerFunction(CUSTOM_DATA_SURVIVES_WITHOUT_MANAGED_FIELDS_PATH, PersistedCustomDataGameTest::customDataSurvivesWithoutManagedFields);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = SyncDataGameTests.defaultTestData(environment, "empty");
        SyncDataGameTests.registerFunctionTest(event, CUSTOM_DATA_SURVIVES_SAVE_AND_LOAD_PATH, SyncDataGameTests.functionKey(CUSTOM_DATA_SURVIVES_SAVE_AND_LOAD_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, CUSTOM_DATA_SURVIVES_WITHOUT_MANAGED_FIELDS_PATH, SyncDataGameTests.functionKey(CUSTOM_DATA_SURVIVES_WITHOUT_MANAGED_FIELDS_PATH), testData);
    }

    public static class CustomOnly extends SyncProbeHolder {
        public String note = "";

        public CustomOnly(ServerLevel level) {
            super(level);
        }

        @Override
        public void saveCustomPersistedData(ValueOutput output, boolean forDrop) {
            if (!note.isEmpty()) output.putString("note", note);
        }

        @Override
        public void loadCustomPersistedData(ValueInput input) {
            note = input.getStringOr("note", "");
        }
    }

    public static class WithFields extends CustomOnly {
        @Persisted
        public int value;

        public WithFields(ServerLevel level) {
            super(level);
        }
    }

    static void customDataSurvivesSaveAndLoad(GameTestHelper helper) {
        var level = helper.getLevel();
        var saved = new WithFields(level);
        saved.value = 3;
        saved.note = "hello";
        var tag = save(level, saved);

        var loaded = new WithFields(level);
        load(level, loaded, tag);
        check(helper, loaded.value == 3, "managed field lost: " + tag);
        check(helper, loaded.note.equals("hello"), "custom data lost: " + tag);
        helper.succeed();
    }

    static void customDataSurvivesWithoutManagedFields(GameTestHelper helper) {
        var level = helper.getLevel();
        var saved = new CustomOnly(level);
        saved.note = "hello";
        var tag = save(level, saved);
        check(helper, !tag.isEmpty(), "custom data was not saved");

        var loaded = new CustomOnly(level);
        load(level, loaded, tag);
        check(helper, loaded.note.equals("hello"), "custom data lost: " + tag);
        helper.succeed();
    }

    private static CompoundTag save(ServerLevel level, SyncProbeHolder holder) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        holder.saveManagedPersistentData(output, false);
        return output.buildResult();
    }

    private static void load(ServerLevel level, SyncProbeHolder holder, CompoundTag tag) {
        holder.loadManagedPersistentData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
