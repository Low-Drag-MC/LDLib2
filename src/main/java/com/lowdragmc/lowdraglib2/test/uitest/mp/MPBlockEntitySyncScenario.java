package com.lowdragmc.lowdraglib2.test.uitest.mp;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.ref.IRef;
import com.lowdragmc.lowdraglib2.test.syncdata.SyncTestBlock;
import com.lowdragmc.lowdraglib2.test.syncdata.SyncTestBlockEntity;
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
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@LDLRegister(name = "mp_block_entity_sync", group = "ldlib2", registry = MPScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MPBlockEntitySyncScenario implements MPScenario {
    private static final BlockPos POS = new BlockPos(4, -60, 2);

    @Override
    public void configure(MPScenarioOptions options) {
        options.clients("A", "B").tags("mp", "sync");
    }

    private static SyncTestBlockEntity onServer(ServerContext sc) {
        return sc.blockEntity(POS, SyncTestBlockEntity.class);
    }

    @Nullable
    private static SyncTestBlockEntity onClient(TestContext ctx) {
        return ctx.clientBlockEntity(POS, SyncTestBlockEntity.class);
    }

    private static SyncTestBlockEntity clientCopy(TestContext ctx) {
        var be = onClient(ctx);
        if (be == null) {
            throw new IllegalStateException("No SyncTestBlockEntity at " + POS + " on this client");
        }
        return be;
    }

    @Nullable
    private static String describe(@Nullable SyncTestBlockEntity blockEntity) {
        return blockEntity == null ? null : blockEntity.describeSynced();
    }

    private static boolean receivedRpc(TestContext ctx, String expected) {
        var be = onClient(ctx);
        return be != null && be.lastRpc.equals(expected);
    }

    private static boolean sentEverything(ServerContext sc) {
        return Arrays.stream(onServer(sc).getSyncStorage().getSyncFields()).noneMatch(IRef::isSyncDirty);
    }

    private static void moveTo(ServerContext sc, ServerPlayer player, double x, double z) {
        player.teleportTo(sc.server().overworld(), x, POS.getY(), z, 180f, 0f);
    }

    @Override
    public void define(MPScenarioBuilder s) {
        s.server("place the block and seed every field", sc -> {
            var level = sc.server().overworld();
            level.setBlockAndUpdate(POS.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(POS, SyncTestBlock.BLOCK.defaultBlockState());
            var be = onServer(sc);
            be.anchor = new BlockPos(1, 2, 3);
            be.label = "STEVE";
            be.codecTags.add("a");
            be.counter = 1;
            be.stack = new ItemStack(Items.DIAMOND, 3);
            be.positions.add(POS);
            be.scores.put("a", 1);
            be.gated = 5;
            be.lazyValue = 7;
            be.serverOnly = 99;
            // the async sync may already have sent the lazy field's default between placing and seeding
            be.markDirty("lazyValue");
            for (var player : sc.players()) {
                moveTo(sc, player, POS.getX() + 0.5, POS.getZ() + 3.5);
            }
        })
        .syncAll("every synced field reaches both clients",
                sc -> onServer(sc).describeSynced(), ctx -> describe(onClient(ctx)))
        .allClients("the first sync carries every synced field", b -> b
                .waitUntil("gated and lazyValue arrived", ctx -> {
                    var be = onClient(ctx);
                    return be != null && be.gated == 5 && be.lazyValue == 7;
                })
                .step("check the client copy", ctx -> {
                    var be = clientCopy(ctx);
                    ctx.check("label", be.label.equals("STEVE"), "STEVE", be.label);
                    ctx.check("a field that is only persisted is not synced", be.serverOnly == 0, 0, be.serverOnly);
                }))
        .serverWaitUntil("the first sync went out", MPBlockEntitySyncScenario::sentEverything)

        .server("change every field, in place where it can be", sc -> {
            var be = onServer(sc);
            be.anchor = new BlockPos(-4, 5, -6);
            be.label = "alex";
            be.codecTags.add("b");
            be.maybeAnchor = new BlockPos(7, 8, 9);
            be.counter = 2;
            be.stack.grow(1);
            be.positions.add(POS.above());
            be.scores.put("b", 2);
            be.gated = 0;
            be.lazyValue = 8;
        })
        .syncAll("the changes reach both clients",
                sc -> onServer(sc).describeSynced(), ctx -> describe(onClient(ctx)))
        .serverSettle(10)
        .allClients("check what each feature let through", b -> b
                .step("check the client copy", ctx -> {
                    var be = clientCopy(ctx);
                    ctx.check("a change travels through the stream codec", be.label.equals("ALEX"), "ALEX", be.label);
                    ctx.check("@ConditionalSynced held back gated=0", be.gated == 5, 5, be.gated);
                    ctx.check("@LazyManaged held back lazyValue=8", be.lazyValue == 7, 7, be.lazyValue);
                    var changes = be.counterChanges;
                    ctx.check("@UpdateListener saw counter 1->2",
                            !changes.isEmpty() && changes.getLast().equals("1->2"), "[..., 1->2]", changes);
                }))
        .server("let the held-back fields through", sc -> {
            var be = onServer(sc);
            be.gated = 6;
            be.markDirty("lazyValue");
        })
        .syncAll("gated and lazyValue reach both clients",
                sc -> onServer(sc).gated + "/" + onServer(sc).lazyValue,
                ctx -> {
                    var be = onClient(ctx);
                    return be == null ? null : be.gated + "/" + be.lazyValue;
                })

        .client("A", "rpc to the server", b -> b
                .step("call rpcPing", ctx -> clientCopy(ctx).rpcToServer("rpcPing", "hello from A")))
        .serverWaitUntil("the server received A's rpc",
                sc -> onServer(sc).lastRpc.equals("LDTestA:hello from A"))
        .server("rpc to every tracking client", sc -> onServer(sc).rpcToTracking("rpcPing", "to everyone"))
        .allClients("every client received it", b -> b
                .waitUntil("rpcPing arrived", ctx -> receivedRpc(ctx, "server:to everyone")))
        .server("rpc to B only", sc -> onServer(sc).rpcToPlayer(sc.player("B"), "rpcPing", "only to B"))
        .client("B", "B received it", b -> b
                .waitUntil("rpcPing arrived", ctx -> receivedRpc(ctx, "server:only to B")))
        .client("A", "A did not", b -> b
                .step("check lastRpc", ctx -> {
                    var lastRpc = clientCopy(ctx).lastRpc;
                    ctx.check("A kept the previous rpc", lastRpc.equals("server:to everyone"), "server:to everyone", lastRpc);
                }))

        .server("send B out of range", sc -> moveTo(sc, sc.player("B"), POS.getX() + 1000.5, POS.getZ() + 1000.5))
        .client("B", "B drops the block's chunk", b -> b
                .timeoutMs(30_000)
                .waitUntil("the block entity is gone", ctx -> onClient(ctx) == null))
        .server("change fields while B is away", sc -> {
            var be = onServer(sc);
            be.label = "bob";
            be.counter = 3;
            be.codecTags.clear();
            be.maybeAnchor = null;
            be.stack = ItemStack.EMPTY;
            be.positions.clear();
            be.scores.clear();
        })
        .sync("A follows along", "A", sc -> onServer(sc).describeSynced(), ctx -> describe(onClient(ctx)))
        .client("A", "check A's copy", b -> b
                .step("check the label", ctx -> {
                    var label = clientCopy(ctx).label;
                    ctx.check("the change came through the stream codec", label.equals("BOB"), "BOB", label);
                }))
        .server("bring B back", sc -> moveTo(sc, sc.player("B"), POS.getX() + 0.5, POS.getZ() + 3.5))
        .sync("B is current once the chunk loads again", "B",
                sc -> onServer(sc).describeSynced(), ctx -> describe(onClient(ctx)))
        .client("B", "check B's fresh copy", b -> b
                .step("check the client copy", ctx -> {
                    var be = clientCopy(ctx);
                    ctx.check("the reload delivered the label through the codec", be.label.equals("bob"), "bob", be.label);
                    ctx.check("the reload carries the held-back fields", be.gated == 6 && be.lazyValue == 8,
                            "6/8", be.gated + "/" + be.lazyValue);
                }))

        .teardownServer("remove the block and gather the players", sc -> {
            var level = sc.server().overworld();
            level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(POS.below(), Blocks.GRASS_BLOCK.defaultBlockState());
            for (var player : sc.players()) {
                moveTo(sc, player, 0.5, 0.5);
            }
        });
    }
}
