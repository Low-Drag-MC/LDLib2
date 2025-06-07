package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

import java.util.function.IntSupplier;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR;

@Deprecated
@NoArgsConstructor
public class ResourceTexture extends TransformTexture {

    @Configurable(name = "ldlib.gui.editor.name.resource", forceUpdate = false)
    public ResourceLocation imageLocation = LDLib2.id("textures/gui/icon.png");

    @Configurable
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 0.02)
    public float offsetX = 0;

    @Configurable
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 0.02)
    public float offsetY = 0;

    @Configurable
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 0.02)
    public float imageWidth = 1;
    @Configurable
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 0.02)
    public float imageHeight = 1;

    @Configurable
    @ConfigColor
    protected int color = -1;
    protected IntSupplier dynamicColor = () -> color;

    public ResourceTexture(ResourceLocation imageLocation, float offsetX, float offsetY, float width, float height) {
        this.imageLocation = imageLocation;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.imageWidth = width;
        this.imageHeight = height;
    }

    public ResourceTexture(String imageLocation) {
        this(ResourceLocation.parse(imageLocation));
    }

    public ResourceTexture(ResourceLocation imageLocation) {
        this(imageLocation, 0, 0, 1, 1);
    }

    public ResourceTexture getSubTexture(float offsetX, float offsetY, float width, float height) {
        return new ResourceTexture(imageLocation,
                this.offsetX + (imageWidth * offsetX),
                this.offsetY + (imageHeight * offsetY),
                this.imageWidth * width,
                this.imageHeight * height);
    }

    public ResourceTexture getSubTexture(double offsetX, double offsetY, double width, double height) {
        return new ResourceTexture(imageLocation,
                this.offsetX + (float)(imageWidth * offsetX),
                this.offsetY + (float)(imageHeight * offsetY),
                this.imageWidth * (float) width,
                this.imageHeight * (float)height);
    }

    public ResourceTexture copy() {
        return getSubTexture(0, 0, 1, 1);
    }

    public ResourceTexture setColor(int color) {
        this.color = color;
        return this;
    }

    public ResourceTexture setDynamicColor(IntSupplier color) {
        this.dynamicColor = color;
        return this;
    }

    public static ResourceTexture fromSpirit(ResourceLocation texture) {
        if (LDLib2.isClient()) {
            var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
            return new ResourceTexture(TextureAtlas.LOCATION_BLOCKS, sprite.getU0(), sprite.getV0(), sprite.getU1() - sprite.getU0(), sprite.getV1() - sprite.getV0());
        } else {
            return new ResourceTexture("");
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        drawSubAreaInternal(graphics, x, y, width, height, 0, 0, 1, 1, partialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    protected void drawSubAreaInternal(GuiGraphics graphics, float x, float y, float width, float height, float drawnU, float drawnV, float drawnWidth, float drawnHeight, float partialTicks) {
        //sub area is just different width and height
        float imageU = this.offsetX + (this.imageWidth * drawnU);
        float imageV = this.offsetY + (this.imageHeight * drawnV);
        float imageWidth = this.imageWidth * drawnWidth;
        float imageHeight = this.imageHeight * drawnHeight;
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tessellator.begin(VertexFormat.Mode.QUADS, POSITION_TEX_COLOR);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, imageLocation);
        var matrix4f = graphics.pose().last().pose();
        var color = dynamicColor.getAsInt();
        bufferbuilder.addVertex(matrix4f, x, y + height, 0).setUv(imageU, imageV + imageHeight).setColor(color);
        bufferbuilder.addVertex(matrix4f, x + width, y + height, 0).setUv(imageU + imageWidth, imageV + imageHeight).setColor(color);
        bufferbuilder.addVertex(matrix4f, x + width, y, 0).setUv(imageU + imageWidth, imageV).setColor(color);
        bufferbuilder.addVertex(matrix4f, x, y, 0).setUv(imageU, imageV).setColor(color);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
    }

}
