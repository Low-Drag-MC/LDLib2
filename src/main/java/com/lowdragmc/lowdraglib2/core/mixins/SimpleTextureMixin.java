package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.gui.texture.ITextureSize;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleTexture.class)
public abstract class SimpleTextureMixin implements ITextureSize {
    @Unique
    public int ldlib2$imageWidth;
    @Unique
    public int ldlib2$imageHeight;

    @Inject(method = "loadContents", at = @At(value = "RETURN"))
    private void ldlib2$recordImageSize(ResourceManager resourceManager, CallbackInfoReturnable<TextureContents> cir) {
        var image = cir.getReturnValue().image();
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
