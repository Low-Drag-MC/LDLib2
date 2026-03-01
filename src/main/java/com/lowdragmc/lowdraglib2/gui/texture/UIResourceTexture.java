package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.editor.resource.BuiltinPath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@KJSBindings
@LDLRegisterClient(name = "ui_resource_texture", registry = "ldlib2:gui_texture")
@NoArgsConstructor
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class UIResourceTexture extends TransformTexture {
    @Persisted
    @Getter
    private IResourcePath resourcePath = new BuiltinPath("");
    @Getter(lazy = true)
    private final IGuiTexture internalTexture = getTextureFromResource();

    public UIResourceTexture(IResourcePath resourcePath) {
        this.resourcePath = resourcePath;
    }

    private IGuiTexture getTextureFromResource() {
        var result = Optional.ofNullable(TexturesResource.INSTANCE.getResourceInstance().getResource(resourcePath))
                .orElse(IGuiTexture.MISSING_TEXTURE);
        // prevent infinite loop
        return result == this ? IGuiTexture.MISSING_TEXTURE : result;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GUIContext context, float x, float y, float width, float height) {
        getInternalTexture().draw(context, x, y, width, height);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        getInternalTexture().draw(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
    }

    @Override
    public IGuiTexture copy() {
        return this;
    }

    @Override
    public IGuiTexture setColor(int color) {
        return getInternalTexture().copy().setColor(color);
    }

    @Override
    public IGuiTexture getRawTexture() {
        return getInternalTexture().getRawTexture();
    }
}
