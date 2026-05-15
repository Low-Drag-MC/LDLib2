package com.lowdragmc.lowdraglib2.test.syncdata;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.lowdraglib2.utils.ByteBufUtil;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@GameTestHolder(LDLib2.MOD_ID)
public class MapSerializationTest {

    public static class NestedReadOnly implements IPersistedSerializable {
        @Persisted public int counter = 0;
        @Persisted public String label = "";

        public NestedReadOnly() {}
        public NestedReadOnly(int counter, String label) {
            this.counter = counter;
            this.label = label;
        }
    }

    public static class MapHolder implements IPersistedSerializable {
        @Persisted public Map<String, Integer> stringInt = new HashMap<>();
        @Persisted public Map<String, ItemStack> stringStack = new HashMap<>();
        @Persisted public Map<Direction, Vector3f> dirVec = new HashMap<>();
        @Persisted public final Map<String, NestedReadOnly> readOnlyValues = new HashMap<>();

        @Persisted
        @ReadOnlyManaged(serializeMethod = "roMapSerialize", deserializeMethod = "roMapDeserialize")
        public final Map<String, NestedReadOnly> roManaged = new HashMap<>();

        // K is read-only (NestedReadOnly is INBTSerializable → INBTSerializableReadOnlyAccessor),
        // V is direct. Uses LinkedHashMap so iteration order is deterministic for the buffer path.
        @Persisted
        @ReadOnlyManaged(serializeMethod = "roKDirectVSerialize", deserializeMethod = "roKDirectVDeserialize")
        public final Map<NestedReadOnly, Integer> roKDirectV = new LinkedHashMap<>();

        // Both K and V are read-only.
        @Persisted
        @ReadOnlyManaged(serializeMethod = "roKRoVSerialize", deserializeMethod = "roKRoVDeserialize")
        public final Map<NestedReadOnly, NestedReadOnly> roKRoV = new LinkedHashMap<>();

        public CompoundTag roMapSerialize(Map<String, NestedReadOnly> m) {
            // Wrap in CompoundTag because RegistryFriendlyByteBuf.readNbt() strictly requires
            // CompoundTag on the buffer path; ListTag works for NBT but fails over the wire.
            var keys = new ListTag();
            m.keySet().stream().sorted().forEach(k -> keys.add(StringTag.valueOf(k)));
            var c = new CompoundTag();
            c.put("keys", keys);
            return c;
        }

        public Map<String, NestedReadOnly> roMapDeserialize(CompoundTag c) {
            var m = new HashMap<String, NestedReadOnly>();
            var keys = c.getList("keys", Tag.TAG_STRING);
            for (Tag e : keys) m.put(e.getAsString(), new NestedReadOnly());
            return m;
        }

        // For readOnlyValues (non-@ReadOnlyManaged), we must pre-populate the SAME keys on
        // both sides before deserialize, since the framework can't fabricate read-only V instances.
        public void prepareReadOnlyValuesKeys(String... keys) {
            for (String k : keys) readOnlyValues.put(k, new NestedReadOnly());
        }

        private static final Comparator<NestedReadOnly> NRO_BY_LABEL_COUNTER =
                Comparator.<NestedReadOnly>comparingInt(n -> n.counter).thenComparing(n -> n.label);

        public CompoundTag roKDirectVSerialize(Map<NestedReadOnly, Integer> m) {
            var keys = new ListTag();
            m.keySet().stream().sorted(NRO_BY_LABEL_COUNTER).forEach(k -> {
                var c = new CompoundTag();
                c.putInt("counter", k.counter);
                c.putString("label", k.label);
                keys.add(c);
            });
            var out = new CompoundTag();
            out.put("keys", keys);
            return out;
        }

        public Map<NestedReadOnly, Integer> roKDirectVDeserialize(CompoundTag tag) {
            var m = new LinkedHashMap<NestedReadOnly, Integer>();
            var keys = tag.getList("keys", Tag.TAG_COMPOUND);
            for (Tag t : keys) {
                var c = (CompoundTag) t;
                m.put(new NestedReadOnly(c.getInt("counter"), c.getString("label")), 0);
            }
            return m;
        }

        public CompoundTag roKRoVSerialize(Map<NestedReadOnly, NestedReadOnly> m) {
            var keys = new ListTag();
            m.keySet().stream().sorted(NRO_BY_LABEL_COUNTER).forEach(k -> {
                var c = new CompoundTag();
                c.putInt("counter", k.counter);
                c.putString("label", k.label);
                keys.add(c);
            });
            var out = new CompoundTag();
            out.put("keys", keys);
            return out;
        }

        public Map<NestedReadOnly, NestedReadOnly> roKRoVDeserialize(CompoundTag tag) {
            var m = new LinkedHashMap<NestedReadOnly, NestedReadOnly>();
            var keys = tag.getList("keys", Tag.TAG_COMPOUND);
            for (Tag t : keys) {
                m.put(new NestedReadOnly(((CompoundTag) t).getInt("counter"), ((CompoundTag) t).getString("label")),
                      new NestedReadOnly());
            }
            return m;
        }
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void directKDirectV_nbtRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = new MapHolder();
        src.stringInt.put("a", 1);
        src.stringInt.put("b", 2);
        src.stringInt.put("c", -42);

        var nbt = src.serializeNBT(provider);

        var dst = new MapHolder();
        dst.deserializeNBT(provider, nbt);

        assertEq(helper, "size", 3, dst.stringInt.size());
        assertEq(helper, "a", 1, dst.stringInt.get("a"));
        assertEq(helper, "b", 2, dst.stringInt.get("b"));
        assertEq(helper, "c", -42, dst.stringInt.get("c"));
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void directKCustomV_nbtRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = new MapHolder();
        src.stringStack.put("diamond", new ItemStack(Items.DIAMOND, 7));
        src.stringStack.put("apple", new ItemStack(Items.APPLE, 64));

        var nbt = src.serializeNBT(provider);
        var dst = new MapHolder();
        dst.deserializeNBT(provider, nbt);

        assertEq(helper, "size", 2, dst.stringStack.size());
        if (!ItemStack.matches(src.stringStack.get("diamond"), dst.stringStack.get("diamond"))) {
            helper.fail("diamond stack mismatch");
            return;
        }
        if (!ItemStack.matches(src.stringStack.get("apple"), dst.stringStack.get("apple"))) {
            helper.fail("apple stack mismatch");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void enumKVectorV_nbtRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = new MapHolder();
        for (var d : Direction.values()) {
            src.dirVec.put(d, new Vector3f(d.getStepX(), d.getStepY(), d.getStepZ()));
        }

        var nbt = src.serializeNBT(provider);
        var dst = new MapHolder();
        dst.deserializeNBT(provider, nbt);

        assertEq(helper, "size", 6, dst.dirVec.size());
        for (var d : Direction.values()) {
            var v = dst.dirVec.get(d);
            assertNotNull(helper, "dirVec[" + d + "]", v);
            if (!new Vector3f(d.getStepX(), d.getStepY(), d.getStepZ()).equals(v)) {
                helper.fail("dirVec[" + d + "] expected " + d.getStepX() + "," + d.getStepY() + "," + d.getStepZ()
                        + " got " + v);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void directKReadOnlyV_nbtRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = new MapHolder();
        src.readOnlyValues.put("k1", new NestedReadOnly(11, "one"));
        src.readOnlyValues.put("k2", new NestedReadOnly(22, "two"));

        var nbt = src.serializeNBT(provider);

        // Pre-populate dst with matching keys (read-only V can't be fabricated by the framework)
        var dst = new MapHolder();
        dst.prepareReadOnlyValuesKeys("k1", "k2");
        var k1Original = dst.readOnlyValues.get("k1");
        var k2Original = dst.readOnlyValues.get("k2");

        dst.deserializeNBT(provider, nbt);

        assertEq(helper, "size", 2, dst.readOnlyValues.size());
        assertEq(helper, "k1.counter", 11, dst.readOnlyValues.get("k1").counter);
        assertEq(helper, "k1.label", "one", dst.readOnlyValues.get("k1").label);
        assertEq(helper, "k2.counter", 22, dst.readOnlyValues.get("k2").counter);
        assertEq(helper, "k2.label", "two", dst.readOnlyValues.get("k2").label);

        // Instance identity preserved (in-place mutation of read-only V)
        if (dst.readOnlyValues.get("k1") != k1Original) {
            helper.fail("k1 instance was replaced; expected in-place update of read-only V");
            return;
        }
        if (dst.readOnlyValues.get("k2") != k2Original) {
            helper.fail("k2 instance was replaced; expected in-place update of read-only V");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void readOnlyManagedMap_nbtRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = new MapHolder();
        src.roManaged.put("alpha", new NestedReadOnly(1, "A"));
        src.roManaged.put("beta", new NestedReadOnly(2, "B"));
        src.roManaged.put("gamma", new NestedReadOnly(3, "G"));

        var nbt = src.serializeNBT(provider);

        // dst has DIFFERENT initial keys — @ReadOnlyManaged should rebuild the whole map.
        var dst = new MapHolder();
        dst.roManaged.put("old1", new NestedReadOnly(99, "X"));
        dst.roManaged.put("old2", new NestedReadOnly(100, "Y"));

        dst.deserializeNBT(provider, nbt);

        assertEq(helper, "size", 3, dst.roManaged.size());
        assertNotNull(helper, "alpha", dst.roManaged.get("alpha"));
        assertNotNull(helper, "beta", dst.roManaged.get("beta"));
        assertNotNull(helper, "gamma", dst.roManaged.get("gamma"));
        if (dst.roManaged.containsKey("old1") || dst.roManaged.containsKey("old2")) {
            helper.fail("old keys not removed by @ReadOnlyManaged rebuild");
            return;
        }
        assertEq(helper, "alpha.counter", 1, dst.roManaged.get("alpha").counter);
        assertEq(helper, "alpha.label", "A", dst.roManaged.get("alpha").label);
        assertEq(helper, "beta.counter", 2, dst.roManaged.get("beta").counter);
        assertEq(helper, "gamma.counter", 3, dst.roManaged.get("gamma").counter);
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void bufferRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = new MapHolder();
        src.stringInt.put("a", 1);
        src.stringInt.put("b", 2);
        src.stringStack.put("d", new ItemStack(Items.DIAMOND, 3));
        src.dirVec.put(Direction.NORTH, new Vector3f(0, 0, -1));

        byte[] bytes = ByteBufUtil.writeCustomData(src::writeToBuff, provider);

        var dst = new MapHolder();
        ByteBufUtil.readCustomData(bytes, dst::readFromBuff, provider);

        assertEq(helper, "stringInt.size", 2, dst.stringInt.size());
        assertEq(helper, "stringInt.a", 1, dst.stringInt.get("a"));
        assertEq(helper, "stringInt.b", 2, dst.stringInt.get("b"));
        assertEq(helper, "stringStack.size", 1, dst.stringStack.size());
        if (!ItemStack.matches(src.stringStack.get("d"), dst.stringStack.get("d"))) {
            helper.fail("stringStack[d] mismatch over buffer");
            return;
        }
        assertEq(helper, "dirVec.size", 1, dst.dirVec.size());
        if (!new Vector3f(0, 0, -1).equals(dst.dirVec.get(Direction.NORTH))) {
            helper.fail("dirVec[NORTH] mismatch over buffer");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void emptyMap_nbtRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = new MapHolder();
        // all maps left empty
        var nbt = src.serializeNBT(provider);

        var dst = new MapHolder();
        // Pre-fill dst with garbage in direct-V maps to ensure deserialize clears them.
        dst.stringInt.put("garbage", 999);
        dst.deserializeNBT(provider, nbt);

        assertEq(helper, "stringInt.size", 0, dst.stringInt.size());
        assertEq(helper, "stringStack.size", 0, dst.stringStack.size());
        assertEq(helper, "dirVec.size", 0, dst.dirVec.size());
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void wireFormatShape_nbt(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = new MapHolder();
        src.stringInt.put("a", 1);
        src.stringInt.put("b", 2);

        CompoundTag nbt = src.serializeNBT(provider);

        // Field name is "stringInt"; the wire format is a flat list of [k0, v0, k1, v1, ...]
        if (!nbt.contains("stringInt")) {
            helper.fail("missing stringInt field in serialized NBT: " + nbt);
            return;
        }
        var listTag = nbt.get("stringInt");
        assertNotNull(helper, "stringInt list tag", listTag);
        if (!(listTag instanceof ListTag list)) {
            helper.fail("stringInt should be a ListTag, got " + listTag.getClass().getSimpleName() + ": " + nbt);
            return;
        }
        assertEq(helper, "stringInt list length (2 entries * 2 = 4)", 4, list.size());
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void readOnlyKDirectV_nbtRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = new MapHolder();
        src.roKDirectV.put(new NestedReadOnly(1, "alpha"), 100);
        src.roKDirectV.put(new NestedReadOnly(2, "beta"), 200);
        src.roKDirectV.put(new NestedReadOnly(3, "gamma"), -7);

        var nbt = src.serializeNBT(provider);

        // dst starts with DIFFERENT keys; @ReadOnlyManaged should rebuild structure.
        var dst = new MapHolder();
        dst.roKDirectV.put(new NestedReadOnly(99, "old"), 0);

        dst.deserializeNBT(provider, nbt);

        assertEq(helper, "size", 3, dst.roKDirectV.size());
        // Find by content (read-only K instances are different references, but should have matching state).
        Integer alphaV = null, betaV = null, gammaV = null;
        for (var e : dst.roKDirectV.entrySet()) {
            if (e.getKey().counter == 1 && "alpha".equals(e.getKey().label)) alphaV = e.getValue();
            else if (e.getKey().counter == 2 && "beta".equals(e.getKey().label)) betaV = e.getValue();
            else if (e.getKey().counter == 3 && "gamma".equals(e.getKey().label)) gammaV = e.getValue();
        }
        assertNotNull(helper, "alpha entry", alphaV);
        assertNotNull(helper, "beta entry", betaV);
        assertNotNull(helper, "gamma entry", gammaV);
        assertEq(helper, "alpha V", 100, alphaV);
        assertEq(helper, "beta V", 200, betaV);
        assertEq(helper, "gamma V", -7, gammaV);
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void readOnlyKReadOnlyV_nbtRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = new MapHolder();
        src.roKRoV.put(new NestedReadOnly(1, "alpha"), new NestedReadOnly(11, "vA"));
        src.roKRoV.put(new NestedReadOnly(2, "beta"), new NestedReadOnly(22, "vB"));

        var nbt = src.serializeNBT(provider);

        var dst = new MapHolder();
        dst.deserializeNBT(provider, nbt);

        assertEq(helper, "size", 2, dst.roKRoV.size());
        NestedReadOnly alphaV = null, betaV = null;
        for (var e : dst.roKRoV.entrySet()) {
            if (e.getKey().counter == 1 && "alpha".equals(e.getKey().label)) alphaV = e.getValue();
            else if (e.getKey().counter == 2 && "beta".equals(e.getKey().label)) betaV = e.getValue();
        }
        assertNotNull(helper, "alpha entry", alphaV);
        assertNotNull(helper, "beta entry", betaV);
        assertEq(helper, "alpha V counter", 11, alphaV.counter);
        assertEq(helper, "alpha V label", "vA", alphaV.label);
        assertEq(helper, "beta V counter", 22, betaV.counter);
        assertEq(helper, "beta V label", "vB", betaV.label);
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void readOnlyKDirectV_bufRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = new MapHolder();
        // Insert in sorted order so that source/target iteration order matches via
        // @ReadOnlyManaged's deserializeUid (which sorts).
        src.roKDirectV.put(new NestedReadOnly(1, "alpha"), 100);
        src.roKDirectV.put(new NestedReadOnly(2, "beta"), 200);

        byte[] bytes = ByteBufUtil.writeCustomData(src::writeToBuff, provider);

        var dst = new MapHolder();
        ByteBufUtil.readCustomData(bytes, dst::readFromBuff, provider);

        assertEq(helper, "size", 2, dst.roKDirectV.size());
        Integer alphaV = null, betaV = null;
        for (var e : dst.roKDirectV.entrySet()) {
            if (e.getKey().counter == 1 && "alpha".equals(e.getKey().label)) alphaV = e.getValue();
            else if (e.getKey().counter == 2 && "beta".equals(e.getKey().label)) betaV = e.getValue();
        }
        assertNotNull(helper, "alpha entry", alphaV);
        assertNotNull(helper, "beta entry", betaV);
        assertEq(helper, "alpha V", 100, alphaV);
        assertEq(helper, "beta V", 200, betaV);
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void readOnlyKReadOnlyV_bufRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = new MapHolder();
        src.roKRoV.put(new NestedReadOnly(1, "alpha"), new NestedReadOnly(11, "vA"));
        src.roKRoV.put(new NestedReadOnly(2, "beta"), new NestedReadOnly(22, "vB"));

        byte[] bytes = ByteBufUtil.writeCustomData(src::writeToBuff, provider);

        var dst = new MapHolder();
        ByteBufUtil.readCustomData(bytes, dst::readFromBuff, provider);

        assertEq(helper, "size", 2, dst.roKRoV.size());
        NestedReadOnly alphaV = null, betaV = null;
        for (var e : dst.roKRoV.entrySet()) {
            if (e.getKey().counter == 1 && "alpha".equals(e.getKey().label)) alphaV = e.getValue();
            else if (e.getKey().counter == 2 && "beta".equals(e.getKey().label)) betaV = e.getValue();
        }
        assertNotNull(helper, "alpha entry", alphaV);
        assertNotNull(helper, "beta entry", betaV);
        assertEq(helper, "alpha V counter", 11, alphaV.counter);
        assertEq(helper, "alpha V label", "vA", alphaV.label);
        assertEq(helper, "beta V counter", 22, betaV.counter);
        assertEq(helper, "beta V label", "vB", betaV.label);
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void directKReadOnlyV_bufRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = new MapHolder();
        src.readOnlyValues.put("k1", new NestedReadOnly(11, "one"));
        src.readOnlyValues.put("k2", new NestedReadOnly(22, "two"));

        byte[] bytes = ByteBufUtil.writeCustomData(src::writeToBuff, provider);

        var dst = new MapHolder();
        dst.prepareReadOnlyValuesKeys("k1", "k2");
        ByteBufUtil.readCustomData(bytes, dst::readFromBuff, provider);

        assertEq(helper, "size", 2, dst.readOnlyValues.size());
        assertEq(helper, "k1.counter", 11, dst.readOnlyValues.get("k1").counter);
        assertEq(helper, "k1.label", "one", dst.readOnlyValues.get("k1").label);
        assertEq(helper, "k2.counter", 22, dst.readOnlyValues.get("k2").counter);
        assertEq(helper, "k2.label", "two", dst.readOnlyValues.get("k2").label);
        helper.succeed();
    }

    // --- helpers (mirrors GraphHierarchySerializationTest) ---

    private static void assertNotNull(GameTestHelper helper, String label, Object value) {
        if (value == null) helper.fail(label + " is null");
    }

    private static void assertEq(GameTestHelper helper, String label, int expected, int actual) {
        if (expected != actual) helper.fail(label + ": expected " + expected + ", got " + actual);
    }

    private static void assertEq(GameTestHelper helper, String label, String expected, String actual) {
        if (!Objects.equals(expected, actual)) helper.fail(label + ": expected '" + expected + "', got '" + actual + "'");
    }
}
