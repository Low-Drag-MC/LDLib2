package com.lowdragmc.lowdraglib2.test.gametest.syncdata;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.ValueIOSerializableReadOnlyAccessor;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ConditionalSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.LazyManaged;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ManagedCodec;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Pins down what the auto sync notices today, field kind by field kind, so changes to change detection can't silently
 * drop or invent updates.
 */
public final class SyncChangeDetectionGameTest {
    private static final String EVERY_FIELD_KIND_SYNCS_EXACTLY_ITS_CHANGES_PATH = "sync_change_detection_every_field_kind_syncs_exactly_its_changes";
    private static final String CONDITIONAL_FIELD_WAITS_FOR_ITS_CONDITION_PATH = "sync_change_detection_conditional_field_waits_for_its_condition";
    private static final String CONDITIONAL_FIELD_IS_SAVED_WHILE_HELD_BACK_PATH = "sync_change_detection_conditional_field_is_saved_while_held_back";
    private static final String LAZY_FIELD_WAITS_FOR_MARK_DIRTY_PATH = "sync_change_detection_lazy_field_waits_for_mark_dirty";
    private static final String LAZY_FIELD_IGNORES_ITS_CONDITION_PATH = "sync_change_detection_lazy_field_ignores_its_condition";
    private static final String CHANGES_IN_ONE_PERIOD_SHARE_ONE_PACKET_PATH = "sync_change_detection_changes_in_one_period_share_one_packet";
    private static final String CODEC_MARK_WORKS_IN_ANY_BUILDER_ORDER_PATH = "sync_change_detection_codec_mark_works_in_any_builder_order";
    private static final String SUBCLASSED_ACCESSORS_KEEP_THEIR_OWN_COMPARISON_PATH = "sync_change_detection_subclassed_accessors_keep_their_own_comparison";
    private static final String CODEC_MARK_NOTICES_A_VALUE_ENCODED_AS_EMPTY_PATH = "sync_change_detection_codec_mark_notices_a_value_encoded_as_empty";
    private static final String NULL_ELEMENTS_SURVIVE_SAVING_AND_THE_FIRST_SYNC_PATH = "sync_change_detection_null_elements_survive_saving_and_the_first_sync";
    private static final String UNSENT_NESTED_CHANGES_NOTIFY_PERSISTENCE_ONCE_PER_CHANGE_PATH = "sync_change_detection_unsent_nested_changes_notify_persistence_once_per_change";

    static void registerFunctions() {
        SyncDataGameTests.registerFunction(EVERY_FIELD_KIND_SYNCS_EXACTLY_ITS_CHANGES_PATH, SyncChangeDetectionGameTest::everyFieldKindSyncsExactlyItsChanges);
        SyncDataGameTests.registerFunction(CONDITIONAL_FIELD_WAITS_FOR_ITS_CONDITION_PATH, SyncChangeDetectionGameTest::conditionalFieldWaitsForItsCondition);
        SyncDataGameTests.registerFunction(CONDITIONAL_FIELD_IS_SAVED_WHILE_HELD_BACK_PATH, SyncChangeDetectionGameTest::conditionalFieldIsSavedWhileHeldBack);
        SyncDataGameTests.registerFunction(LAZY_FIELD_WAITS_FOR_MARK_DIRTY_PATH, SyncChangeDetectionGameTest::lazyFieldWaitsForMarkDirty);
        SyncDataGameTests.registerFunction(LAZY_FIELD_IGNORES_ITS_CONDITION_PATH, SyncChangeDetectionGameTest::lazyFieldIgnoresItsCondition);
        SyncDataGameTests.registerFunction(CHANGES_IN_ONE_PERIOD_SHARE_ONE_PACKET_PATH, SyncChangeDetectionGameTest::changesInOnePeriodShareOnePacket);
        SyncDataGameTests.registerFunction(CODEC_MARK_WORKS_IN_ANY_BUILDER_ORDER_PATH, SyncChangeDetectionGameTest::codecMarkWorksInAnyBuilderOrder);
        SyncDataGameTests.registerFunction(SUBCLASSED_ACCESSORS_KEEP_THEIR_OWN_COMPARISON_PATH, SyncChangeDetectionGameTest::subclassedAccessorsKeepTheirOwnComparison);
        SyncDataGameTests.registerFunction(CODEC_MARK_NOTICES_A_VALUE_ENCODED_AS_EMPTY_PATH, SyncChangeDetectionGameTest::codecMarkNoticesAValueEncodedAsEmpty);
        SyncDataGameTests.registerFunction(NULL_ELEMENTS_SURVIVE_SAVING_AND_THE_FIRST_SYNC_PATH, SyncChangeDetectionGameTest::nullElementsSurviveSavingAndTheFirstSync);
        SyncDataGameTests.registerFunction(UNSENT_NESTED_CHANGES_NOTIFY_PERSISTENCE_ONCE_PER_CHANGE_PATH, SyncChangeDetectionGameTest::unsentNestedChangesNotifyPersistenceOncePerChange);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = SyncDataGameTests.defaultTestData(environment, "empty");
        SyncDataGameTests.registerFunctionTest(event, EVERY_FIELD_KIND_SYNCS_EXACTLY_ITS_CHANGES_PATH, SyncDataGameTests.functionKey(EVERY_FIELD_KIND_SYNCS_EXACTLY_ITS_CHANGES_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, CONDITIONAL_FIELD_WAITS_FOR_ITS_CONDITION_PATH, SyncDataGameTests.functionKey(CONDITIONAL_FIELD_WAITS_FOR_ITS_CONDITION_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, CONDITIONAL_FIELD_IS_SAVED_WHILE_HELD_BACK_PATH, SyncDataGameTests.functionKey(CONDITIONAL_FIELD_IS_SAVED_WHILE_HELD_BACK_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, LAZY_FIELD_WAITS_FOR_MARK_DIRTY_PATH, SyncDataGameTests.functionKey(LAZY_FIELD_WAITS_FOR_MARK_DIRTY_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, LAZY_FIELD_IGNORES_ITS_CONDITION_PATH, SyncDataGameTests.functionKey(LAZY_FIELD_IGNORES_ITS_CONDITION_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, CHANGES_IN_ONE_PERIOD_SHARE_ONE_PACKET_PATH, SyncDataGameTests.functionKey(CHANGES_IN_ONE_PERIOD_SHARE_ONE_PACKET_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, CODEC_MARK_WORKS_IN_ANY_BUILDER_ORDER_PATH, SyncDataGameTests.functionKey(CODEC_MARK_WORKS_IN_ANY_BUILDER_ORDER_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, SUBCLASSED_ACCESSORS_KEEP_THEIR_OWN_COMPARISON_PATH, SyncDataGameTests.functionKey(SUBCLASSED_ACCESSORS_KEEP_THEIR_OWN_COMPARISON_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, CODEC_MARK_NOTICES_A_VALUE_ENCODED_AS_EMPTY_PATH, SyncDataGameTests.functionKey(CODEC_MARK_NOTICES_A_VALUE_ENCODED_AS_EMPTY_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, NULL_ELEMENTS_SURVIVE_SAVING_AND_THE_FIRST_SYNC_PATH, SyncDataGameTests.functionKey(NULL_ELEMENTS_SURVIVE_SAVING_AND_THE_FIRST_SYNC_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, UNSENT_NESTED_CHANGES_NOTIFY_PERSISTENCE_ONCE_PER_CHANGE_PATH, SyncDataGameTests.functionKey(UNSENT_NESTED_CHANGES_NOTIFY_PERSISTENCE_ONCE_PER_CHANGE_PATH), testData);
    }


    /**
     * Hands its resources out through an override, so they can't be assumed to be the ones it saves.
     */
    public static class PassThroughItems extends SyncProbeHolder.ExposedItems {
        public PassThroughItems(int size) {
            super(size);
        }

        @Override
        public ItemResource getResource(int index) {
            return super.getResource(index);
        }
    }

    public static class Probe extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public int intValue;
        @Persisted
        @DescSynced
        public long longValue;
        @Persisted
        @DescSynced
        public float floatValue;
        @Persisted
        @DescSynced
        public double doubleValue;
        @Persisted
        @DescSynced
        public boolean boolValue;
        @Persisted
        @DescSynced
        public String text = "text";
        @Persisted
        @DescSynced
        public Direction facing = Direction.NORTH;
        @Persisted
        @DescSynced
        public Item item = Items.STONE;
        @Persisted
        @DescSynced
        public UUID owner = new UUID(1, 2);
        @Persisted
        @DescSynced
        public Identifier location = LDLib2.id("a");
        @Persisted
        @DescSynced
        public BlockState state = Blocks.STONE.defaultBlockState();
        @Persisted
        @DescSynced
        public ItemStack stack = new ItemStack(Items.APPLE, 2);
        @Persisted
        @DescSynced
        public FluidStack fluid = new FluidStack(Fluids.WATER, 100);
        @Persisted
        @DescSynced
        public BlockPos pos = new BlockPos.MutableBlockPos(1, 2, 3);
        @Persisted
        @DescSynced
        public Vector3f vector = new Vector3f(1, 2, 3);
        @Persisted
        @DescSynced
        public int[] ints = new int[3];
        @Persisted
        @DescSynced
        public ItemStack[] stacks = {new ItemStack(Items.APPLE), ItemStack.EMPTY};
        @Persisted
        @DescSynced
        public List<String> strings = new ArrayList<>(List.of("a"));
        @Persisted
        @DescSynced
        public List<Integer> integers = new ArrayList<>(List.of(1000));
        @Persisted
        @DescSynced
        public List<BlockPos> positions = new ArrayList<>(List.of(BlockPos.ZERO));
        @Persisted
        @DescSynced
        public Map<String, Integer> scores = new HashMap<>(Map.of("a", 1));
        @Persisted
        @DescSynced
        @ManagedCodec("tagsCodec")
        public List<String> codecTags = new ArrayList<>(List.of("a"));
        @Persisted
        @DescSynced
        public final SyncProbeHolder.ExposedItems items = new SyncProbeHolder.ExposedItems(3);
        @Persisted
        @DescSynced
        public final SyncProbeHolder.ExposedTank tank = new SyncProbeHolder.ExposedTank(1000);
        @Persisted
        @DescSynced
        public final LockableItems lockableItems = new LockableItems(2);
        @Persisted
        @DescSynced
        public final PassThroughItems passThroughItems = new PassThroughItems(2);
        @Persisted
        @DescSynced
        public List<Direction> directions = new ArrayList<>(List.of(Direction.UP));
        @Persisted
        @DescSynced
        public Map<Direction, Item> itemsBySide = new HashMap<>(Map.of(Direction.NORTH, Items.STONE));
        @Persisted
        @DescSynced
        public final LockableTank lockableTank = new LockableTank(1000);
        @Persisted
        @DescSynced
        public final List<SyncProbeHolder.ExposedItems> itemList = new ArrayList<>(List.of(new SyncProbeHolder.ExposedItems(2)));
        @Persisted
        @DescSynced
        public final Map<String, MapSerializationGameTest.NestedReadOnly> nestedMap = new HashMap<>(Map.of("a", new MapSerializationGameTest.NestedReadOnly(1, "x")));
        @Persisted
        @DescSynced
        public final Part part = new Part(this::notifyPersistence);
        @DescSynced
        @ConditionalSynced(methodName = "isPositive")
        public int gated;
        @DescSynced
        @LazyManaged
        public int lazy;
        @Persisted
        public int persistOnly;
        @Persisted
        @DescSynced
        public final SyncProbeHolder.ExposedTank[] tanks = {new SyncProbeHolder.ExposedTank(1000), new SyncProbeHolder.ExposedTank(1000)};
        public boolean shown;
        @Persisted
        @DescSynced
        @ConditionalSynced(methodName = "isShown")
        public final SyncProbeHolder.ExposedItems shownItems = new SyncProbeHolder.ExposedItems(2);
        @Persisted
        @DescSynced
        @ConditionalSynced(methodName = "isShown")
        public final SyncProbeHolder.ExposedTank[] shownTanks = {new SyncProbeHolder.ExposedTank(1000)};
        @Persisted
        @DescSynced
        @ConditionalSynced(methodName = "isShown")
        public int[] shownInts = new int[2];
        @DescSynced
        @LazyManaged
        @ConditionalSynced(methodName = "isShown")
        public int shownLazy;

        public Probe(ServerLevel level) {
            super(level);
            items.put(0, new ItemStack(Items.IRON_INGOT, 4));
            items.put(1, new ItemStack(Items.GOLD_INGOT, 4));
            tank.fill(Fluids.WATER, 200);
            lockableItems.put(0, new ItemStack(Items.IRON_INGOT, 4));
            passThroughItems.put(0, new ItemStack(Items.IRON_INGOT, 4));
            lockableTank.fill(Fluids.WATER, 200);
            itemList.getFirst().put(0, new ItemStack(Items.IRON_INGOT, 4));
        }

        private boolean isPositive(int value) {
            return value > 0;
        }

        private boolean isShown(SyncProbeHolder.ExposedItems value) {
            return shown;
        }

        private boolean isShown(SyncProbeHolder.ExposedTank[] value) {
            return shown;
        }

        private boolean isShown(int[] value) {
            return shown;
        }

        private boolean isShown(int value) {
            return shown;
        }

        private static Codec<List<String>> tagsCodec() {
            return Codec.STRING.listOf().xmap(ArrayList::new, list -> list);
        }
    }

    private record Case(String name, String field, Consumer<Probe> change, boolean expectSync, boolean expectPersist) {
        static Case synced(String name, String field, Consumer<Probe> change) {
            return new Case(name, field, change, true, true);
        }

        static Case unchanged(String name, String field, Consumer<Probe> change) {
            return new Case(name, field, change, false, false);
        }
    }

    private static final List<Case> CASES = List.of(
            Case.synced("int assigned", "intValue", p -> p.intValue = 5),
            Case.synced("long assigned", "longValue", p -> p.longValue = 1L << 40),
            Case.synced("float assigned", "floatValue", p -> p.floatValue = 0.5f),
            Case.synced("double assigned", "doubleValue", p -> p.doubleValue = 0.25),
            Case.synced("boolean assigned", "boolValue", p -> p.boolValue = true),
            Case.synced("String assigned", "text", p -> p.text = "changed"),
            Case.synced("enum assigned", "facing", p -> p.facing = Direction.SOUTH),
            Case.synced("registry entry assigned", "item", p -> p.item = Items.DIAMOND),
            Case.synced("UUID assigned", "owner", p -> p.owner = new UUID(3, 4)),
            Case.synced("Identifier assigned", "location", p -> p.location = LDLib2.id("b")),
            Case.synced("BlockState assigned", "state", p -> p.state = Blocks.DIRT.defaultBlockState()),
            Case.synced("ItemStack assigned", "stack", p -> p.stack = new ItemStack(Items.DIAMOND)),
            Case.synced("ItemStack grown in place", "stack", p -> p.stack.grow(1)),
            Case.synced("ItemStack renamed in place", "stack", p -> p.stack.set(DataComponents.CUSTOM_NAME, Component.literal("renamed"))),
            Case.synced("FluidStack grown in place", "fluid", p -> p.fluid.grow(10)),
            Case.synced("mutable BlockPos moved in place", "pos", p -> ((BlockPos.MutableBlockPos) p.pos).move(Direction.UP)),
            Case.synced("Vector3f changed in place", "vector", p -> p.vector.x = 5),
            Case.synced("int[] element assigned", "ints", p -> p.ints[1] = 7),
            Case.synced("int[] replaced", "ints", p -> p.ints = new int[]{0, 7, 0}),
            Case.synced("ItemStack[] element assigned", "stacks", p -> p.stacks[1] = new ItemStack(Items.STICK)),
            Case.synced("ItemStack[] element grown in place", "stacks", p -> p.stacks[0].grow(1)),
            Case.synced("List<String> added to", "strings", p -> p.strings.add("b")),
            Case.synced("List<String> element set", "strings", p -> p.strings.set(0, "z")),
            Case.synced("List<String> element set to null", "strings", p -> p.strings.set(0, null)),
            Case.synced("List<Direction> element set", "directions", p -> p.directions.set(0, Direction.DOWN)),
            Case.synced("Map<Direction, Item> value replaced", "itemsBySide", p -> p.itemsBySide.put(Direction.NORTH, Items.DIAMOND)),
            Case.synced("Map<Direction, Item> key added", "itemsBySide", p -> p.itemsBySide.put(Direction.SOUTH, Items.APPLE)),
            Case.synced("List<Integer> element set", "integers", p -> p.integers.set(0, 2000)),
            Case.synced("List<BlockPos> added to", "positions", p -> p.positions.add(new BlockPos(4, 5, 6))),
            Case.synced("Map key added", "scores", p -> p.scores.put("b", 2)),
            Case.synced("Map value replaced", "scores", p -> p.scores.put("a", 5)),
            Case.synced("@ManagedCodec list added to", "codecTags", p -> p.codecTags.add("b")),
            Case.synced("container slot set", "items", p -> p.items.put(2, new ItemStack(Items.DIAMOND, 3))),
            Case.synced("container stack grown in place", "items", p -> p.items.live(1).grow(1)),
            Case.synced("container stack renamed in place", "items", p -> p.items.live(1).set(DataComponents.CUSTOM_NAME, Component.literal("renamed"))),
            Case.synced("container slot emptied", "items", p -> p.items.put(0, ItemStack.EMPTY)),
            Case.synced("container resized", "items", p -> p.items.resize(5)),
            Case.synced("tank filled", "tank", p -> p.tank.fill(Fluids.WATER, 100)),
            Case.synced("tank fluid grown in place", "tank", p -> p.tank.live().grow(10)),
            Case.synced("tank fluid renamed in place", "tank", p -> p.tank.live().set(DataComponents.CUSTOM_NAME, Component.literal("renamed"))),
            Case.synced("container reading slots through an override: slot set", "passThroughItems", p -> p.passThroughItems.put(1, new ItemStack(Items.DIAMOND))),
            Case.synced("container reading slots through an override: grown in place", "passThroughItems", p -> p.passThroughItems.live(0).grow(1)),
            Case.synced("tank drained empty", "tank", p -> p.tank.drain(1000)),
            Case.synced("tank in an array filled", "tanks", p -> p.tanks[1].fill(Fluids.WATER, 100)),
            Case.synced("custom container slot set", "lockableItems", p -> p.lockableItems.put(1, new ItemStack(Items.DIAMOND))),
            Case.synced("custom container stack grown in place", "lockableItems", p -> p.lockableItems.live(0).grow(1)),
            Case.synced("custom container's own state changed", "lockableItems", p -> p.lockableItems.locked = true),
            Case.synced("custom tank filled", "lockableTank", p -> p.lockableTank.fill(Fluids.WATER, 100)),
            Case.synced("custom tank's own state changed", "lockableTank", p -> p.lockableTank.locked = true),
            Case.synced("container in a List: slot set", "itemList", p -> p.itemList.getFirst().put(1, new ItemStack(Items.DIAMOND))),
            Case.synced("container in a List: stack grown in place", "itemList", p -> p.itemList.getFirst().live(0).grow(1)),
            Case.synced("container added to a List", "itemList", p -> p.itemList.add(new SyncProbeHolder.ExposedItems(1))),
            Case.synced("ValueIOSerializable in a Map changed in place", "nestedMap", p -> p.nestedMap.get("a").counter = 5),
            Case.synced("nested managed field assigned", "part", p -> p.part.progress = 9),
            Case.synced("nested managed boolean assigned", "part", p -> p.part.working = true),
            new Case("persist-only field assigned", "persistOnly", p -> p.persistOnly = 3, false, true),

            Case.unchanged("int assigned the same value", "intValue", p -> p.intValue = p.intValue),
            Case.unchanged("String replaced by an equal copy", "text", p -> p.text = new String(p.text)),
            Case.unchanged("ItemStack replaced by an equal copy", "stack", p -> p.stack = p.stack.copy()),
            Case.unchanged("BlockPos replaced by an equal copy", "pos", p -> p.pos = p.pos.immutable()),
            Case.unchanged("Vector3f replaced by an equal copy", "vector", p -> p.vector = new Vector3f(p.vector)),
            Case.unchanged("List<String> element set to an equal copy", "strings", p -> p.strings.set(0, new String(p.strings.get(0)))),
            Case.unchanged("List<Direction> element set to the same value", "directions", p -> p.directions.set(0, Direction.UP)),
            Case.unchanged("container slot set to an equal stack", "items", p -> p.items.put(0, p.items.live(0).copy())),
            Case.unchanged("container in a List: slot set to an equal stack", "itemList", p -> p.itemList.getFirst().put(0, p.itemList.getFirst().live(0).copy())),
            Case.unchanged("ValueIOSerializable in a Map given an equal value", "nestedMap", p -> p.nestedMap.get("a").label = new String(p.nestedMap.get("a").label)),
            Case.unchanged("tank refilled with an equal copy", "tank", p -> p.tank.set(0, p.tank.getResource(0), p.tank.getAmountAsInt(0))),
            Case.unchanged("tank in an array refilled with an equal copy", "tanks", p -> p.tanks[0].set(0, p.tanks[0].getResource(0), p.tanks[0].getAmountAsInt(0))),
            Case.unchanged("custom container slot set to an equal stack", "lockableItems", p -> p.lockableItems.put(0, p.lockableItems.live(0).copy())),
            Case.unchanged("nested managed field assigned the same value", "part", p -> p.part.progress = p.part.progress)
    );

    static void everyFieldKindSyncsExactlyItsChanges(GameTestHelper helper) {
        var level = helper.getLevel();
        var failures = new ArrayList<String>();
        for (var c : CASES) {
            var server = settled(level);
            var client = new Probe(level);
            server.initClient(client);
            if (server.syncTo(client)) {
                failures.add(c.name + ": sent " + server.syncFieldNames(server.sentChanged) + " before anything changed");
                continue;
            }

            c.change.accept(server);
            boolean sent = server.syncTo(client);
            if (c.expectSync) {
                int index = server.syncIndex(c.field);
                if (!sent) {
                    failures.add(c.name + ": not synced");
                } else if (server.sentChanged.cardinality() != 1 || !server.sentChanged.get(index)) {
                    failures.add(c.name + ": sent " + server.syncFieldNames(server.sentChanged) + ", expected [" + c.field + "]");
                } else if (!client.syncedValue(c.field).equals(server.syncedValue(c.field))) {
                    failures.add(c.name + ": client has " + client.syncedValue(c.field) + ", server " + server.syncedValue(c.field));
                }
            } else if (sent) {
                failures.add(c.name + ": sent " + server.syncFieldNames(server.sentChanged) + ", expected nothing");
            }
            if (c.expectPersist != server.persistenceNotices > 0) {
                failures.add(c.name + ": " + server.persistenceNotices + " persistence notices, expected " + (c.expectPersist ? "some" : "none"));
            }

            server.persistenceNotices = 0;
            if (server.syncTo(client)) {
                failures.add(c.name + ": sent " + server.syncFieldNames(server.sentChanged) + " again with nothing changed");
            }
            if (server.persistenceNotices > 0) {
                failures.add(c.name + ": notified persistence again with nothing changed");
            }
        }
        report(helper, failures);
    }

    static void conditionalFieldWaitsForItsCondition(GameTestHelper helper) {
        var level = helper.getLevel();
        var failures = new ArrayList<String>();
        var server = settled(level);
        var client = new Probe(level);
        server.initClient(client);

        server.gated = -1;
        if (server.syncTo(client)) failures.add("synced while the condition is false: " + server.syncFieldNames(server.sentChanged));
        server.gated = 4;
        if (!server.syncTo(client) || client.gated != 4) failures.add("not synced once the condition holds, client has " + client.gated);
        server.gated = -2;
        if (server.syncTo(client) || client.gated != 4) failures.add("synced after the condition failed again, client has " + client.gated);
        if (server.persistenceNotices > 0) failures.add("a field that is only synced notified persistence");
        report(helper, failures);
    }

    static void conditionalFieldIsSavedWhileHeldBack(GameTestHelper helper) {
        var level = helper.getLevel();
        var failures = new ArrayList<String>();
        var cases = List.of(
                Case.synced("container", "shownItems", p -> p.shownItems.put(0, new ItemStack(Items.DIAMOND))),
                Case.synced("array of tanks", "shownTanks", p -> p.shownTanks[0].fill(Fluids.WATER, 100)),
                Case.synced("int[]", "shownInts", p -> p.shownInts[1] = 7));
        for (var c : cases) {
            var server = settled(level);
            var client = new Probe(level);
            server.initClient(client);

            c.change.accept(server);
            if (server.syncTo(client)) failures.add(c.name + ": synced while held back: " + server.syncFieldNames(server.sentChanged));
            if (server.persistenceNotices == 0) failures.add(c.name + ": not saved while held back");
            server.persistenceNotices = 0;
            if (server.syncTo(client) || server.persistenceNotices > 0) failures.add(c.name + ": noticed again with nothing changed");

            server.shown = true;
            if (!server.syncTo(client) || !server.sentChanged.get(server.syncIndex(c.field))) {
                failures.add(c.name + ": the change made while held back was not sent once shown");
            } else if (!client.syncedValue(c.field).equals(server.syncedValue(c.field))) {
                failures.add(c.name + ": client has " + client.syncedValue(c.field) + ", server " + server.syncedValue(c.field));
            }
            if (server.persistenceNotices > 0) failures.add(c.name + ": saved again just for being shown");
            if (server.syncTo(client)) failures.add(c.name + ": sent again after being shown");
        }
        report(helper, failures);
    }

    static void lazyFieldWaitsForMarkDirty(GameTestHelper helper) {
        var level = helper.getLevel();
        var failures = new ArrayList<String>();
        var server = settled(level);
        var client = new Probe(level);
        server.initClient(client);

        server.lazy = 3;
        if (server.syncTo(client)) failures.add("synced without markDirty: " + server.syncFieldNames(server.sentChanged));
        server.markDirty("lazy");
        if (!server.syncTo(client) || client.lazy != 3) failures.add("not synced after markDirty, client has " + client.lazy);
        if (server.syncTo(client)) failures.add("synced again after markDirty was consumed");
        report(helper, failures);
    }

    static void lazyFieldIgnoresItsCondition(GameTestHelper helper) {
        var level = helper.getLevel();
        var failures = new ArrayList<String>();
        var server = settled(level);
        var client = new Probe(level);
        server.initClient(client);

        server.shownLazy = 3;
        server.markDirty("shownLazy");
        if (!server.syncTo(client) || client.shownLazy != 3) failures.add("not synced after markDirty, client has " + client.shownLazy);
        if (server.syncTo(client)) failures.add("synced again after markDirty was consumed");
        report(helper, failures);
    }

    static void changesInOnePeriodShareOnePacket(GameTestHelper helper) {
        var level = helper.getLevel();
        var failures = new ArrayList<String>();
        var server = settled(level);
        var client = new Probe(level);
        server.initClient(client);

        server.intValue = 1;
        server.stack.grow(1);
        server.items.put(2, new ItemStack(Items.DIAMOND));
        server.part.progress = 3;
        server.packets = 0;
        server.syncTo(client);
        var expected = List.of("intValue", "stack", "items", "part");
        if (server.packets != 1) failures.add(server.packets + " packets for one period");
        for (var field : expected) {
            if (server.sentChanged == null || !server.sentChanged.get(server.syncIndex(field))) {
                failures.add(field + " missing from " + server.syncFieldNames(server.sentChanged));
            } else if (!client.syncedValue(field).equals(server.syncedValue(field))) {
                failures.add(field + ": client has " + client.syncedValue(field) + ", server " + server.syncedValue(field));
            }
        }
        if (server.sentChanged != null && server.sentChanged.cardinality() != expected.size()) {
            failures.add("sent " + server.syncFieldNames(server.sentChanged) + ", expected " + expected);
        }
        if (server.persistenceNotices == 0) failures.add("persistence was not notified");
        report(helper, failures);
    }

    @SuppressWarnings("unchecked")
    static void codecMarkWorksInAnyBuilderOrder(GameTestHelper helper) {
        var failures = new ArrayList<String>();
        var listType = (Class<List<String>>) (Class<?>) List.class;
        var codecFirst = CustomDirectAccessor.builder(listType)
                .codec(Codec.STRING.listOf()).streamCodec(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).codecMark().build();
        var markFirst = CustomDirectAccessor.builder(listType)
                .codecMark().codec(Codec.STRING.listOf()).streamCodec(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())).build();
        for (var accessor : List.of(codecFirst, markFirst)) {
            var list = new ArrayList<>(List.of("a"));
            var mark = accessor.obtainManagedMark(list);
            if (accessor.updateMark(mark, list) != null) failures.add("an unchanged list got a new mark");
            list.add("b");
            if (accessor.updateMark(mark, list) == null || !accessor.areDifferent(mark, list)) failures.add("adding to the list went unnoticed");
        }
        var overridden = CustomDirectAccessor.builder(listType)
                .codec(Codec.STRING.listOf()).streamCodec(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
                .codecMark().copyMark(ArrayList::new).build();
        if (!(overridden.obtainManagedMark(new ArrayList<>(List.of("a"))) instanceof ArrayList<?>)) {
            failures.add("copyMark after codecMark did not replace it");
        }
        report(helper, failures);
    }

    @SuppressWarnings("unchecked")
    static void subclassedAccessorsKeepTheirOwnComparison(GameTestHelper helper) {
        var failures = new ArrayList<String>();
        var listType = (Class<List<String>>) (Class<?>) List.class;
        var alwaysChanged = new CustomDirectAccessor<>(listType, false, Codec.STRING.listOf(),
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), null) {
            @Override
            public boolean areDifferent(@NotNull Object managedMark, @NotNull List<String> value) {
                return true;
            }
        };
        var list = List.of("a");
        if (alwaysChanged.updateMark(alwaysChanged.obtainManagedMark(list), list) == null) {
            failures.add("the areDifferent of a subclass was passed over");
        }

        var version = new int[1];
        var versioned = new ValueIOSerializableReadOnlyAccessor() {
            @Override
            public <T> T readReadOnlyValue(DynamicOps<T> op, @NotNull ValueIOSerializable value) {
                return op.createInt(version[0]);
            }
        };
        var items = new ItemStacksResourceHandler(1);
        var mark = versioned.obtainManagedMark(items);
        if (versioned.updateMark(mark, items) != null) failures.add("an unchanged value got a new mark");
        version[0] = 1;
        if (versioned.updateMark(mark, items) == null) failures.add("the readReadOnlyValue of a subclass was passed over");
        report(helper, failures);
    }

    static void codecMarkNoticesAValueEncodedAsEmpty(GameTestHelper helper) {
        var failures = new ArrayList<String>();
        var blankIsEmpty = new Codec<String>() {
            @Override
            public <T> DataResult<T> encode(String input, DynamicOps<T> ops, T prefix) {
                return DataResult.success(input.isEmpty() ? ops.empty() : ops.createString(input));
            }

            @Override
            public <T> DataResult<Pair<String, T>> decode(DynamicOps<T> ops, T input) {
                return DataResult.success(Pair.of(ops.getStringValue(input).result().orElse(""), input));
            }
        };
        var accessor = CustomDirectAccessor.builder(String.class).codec(blankIsEmpty).streamCodec(ByteBufCodecs.STRING_UTF8).codecMark().build();
        var mark = accessor.obtainManagedMark("a");
        var emptyMark = accessor.updateMark(mark, "");
        if (emptyMark == null || !accessor.areDifferent(mark, "")) failures.add("a value encoded as empty looked unchanged");
        if (emptyMark != null && accessor.updateMark(emptyMark, "") != null) failures.add("an unchanged empty value got a new mark");
        report(helper, failures);
    }

    public static class NullableElements extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public List<String> strings = new ArrayList<>(Arrays.asList("a", null));
        @Persisted
        @DescSynced
        public List<Integer> numbers = new ArrayList<>(Arrays.asList(1, null));
        @Persisted
        @DescSynced
        public Set<String> names = new HashSet<>(Arrays.asList("x", null));

        public NullableElements(ServerLevel level) {
            super(level);
        }
    }

    static void nullElementsSurviveSavingAndTheFirstSync(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        var failures = new ArrayList<String>();
        var source = new NullableElements(level);

        var tag = save(access, source);
        var loaded = new NullableElements(level);
        loaded.strings.clear();
        loaded.numbers.clear();
        loaded.names.clear();
        try {
            load(access, loaded, tag);
            if (!source.strings.equals(loaded.strings)) failures.add("saved strings " + source.strings + " loaded as " + loaded.strings);
            if (!source.numbers.equals(loaded.numbers)) failures.add("saved numbers " + source.numbers + " loaded as " + loaded.numbers);
            if (!source.names.equals(loaded.names)) failures.add("saved names " + source.names + " loaded as " + loaded.names);
        } catch (Exception e) {
            failures.add("loading failed: " + e);
        }

        var client = new NullableElements(level);
        client.strings.clear();
        client.numbers.clear();
        client.names.clear();
        try {
            source.initClient(client);
            if (!source.strings.equals(client.strings)) failures.add("first sync gave strings " + client.strings);
            if (!source.numbers.equals(client.numbers)) failures.add("first sync gave numbers " + client.numbers);
            if (!source.names.equals(client.names)) failures.add("first sync gave names " + client.names);
        } catch (Exception e) {
            failures.add("first sync failed: " + e);
        }

        var corrupt = save(access, source);
        corrupt.getCompoundOrEmpty("managed").put("numbers",
                NbtOps.INSTANCE.createList(Stream.of(IntTag.valueOf(1), StringTag.valueOf("not a number"), IntTag.valueOf(3))));
        var damaged = new NullableElements(level);
        load(access, damaged, corrupt);
        if (!Arrays.asList(1, null, 3).equals(damaged.numbers)) failures.add("an element that fails to load became " + damaged.numbers);
        report(helper, failures);
    }

    private static CompoundTag save(HolderLookup.Provider access, SyncProbeHolder holder) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, access);
        holder.saveManagedPersistentData(output, false);
        return output.buildResult();
    }

    private static void load(HolderLookup.Provider access, SyncProbeHolder holder, CompoundTag tag) {
        holder.loadManagedPersistentData(TagValueInput.create(ProblemReporter.DISCARDING, access, tag));
    }

    public static class Nesting extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public final Part part = new Part(this::notifyPersistence);

        public Nesting(ServerLevel level) {
            super(level);
        }

        private void pollWithoutSending() {
            for (var field : getRootStorage().getNonLazyFields()) {
                field.update();
            }
        }
    }

    static void unsentNestedChangesNotifyPersistenceOncePerChange(GameTestHelper helper) {
        var level = helper.getLevel();
        var failures = new ArrayList<String>();
        var server = new Nesting(level);
        server.settle();
        var client = new Nesting(level);
        server.initClient(client);

        server.part.progress = 1;
        server.pollWithoutSending();
        long afterChange = server.persistenceNotices;
        if (afterChange == 0) failures.add("a nested change was not persisted");
        for (int i = 0; i < 5; i++) {
            server.pollWithoutSending();
        }
        if (server.persistenceNotices != afterChange) {
            failures.add("an unsent nested change notified persistence again on every look: " + afterChange + " -> " + server.persistenceNotices);
        }

        server.part.progress = 2;
        server.pollWithoutSending();
        if (server.persistenceNotices == afterChange) failures.add("a second change while the first was unsent was not persisted");

        if (!server.syncTo(client) || client.part.progress != 2) failures.add("the client got progress " + client.part.progress + " once it was sent");
        if (server.syncTo(client)) failures.add("sent again with nothing changed: " + server.syncFieldNames(server.sentChanged));
        report(helper, failures);
    }

    private static Probe settled(ServerLevel level) {
        var probe = new Probe(level);
        probe.settle();
        return probe;
    }

    private static void report(GameTestHelper helper, List<String> failures) {
        if (failures.isEmpty()) {
            helper.succeed();
        } else {
            helper.fail(failures.size() + " failed:\n" + String.join("\n", failures));
        }
    }
}
