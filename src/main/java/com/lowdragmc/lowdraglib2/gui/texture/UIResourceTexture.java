package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.editor.resource.BuiltinPath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

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
        return Optional.ofNullable(TexturesResource.INSTANCE.getResourceInstance().getResource(resourcePath))
                .orElse(IGuiTexture.MISSING_TEXTURE);
    }

    @Override
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        getInternalTexture().draw(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
    }

    @Override
    public IGuiTexture copy() {
        return new UIResourceTexture(resourcePath);
    }

}
