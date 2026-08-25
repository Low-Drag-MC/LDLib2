package com.lowdragmc.lowdraglib2.test.uitest.mp;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.test.TestBlock;
import com.lowdragmc.lowdraglib2.test.TestBlockEntity;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenario;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/**
 * The end-to-end smoke test for the multi-process harness, against {@link TestBlockEntity}'s
 * {@code @DescSynced int intValue} and its server-clicked +/- buttons:
 *
 * <ol>
 *   <li>the dedicated server places the block and seeds the value,</li>
 *   <li>every client's block-entity copy converges on it (world sync),</li>
 *   <li>client A opens the UI and clicks {@code +} (C2S UI event → server mutation),</li>
 *   <li>the server asserts the authoritative value moved,</li>
 *   <li>client B opens its own view and sees the updated value (per-player UI sync).</li>
 * </ol>
 *
 * <p>Also serves as the dist-loading probe for {@code MPScenarioDistLoadingTest}: it deliberately
 * contains every lambda shape a scenario author will reach for — {@code Consumer<ServerContext>},
 * {@code Consumer<ScenarioBuilder>} blocks, inner {@code ctx -> ...} steps, typed sync getters, and
 * a client-only class literal inside a block.
 */
@LDLRegister(name = "mp_sync_smoke", group = "ldlib2", registry = MPScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MPSyncSmokeScenario implements MPScenario {

    /** On the default superflat surface (grass top at y = -61), next to world spawn. */
    private static final BlockPos POS = new BlockPos(2, -60, 2);

    @Override
    public void configure(MPScenarioOptions options) {
        options.clients("A", "B").tags("mp", "sync");
    }

    @Override
    public void define(MPScenarioBuilder s) {
        s.server("place test block and seed intValue=42", sc -> {
            var level = sc.server().overworld();
            level.setBlockAndUpdate(POS.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(POS, TestBlock.BLOCK.defaultBlockState());
            sc.setField(sc.blockEntity(POS, TestBlockEntity.class), "intValue", 42);
            // Everyone next to the block, in creative, so the client-side use passes reach checks.
            for (var player : sc.players()) {
                player.setGameMode(GameType.CREATIVE);
                // 26.1 dropped the short overload; this is what it used to delegate to - absolute
                // coordinates (no Relative flags) and the camera reset back onto the player.
                player.teleportTo(level, POS.getX() + 0.5, POS.getY(), POS.getZ() + 2.5,
                        Set.of(), 180f, 0f, true);
            }
        })
        .syncAll("intValue=42 reaches every client's block entity",
                sc -> sc.<Integer>getField(sc.blockEntity(POS, TestBlockEntity.class), "intValue"),
                ctx -> {
                    var blockEntity = ctx.clientBlockEntity(POS, TestBlockEntity.class);
                    return blockEntity == null ? null : ctx.getField(blockEntity, "intValue");
                })
        .client("A", "open the ui and click +", b -> b
                .useBlock(POS)
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .waitForText("#int_value_label", "42")
                .click("#btn_inc")
                .waitForText("#int_value_label", "43")
                .screenshot("a_after_increment")
                .closeScreen())
        .serverWaitUntil("the dedicated server observed the increment",
                sc -> sc.<Integer>getField(sc.blockEntity(POS, TestBlockEntity.class), "intValue") == 43)
        .server("assert the authoritative value", sc -> {
            int value = sc.getField(sc.blockEntity(POS, TestBlockEntity.class), "intValue");
            sc.check("intValue on the dedicated server", value == 43, 43, value);
        })
        .client("B", "the other client sees the update", b -> b
                .useBlock(POS)
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .waitForText("#int_value_label", "43")
                .screenshot("b_synced_view")
                .step("client copy of the block entity also converged", ctx -> {
                    var blockEntity = ctx.clientBlockEntity(POS, TestBlockEntity.class);
                    ctx.check("client block entity exists on B", blockEntity != null);
                    if (blockEntity != null) {
                        int value = ctx.getField(blockEntity, "intValue");
                        ctx.check("intValue on B's block entity", value == 43, 43, value);
                    }
                }))
        .allClients("everyone closes their screen", b -> b.closeScreen())
        .teardownAllClients("ensure screens are closed", b -> b.closeScreen())
        .teardownServer("remove the test block", sc -> {
            var level = sc.server().overworld();
            level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(POS.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        });
    }
}
