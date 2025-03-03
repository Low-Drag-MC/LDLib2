package com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.sceneobject.utils;

import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.sceneobject.ISceneRendering;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.sceneobject.SceneObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlockModelObject extends SceneObject implements ISceneRendering {
    public BlockState blockState = Blocks.STONE.defaultBlockState();

    public BlockModelObject() {
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void drawInternal(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks) {
        var renderer = Minecraft.getInstance().getBlockRenderer();
        poseStack.translate(-0.5, -0.5, -0.5);
        renderer.renderSingleBlock(blockState, poseStack, bufferSource, 0xf000f0, OverlayTexture.NO_OVERLAY);
    }
}
