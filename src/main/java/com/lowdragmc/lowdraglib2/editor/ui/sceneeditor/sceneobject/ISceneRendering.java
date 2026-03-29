package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * @author KilaBash
 * @date 2024/06/26
 * @implNote A scene object that can be rendered in the scene editor.
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Environment(EnvType.CLIENT)
public interface ISceneRendering extends ISceneObject {
    /**
     * Called before draw the object in the scene.
     * before the transform is applied. all children will be drawn after this.
     */
    @Environment(EnvType.CLIENT)
    default void preDraw(float partialTicks){
    }

    /**
     * Called after draw the object in the scene.
     * after the transform is applied. all children will be drawn before this.
     */
    @Environment(EnvType.CLIENT)
    default void postDraw(float partialTicks){
    }

    /**
     * Draw the object in the scene. execute transform here.
     */
    @Environment(EnvType.CLIENT)
    default void draw(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks){
        poseStack.pushPose();
        poseStack.mulPose(transform().localToWorldMatrix());
        drawInternal(poseStack, bufferSource, partialTicks);
        poseStack.popPose();
    }

    /**
     * Draw the object in the scene.
     */
    @Environment(EnvType.CLIENT)
    void drawInternal(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks);
}
