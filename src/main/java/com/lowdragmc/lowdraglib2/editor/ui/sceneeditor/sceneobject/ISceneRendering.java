package com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject;

import com.lowdragmc.lowdraglib2.client.scene.SceneRenderContext;

/**
 * @author KilaBash
 * @date 2024/06/26
 * @implNote A scene object that can be rendered in the scene editor.
 */
public interface ISceneRendering extends ISceneObject {
    /** Before the transform is applied; children render after. */
    default void preDraw(float partialTicks) {
    }

    /** After the transform is applied; children render before. */
    default void postDraw(float partialTicks) {
    }

    /** Apply transform then call {@link #drawInternal}. */
    default void draw(SceneRenderContext ctx) {
        var poseStack = ctx.poseStack();
        poseStack.pushPose();
        poseStack.mulPose(transform().localToWorldMatrix());
        drawInternal(ctx);
        poseStack.popPose();
    }

    /** Submit / draw the object in the scene. */
    void drawInternal(SceneRenderContext ctx);
}
