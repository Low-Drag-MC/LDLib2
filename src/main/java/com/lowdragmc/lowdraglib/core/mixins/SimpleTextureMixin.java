package com.lowdragmc.lowdraglib.core.mixins;

import com.lowdragmc.lowdraglib.gui.texture.ITextureSize;
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
    public int ldlib$imageWidth;
    @Unique
    public int ldlib$imageHeight;

    @Inject(method = "doLoad", at = @At(value = "HEAD"))
    private void ldlib$recordImageSize(NativeImage image, boolean blur, boolean clamp, CallbackInfo ci) {
        this.ldlib$imageWidth = image.getWidth();
        this.ldlib$imageHeight = image.getHeight();
    }

    @Override
    public int ldlib$getImageWidth() {
        return ldlib$imageWidth;
    }

    @Override
    public int ldlib$getImageHeight() {
        return ldlib$imageHeight;
    }
}
