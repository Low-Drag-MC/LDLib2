package com.lowdragmc.lowdraglib.gui.texture;

import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.mojang.datafixers.util.Either;
import lombok.NoArgsConstructor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;

@LDLRegisterClient(name = "ui_resource_texture", registry = "ldlib:gui_texture")
@NoArgsConstructor
public final class UIResourceTexture implements IGuiTexture, IPersistedSerializable {
    public final static ThreadLocal<Resource<IGuiTexture>> RESOURCE = ThreadLocal.withInitial(() -> null);
    private Either<String, File> key;
    @Nullable
    @Persisted
    private IGuiTexture fallbackTexture;

    public UIResourceTexture(@Nullable IGuiTexture fallbackTexture, Either<String, File> key) {
        this.fallbackTexture = fallbackTexture;
        this.key = key;
        this.fallbackTexture = RESOURCE.get() == null ? IGuiTexture.MISSING_TEXTURE : RESOURCE.get().getResourceOrDefault(key, IGuiTexture.MISSING_TEXTURE);
    }

    public IGuiTexture getTexture() {
        var resource = RESOURCE.get();
        return resource == null ? getFallbackTexture() : resource.getResourceOrDefault(key, getFallbackTexture());
    }

    public IGuiTexture getFallbackTexture() {
        return fallbackTexture == null ? IGuiTexture.MISSING_TEXTURE : fallbackTexture;
    }

    @Override
    public IGuiTexture setColor(int color) {
        return getTexture().setColor(color);
    }

    @Override
    public IGuiTexture rotate(float degree) {
        return getTexture().rotate(degree);
    }

    @Override
    public IGuiTexture scale(float scale) {
        return getTexture().scale(scale);
    }

    @Override
    public IGuiTexture transform(int xOffset, int yOffset) {
        return getTexture().transform(xOffset, yOffset);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
        getTexture().draw(graphics, mouseX, mouseY, x, y, width, height);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void updateTick() {
        getTexture().updateTick();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawSubArea(GuiGraphics graphics, float x, float y, float width, float height, float drawnU, float drawnV, float drawnWidth, float drawnHeight) {
        getTexture().drawSubArea(graphics, x, y, width, height, drawnU, drawnV, drawnWidth, drawnHeight);
    }

    @Override
    public void createPreview(ConfiguratorGroup father) {
        getTexture().createPreview(father);
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        getTexture().buildConfigurator(father);
    }

    @Override
    public IGuiTexture copy() {
        return new UIResourceTexture(fallbackTexture, key);
    }

    @Override
    public Tag serializeAdditionalNBT(HolderLookup.@NotNull Provider provider) {
        return key.map(
                l -> {
                    var key = new CompoundTag();
                    key.putString("key", l);
                    key.putString("type", "builtin");
                    return key;
                }, r-> {
                    var key = new CompoundTag();
                    key.putString("key", r.getPath());
                    key.putString("type", "file");
                    return key;
                }
        );
    }

    @Override
    public void deserializeAdditionalNBT(Tag tag, HolderLookup.@NotNull Provider provider) {
        if (tag instanceof CompoundTag compoundTag) {
            var key = compoundTag.getString("key");
            var type = compoundTag.getString("type");
            if (type.equals("file")) {
                this.key = Either.right(new File(key));
            } else  {
                this.key = Either.left(key);
            }
        }
    }
}
