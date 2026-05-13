package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;

import java.util.function.Supplier;

@KJSBindings
public class DynamicTexture implements IGuiTexture {
    public Supplier<IGuiTexture> textureSupplier;

    public DynamicTexture(Supplier<IGuiTexture> rendererSupplier) {
        this.textureSupplier = rendererSupplier;
    }

    public static DynamicTexture of(Supplier<IGuiTexture> rendererSupplier) {
        return new DynamicTexture(rendererSupplier);
    }

    @LDLRegisterClient(name = "dynamic", registry = "ldlib2:gui_texture_renderer")
    public static final class DynamicTextureRenderer implements RegisteredGuiTextureRenderer<DynamicTexture, DynamicTextureRenderer> {
        @Override
        public Class<DynamicTexture> type() {
            return DynamicTexture.class;
        }

        @Override
        public void draw(DynamicTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            context.drawTexture(texture.textureSupplier.get(), x, y, width, height);
        }
    }
}
