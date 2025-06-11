package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.registry.ILDLRegisterClient;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Supplier;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX;

public interface IGuiTexture extends IConfigurable, ILDLRegisterClient<IGuiTexture, Supplier<IGuiTexture>> {
    IGuiTexture MISSING_TEXTURE = new IGuiTexture() {
        @Override
        public IGuiTexture copy() {
            return this;
        }

        @OnlyIn(Dist.CLIENT)
        @Override
        public void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tessellator.begin(VertexFormat.Mode.QUADS, POSITION_TEX);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TextureManager.INTENTIONAL_MISSING_TEXTURE);
            var matrix4f = graphics.pose().last().pose();
            bufferbuilder.addVertex(matrix4f, x, y + height, 0).setUv(0, 1);
            bufferbuilder.addVertex(matrix4f, x + width, y + height, 0).setUv(1, 1);
            bufferbuilder.addVertex(matrix4f, x + width, y, 0).setUv(1, 0);
            bufferbuilder.addVertex(matrix4f, x, y, 0).setUv(0, 0);
            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        }
    };

    Codec<IGuiTexture> CODEC = LDLib2Registries.GUI_TEXTURES.optionalCodec().dispatch(ILDLRegisterClient::getRegistryHolderOptional,
            optional -> optional.map(holder -> PersistedParser.createCodec(holder.value()).fieldOf("data"))
                    .orElseGet(() -> MapCodec.unit(MISSING_TEXTURE)));

    @LDLRegisterClient(name = "empty", registry = "ldlib2:gui_texture", manual = true)
    final class EmptyTexture implements IGuiTexture {
        @Override
        public IGuiTexture copy() { return EMPTY; }

        @OnlyIn(Dist.CLIENT)
        @Override
        public void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {}
    }

    EmptyTexture EMPTY = new EmptyTexture();

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
    @Deprecated
    default void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height) {
        draw(graphics, mouseX, mouseY, x, y, width, height, 0);
    }

    @OnlyIn(Dist.CLIENT)
    void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks);

    default IGuiTexture copy() {
        return CODEC.encodeStart(NbtOps.INSTANCE, this).result().map(tag -> CODEC.parse(NbtOps.INSTANCE, tag).result()
                .orElse(IGuiTexture.MISSING_TEXTURE))
                .orElse(IGuiTexture.MISSING_TEXTURE);
    }

    // ***************** EDITOR  ***************** //
    @OnlyIn(Dist.CLIENT)
    default void createPreview(ConfiguratorGroup father) {
        father.addConfigurators(new Configurator("ldlib.gui.editor.group.preview")
                .addChild(new UIElement().layout(layout -> {
                    layout.setAspectRatio(1.0f);
                    layout.setWidthPercent(80);
                    layout.setAlignSelf(YogaAlign.CENTER);
                    layout.setPadding(YogaEdge.ALL, 3);
                }).style(style -> style.backgroundTexture(Sprites.BORDER1_RT1))
                        .addChild(new UIElement().layout(layout -> {
                            layout.setWidthPercent(100);
                            layout.setHeightPercent(100);
                        }).style(style -> style.backgroundTexture(this)))));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    default void buildConfigurator(ConfiguratorGroup father) {
        createPreview(father);
        IConfigurable.super.buildConfigurator(father);
    }

    @Nullable
    static ResourceLocation getTextureFromFile(File filePath) {
        String fullPath = filePath.getPath().replace('\\', '/');

        // find the "assets/" directory in the path
        var assetsIndex = fullPath.indexOf("assets/");
        if (assetsIndex == -1) {
            return null;
        }

        var relativePath = fullPath.substring(assetsIndex + "assets/".length());

        // find mod_id
        var slashIndex = relativePath.indexOf('/');
        if (slashIndex == -1) {
            return null;
        }

        var modId = relativePath.substring(0, slashIndex);
        var subPath = relativePath.substring(slashIndex + 1);
        var location = modId + ":" + subPath;

        if (LDLib2.isValidResourceLocation(location)) {
            return ResourceLocation.parse(location);
        }
        return null;
    }
}
