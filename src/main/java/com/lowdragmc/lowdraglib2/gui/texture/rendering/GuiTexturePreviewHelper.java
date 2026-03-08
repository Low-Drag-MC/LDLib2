package com.lowdragmc.lowdraglib2.gui.texture.rendering;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.AlignItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GuiTexturePreviewHelper {
    private GuiTexturePreviewHelper() {
    }

    public static void createPreview(IGuiTexture texture, ConfiguratorGroup father) {
        var preview = new UIElement().layout(layout -> {
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
                }).style(style -> style.backgroundTexture(texture)));
        father.addConfigurators(new Configurator("ldlib.gui.editor.group.preview").addChild(preview));
    }
}
