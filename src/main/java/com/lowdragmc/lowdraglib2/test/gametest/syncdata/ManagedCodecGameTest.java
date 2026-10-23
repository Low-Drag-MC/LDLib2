package com.lowdragmc.lowdraglib2.test.gametest.syncdata;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.ManagedFieldUtils;
import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ManagedCodec;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.IPersistManagedHolder;
import com.lowdragmc.lowdraglib2.syncdata.holder.ISyncMangedHolder;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import com.lowdragmc.lowdraglib2.utils.ByteBufUtil;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;

public final class ManagedCodecGameTest {
    private static final String CODEC_SHAPES_SAVED_VALUE_PATH = "managed_codec_codec_shapes_saved_value";
    private static final String STORAGE_SYNCS_THROUGH_CODECS_PATH = "managed_codec_storage_syncs_through_codecs";
    private static final String STORAGE_PERSISTS_THROUGH_CODEC_PATH = "managed_codec_storage_persists_through_codec";
    private static final String SUBCLASS_OVERRIDES_STREAM_CODEC_PATH = "managed_codec_subclass_overrides_stream_codec";
    private static final String STREAM_CODEC_SYNCS_VALUE_PATH = "managed_codec_stream_codec_syncs_value";
    private static final String SUBCLASS_OVERRIDES_CODEC_PATH = "managed_codec_subclass_overrides_codec";
    private static final String CODEC_FIELD_NOTICES_IN_PLACE_CHANGE_PATH = "managed_codec_codec_field_notices_in_place_change";
    private static final String BAD_REFERENCE_FAILS_LOUDLY_PATH = "managed_codec_bad_reference_fails_loudly";
    private static final String CODEC_METHOD_MAY_RESOLVE_ANOTHER_CODEC_PATH = "managed_codec_codec_method_may_resolve_another_codec";

    private ManagedCodecGameTest() {
    }

    static void registerFunctions() {
        SyncDataGameTests.registerFunction(CODEC_SHAPES_SAVED_VALUE_PATH, ManagedCodecGameTest::codecShapesSavedValue);
        SyncDataGameTests.registerFunction(STORAGE_SYNCS_THROUGH_CODECS_PATH, ManagedCodecGameTest::storageSyncsThroughCodecs);
        SyncDataGameTests.registerFunction(STORAGE_PERSISTS_THROUGH_CODEC_PATH, ManagedCodecGameTest::storagePersistsThroughCodec);
        SyncDataGameTests.registerFunction(SUBCLASS_OVERRIDES_STREAM_CODEC_PATH, ManagedCodecGameTest::subclassOverridesStreamCodec);
        SyncDataGameTests.registerFunction(STREAM_CODEC_SYNCS_VALUE_PATH, ManagedCodecGameTest::streamCodecSyncsValue);
        SyncDataGameTests.registerFunction(SUBCLASS_OVERRIDES_CODEC_PATH, ManagedCodecGameTest::subclassOverridesCodec);
        SyncDataGameTests.registerFunction(CODEC_FIELD_NOTICES_IN_PLACE_CHANGE_PATH, ManagedCodecGameTest::codecFieldNoticesInPlaceChange);
        SyncDataGameTests.registerFunction(BAD_REFERENCE_FAILS_LOUDLY_PATH, ManagedCodecGameTest::badReferenceFailsLoudly);
        SyncDataGameTests.registerFunction(CODEC_METHOD_MAY_RESOLVE_ANOTHER_CODEC_PATH, ManagedCodecGameTest::codecMethodMayResolveAnotherCodec);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = SyncDataGameTests.defaultTestData(environment, "empty");
        SyncDataGameTests.registerFunctionTest(event, CODEC_SHAPES_SAVED_VALUE_PATH, SyncDataGameTests.functionKey(CODEC_SHAPES_SAVED_VALUE_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, STORAGE_SYNCS_THROUGH_CODECS_PATH, SyncDataGameTests.functionKey(STORAGE_SYNCS_THROUGH_CODECS_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, STORAGE_PERSISTS_THROUGH_CODEC_PATH, SyncDataGameTests.functionKey(STORAGE_PERSISTS_THROUGH_CODEC_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, SUBCLASS_OVERRIDES_STREAM_CODEC_PATH, SyncDataGameTests.functionKey(SUBCLASS_OVERRIDES_STREAM_CODEC_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, STREAM_CODEC_SYNCS_VALUE_PATH, SyncDataGameTests.functionKey(STREAM_CODEC_SYNCS_VALUE_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, SUBCLASS_OVERRIDES_CODEC_PATH, SyncDataGameTests.functionKey(SUBCLASS_OVERRIDES_CODEC_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, CODEC_FIELD_NOTICES_IN_PLACE_CHANGE_PATH, SyncDataGameTests.functionKey(CODEC_FIELD_NOTICES_IN_PLACE_CHANGE_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, BAD_REFERENCE_FAILS_LOUDLY_PATH, SyncDataGameTests.functionKey(BAD_REFERENCE_FAILS_LOUDLY_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, CODEC_METHOD_MAY_RESOLVE_ANOTHER_CODEC_PATH, SyncDataGameTests.functionKey(CODEC_METHOD_MAY_RESOLVE_ANOTHER_CODEC_PATH), testData);
    }

    public static final class Codecs {
        static Codec<String> name() {
            return Codec.STRING;
        }

        static StreamCodec<ByteBuf, String> shoutStream() {
            return ByteBufCodecs.STRING_UTF8.map(s -> s.toUpperCase(Locale.ROOT), s -> s);
        }

        static IAccessor<Integer> intAsString() {
            return CustomDirectAccessor.builder(Integer.class)
                    .codec(Codec.STRING.xmap(Integer::parseInt, String::valueOf))
                    .streamCodec(ByteBufCodecs.VAR_INT)
                    .build();
        }

        Codec<Integer> instanceCodec() {
            return Codec.INT;
        }
    }

    public static class CodecHolder implements IPersistedSerializable {
        @Persisted
        @ManagedCodec("posCodec")
        public BlockPos pos = BlockPos.ZERO;
        @Persisted
        public BlockPos plainPos = BlockPos.ZERO;
        @Persisted
        @ManagedCodec(owner = Codecs.class, value = "name", streamCodec = "shoutStream")
        public String name = "";
        @Persisted
        @ManagedCodec("tagsCodec")
        public List<String> tags = new ArrayList<>();
        @Persisted
        @ManagedCodec(owner = Codecs.class, value = "intAsString")
        public int count;
        @Configurable
        @ManagedCodec("posCodec")
        public BlockPos configured = BlockPos.ZERO;

        protected Codec<BlockPos> posCodec() {
            return Codec.LONG.xmap(BlockPos::of, BlockPos::asLong);
        }

        private static Codec<List<String>> tagsCodec() {
            return Codec.STRING.listOf().xmap(ArrayList::new, list -> list);
        }
    }

    public static class SubCodecHolder extends CodecHolder {
        @Override
        protected Codec<BlockPos> posCodec() {
            return BlockPos.CODEC;
        }
    }

    public static class BrokenHolder {
        @Persisted
        @ManagedCodec("missing")
        public int missing;
        @Persisted
        @ManagedCodec("needsArgument")
        public int needsArgument;
        @Persisted
        @ManagedCodec("notACodec")
        public int wrongKind;
        @Persisted
        @ManagedCodec(owner = Codecs.class, value = "intAsString", streamCodec = "shoutStream")
        public int accessorWithStream;
        @Persisted
        @ManagedCodec(owner = Codecs.class, value = "instanceCodec")
        public int ownerInstanceMethod;
        @Persisted
        @ManagedCodec("throwing")
        public int throwing;
        @Persisted
        @ManagedCodec("returnsNull")
        public int returnsNull;

        Codec<Integer> needsArgument(int unused) {
            return Codec.INT;
        }

        String notACodec() {
            return "";
        }

        Codec<Integer> throwing() {
            throw new IllegalStateException("codec unavailable");
        }

        @Nullable
        Codec<Integer> returnsNull() {
            return null;
        }
    }

    public static class InnerHolder {
        @Persisted
        @ManagedCodec("innerCodec")
        public int value;

        private static Codec<Integer> innerCodec() {
            return Codec.INT;
        }
    }

    public static class ReentrantHolder {
        @Persisted
        @ManagedCodec("outerCodec")
        public int value;

        private static Codec<Integer> outerCodec() {
            ManagedFieldUtils.createKey(field(InnerHolder.class, "value")).createRef(new InnerHolder());
            return Codec.INT;
        }
    }

    private static <T extends CodecHolder> T filled(T holder) {
        holder.pos = new BlockPos(1, -2, 3);
        holder.plainPos = new BlockPos(1, -2, 3);
        holder.name = "steve";
        holder.tags.addAll(List.of("a", "b"));
        holder.count = 42;
        holder.configured = new BlockPos(4, 5, 6);
        return holder;
    }

    public static class SyncedHolder implements IManaged, ISyncMangedHolder, IPersistManagedHolder {
        // sync() hands the captured packet to whoever tracks this chunk, and nobody can
        private static final ChunkPos BEYOND_WORLD_BORDER = new ChunkPos(2_000_000, 2_000_000);

        private final FieldManagedStorage storage = new FieldManagedStorage(this);
        private final ServerLevel level;
        @Nullable
        private BitSet sentChanged;
        private byte[] sentData;

        @Persisted
        @DescSynced
        @ManagedCodec(value = "posCodec", streamCodec = "posStream")
        public BlockPos pos = BlockPos.ZERO;
        @DescSynced
        @ManagedCodec("tagsCodec")
        public List<String> tags = new ArrayList<>();
        @Persisted
        @DescSynced
        @ManagedCodec("posCodec")
        @Nullable
        public BlockPos maybePos = null;

        public SyncedHolder(ServerLevel level) {
            this.level = level;
        }

        protected Codec<BlockPos> posCodec() {
            return Codec.LONG.xmap(BlockPos::of, BlockPos::asLong);
        }

        protected StreamCodec<ByteBuf, BlockPos> posStream() {
            return ByteBufCodecs.VAR_LONG.map(BlockPos::of, BlockPos::asLong);
        }

        private static Codec<List<String>> tagsCodec() {
            return Codec.STRING.listOf().xmap(ArrayList::new, list -> list);
        }

        @Override
        public IManagedStorage getSyncStorage() {
            return storage;
        }

        @Override
        public IManagedStorage getRootStorage() {
            return storage;
        }

        @Override
        public ServerLevel getServerLevel() {
            return level;
        }

        @Override
        public ChunkPos getTrackingPos() {
            return BEYOND_WORLD_BORDER;
        }

        @Override
        public void notifyPersistence() {
        }

        @Override
        public CustomPacketPayload createSyncPacket(BitSet changed, byte[] data, CompoundTag extra) {
            sentChanged = changed;
            sentData = data;
            return new CapturedSync();
        }

        boolean syncTo(SyncedHolder client) {
            sentData = null;
            sync(false);
            if (sentData == null) return false;
            client.handleSyncPacket(level.registryAccess(), sentChanged, sentData, new CompoundTag());
            return true;
        }
    }

    public static class ShiftedSyncedHolder extends SyncedHolder {
        public ShiftedSyncedHolder(ServerLevel level) {
            super(level);
        }

        @Override
        protected StreamCodec<ByteBuf, BlockPos> posStream() {
            return ByteBufCodecs.VAR_LONG.map(value -> BlockPos.of(value).above(), BlockPos::asLong);
        }
    }

    private record CapturedSync() implements CustomPacketPayload {
        private static final Type<CapturedSync> TYPE = new Type<>(LDLib2.id("managed_codec_test"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void codecShapesSavedValue(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = filled(new CodecHolder());
        var nbt = save(src, provider);

        check(helper, nbt.get("pos") instanceof LongTag(var pos) && pos == src.pos.asLong(), "pos saved as long: " + nbt.get("pos"));
        check(helper, nbt.get("plainPos") instanceof IntArrayTag, "plainPos keeps its accessor's format: " + nbt.get("plainPos"));
        check(helper, nbt.get("count") instanceof StringTag(var count) && count.equals("42"), "count saved by its accessor: " + nbt.get("count"));
        check(helper, nbt.get("configured") instanceof LongTag, "@Configurable field saved with its codec: " + nbt.get("configured"));

        var dst = new CodecHolder();
        load(dst, provider, nbt);
        check(helper, src.pos.equals(dst.pos), "pos read back: " + dst.pos);
        check(helper, src.plainPos.equals(dst.plainPos), "plainPos read back: " + dst.plainPos);
        check(helper, dst.name.equals("steve"), "name read back with the codec, not the stream codec: " + dst.name);
        check(helper, dst.tags.equals(List.of("a", "b")), "tags read back: " + dst.tags);
        check(helper, dst.count == 42, "count read back: " + dst.count);
        check(helper, src.configured.equals(dst.configured), "configured read back: " + dst.configured);
        helper.succeed();
    }

    private static void storageSyncsThroughCodecs(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        var server = new SyncedHolder(level);
        server.pos = new BlockPos(5, 6, 7);
        server.tags.add("a");

        var client = new SyncedHolder(level);
        client.deserializeInitialData(access, server.serializeInitialData(access));
        check(helper, client.pos.equals(server.pos), "initial pos: " + client.pos);
        check(helper, client.tags.equals(List.of("a")), "initial tags: " + client.tags);
        check(helper, client.maybePos == null, "initial null pos: " + client.maybePos);

        server.pos = new BlockPos(-1, 2, -3);
        server.tags.add("b");
        server.maybePos = new BlockPos(9, 9, 9);
        check(helper, server.syncTo(client), "changes were not synced");
        check(helper, server.sentChanged != null && server.sentChanged.cardinality() == 3, "changed fields: " + server.sentChanged);
        check(helper, client.pos.equals(server.pos), "synced pos: " + client.pos);
        check(helper, client.tags.equals(List.of("a", "b")), "synced tags: " + client.tags);
        check(helper, server.maybePos.equals(client.maybePos), "synced maybePos: " + client.maybePos);

        server.tags.add("c");
        check(helper, server.syncTo(client), "adding to the list in place was not synced");
        check(helper, server.sentChanged != null && server.sentChanged.cardinality() == 1, "only tags changed: " + server.sentChanged);
        check(helper, client.tags.equals(List.of("a", "b", "c")), "synced in-place tags: " + client.tags);

        server.maybePos = null;
        check(helper, server.syncTo(client), "nulling a field was not synced");
        check(helper, client.maybePos == null, "maybePos synced back to null: " + client.maybePos);
        check(helper, !server.syncTo(client), "unchanged fields were synced again");
        helper.succeed();
    }

    private static void storagePersistsThroughCodec(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        var src = new SyncedHolder(level);
        src.pos = new BlockPos(5, 6, 7);
        src.tags.add("a");
        src.maybePos = new BlockPos(1, 1, 1);

        var tag = savePersistent(src, access);
        var managed = tag.getCompoundOrEmpty("managed");
        check(helper, managed.get("pos") instanceof LongTag, "pos saved with its codec: " + managed.get("pos"));
        check(helper, !managed.contains("tags"), "a @DescSynced-only field was saved");

        var dst = new SyncedHolder(level);
        loadPersistent(dst, access, tag);
        check(helper, src.pos.equals(dst.pos), "pos loaded: " + dst.pos);
        check(helper, src.maybePos.equals(dst.maybePos), "maybePos loaded: " + dst.maybePos);
        check(helper, dst.tags.isEmpty(), "tags loaded though never saved: " + dst.tags);

        src.maybePos = null;
        tag = savePersistent(src, access);
        loadPersistent(dst, access, tag);
        check(helper, dst.maybePos == null, "null maybePos loaded: " + dst.maybePos);
        helper.succeed();
    }

    private static void subclassOverridesStreamCodec(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        var pos = new BlockPos(5, 6, 7);

        var shiftedServer = new ShiftedSyncedHolder(level);
        shiftedServer.pos = pos;
        var shiftedClient = new ShiftedSyncedHolder(level);
        shiftedClient.deserializeInitialData(access, shiftedServer.serializeInitialData(access));
        check(helper, shiftedClient.pos.equals(pos), "initial sync goes through the codec, not the stream codec: " + shiftedClient.pos);
        shiftedServer.pos = pos.east();
        check(helper, shiftedServer.syncTo(shiftedClient), "subclass change was not synced");
        check(helper, shiftedClient.pos.equals(pos.east().above()), "subclass syncs with its own stream codec: " + shiftedClient.pos);

        var server = new SyncedHolder(level);
        var client = new SyncedHolder(level);
        client.deserializeInitialData(access, server.serializeInitialData(access));
        server.pos = pos.east();
        check(helper, server.syncTo(client), "base class change was not synced");
        check(helper, client.pos.equals(pos.east()), "base class picked up the subclass's stream codec: " + client.pos);
        helper.succeed();
    }

    private static void streamCodecSyncsValue(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var src = filled(new CodecHolder());
        byte[] bytes = ByteBufUtil.writeCustomData(src::writeToBuff, provider);

        var dst = new CodecHolder();
        ByteBufUtil.readCustomData(bytes, dst::readFromBuff, provider);
        check(helper, src.pos.equals(dst.pos), "pos synced through its codec: " + dst.pos);
        check(helper, dst.name.equals("STEVE"), "name synced through its stream codec: " + dst.name);
        check(helper, dst.tags.equals(List.of("a", "b")), "tags synced: " + dst.tags);
        check(helper, dst.count == 42, "count synced: " + dst.count);
        helper.succeed();
    }

    private static void subclassOverridesCodec(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var base = filled(new CodecHolder());
        var sub = filled(new SubCodecHolder());

        check(helper, save(base, provider).get("pos") instanceof LongTag, "base class saves pos with its own codec");
        var subNbt = save(sub, provider);
        check(helper, subNbt.get("pos") instanceof IntArrayTag, "subclass saves pos with its override: " + subNbt.get("pos"));
        check(helper, save(base, provider).get("pos") instanceof LongTag, "base class picked up the subclass's codec");

        var dst = new SubCodecHolder();
        load(dst, provider, subNbt);
        check(helper, sub.pos.equals(dst.pos), "subclass pos read back: " + dst.pos);
        helper.succeed();
    }

    private static void codecFieldNoticesInPlaceChange(GameTestHelper helper) {
        var holder = new CodecHolder();
        var ref = ManagedFieldUtils.createKey(field(CodecHolder.class, "tags")).createRef(holder);

        ref.update();
        check(helper, !ref.isPersistedDirty(), "unchanged list marked dirty");
        holder.tags.add("c");
        ref.update();
        check(helper, ref.isPersistedDirty(), "adding to the list was not noticed");
        helper.succeed();
    }

    private static void badReferenceFailsLoudly(GameTestHelper helper) {
        var holder = new BrokenHolder();
        for (var name : List.of("missing", "needsArgument", "wrongKind", "accessorWithStream", "ownerInstanceMethod", "throwing", "returnsNull")) {
            try {
                ManagedFieldUtils.createKey(field(BrokenHolder.class, name)).createRef(holder);
                helper.fail(name + " resolved an accessor");
            } catch (IllegalArgumentException e) {
                check(helper, e.getMessage().contains("@ManagedCodec"), name + " failed with: " + e.getMessage());
                if (name.equals("throwing")) {
                    check(helper, e.getCause() instanceof IllegalStateException cause && cause.getMessage().equals("codec unavailable"),
                            "the method's own exception is not the cause: " + e.getCause());
                }
            }
        }
        helper.succeed();
    }

    private static void codecMethodMayResolveAnotherCodec(GameTestHelper helper) {
        ManagedFieldUtils.createKey(field(ReentrantHolder.class, "value")).createRef(new ReentrantHolder());
        helper.succeed();
    }

    private static CompoundTag save(IPersistedSerializable holder, HolderLookup.Provider provider) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        holder.serialize(output);
        return output.buildResult();
    }

    private static void load(IPersistedSerializable holder, HolderLookup.Provider provider, CompoundTag tag) {
        holder.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));
    }

    private static CompoundTag savePersistent(IPersistManagedHolder holder, HolderLookup.Provider provider) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        holder.saveManagedPersistentData(output, false);
        return output.buildResult();
    }

    private static void loadPersistent(IPersistManagedHolder holder, HolderLookup.Provider provider, CompoundTag tag) {
        holder.loadManagedPersistentData(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));
    }

    private static Field field(Class<?> owner, String name) {
        try {
            return owner.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
