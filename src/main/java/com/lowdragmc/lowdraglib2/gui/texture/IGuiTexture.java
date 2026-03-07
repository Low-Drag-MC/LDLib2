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
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.vfyjxf.taffy.style.AlignItems;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Supplier;

@KJSBindings
@FunctionalInterface
public interface IGuiTexture extends IPersistedSerializable, IConfigurable, ILDLRegisterClient<IGuiTexture, Supplier<IGuiTexture>> {
    //region builtin textures
    @LDLRegisterClient(name = "empty", registry = "ldlib2:gui_texture", manual = true)
    final class EmptyTexture implements IGuiTexture {
        @Override
        public IGuiTexture copy() { return EMPTY; }

        @OnlyIn(Dist.CLIENT)
        @Override
        public void draw(GUIContext context, float x, float y, float width, float height) {}
    }

    @LDLRegisterClient(name = "missing", registry = "ldlib2:gui_texture", manual = true)
    final class MissingTexture implements IGuiTexture {
        @Override
        public IGuiTexture copy() { return MISSING_TEXTURE; }

        @OnlyIn(Dist.CLIENT)
        @Override
        public void draw(GUIContext context, float x, float y, float width, float height) {
            context.blitSprite(RenderPipelines.GUI_TEXTURED, context.graphics.guiSprites.missingSprite(),
                    x, y, width, height, -1
            );
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
            return MapCodec.unitCodec(MISSING_TEXTURE);
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
        return (context, x, y, width, height) -> {
            IGuiTexture.this.getRawTexture().copy().draw(context, x, y, width, height);
            other.getRawTexture().copy().setColor(ColorUtils.color(lerp, lerp, lerp, lerp))
                    .draw(context, x, y, width, height);
        };
    }

    void draw(GUIContext context, float x, float y, float width, float height);

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
    static Identifier getTextureFromFile(File filePath) {
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
            return Identifier.parse(location);
        }
        return null;
    }
}
