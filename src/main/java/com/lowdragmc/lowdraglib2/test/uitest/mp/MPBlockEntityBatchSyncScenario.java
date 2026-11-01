package com.lowdragmc.lowdraglib2.test.uitest.mp;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.SyncTestBlock;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.SyncTestBlockEntity;
import com.lowdragmc.lowdraglib2.uitest.ServerContext;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenario;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Block entities that change in the same async period share a sync packet per chunk.
 */
@LDLRegister(name = "mp_block_entity_batch_sync", group = "ldlib2", registry = MPScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MPBlockEntityBatchSyncScenario implements MPScenario {
    // three in chunk (0, 0), one in chunk (1, 0)
    private static final List<BlockPos> POSITIONS = List.of(
            new BlockPos(8, -60, 8), new BlockPos(9, -60, 8), new BlockPos(10, -60, 8), new BlockPos(24, -60, 8));

    @Override
    public void configure(MPScenarioOptions options) {
        options.clients("A", "B").tags("mp", "sync");
    }

    private static String describeServer(ServerContext sc) {
        return POSITIONS.stream().map(pos -> {
            var be = sc.server().overworld().getBlockEntity(pos) instanceof SyncTestBlockEntity syncTest ? syncTest : null;
            return pos.toShortString() + "=" + (be == null ? "none" : be.describeSynced() + " label=" + be.label);
        }).collect(Collectors.joining("; "));
    }

    private static String describeClient(TestContext ctx) {
        return POSITIONS.stream().map(pos -> {
            var be = ctx.clientBlockEntity(pos, SyncTestBlockEntity.class);
            return pos.toShortString() + "=" + (be == null ? "none" : be.describeSynced() + " label=" + be.label.toLowerCase());
        }).collect(Collectors.joining("; "));
    }

    private static SyncTestBlockEntity onServer(ServerContext sc, int index) {
        return sc.blockEntity(POSITIONS.get(index), SyncTestBlockEntity.class);
    }

    @Override
    public void define(MPScenarioBuilder s) {
        s.server("place four block entities across two chunks", sc -> {
            var level = sc.server().overworld();
            for (int i = 0; i < POSITIONS.size(); i++) {
                var pos = POSITIONS.get(i);
                level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(pos, SyncTestBlock.BLOCK.defaultBlockState());
                var be = onServer(sc, i);
                be.counter = i + 1;
                be.label = "first" + i;
            }
            for (var player : sc.players()) {
                player.teleportTo(level, 16.5, -60, 2.5, Set.of(), 0f, 0f, true);
            }
        })
        .syncAll("both clients see all four", MPBlockEntityBatchSyncScenario::describeServer, MPBlockEntityBatchSyncScenario::describeClient)

        .server("change all four in the same tick", sc -> {
            for (int i = 0; i < POSITIONS.size(); i++) {
                var be = onServer(sc, i);
                be.counter += 10;
                be.label = "second" + i;
                be.stack = new ItemStack(Items.DIAMOND, i + 1);
                be.positions.add(POSITIONS.get(i));
                be.scores.put("i", i);
            }
        })
        .syncAll("every change reaches both clients", MPBlockEntityBatchSyncScenario::describeServer, MPBlockEntityBatchSyncScenario::describeClient)

        .server("remove one while its neighbours change", sc -> {
            var level = sc.server().overworld();
            level.setBlockAndUpdate(POSITIONS.get(1), Blocks.AIR.defaultBlockState());
            onServer(sc, 0).counter = 100;
            onServer(sc, 2).label = "third";
            onServer(sc, 3).stack.grow(5);
        })
        .syncAll("the neighbours' changes arrive and the removed one is gone",
                MPBlockEntityBatchSyncScenario::describeServer, MPBlockEntityBatchSyncScenario::describeClient)

        .teardownServer("remove the block entities and gather the players", sc -> {
            var level = sc.server().overworld();
            for (var pos : POSITIONS) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
            }
            for (var player : sc.players()) {
                player.teleportTo(level, 0.5, -60, 0.5, Set.of(), 0f, 0f, true);
            }
        });
    }
}
