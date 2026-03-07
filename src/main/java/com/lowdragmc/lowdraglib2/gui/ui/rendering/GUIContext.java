package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderPipelines;
import com.lowdragmc.lowdraglib2.gui.texture.renderstate.FloatBlitRenderState;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.renderstate.FloatColoredRectangleRenderState;
import com.lowdragmc.lowdraglib2.gui.texture.renderstate.FloatColoredTriangleRenderState;
import com.lowdragmc.lowdraglib2.gui.texture.renderstate.FloatRoundedRectRenderState;
import com.lowdragmc.lowdraglib2.gui.texture.renderstate.FloatTiledBlitRenderState;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.*;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.*;

import javax.annotation.Nullable;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

public class GUIContext {
    @OnlyIn(Dist.CLIENT)
    public GuiGraphics graphics;
    @OnlyIn(Dist.CLIENT)
    public int mouseX, mouseY;
    @OnlyIn(Dist.CLIENT)
    public float partialTick;
    @OnlyIn(Dist.CLIENT)
    public EnhancedPoseStack pose;
    @OnlyIn(Dist.CLIENT)
    public Minecraft mc;

    // runtime
    @OnlyIn(Dist.CLIENT)
    public boolean refreshLocalMouse = true;
    /**
     * Current element tint color (ARGB), set by UIElement before drawing its background/overlay textures.
     * -1 (0xFFFFFFFF) means no tint. Textures read this to multiply (per-channel) with their own color.
     */
    @OnlyIn(Dist.CLIENT)
    public int elementColor = -1;
    @OnlyIn(Dist.CLIENT)
    public float localMouseX, localMouseY;
    @OnlyIn(Dist.CLIENT)
    public Stack<UIVisualLayer> visualLayers = new Stack<>();
    @OnlyIn(Dist.CLIENT)
    private final List<PostCall> postRenderingCalls = new ArrayList<>();

    private record PostCall(Consumer<GUIContext> call, Matrix3x2f pose) {}

    @OnlyIn(Dist.CLIENT)
    public static GUIContext of(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var context = new GUIContext();
        context.graphics = graphics;
        context.mouseX = mouseX;
        context.mouseY = mouseY;
        context.partialTick = partialTick;
        context.pose = new EnhancedPoseStack(graphics.pose()).setOnTransform(context::refreshLocalMouse);
        context.mc = Minecraft.getInstance();
        context.refreshLocalMouse();
        return context;
    }

    @OnlyIn(Dist.CLIENT)
    public void drawTexture(IGuiTexture texture, float x, float y, float width, float height) {
        texture.draw(this, x, y, width, height);
    }

    @OnlyIn(Dist.CLIENT)
    public void enableScissor(float x, float y, float width, float height) {
        graphics.enableScissor(Mth.floor(x), Mth.floor(y), Mth.ceil(x + width), Mth.ceil(y + height));
    }

    @OnlyIn(Dist.CLIENT)
    public @Nullable ScreenRectangle peekScissor() {
        return graphics.peekScissorStack();
    }

    @OnlyIn(Dist.CLIENT)
    public void disableScissor() {
        graphics.disableScissor();
    }

    @OnlyIn(Dist.CLIENT)
    public void refreshLocalMouse() {
        var realMouse = pose.pose.invert(new Matrix3x2f()).transformPosition(new Vector2f(mouseX, mouseY));
        localMouseX = realMouse.x;
        localMouseY = realMouse.y;
    }

    @OnlyIn(Dist.CLIENT)
    public void pushVisualLayer(UIVisualLayer layer) {
        // todo visual layer
//        graphics.flush();
//        if (visualLayers.isEmpty()) {
//            int[] fbo = new int[1];
//            GL30.glGetIntegerv(GL30.GL_FRAMEBUFFER_BINDING, fbo);
//            lastFBO = fbo[0];
//        }
//        visualLayers.push(layer);
//        layer.bind(this);
//        layer.clear();
    }

    @OnlyIn(Dist.CLIENT)
    public void popVisualLayer() {
        // todo visual layer

//        var popped = visualLayers.pop();
//        if (popped != null) {
//            graphics.flush();
//            popped.unbind();
//            var mainTarget = Minecraft.getInstance().getMainRenderTarget();
//            if (visualLayers.isEmpty()) {
//                if (lastFBO == -1) {
//                    mainTarget.bindWrite(false);
//                } else {
//                    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, lastFBO);
//                }
//            } else {
//                visualLayers.peek().bind(this);
//            }
//            popped.draw(this);
//            popped.release();
//        }
    }

    @OnlyIn(Dist.CLIENT)
    public void setElementColor(int elementColor) {
        if (this.elementColor == elementColor) return;
        this.elementColor = elementColor;
    }

    @OnlyIn(Dist.CLIENT)
    public void resetElementColor() {
        if (this.elementColor == -1) return;
        this.elementColor = -1;
    }

    // region rendering

    /// why we do it? because graphic doesn't support float by default.

    public void postRendering(Consumer<GUIContext> call) {
        postRenderingCalls.add(new PostCall(call, new Matrix3x2f(pose.pose)));
    }

    public void callPostRendering() {
        for (var postRenderingCall : postRenderingCalls) {
            pose.pushPose();
            pose.pose.set(postRenderingCall.pose);
            postRenderingCall.call.accept(this);
            pose.popPose();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public GuiRenderState getRenderState() {
        return graphics.guiRenderState;
    }

    @OnlyIn(Dist.CLIENT)
    public void submitItem(GuiItemRenderState itemState) {
        graphics.guiRenderState.submitItem(itemState);
    }

    @OnlyIn(Dist.CLIENT)
    public void submitText(GuiTextRenderState textState) {
        graphics.guiRenderState.submitText(textState);

    }

    @OnlyIn(Dist.CLIENT)
    public void submitPicturesInPictureState(PictureInPictureRenderState picturesInPictureState) {
        graphics.guiRenderState.submitPicturesInPictureState(picturesInPictureState);
    }

    @OnlyIn(Dist.CLIENT)
    public void submitGuiElement(GuiElementRenderState blitState) {
        graphics.guiRenderState.submitGuiElement(blitState);
    }

    @OnlyIn(Dist.CLIENT)
    public MultiBufferSource.BufferSource bufferSource() {
        return mc.renderBuffers().bufferSource();
    }

    @OnlyIn(Dist.CLIENT)
    public void endBatch() {
        bufferSource().endBatch();
    }

    @OnlyIn(Dist.CLIENT)
    public void fill(
            RenderPipeline renderPipeline, float x0, float y0, float x1, float y1,
            int colorU0V0, int colorU0V1, int colorU1V1, int colorU1V0
    ) {
        this.submitGuiElement(
                new FloatColoredRectangleRenderState(
                        renderPipeline, TextureSetup.noTexture(), this.pose.copyPose(), x0, y0, x1, y1,
                        ColorUtils.mulColor(colorU0V0, elementColor), ColorUtils.mulColor(colorU0V1, elementColor), ColorUtils.mulColor(colorU1V1, elementColor), ColorUtils.mulColor(colorU1V0, elementColor), graphics.peekScissorStack()
                )
        );
    }

    @OnlyIn(Dist.CLIENT)
    public void fillTriangle(
            RenderPipeline renderPipeline,
            Vector2f position0, Vector2f position1, Vector2f position2,
            int color0, int color1, int color2
    ) {
        this.submitGuiElement(
                new FloatColoredTriangleRenderState(
                        renderPipeline, TextureSetup.noTexture(), this.pose.copyPose(),
                        position0, position1, position2,
                        ColorUtils.mulColor(color0, elementColor),
                        ColorUtils.mulColor(color1, elementColor),
                        ColorUtils.mulColor(color2, elementColor),
                        graphics.peekScissorStack()
                )
        );
    }

    @OnlyIn(Dist.CLIENT)
    public void fillTriangle(
            RenderPipeline renderPipeline,
            Vector2f position0, Vector2f position1, Vector2f position2,
            int color
    ) {
        this.fillTriangle(renderPipeline, position0, position1, position2, color, color, color);
    }

    @OnlyIn(Dist.CLIENT)
    public void blit(
            RenderPipeline renderPipeline,
            Identifier texture,
            float x, float y,
            float u, float v,
            float width, float height,
            float srcWidth, float srcHeight,
            float textureWidth, float textureHeight,
            int color
    ) {
        this.innerBlit(
                renderPipeline,
                texture,
                x,
                x + width,
                y,
                y + height,
                (u) / textureWidth,
                (u + srcWidth) / textureWidth,
                (v) / textureHeight,
                (v + srcHeight) / textureHeight,
                color
        );
    }

    @OnlyIn(Dist.CLIENT)
    public void blit(
            RenderPipeline renderPipeline,
            Identifier texture,
            float x, float y,
            float width, float height,
            float u0, float v0,
            float u1, float v1,
            int color
    ) {
        this.innerBlit(
                renderPipeline,
                texture,
                x, x + width,
                y, y + height,
                u0, u1,
                v0, v1,
                color
        );
    }

    @OnlyIn(Dist.CLIENT)
    public void blitSprite(RenderPipeline renderPipeline, Identifier location,
                           float x, float y, float width, float height, int color) {
        var sprite = graphics.guiSprites.getSprite(location);
        var scaling = getSpriteScaling(sprite);
        switch (scaling) {
            case GuiSpriteScaling.Stretch stretch:
                this.blitSprite(renderPipeline, sprite, x, y, width, height, color);
                break;
            case GuiSpriteScaling.Tile tile:
                this.blitTiledSprite(renderPipeline, sprite, x, y, width, height, 0, 0, tile.width(), tile.height(), tile.width(), tile.height(), color);
                break;
            case GuiSpriteScaling.NineSlice nineSlice:
                this.blitNineSlicedSprite(renderPipeline, sprite, nineSlice, x, y, width, height, color);
                break;
            default:
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void blitSprite(RenderPipeline renderPipeline, TextureAtlasSprite sprite,
                           float x, float y, float width, float height, int color) {
        if (width != 0 && height != 0) {
            this.innerBlit(
                    renderPipeline, sprite.atlasLocation(),
                    x, x + width, y, y + height,
                    sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(),
                    color
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void blitSprite(
            RenderPipeline renderPipeline,
            TextureAtlasSprite sprite,
            float spriteWidth,
            float spriteHeight,
            float textureX,
            float textureY,
            float x,
            float y,
            float width,
            float height,
            int color
    ) {
        if (width != 0 && height != 0) {
            this.innerBlit(
                    renderPipeline,
                    sprite.atlasLocation(),
                    x,
                    x + width,
                    y,
                    y + height,
                    sprite.getU(textureX / spriteWidth),
                    sprite.getU((textureX + width) / spriteWidth),
                    sprite.getV(textureY / spriteHeight),
                    sprite.getV((textureY + height) / spriteHeight),
                    color
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void blitTiledSprite(
            RenderPipeline renderPipeline,
            TextureAtlasSprite sprite,
            float x, float y, float width, float height,
            float textureX, float textureY, float tileWidth, float tileHeight, float spriteWidth, float spriteHeight,
            int color
    ) {
        if (width > 0 && height > 0) {
            if (tileWidth > 0 && tileHeight > 0) {
                var spriteTexture = mc.getTextureManager().getTexture(sprite.atlasLocation());
                var texture = spriteTexture.getTextureView();
                this.submitTiledBlit(
                        renderPipeline,
                        texture,
                        spriteTexture.getSampler(),
                        tileWidth,
                        tileHeight,
                        x,
                        y,
                        x + width,
                        y + height,
                        sprite.getU(textureX / spriteWidth),
                        sprite.getU((textureX + tileWidth) / spriteWidth),
                        sprite.getV(textureY / spriteHeight),
                        sprite.getV((textureY + tileHeight) / spriteHeight),
                        color
                );
            } else {
                throw new IllegalArgumentException("Tile size must be positive, got " + tileWidth + "x" + tileHeight);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void blitNineSlicedSprite(
            RenderPipeline renderPipeline, TextureAtlasSprite sprite, GuiSpriteScaling.NineSlice nineSlice,
            float x, float y, float width, float height, int color
    ) {
        var border = nineSlice.border();
        int borderLeft = (int) Math.min(border.left(), width / 2);
        int borderRight = (int) Math.min(border.right(), width / 2);
        int borderTop = (int) Math.min(border.top(), height / 2);
        int borderBottom = (int) Math.min(border.bottom(), height / 2);
        var sw = nineSlice.width();
        var sh = nineSlice.height();
        if (width == nineSlice.width() && height == nineSlice.height()) {
            this.blitSprite(renderPipeline, sprite, sw, sh, 0, 0, x, y, width, height, color);
        } else if (height == nineSlice.height()) {
            this.blitSprite(renderPipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, borderLeft, height, color);
            this.blitNineSliceInnerSegment(
                    renderPipeline, nineSlice, sprite,
                    x + borderLeft, y,
                    width - borderRight - borderLeft,
                    height,
                    borderLeft,
                    0,
                    nineSlice.width() - borderRight - borderLeft,
                    nineSlice.height(),
                    nineSlice.width(),
                    nineSlice.height(),
                    color
            );
            this.blitSprite(
                    renderPipeline,
                    sprite,
                    nineSlice.width(),
                    nineSlice.height(),
                    nineSlice.width() - borderRight,
                    0,
                    x + width - borderRight,
                    y,
                    borderRight,
                    height,
                    color
            );
        } else if (width == nineSlice.width()) {
            this.blitSprite(renderPipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, width, borderTop, color);
            this.blitNineSliceInnerSegment(
                    renderPipeline,
                    nineSlice,
                    sprite,
                    x,
                    y + borderTop,
                    width,
                    height - borderBottom - borderTop,
                    0,
                    borderTop,
                    nineSlice.width(),
                    nineSlice.height() - borderBottom - borderTop,
                    nineSlice.width(),
                    nineSlice.height(),
                    color
            );
            this.blitSprite(
                    renderPipeline,
                    sprite,
                    nineSlice.width(),
                    nineSlice.height(),
                    0,
                    nineSlice.height() - borderBottom,
                    x,
                    y + height - borderBottom,
                    width,
                    borderBottom,
                    color
            );
        } else {
            this.blitSprite(renderPipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, borderLeft, borderTop, color);
            this.blitNineSliceInnerSegment(
                    renderPipeline,
                    nineSlice,
                    sprite,
                    x + borderLeft,
                    y,
                    width - borderRight - borderLeft,
                    borderTop,
                    borderLeft,
                    0,
                    nineSlice.width() - borderRight - borderLeft,
                    borderTop,
                    nineSlice.width(),
                    nineSlice.height(),
                    color
            );
            this.blitSprite(
                    renderPipeline,
                    sprite,
                    nineSlice.width(),
                    nineSlice.height(),
                    nineSlice.width() - borderRight,
                    0,
                    x + width - borderRight,
                    y,
                    borderRight,
                    borderTop,
                    color
            );
            this.blitSprite(
                    renderPipeline,
                    sprite,
                    nineSlice.width(),
                    nineSlice.height(),
                    0,
                    nineSlice.height() - borderBottom,
                    x,
                    y + height - borderBottom,
                    borderLeft,
                    borderBottom,
                    color
            );
            this.blitNineSliceInnerSegment(
                    renderPipeline,
                    nineSlice,
                    sprite,
                    x + borderLeft,
                    y + height - borderBottom,
                    width - borderRight - borderLeft,
                    borderBottom,
                    borderLeft,
                    nineSlice.height() - borderBottom,
                    nineSlice.width() - borderRight - borderLeft,
                    borderBottom,
                    nineSlice.width(),
                    nineSlice.height(),
                    color
            );
            this.blitSprite(
                    renderPipeline,
                    sprite,
                    nineSlice.width(),
                    nineSlice.height(),
                    nineSlice.width() - borderRight,
                    nineSlice.height() - borderBottom,
                    x + width - borderRight,
                    y + height - borderBottom,
                    borderRight,
                    borderBottom,
                    color
            );
            this.blitNineSliceInnerSegment(
                    renderPipeline,
                    nineSlice,
                    sprite,
                    x,
                    y + borderTop,
                    borderLeft,
                    height - borderBottom - borderTop,
                    0,
                    borderTop,
                    borderLeft,
                    nineSlice.height() - borderBottom - borderTop,
                    nineSlice.width(),
                    nineSlice.height(),
                    color
            );
            this.blitNineSliceInnerSegment(
                    renderPipeline,
                    nineSlice,
                    sprite,
                    x + borderLeft,
                    y + borderTop,
                    width - borderRight - borderLeft,
                    height - borderBottom - borderTop,
                    borderLeft,
                    borderTop,
                    nineSlice.width() - borderRight - borderLeft,
                    nineSlice.height() - borderBottom - borderTop,
                    nineSlice.width(),
                    nineSlice.height(),
                    color
            );
            this.blitNineSliceInnerSegment(
                    renderPipeline,
                    nineSlice,
                    sprite,
                    x + width - borderRight,
                    y + borderTop,
                    borderRight,
                    height - borderBottom - borderTop,
                    nineSlice.width() - borderRight,
                    borderTop,
                    borderRight,
                    nineSlice.height() - borderBottom - borderTop,
                    nineSlice.width(),
                    nineSlice.height(),
                    color
            );
        }
    }

    private void blitNineSliceInnerSegment(
            RenderPipeline renderPipeline, GuiSpriteScaling.NineSlice nineSlice, TextureAtlasSprite sprite,
            float x, float y, float width, float height,
            int textureX, int textureY, int textureWidth, int textureHeight,
            int spriteWidth,
            int spriteHeight,
            int color
    ) {
        if (width > 0 && height > 0) {
            if (nineSlice.stretchInner()) {
                this.innerBlit(
                        renderPipeline,
                        sprite.atlasLocation(),
                        x,
                        x + width,
                        y,
                        y + height,
                        sprite.getU((float)textureX / spriteWidth),
                        sprite.getU((float)(textureX + textureWidth) / spriteWidth),
                        sprite.getV((float)textureY / spriteHeight),
                        sprite.getV((float)(textureY + textureHeight) / spriteHeight),
                        color
                );
            } else {
                this.blitTiledSprite(
                        renderPipeline, sprite, x, y, width, height, textureX, textureY, textureWidth, textureHeight, spriteWidth, spriteHeight, color
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void submitTiledBlit(
            RenderPipeline pipeline,
            GpuTextureView textureView,
            GpuSampler sampler,
            float tileWidth, float tileHeight,
            float x0, float y0, float x1, float y1, float u0, float u1, float v0, float v1, int color
    ) {
        submitGuiElement(
                new FloatTiledBlitRenderState(
                        pipeline,
                        TextureSetup.singleTexture(textureView, sampler),
                        pose.copyPose(),
                        tileWidth,
                        tileHeight,
                        x0, y0, x1, y1, u0, u1, v0, v1, ColorUtils.mulColor(color, elementColor), graphics.peekScissorStack()
                )
        );
    }

    @OnlyIn(Dist.CLIENT)
    private void innerBlit(
            RenderPipeline renderPipeline, Identifier location, float x0, float x1, float y0, float y1, float u0, float u1, float v0, float v1, int color
    ) {
        var texture = mc.getTextureManager().getTexture(location);
        submitGuiElement(
                new FloatBlitRenderState(
                        renderPipeline,
                        TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler()),
                        pose.copyPose(),
                        x0, y0, x1, y1, u0, u1, v0, v1, ColorUtils.mulColor(color, elementColor), graphics.peekScissorStack()
                )
        );
    }

    @OnlyIn(Dist.CLIENT)
    public static GuiSpriteScaling getSpriteScaling(TextureAtlasSprite sprite) {
        return sprite.contents().getAdditionalMetadata(GuiMetadataSection.TYPE).orElse(GuiMetadataSection.DEFAULT).scaling();
    }

    @OnlyIn(Dist.CLIENT)
    public void fillRoundedRect(float x, float y, float w, float h, Vector4f radius, int color) {
        this.submitGuiElement(
                new FloatRoundedRectRenderState(
                        LDLibRenderPipelines.ROUNDED_RECT,
                        TextureSetup.noTexture(),
                        this.pose.copyPose(),
                        x, y, w, h,
                        radius.x, radius.y, radius.z, radius.w,
                        ColorUtils.mulColor(color, elementColor),
                        0f,
                        graphics.peekScissorStack()
                )
        );
    }

    @OnlyIn(Dist.CLIENT)
    public void borderRoundedRect(float x, float y, float w, float h, Vector4f radius, float border, int borderColor) {
        this.submitGuiElement(
                new FloatRoundedRectRenderState(
                        LDLibRenderPipelines.ROUNDED_RECT,
                        TextureSetup.noTexture(),
                        this.pose.copyPose(),
                        x, y, w, h,
                        radius.x, radius.y, radius.z, radius.w,
                        ColorUtils.mulColor(borderColor, elementColor),
                        border,
                        graphics.peekScissorStack()
                )
        );
    }


    // endregion
}
