package com.lowdragmc.lowdraglib.gui.texture;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.lowdraglib.gui.editor.runtime.PersistedParser;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.core.HolderLookup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.function.Function;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX;

public interface IGuiTexture extends IConfigurable {

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
    
    IGuiTexture EMPTY = new IGuiTexture() {
        @OnlyIn(Dist.CLIENT)
        @Override
        public void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {

        }
    };

    IGuiTexture MISSING_TEXTURE = new IGuiTexture() {
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

    @OnlyIn(Dist.CLIENT)
    default void drawSubArea(GuiGraphics graphics, float x, float y, float width, float height, float drawnU, float drawnV, float drawnWidth, float drawnHeight) {
        draw(graphics, 0, 0, x, y, (int) width, (int) height);
    }

    default IGuiTexture copy() {
        var tag = serializeWrapper(this);
        return tag == null ? this : deserializeWrapper(tag);
    }

    // ***************** EDITOR  ***************** //

    Function<String, AnnotationDetector.Wrapper<LDLRegister, IGuiTexture>> CACHE = Util.memoize(type -> {
        for (var wrapper : AnnotationDetector.REGISTER_TEXTURES) {
            if (wrapper.annotation().name().equals(type)) {
                return wrapper;
            }
        }
        return null;
    });

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

    @Nullable
    static CompoundTag serializeWrapper(IGuiTexture texture, HolderLookup.Provider provider) {
        if (texture.isLDLRegister()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", texture.name());
            CompoundTag data = new CompoundTag();
            PersistedParser.serializeNBT(data, texture.getClass(), texture, provider);
            tag.put("data", data);
            return tag;
        }
        return null;
    }

    @NotNull
    static IGuiTexture deserializeWrapper(CompoundTag tag, HolderLookup.Provider provider) {
        var type = tag.getString("type");
        var data = tag.getCompound("data");
        var wrapper = CACHE.apply(type);
        IGuiTexture value = wrapper == null ? IGuiTexture.EMPTY : wrapper.creator().get();
        PersistedParser.deserializeNBT(data, new HashMap<>(), value.getClass(), value, provider);
        return value;
    }

    default void setUIResource(Resource<IGuiTexture> texturesResource) {

    }
}
