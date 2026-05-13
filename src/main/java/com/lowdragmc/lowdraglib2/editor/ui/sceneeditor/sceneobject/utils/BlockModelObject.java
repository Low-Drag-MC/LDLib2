package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils;

import com.lowdragmc.lowdraglib2.client.scene.SceneRenderContext;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneRendering;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.SceneObject;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlockModelObject extends SceneObject implements ISceneRendering {
    public BlockState blockState = Blocks.STONE.defaultBlockState();

    public BlockModelObject() {
    }

    @Override
    public void drawInternal(SceneRenderContext ctx) {
//        var renderer = Minecraft.getInstance().getBlockRenderer();
//        ctx.poseStack().translate(-0.5, -0.5, -0.5);
//        renderer.renderSingleBlock(blockState, ctx.poseStack(), ctx.bufferSource(), 0xf000f0, OverlayTexture.NO_OVERLAY, net.minecraft.world.level.EmptyBlockAndTintGetter.INSTANCE, BlockPos.ZERO);
    }
}
