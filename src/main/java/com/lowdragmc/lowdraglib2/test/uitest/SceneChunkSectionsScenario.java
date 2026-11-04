package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.scene.FBOWorldSceneRenderer;
import com.lowdragmc.lowdraglib2.client.scene.SceneChunkSections;
import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.capture.FrameCapture;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Scenes draw their terrain through ChunkSection UBOs of their own. Sharing the game's chunk section storage made it
 * flip between the UBO kind and the instanced one the world uses under multi-draw indirect, recreating both every
 * frame and logging a resize for each.
 */
@LDLRegisterClient(name = "scene_chunk_sections", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class SceneChunkSectionsScenario implements UIScenario {

    private static final String SCENE = "scene";
    private static final String FBO = "fbo";
    private static final String CAPACITY = "capacity";
    private static final int SIDE = 5;
    private static final int HEIGHT = 3;
    /** One block a section, past the 64 the scene storage starts with. */
    private static final int SECTIONS = SIDE * HEIGHT * SIDE;
    private static final int FBO_SIZE = 128;

    private static final List<String> RESIZES = new CopyOnWriteArrayList<>();
    private static final AbstractAppender APPENDER = new AbstractAppender("scene_chunk_sections", null, null,
            true, Property.EMPTY_ARRAY) {
        @Override
        public void append(LogEvent event) {
            var message = event.getMessage().getFormattedMessage();
            if (event.getLoggerName().contains("DynamicGpuDataStorage") && message.contains("Chunk Sections")) {
                RESIZES.add(message);
            }
        }
    };

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("scene").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("a cached scene of " + SECTIONS + " sections", SceneChunkSectionsScenario::sceneUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .frames(3)
                .waitUntil("the compile finished", ctx -> !renderer(ctx).isCompiling())
                .frames(10)
                .step("the scene draws", SceneChunkSectionsScenario::checkDraws)
                .step("the scene storage holds every section", ctx -> {
                    var storage = storage();
                    ctx.require("the scene made its storage", storage != null);
                    int capacity = ctx.getField(storage, "capacity");
                    ctx.put(CAPACITY, capacity);
                    ctx.check("its capacity grew to the section count", capacity >= SECTIONS,
                            ">= " + SECTIONS, String.valueOf(capacity));
                })

                .group("frames in a row", g -> g
                        .step("listen for chunk section resizes", ctx -> {
                            ctx.attach("world_multi_draw_indirect", String.valueOf(
                                    ctx.<Boolean>getField(ctx.mc().levelRenderer, "usingMultiDrawIndirectForTerrain")));
                            RESIZES.clear();
                            APPENDER.start();
                            rootLogger().addAppender(APPENDER);
                        })
                        .frames(60)
                        .step("nothing was resized", ctx -> {
                            rootLogger().removeAppender(APPENDER);
                            if (!RESIZES.isEmpty()) ctx.attach("first_resize", RESIZES.getFirst());
                            ctx.check("no chunk section storage resized", RESIZES.isEmpty(),
                                    "0", String.valueOf(RESIZES.size()));
                            var storage = storage();
                            ctx.require("the scene storage is still there", storage != null);
                            int capacity = ctx.getField(storage, "capacity");
                            ctx.check("the scene storage kept its capacity", capacity == ctx.<Integer>get(CAPACITY),
                                    ctx.<Integer>get(CAPACITY), capacity);
                        }))

                .group("a client stop", g -> g
                        .step("releases the storage", ctx -> {
                            SceneChunkSections.onClientStopping(new ClientStoppingEvent(ctx.mc()));
                            ctx.check("the storage is gone", storage() == null);
                        })
                        .frames(3)
                        .check("a later frame makes a new one", ctx -> storage() != null)
                        .step("the scene still draws", SceneChunkSectionsScenario::checkDraws))
                .closeScreen()
                .frames(3)

                .group("a scene drawn while the game's storage is the instanced kind", g -> g
                        .step("leaves it alone", ctx -> {
                            var fbo = new FBOWorldSceneRenderer(level(), FBO_SIZE, FBO_SIZE);
                            ctx.put(FBO, fbo);
                            fbo.addRenderedBlocks(blocks(), null);
                            fbo.setCameraLookAt(new Vector3f(SIDE * 8f, HEIGHT * 8f, SIDE * 8f), 160, Math.PI / 4, Math.PI / 6);
                            ctx.require("no scene has drawn this frame", !writtenThisFrame());

                            // what the world leaves there when it draws by multi-draw indirect, on any machine
                            var dynamic = RenderSystem.getDynamicUniforms();
                            dynamic.writeChunkSectionsInstanced(List.of(new DynamicGpuData.ChunkSectionInfo(0, 0, 0, 1f)));
                            Object before = ctx.getField(dynamic, "chunkSections");
                            fbo.drawScene(0, 0, FBO_SIZE, FBO_SIZE, -1, -1);
                            ctx.check("the scene wrote its sections", writtenThisFrame());
                            ctx.check("the game's chunk section storage is the one it was",
                                    ctx.getField(dynamic, "chunkSections") == before);
                        }));

        s.teardown("stop listening and release the target", ctx -> {
            rootLogger().removeAppender(APPENDER);
            FBOWorldSceneRenderer fbo = ctx.get(FBO, null);
            if (fbo != null) fbo.releaseResource();
        });
    }

    private static List<BlockPos> blocks() {
        var blocks = new ArrayList<BlockPos>();
        for (int x = 0; x < SIDE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIDE; z++) {
                    blocks.add(new BlockPos(x * 16, y * 16, z * 16));
                }
            }
        }
        return blocks;
    }

    private static TrackedDummyWorld level() {
        var level = new TrackedDummyWorld();
        for (var pos : blocks()) {
            level.addBlock(pos, BlockInfo.fromBlock(Blocks.GOLD_BLOCK));
        }
        return level;
    }

    private static ModularUI sceneUI(TestContext ctx) {
        var scene = new Scene();
        scene.createScene(level());
        scene.setRenderedCore(blocks(), null);
        scene.useCacheBuffer();
        scene.layout(layout -> layout.width(200).height(200));
        ctx.put(SCENE, scene);
        var root = new UIElement().layout(layout -> layout.width(220).height(220)).addChild(scene);
        return new ModularUI(UI.of(root), ctx.player());
    }

    private static WorldSceneRenderer renderer(TestContext ctx) {
        var renderer = ctx.<Scene>get(SCENE).<WorldSceneRenderer>getRenderer();
        ctx.require("the scene has a renderer", renderer != null);
        return renderer;
    }

    private static void checkDraws(TestContext ctx) {
        var image = FrameCapture.grab();
        try {
            ctx.check("the scene drew its blocks", !SceneCompileCancelScenario.isRegionUniform(
                    image, ctx.get(SCENE), ctx.mc().getWindow().getGuiScale()));
        } finally {
            FrameCapture.closeQuietly(image);
        }
    }

    @Nullable
    private static Object storage() {
        return staticField("storage");
    }

    private static boolean writtenThisFrame() {
        return (Boolean) staticField("writtenThisFrame");
    }

    private static Object staticField(String name) {
        try {
            var field = SceneChunkSections.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot read SceneChunkSections." + name, e);
        }
    }

    private static Logger rootLogger() {
        return (Logger) LogManager.getRootLogger();
    }
}
