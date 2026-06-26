package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderPipelines;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.GuiTextureClientRenderers;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.GuiTextureRendererRegistry;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ColorSelector.ColorSelectorClientTextures;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList.TreeListClientTextures;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.gui.util.UISoundPlayerClient;
import com.lowdragmc.lowdraglib2.gui.texture.renderstate.FloatBlitRenderState;
import com.lowdragmc.lowdraglib2.gui.texture.renderstate.FloatColoredRectangleRenderState;
import com.lowdragmc.lowdraglib2.gui.texture.renderstate.FloatColoredTriangleRenderState;
import com.lowdragmc.lowdraglib2.gui.texture.renderstate.FloatRoundedRectRenderState;
import com.lowdragmc.lowdraglib2.gui.texture.renderstate.FloatTiledBlitRenderState;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

public class GUIContext implements IGUIContext {
    public GuiGraphicsExtractor graphics;
    public int mouseX, mouseY;
    public float partialTick;
    public EnhancedPoseStack pose;
    public Minecraft mc;

    // runtime
    public boolean refreshLocalMouse = true;
    /**
     * Current element tint color (ARGB), set by UIElement before drawing its background/overlay textures.
     * -1 (0xFFFFFFFF) means no tint. Textures read this to multiply (per-channel) with their own color.
     */
    public int elementColor = -1;
    public float localMouseX, localMouseY;
    private final Deque<VisualLayerFrame> visualLayers = new ArrayDeque<>();
    private final List<PostCall> postRenderingCalls = new ArrayList<>();

    private record PostCall(Consumer<GUIContext> call, Matrix3x2f pose) {}

    private record VisualLayerFrame(
            UIElement element,
            GuiGraphicsExtractor savedGraphics,
            EnhancedPoseStack savedPose,
            GuiRenderState subState
    ) {}

    public static GUIContext of(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        UIElementClientRenderers.init();
        GuiTextureClientRenderers.init();
        ColorSelectorClientTextures.init();
        TreeListClientTextures.init();
        DrawerHelperClient.installSharedHooks();
        UISoundPlayerClient.installSharedHooks();
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

    @Override
    public Matrix3x2f currentPose() {
        return pose.pose;
    }

    @Override
    public void pushTransform(Transform2D transform, UIElement element) {
        transform.pushPose(this, element);
    }

    @Override
    public void popTransform(Transform2D transform) {
        transform.popPose(this);
    }

    @Override
    public boolean isInsideScissor(float minX, float minY, float width, float height) {
        var peek = peekScissor();
        return peek == null || peek.intersects(new ScreenRectangle(
                Mth.floor(minX),
                Mth.floor(minY),
                Mth.ceil(width),
                Mth.ceil(height)
        ));
    }

    public void drawTexture(IGuiTexture texture, float x, float y, float width, float height) {
        GuiTextureRendererRegistry.findRenderer(texture).draw(texture, this, x, y, width, height);
    }

    public void enableScissor(float x, float y, float width, float height) {
        graphics.enableScissor(Mth.floor(x), Mth.floor(y), Mth.ceil(x + width), Mth.ceil(y + height));
    }

    public @Nullable ScreenRectangle peekScissor() {
        return graphics.peekScissorStack();
    }

    public void disableScissor() {
        graphics.disableScissor();
    }

    public void refreshLocalMouse() {
        var realMouse = pose.pose.invert(new Matrix3x2f()).transformPosition(new Vector2f(mouseX, mouseY));
        localMouseX = realMouse.x;
        localMouseY = realMouse.y;
    }

    @Override
    public void pushVisualLayer(UIElement element) {
        // Swap in a fresh sub GuiRenderState so subsequent draws by this element's
        // subtree accumulate in isolation. On pop, the captured state is attached
        // to a VisualLayerPipState which renders to an off-target and composites
        // back with opacity / mask.
        var savedGraphics = this.graphics;
        var savedPose = this.pose;

        var subState = new GuiRenderState();
        var subGraphics = new GuiGraphicsExtractor(mc, subState, mouseX, mouseY);
        this.graphics = subGraphics;
        this.pose = new EnhancedPoseStack(subGraphics.pose()).setOnTransform(this::refreshLocalMouse);
        // Mirror the outer screen-space pose so child draws record their natural
        // screen coords into the sub-state. The sub-renderer's ortho is set up
        // off-target = full screen, so screen coords map straight to off-target.
        this.pose.pose.set(savedPose.pose);

        visualLayers.push(new VisualLayerFrame(element, savedGraphics, savedPose, subState));
        refreshLocalMouse();
    }

    @Override
    public void popVisualLayer() {
        var frame = visualLayers.pop();
        var savedScissor = frame.savedGraphics().peekScissorStack();

        // Restore outer graphics + pose before attaching the PIP state
        this.graphics = frame.savedGraphics();
        this.pose = frame.savedPose();
        refreshLocalMouse();

        var style = frame.element().getStyle();
        float opacity = style.opacity();
        IGuiTexture mask = style.mask();
        IGuiTexture effectiveMask = (style.clip().isMask() && mask != null && mask != IGuiTexture.EMPTY) ? mask : null;

        // Off-target spans the full window in logical px — matches the sub
        // GuiRenderer's main-window ortho, so recorded screen-coords render at
        // their original positions. The outer blit is also full-screen; areas
        // outside the element's subtree start cleared (alpha 0) and stay invisible.
        var window = mc.getWindow();
        int screenW = window.getGuiScaledWidth();
        int screenH = window.getGuiScaledHeight();

        var element = frame.element();
        var pipState = new VisualLayerPipState(
                frame.subState(),
                0, 0, screenW, screenH,
                opacity,
                effectiveMask,
                element.getPositionX(), element.getPositionY(),
                element.getSizeWidth(), element.getSizeHeight(),
                style.clip().isDynamicMask(),
                new Matrix3x2f(),
                new Matrix3x2f(frame.savedPose().pose),
                savedScissor
        );
        this.graphics.guiRenderState.addPicturesInPictureState(pipState);
    }

    public void setElementColor(int elementColor) {
        if (this.elementColor == elementColor) return;
        this.elementColor = elementColor;
    }

    public void resetElementColor() {
        if (this.elementColor == -1) return;
        this.elementColor = -1;
    }

    @Override
    public int getElementColor() {
        return elementColor;
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

    public GuiRenderState getRenderState() {
        return graphics.guiRenderState;
    }

    public void addItem(GuiItemRenderState itemState) {
        graphics.guiRenderState.addItem(itemState);
    }

    public void addText(GuiTextRenderState textState) {
        graphics.guiRenderState.addText(textState);
    }

    public void addPicturesInPictureState(PictureInPictureRenderState picturesInPictureState) {
        graphics.guiRenderState.addPicturesInPictureState(picturesInPictureState);
    }

    public void addGuiElement(GuiElementRenderState blitState) {
        graphics.guiRenderState.addGuiElement(blitState);
    }

    public void fill(
            RenderPipeline renderPipeline, float x0, float y0, float x1, float y1,
            int colorU0V0, int colorU0V1, int colorU1V1, int colorU1V0
    ) {
        this.addGuiElement(
                new FloatColoredRectangleRenderState(
                        renderPipeline, TextureSetup.noTexture(), this.pose.copyPose(), x0, y0, x1, y1,
                        ColorUtils.mulColor(colorU0V0, elementColor), ColorUtils.mulColor(colorU0V1, elementColor), ColorUtils.mulColor(colorU1V1, elementColor), ColorUtils.mulColor(colorU1V0, elementColor), graphics.peekScissorStack()
                )
        );
    }

    public void fillTriangle(
            RenderPipeline renderPipeline,
            Vector2f position0, Vector2f position1, Vector2f position2,
            int color0, int color1, int color2
    ) {
        this.addGuiElement(
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

    public void fillTriangle(
            RenderPipeline renderPipeline,
            Vector2f position0, Vector2f position1, Vector2f position2,
            int color
    ) {
        this.fillTriangle(renderPipeline, position0, position1, position2, color, color, color);
    }

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
                u / textureWidth,
                (u + srcWidth) / textureWidth,
                v / textureHeight,
                (v + srcHeight) / textureHeight,
                color
        );
    }

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

    public void blitSprite(RenderPipeline renderPipeline, Identifier location,
                           float x, float y, float width, float height, int color) {
        var sprite = graphics.guiSprites.getSprite(location);
        var scaling = getSpriteScaling(sprite);
        switch (scaling) {
            case GuiSpriteScaling.Stretch stretch ->
                    this.blitSprite(renderPipeline, sprite, x, y, width, height, color);
            case GuiSpriteScaling.Tile tile ->
                    this.blitTiledSprite(renderPipeline, sprite, x, y, width, height, 0, 0, tile.width(), tile.height(), tile.width(), tile.height(), color);
            case GuiSpriteScaling.NineSlice nineSlice ->
                    this.blitNineSlicedSprite(renderPipeline, sprite, nineSlice, x, y, width, height, color);
            default -> {
            }
        }
    }

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
                        sprite.getU((float) textureX / spriteWidth),
                        sprite.getU((float) (textureX + textureWidth) / spriteWidth),
                        sprite.getV((float) textureY / spriteHeight),
                        sprite.getV((float) (textureY + textureHeight) / spriteHeight),
                        color
                );
            } else {
                this.blitTiledSprite(
                        renderPipeline, sprite, x, y, width, height, textureX, textureY, textureWidth, textureHeight, spriteWidth, spriteHeight, color
                );
            }
        }
    }

    private void submitTiledBlit(
            RenderPipeline pipeline,
            GpuTextureView textureView,
            GpuSampler sampler,
            float tileWidth, float tileHeight,
            float x0, float y0, float x1, float y1, float u0, float u1, float v0, float v1, int color
    ) {
        addGuiElement(
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

    public void innerBlit(
            RenderPipeline renderPipeline, Identifier location, float x0, float x1, float y0, float y1, float u0, float u1, float v0, float v1, int color
    ) {
        var texture = mc.getTextureManager().getTexture(location);
        addGuiElement(
                new FloatBlitRenderState(
                        renderPipeline,
                        TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler()),
                        pose.copyPose(),
                        x0, y0, x1, y1, u0, u1, v0, v1, ColorUtils.mulColor(color, elementColor), graphics.peekScissorStack()
                )
        );
    }

    public static GuiSpriteScaling getSpriteScaling(TextureAtlasSprite sprite) {
        return sprite.contents().getAdditionalMetadata(GuiMetadataSection.TYPE).orElse(GuiMetadataSection.DEFAULT).scaling();
    }

    public void fillRoundedRect(float x, float y, float w, float h, Vector4f radius, int color) {
        this.addGuiElement(
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

    public void borderRoundedRect(float x, float y, float w, float h, Vector4f radius, float border, int borderColor) {
        this.addGuiElement(
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
