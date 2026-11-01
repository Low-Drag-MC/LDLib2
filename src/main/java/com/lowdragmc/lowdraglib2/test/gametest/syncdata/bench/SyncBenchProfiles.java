package com.lowdragmc.lowdraglib2.test.gametest.syncdata.bench;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.misc.FluidStorage;
import com.lowdragmc.lowdraglib2.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ManagedCodec;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.SyncProbeHolder;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * One holder class per kind of managed field, so the cost of each kind is measured on its own. {@link #MIXED} copies
 * the field mix of a GregTech-style machine.
 */
public final class SyncBenchProfiles {
    private SyncBenchProfiles() {}

    /**
     * @param work changes one field the way a working machine would, given the period number
     */
    public record Profile<H extends SyncProbeHolder>(String name, String fields, Function<ServerLevel, H> factory, Work<H> work) {
        public H create(ServerLevel level) {
            return factory.apply(level);
        }
    }

    @FunctionalInterface
    public interface Work<H> {
        void apply(H holder, int period);
    }

    public enum Mode {
        IDLE, AUTO, MANUAL
    }

    private static final Item[] ITEMS = {Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.COAL, Items.REDSTONE,
            Items.DIAMOND, Items.EMERALD, Items.LAPIS_LAZULI, Items.QUARTZ};

    private static void fill(ItemStackTransfer transfer) {
        for (int i = 0; i < transfer.getSlots(); i++) {
            transfer.setStackInSlot(i, new ItemStack(ITEMS[i % ITEMS.length], 16 + i));
        }
    }

    public static class Primitives extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public boolean working;
        @Persisted
        @DescSynced
        public boolean active;
        @Persisted
        @DescSynced
        public boolean wasActive;
        @Persisted
        @DescSynced
        public boolean allowOutput = true;
        @Persisted
        @DescSynced
        public int progress;
        @Persisted
        @DescSynced
        public int maxProgress = 200;
        @Persisted
        @DescSynced
        public int tier = 1;
        @Persisted
        @DescSynced
        public long energy = 100_000;
        @Persisted
        @DescSynced
        public float speed = 1;
        @Persisted
        @DescSynced
        public Direction facing = Direction.NORTH;

        public Primitives(ServerLevel level) {
            super(level);
        }
    }

    public static class Values extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public String status = "idle";
        @Persisted
        @DescSynced
        public UUID owner = new UUID(42, 7);
        @Persisted
        @DescSynced
        public Identifier recipe = LDLib2.id("recipe");
        @Persisted
        @DescSynced
        public BlockState casing = Blocks.IRON_BLOCK.defaultBlockState();
        @Persisted
        @DescSynced
        public Item filter = Items.IRON_INGOT;

        public Values(ServerLevel level) {
            super(level);
        }
    }

    public static class MutableValues extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public ItemStack stack = new ItemStack(Items.IRON_INGOT, 16);
        @Persisted
        @DescSynced
        public FluidStack fluid = new FluidStack(Fluids.WATER, 1000);
        @Persisted
        @DescSynced
        public BlockPos target = new BlockPos(10, 64, -10);
        @Persisted
        @DescSynced
        public Vector3f offset = new Vector3f(0.5f, 1, 0.5f);

        public MutableValues(ServerLevel level) {
            super(level);
        }
    }

    public static class Codecs extends SyncProbeHolder {
        @Persisted
        @DescSynced
        @ManagedCodec("tagsCodec")
        public List<String> tags = new ArrayList<>(List.of("ore", "ingot", "dust", "plate"));
        @Persisted
        @DescSynced
        @ManagedCodec("posCodec")
        public BlockPos anchor = new BlockPos(10, 64, -10);

        public Codecs(ServerLevel level) {
            super(level);
        }

        private static Codec<List<String>> tagsCodec() {
            return Codec.STRING.listOf().xmap(ArrayList::new, list -> list);
        }

        private static Codec<BlockPos> posCodec() {
            return Codec.LONG.xmap(BlockPos::of, BlockPos::asLong);
        }
    }

    public static class Containers extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public final ItemStackTransfer items = new ItemStackTransfer(9);
        @Persisted
        @DescSynced
        public final FluidStorage tank = new FluidStorage(16000);

        public Containers(ServerLevel level) {
            super(level);
            fill(items);
            tank.fill(new FluidStack(Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public static class CustomContainers extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public final LockableItems items = new LockableItems(9);
        @Persisted
        @DescSynced
        public final LockableTank tank = new LockableTank(16000);

        public CustomContainers(ServerLevel level) {
            super(level);
            fill(items);
            tank.fill(new FluidStack(Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public static class VersionedContainers extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public final VersionedItems items = new VersionedItems(9);
        @Persisted
        @DescSynced
        public final VersionedTank tank = new VersionedTank(16000);

        public VersionedContainers(ServerLevel level) {
            super(level);
            fill(items);
            tank.fill(new FluidStack(Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public static class CollectionFields extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public List<String> names = new ArrayList<>();
        @Persisted
        @DescSynced
        public List<Integer> amounts = new ArrayList<>();
        @Persisted
        @DescSynced
        public Map<String, Integer> counts = new HashMap<>();
        @Persisted
        @DescSynced
        public List<BlockPos> links = new ArrayList<>();

        public CollectionFields(ServerLevel level) {
            super(level);
            for (int i = 0; i < 8; i++) {
                names.add("entry" + i);
                amounts.add(1000 + i);
                counts.put("key" + i, 1000 + i);
                links.add(new BlockPos(i, 64, -i));
            }
        }
    }

    public static class ArrayFields extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public int[] levels = new int[16];
        @Persisted
        @DescSynced
        public ItemStack[] slots = new ItemStack[9];

        public ArrayFields(ServerLevel level) {
            super(level);
            for (int i = 0; i < slots.length; i++) {
                slots[i] = new ItemStack(ITEMS[i], 16 + i);
            }
        }
    }

    public static class Nested extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public final Part logic = new Part(this::notifyPersistence);

        public Nested(ServerLevel level) {
            super(level);
        }
    }

    public static class Mixed extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public boolean working;
        @Persisted
        @DescSynced
        public boolean active;
        @Persisted
        @DescSynced
        public boolean allowOutput = true;
        @Persisted
        @DescSynced
        public int tier = 1;
        @Persisted
        @DescSynced
        public long energy = 100_000;
        @Persisted
        @DescSynced
        public Direction facing = Direction.NORTH;
        @Persisted
        @DescSynced
        public Direction outputFacing = Direction.SOUTH;
        @Persisted
        @DescSynced
        public Mode mode = Mode.AUTO;
        @Persisted
        @DescSynced
        public final ItemStackTransfer importItems = new ItemStackTransfer(9);
        @Persisted
        @DescSynced
        public final ItemStackTransfer exportItems = new ItemStackTransfer(9);
        @Persisted
        @DescSynced
        public final FluidStorage tank = new FluidStorage(16000);
        @Persisted
        @DescSynced
        public final Part logic = new Part(this::notifyPersistence);

        public Mixed(ServerLevel level) {
            super(level);
            fill(importItems);
            tank.fill(new FluidStack(Fluids.WATER, 8000), IFluidHandler.FluidAction.EXECUTE);
        }

        public void work(int period) {
            logic.progress = period % logic.duration;
            working = true;
            energy -= 32;
            if (period % 20 == 0) {
                exportItems.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, period / 20 % 64 + 1));
            }
        }
    }

    public static final Profile<Primitives> PRIMITIVES = new Profile<>("primitives",
            "4 boolean, 3 int, long, float, enum", Primitives::new, (h, p) -> h.progress = p);
    public static final Profile<Values> VALUES = new Profile<>("immutable values",
            "String, UUID, Identifier, BlockState, Item", Values::new, (h, p) -> h.status = (p & 1) == 0 ? "busy" : "idle");
    public static final Profile<MutableValues> MUTABLE_VALUES = new Profile<>("mutable values",
            "ItemStack, FluidStack, BlockPos, Vector3f", MutableValues::new, (h, p) -> h.stack.setCount(16 + (p & 1)));
    public static final Profile<Codecs> CODECS = new Profile<>("@ManagedCodec",
            "List<String>(4), BlockPos", Codecs::new, (h, p) -> h.tags.set(0, (p & 1) == 0 ? "ore" : "raw"));
    public static final Profile<Containers> CONTAINERS = new Profile<>("containers",
            "ItemStackTransfer(9 full), FluidStorage", Containers::new,
            (h, p) -> h.items.setStackInSlot(0, new ItemStack(ITEMS[0], 16 + (p & 1))));
    public static final Profile<CustomContainers> CUSTOM_CONTAINERS = new Profile<>("containers overriding serialize",
            "ItemStackTransfer(9 full) and FluidStorage subclasses that save extra state", CustomContainers::new,
            (h, p) -> h.items.setStackInSlot(0, new ItemStack(ITEMS[0], 16 + (p & 1))));
    public static final Profile<VersionedContainers> VERSIONED_CONTAINERS = new Profile<>("containers with a content version",
            "the same subclasses, counting their changes through IContentVersion", VersionedContainers::new,
            (h, p) -> h.items.setStackInSlot(0, new ItemStack(ITEMS[0], 16 + (p & 1))));
    public static final Profile<CollectionFields> COLLECTIONS = new Profile<>("collections",
            "List<String>(8), List<Integer>(8), Map<String,Integer>(8), List<BlockPos>(8)", CollectionFields::new,
            (h, p) -> h.amounts.set(0, 1000 + (p & 1)));
    public static final Profile<ArrayFields> ARRAYS = new Profile<>("arrays",
            "int[16], ItemStack[9]", ArrayFields::new, (h, p) -> h.levels[0] = p);
    public static final Profile<Nested> NESTED = new Profile<>("nested IManaged",
            "1 sub-object with 8 primitives", Nested::new, (h, p) -> h.logic.progress = p);
    public static final Profile<Mixed> MIXED = new Profile<>("GTCEu-like machine",
            "3 boolean, int, long, 2 Direction, enum, 2 ItemStackTransfer(9), FluidStorage, nested logic(8)",
            Mixed::new, Mixed::work);

    public static final List<Profile<?>> ALL = List.of(PRIMITIVES, VALUES, MUTABLE_VALUES, CODECS, CONTAINERS,
            CUSTOM_CONTAINERS, VERSIONED_CONTAINERS, COLLECTIONS, ARRAYS, NESTED, MIXED);
}
