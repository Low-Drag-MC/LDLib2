package com.lowdragmc.lowdraglib.gui.texture;

import com.lowdragmc.lowdraglib.LDLibRegistries;
import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.editor.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.registry.ILDLRegisterClient;
import com.lowdragmc.lowdraglib.utils.PersistedParser;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtOps;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;

import java.util.function.Supplier;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX;

public interface IGuiTexture extends IConfigurable, ILDLRegisterClient<IGuiTexture, Supplier<IGuiTexture>> {
    IGuiTexture EMPTY = new IGuiTexture() {
        @Override
        public IGuiTexture copy() {
            return this;
        }

        @OnlyIn(Dist.CLIENT)
        @Override
        public void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {

        }
    };

    IGuiTexture MISSING_TEXTURE = new IGuiTexture() {
        @Override
        public IGuiTexture copy() {
            return this;
        }

        @OnlyIn(Dist.CLIENT)
        @Override
        public void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tessellator.begin(VertexFormat.Mode.QUADS, POSITION_TEX);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, TextureManager.INTENTIONAL_MISSING_TEXTURE);
            var matrix4f = graphics.pose().last().pose();
            bufferbuilder.addVertex(matrix4f, x, y + height, 0).setUv(0, 1);
            bufferbuilder.addVertex(matrix4f, x + width, y + height, 0).setUv(1, 1);
            bufferbuilder.addVertex(matrix4f, x + width, y, 0).setUv(1, 0);
            bufferbuilder.addVertex(matrix4f, x, y, 0).setUv(0, 0);
            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        }
    };

    Codec<IGuiTexture> CODEC = LDLibRegistries.GUI_TEXTURES.optionalCodec().dispatch(ILDLRegisterClient::getRegistryHolderOptional,
            optional -> optional.map(holder -> PersistedParser.createCodec(holder.value()).fieldOf("data"))
                    .orElseGet(() -> MapCodec.unit(MISSING_TEXTURE)));

    default IGuiTexture setColor(int color){
        return this;
    }

    default IGuiTexture rotate(float degree) {
        return this;
    }

    default IGuiTexture scale(float scale) {
        return this;
    }

    default IGuiTexture transform(int xOffset, int yOffset) {
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height);

    @OnlyIn(Dist.CLIENT)
    default void updateTick() { }

    @OnlyIn(Dist.CLIENT)
    default void drawSubArea(GuiGraphics graphics, float x, float y, float width, float height, float drawnU, float drawnV, float drawnWidth, float drawnHeight) {
        draw(graphics, 0, 0, x, y, (int) width, (int) height);
    }

    default IGuiTexture copy() {
        return CODEC.encodeStart(NbtOps.INSTANCE, this).result().map(tag -> CODEC.parse(NbtOps.INSTANCE, tag).result()
                .orElse(IGuiTexture.MISSING_TEXTURE))
                .orElse(IGuiTexture.MISSING_TEXTURE);
    }

    // ***************** EDITOR  ***************** //
    default void createPreview(ConfiguratorGroup father) {
        father.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview",
                new ImageWidget(0, 0, 100, 100, this)
                        .setBorder(2, ColorPattern.T_WHITE.color)));
    }

    @Override
    default void buildConfigurator(ConfiguratorGroup father) {
        createPreview(father);
        IConfigurable.super.buildConfigurator(father);
    }

    default void setUIResource(Resource<IGuiTexture> texturesResource) {

    }
}
