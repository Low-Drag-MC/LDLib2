package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.editor.resource.BuiltinPath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
        return result == this ? IGuiTexture.MISSING_TEXTURE : result;
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

    @LDLRegisterClient(name = "ui_resource_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredUIResourceTextureRenderer implements RegisteredGuiTextureRenderer<UIResourceTexture, RegisteredUIResourceTextureRenderer> {
        @Override
        public Class<UIResourceTexture> type() {
            return UIResourceTexture.class;
        }

        @Override
        public void draw(UIResourceTexture texture, GUIContext context, float x, float y, float width, float height) {
            TransformTextureRenderer.draw(texture, context, x, y, width, height, this::drawInternal);
        }

        private void drawInternal(UIResourceTexture texture, GUIContext context, float x, float y, float width, float height) {
            context.drawTexture(texture.getInternalTexture(), x, y, width, height);
        }
    }
}
