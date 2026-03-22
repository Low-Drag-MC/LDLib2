package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.ILDLRegisterClient;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.vfyjxf.taffy.style.AlignItems;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Supplier;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX;

@KJSBindings
@FunctionalInterface
public interface IGuiTexture extends IPersistedSerializable, IConfigurable, ILDLRegisterClient<IGuiTexture, Supplier<IGuiTexture>> {
    //region builtin textures
    @LDLRegisterClient(name = "empty", registry = "ldlib2:gui_texture", environment = RegistrationEnvironment.MANUAL)
    final class EmptyTexture implements IGuiTexture {
        @Override
        public IGuiTexture copy() { return EMPTY; }

        @OnlyIn(Dist.CLIENT)
        @Override
        public void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {}
    }

    @LDLRegisterClient(name = "missing", registry = "ldlib2:gui_texture", environment = RegistrationEnvironment.MANUAL)
    final class MissingTexture implements IGuiTexture {
        @Override
        public IGuiTexture copy() { return MISSING_TEXTURE; }

        @OnlyIn(Dist.CLIENT)
        @Override
        public void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tessellator.begin(VertexFormat.Mode.QUADS, POSITION_TEX);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, MissingTextureAtlasSprite.getTexture().getId());
            var matrix4f = graphics.pose().last().pose();
            bufferbuilder.addVertex(matrix4f, x, y + height, 0).setUv(0, 1);
            bufferbuilder.addVertex(matrix4f, x + width, y + height, 0).setUv(1, 1);
            bufferbuilder.addVertex(matrix4f, x + width, y, 0).setUv(1, 0);
            bufferbuilder.addVertex(matrix4f, x, y, 0).setUv(0, 0);
            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        }
    }
    //endregion

    EmptyTexture EMPTY = new EmptyTexture();
    MissingTexture MISSING_TEXTURE = new MissingTexture();

    Codec<IGuiTexture> CODEC = createCodec();
    static Codec<IGuiTexture> createCodec() {
        if (LDLib2.isClient()) {
            return LDLib2Registries.GUI_TEXTURES.optionalCodec().dispatch(ILDLRegisterClient::getRegistryHolderOptional,
                    optional -> optional.map(holder -> PersistedParser.createCodec(holder.value()).fieldOf("data"))
                            .orElseGet(() -> MapCodec.unit(MISSING_TEXTURE)));
        } else {
            return Codec.unit(MISSING_TEXTURE);
        }
    }

    static DynamicTexture dynamic(Supplier<IGuiTexture> textureSupplier) {
        return DynamicTexture.of(textureSupplier);
    }

    static GuiTextureGroup group(IGuiTexture... textures) {
        return GuiTextureGroup.of(textures);
    }

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

    /**
     * Retrieves the raw underlying {@code IGuiTexture} instance without any modifications
     * or transformations applied.
     *
     * @return the raw {@code IGuiTexture} instance, typically itself.
     */
    default IGuiTexture getRawTexture() {
        return this;
    }

    /**
     * Creates a copy of this texture.
     */
    default IGuiTexture copy() {
        try {
            return CODEC.encodeStart(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), this)
                    .result()
                    .map(tag -> CODEC.parse(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), tag).result()
                            .orElse(this))
                    .orElse(this);
        } catch (Exception e) {
            return this;
        }
    }

    /**
     * Creates a new interpolated {@code IGuiTexture} by merging this texture with another texture.
     * The interpolation is controlled by the {@code lerp} parameter.
     *
     * @param other the {@code IGuiTexture} to interpolate with; represents the target texture.
     * @param lerp  the interpolation factor between 0.0 and 1.0, where 0.0 represents this texture
     *              and 1.0 represents the {@code other} texture.
     * @return a new {@code IGuiTexture} that represents the interpolated texture.
     */
    default IGuiTexture interpolate(IGuiTexture other, float lerp) {
        return new IGuiTexture() {
            @Override
            @OnlyIn(Dist.CLIENT)
            public void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
                IGuiTexture.this.getRawTexture().copy().draw(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
                other.getRawTexture().copy().setColor(ColorUtils.color(lerp, lerp, lerp, lerp))
                        .draw(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
            }
        };
    }

    @OnlyIn(Dist.CLIENT)
    void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks);

    @OnlyIn(Dist.CLIENT)
    default void draw(GUIContext context, float x, float y, float width, float height) {
        draw(context.graphics, context.localMouseX, context.localMouseY, x, y, width, height, context.partialTick);
    }

    // ***************** EDITOR  ***************** //
    @OnlyIn(Dist.CLIENT)
    default void createPreview(ConfiguratorGroup father) {
        father.addConfigurators(new Configurator("ldlib.gui.editor.group.preview")
                .addChild(new UIElement().layout(layout -> {
                    layout.setPipelineState(StyleOrigin.DEFAULT);
                    layout.setAspectRatio(1.0f);
                    layout.widthPercent(80);
                    layout.alignSelf(AlignItems.CENTER);
                    layout.paddingAll(3);
                    layout.setPipelineState(StyleOrigin.INLINE);
                }).style(style -> Style.defaultPipeline(style, s -> s.backgroundTexture(Sprites.BORDER1_RT1)))
                        .addClass("preview_bg")
                        .addChild(new UIElement().layout(layout -> {
                            layout.widthPercent(100);
                            layout.heightPercent(100);
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
