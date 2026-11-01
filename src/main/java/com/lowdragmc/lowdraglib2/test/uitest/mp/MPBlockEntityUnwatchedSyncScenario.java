package com.lowdragmc.lowdraglib2.test.uitest.mp;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.ref.IRef;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.SyncTestBlock;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.SyncTestBlockEntity;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.bench.SyncBenchBlock;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.bench.SyncBenchBlockEntity;
import com.lowdragmc.lowdraglib2.uitest.ServerContext;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenario;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;

/**
 * Changes made while nobody watches a chunk are persisted but not sent, and whoever watches it next gets their latest
 * values, including ones made right as they start watching.
 */
@LDLRegister(name = "mp_block_entity_unwatched_sync", group = "ldlib2", registry = MPScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MPBlockEntityUnwatchedSyncScenario implements MPScenario {
    private static final BlockPos TEST_POS = new BlockPos(40, -60, 40);
    private static final BlockPos BENCH_POS = new BlockPos(41, -60, 40);
    private static final int CHANGES_AT_RETURN = 40;

    @Override
    public void configure(MPScenarioOptions options) {
        options.clients("A", "B").tags("mp", "sync");
    }

    private static String describe(@Nullable SyncTestBlockEntity test, @Nullable SyncBenchBlockEntity bench, boolean client) {
        var testPart = test == null ? "none" : test.describeSynced() + " label=" + (client ? test.label.toLowerCase() : test.label);
        var benchPart = bench == null ? "none" : "tier=" + bench.tier + " energy=" + bench.energy + " progress=" + bench.logic.progress
                + " working=" + bench.logic.working + " input=" + bench.importItems.getStackInSlot(0).getCount()
                + " output=" + bench.exportItems.getStackInSlot(0) + " tank=" + bench.tank.getFluidAmount();
        return testPart + " | " + benchPart;
    }

    private static String onServer(ServerContext sc) {
        return describe(sc.blockEntity(TEST_POS, SyncTestBlockEntity.class), sc.blockEntity(BENCH_POS, SyncBenchBlockEntity.class), false);
    }

    private static String onClient(TestContext ctx) {
        return describe(ctx.clientBlockEntity(TEST_POS, SyncTestBlockEntity.class), ctx.clientBlockEntity(BENCH_POS, SyncBenchBlockEntity.class), true);
    }

    private static boolean waitsUnsent(ServerContext sc) {
        var test = sc.blockEntity(TEST_POS, SyncTestBlockEntity.class).getSyncStorage().getSyncFields();
        var bench = sc.blockEntity(BENCH_POS, SyncBenchBlockEntity.class).getSyncStorage().getSyncFields();
        return Arrays.stream(test).anyMatch(IRef::isSyncDirty) && Arrays.stream(bench).anyMatch(IRef::isSyncDirty);
    }

    private static void moveTo(ServerContext sc, ServerPlayer player, double x, double z) {
        player.teleportTo(sc.server().overworld(), x, TEST_POS.getY(), z, Set.of(), 180f, 0f, true);
    }

    private static void moveNear(ServerContext sc, ServerPlayer player) {
        moveTo(sc, player, TEST_POS.getX() + 0.5, TEST_POS.getZ() + 3.5);
    }

    @Override
    public void define(MPScenarioBuilder s) {
        s.server("place the block entities and gather the players", sc -> {
            var level = sc.server().overworld();
            for (var pos : new BlockPos[]{TEST_POS, BENCH_POS}) {
                level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            }
            level.setBlockAndUpdate(TEST_POS, SyncTestBlock.BLOCK.defaultBlockState());
            level.setBlockAndUpdate(BENCH_POS, SyncBenchBlock.BLOCK.defaultBlockState());
            var test = sc.blockEntity(TEST_POS, SyncTestBlockEntity.class);
            test.counter = 1;
            test.label = "start";
            for (var player : sc.players()) {
                moveNear(sc, player);
            }
        })
        .syncAll("both clients see the block entities", MPBlockEntityUnwatchedSyncScenario::onServer, MPBlockEntityUnwatchedSyncScenario::onClient)

        .server("send everyone away", sc -> {
            for (var player : sc.players()) {
                moveTo(sc, player, TEST_POS.getX() + 1000.5, TEST_POS.getZ() + 1000.5);
            }
        })
        .allClients("the chunk is dropped", b -> b
                .timeoutMs(30_000)
                .waitUntil("the block entities are gone", ctx -> ctx.clientBlockEntity(TEST_POS, SyncTestBlockEntity.class) == null
                        && ctx.clientBlockEntity(BENCH_POS, SyncBenchBlockEntity.class) == null))
        .server("a first change, found unwatched", sc -> {
            sc.blockEntity(TEST_POS, SyncTestBlockEntity.class).counter = 2;
            sc.blockEntity(BENCH_POS, SyncBenchBlockEntity.class).energy -= 1;
        })
        .serverSettle(10)

        .server("change both while nobody watches", sc -> {
            sc.blockEntity(BENCH_POS, SyncBenchBlockEntity.class).setChangedCalls = 0;
            var test = sc.blockEntity(TEST_POS, SyncTestBlockEntity.class);
            test.label = "away";
            test.counter = 3;
            test.stack = new ItemStack(Items.DIAMOND, 2);
            test.positions.add(TEST_POS);
            test.scores.put("away", 1);
            var bench = sc.blockEntity(BENCH_POS, SyncBenchBlockEntity.class);
            bench.tier = 5;
            bench.logic.progress = 10;
            bench.logic.working = true;
            bench.importItems.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 33));
            bench.tank.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);
        })
        .serverSettle(10)
        // persisted = setChanged reached the chunk; chunk.isUnsaved() is no oracle since the server saves eagerly
        .serverWaitUntil("the changes are persisted and wait unsent", sc ->
                sc.blockEntity(BENCH_POS, SyncBenchBlockEntity.class).setChangedCalls > 0 && waitsUnsent(sc))
        .server("change them again", sc -> {
            var test = sc.blockEntity(TEST_POS, SyncTestBlockEntity.class);
            test.label = "still away";
            test.stack.grow(3);
            test.scores.put("away", 2);
            var bench = sc.blockEntity(BENCH_POS, SyncBenchBlockEntity.class);
            bench.logic.progress = 20;
            bench.exportItems.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 4));
        })
        .serverSettle(10)

        .server("bring A back", sc -> moveNear(sc, sc.player("A")))
        .serverWaitUntil("keep changing while A starts watching", sc -> {
            int changes = sc.get("changes") == null ? 0 : sc.<Integer>get("changes");
            var test = sc.blockEntity(TEST_POS, SyncTestBlockEntity.class);
            test.counter++;
            var bench = sc.blockEntity(BENCH_POS, SyncBenchBlockEntity.class);
            bench.logic.progress++;
            bench.importItems.getStackInSlot(0).shrink(changes % 2);
            sc.put("changes", changes + 1);
            return changes + 1 >= CHANGES_AT_RETURN;
        })
        .sync("A gets the latest values", "A", MPBlockEntityUnwatchedSyncScenario::onServer, MPBlockEntityUnwatchedSyncScenario::onClient)
        .client("A", "A's copy came through the stream codec", b -> b
                .step("check the label", ctx -> {
                    var label = ctx.clientBlockEntity(TEST_POS, SyncTestBlockEntity.class).label;
                    ctx.check("label after the changes", label.equalsIgnoreCase("still away"), "still away", label);
                }))

        .server("bring B back", sc -> moveNear(sc, sc.player("B")))
        .sync("B gets the latest values", "B", MPBlockEntityUnwatchedSyncScenario::onServer, MPBlockEntityUnwatchedSyncScenario::onClient)

        .teardownServer("remove the block entities and gather the players", sc -> {
            var level = sc.server().overworld();
            for (var pos : new BlockPos[]{TEST_POS, BENCH_POS}) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
            }
            for (var player : sc.players()) {
                moveTo(sc, player, 0.5, 0.5);
            }
        });
    }
}
