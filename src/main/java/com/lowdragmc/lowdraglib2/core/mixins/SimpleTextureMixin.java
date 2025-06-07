package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.gui.texture.ITextureSize;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SimpleTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleTexture.class)
public class SimpleTextureMixin implements ITextureSize {
    @Unique
    public int ldlib2$imageWidth;
    @Unique
    public int ldlib2$imageHeight;

    @Inject(method = "doLoad", at = @At(value = "HEAD"))
    private void ldlib2$recordImageSize(NativeImage image, boolean blur, boolean clamp, CallbackInfo ci) {
        this.ldlib2$imageWidth = image.getWidth();
        this.ldlib2$imageHeight = image.getHeight();
    }

    @Override
    public int ldlib2$getImageWidth() {
        return ldlib2$imageWidth;
    }

    @Override
    public int ldlib2$getImageHeight() {
        return ldlib2$imageHeight;
    }
}
