package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.GuiTexturePreviewHelper;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.ILDLRegisterClient;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Supplier;

@KJSBindings
public interface IGuiTexture extends IPersistedSerializable, IConfigurable, ILDLRegisterClient<IGuiTexture, Supplier<IGuiTexture>> {
    //region builtin textures
    @LDLRegisterClient(name = "empty", registry = "ldlib2:gui_texture", environment = RegistrationEnvironment.MANUAL)
    final class EmptyTexture implements IGuiTexture {
        @Override
        public IGuiTexture copy() { return EMPTY; }
    }

    @LDLRegisterClient(name = "missing", registry = "ldlib2:gui_texture", environment = RegistrationEnvironment.MANUAL)
    final class MissingTexture implements IGuiTexture {
        @Override
        public IGuiTexture copy() { return MISSING_TEXTURE; }
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
        return InterpolatedTexture.of(getRawTexture().copy(), other.getRawTexture().copy(), lerp);
    }

    // ***************** EDITOR  ***************** //
    default void createPreview(ConfiguratorGroup father) {
        GuiTexturePreviewHelper.createPreview(this, father);
    }

    @Override
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
