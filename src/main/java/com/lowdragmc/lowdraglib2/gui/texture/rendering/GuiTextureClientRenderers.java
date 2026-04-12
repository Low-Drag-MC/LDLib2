package com.lowdragmc.lowdraglib2.gui.texture.rendering;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.gui.texture.AnimationTexture;
import com.lowdragmc.lowdraglib2.gui.texture.AnimationTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.FluidStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.FluidStackTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroupRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.InterpolatedTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.RectTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTextureInterpolation;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTextureInterpolationRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.VanillaSpriteTexture;
import com.lowdragmc.lowdraglib2.gui.texture.VanillaSpriteTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.UIResourceTexture;
import com.lowdragmc.lowdraglib2.gui.texture.UIResourceTextureRenderer;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GuiTextureClientRenderers {
    private static boolean initialized;

    private GuiTextureClientRenderers() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        GuiTextureRendererBootstrap.applyRegistry(LDLib2Registries.GUI_TEXTURE_RENDERER_ENTRIES);
    }

    @LDLRegisterClient(name = "empty", registry = "ldlib2:gui_texture_renderer")
    public static final class EmptyTextureRenderer implements RegisteredGuiTextureRenderer<IGuiTexture.EmptyTexture, EmptyTextureRenderer> {
        @Override
        public Class<IGuiTexture.EmptyTexture> type() {
            return IGuiTexture.EmptyTexture.class;
        }

        @Override
        public void draw(IGuiTexture.EmptyTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
        }
    }

    @LDLRegisterClient(name = "missing", registry = "ldlib2:gui_texture_renderer")
    public static final class MissingTextureRenderer implements RegisteredGuiTextureRenderer<IGuiTexture.MissingTexture, MissingTextureRenderer> {
        @Override
        public Class<IGuiTexture.MissingTexture> type() {
            return IGuiTexture.MissingTexture.class;
        }

        @Override
        public void draw(IGuiTexture.MissingTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            context.blitSprite(RenderPipelines.GUI_TEXTURED, context.graphics.guiSprites.missingSprite(), x, y, width, height, -1);
        }
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

    @LDLRegisterClient(name = "animation_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredAnimationTextureRenderer implements RegisteredGuiTextureRenderer<AnimationTexture, RegisteredAnimationTextureRenderer> {
        @Override
        public Class<AnimationTexture> type() {
            return AnimationTexture.class;
        }

        @Override
        public void draw(AnimationTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            AnimationTextureRenderer.draw(texture, context, x, y, width, height);
        }
    }

    @LDLRegisterClient(name = "item_stack_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredItemStackTextureRenderer implements RegisteredGuiTextureRenderer<ItemStackTexture, RegisteredItemStackTextureRenderer> {
        @Override
        public Class<ItemStackTexture> type() {
            return ItemStackTexture.class;
        }

        @Override
        public void draw(ItemStackTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            ItemStackTextureRenderer.draw(texture, context, x, y, width, height);
        }
    }

    @LDLRegisterClient(name = "fluid_stack_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredFluidStackTextureRenderer implements RegisteredGuiTextureRenderer<FluidStackTexture, RegisteredFluidStackTextureRenderer> {
        @Override
        public Class<FluidStackTexture> type() {
            return FluidStackTexture.class;
        }

        @Override
        public void draw(FluidStackTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            FluidStackTextureRenderer.draw(texture, context, x, y, width, height);
        }
    }

    @LDLRegisterClient(name = "group_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredGuiTextureGroupRenderer implements RegisteredGuiTextureRenderer<GuiTextureGroup, RegisteredGuiTextureGroupRenderer> {
        @Override
        public Class<GuiTextureGroup> type() {
            return GuiTextureGroup.class;
        }

        @Override
        public void draw(GuiTextureGroup texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            GuiTextureGroupRenderer.draw(texture, context, x, y, width, height);
        }
    }

    @LDLRegisterClient(name = "ui_resource_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredUIResourceTextureRenderer implements RegisteredGuiTextureRenderer<UIResourceTexture, RegisteredUIResourceTextureRenderer> {
        @Override
        public Class<UIResourceTexture> type() {
            return UIResourceTexture.class;
        }

        @Override
        public void draw(UIResourceTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            UIResourceTextureRenderer.draw(texture, context, x, y, width, height);
        }
    }

    @LDLRegisterClient(name = "text_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredTextTextureRenderer implements RegisteredGuiTextureRenderer<TextTexture, RegisteredTextTextureRenderer> {
        @Override
        public Class<TextTexture> type() {
            return TextTexture.class;
        }

        @Override
        public void draw(TextTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            TextTextureRenderer.draw(texture, context, x, y, width, height);
        }
    }

    @LDLRegisterClient(name = "interpolated", registry = "ldlib2:gui_texture_renderer")
    public static final class InterpolatedTextureRenderer implements RegisteredGuiTextureRenderer<InterpolatedTexture, InterpolatedTextureRenderer> {
        @Override
        public Class<InterpolatedTexture> type() {
            return InterpolatedTexture.class;
        }

        @Override
        public void draw(InterpolatedTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            context.drawTexture(texture.from().copy(), x, y, width, height);
            context.drawTexture(texture.to().copy().setColor(ColorUtils.color(texture.lerp(), texture.lerp(), texture.lerp(), texture.lerp())),
                    x, y, width, height);
        }
    }

    @LDLRegisterClient(name = "rect_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredRectTextureRenderer implements RegisteredGuiTextureRenderer<RectTexture, RegisteredRectTextureRenderer> {
        @Override
        public Class<RectTexture> type() {
            return RectTexture.class;
        }

        @Override
        public void draw(RectTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            RectTextureRenderer.draw(texture, context, x, y, width, height);
        }
    }

    @LDLRegisterClient(name = "color_rect_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredColorRectTextureRenderer implements RegisteredGuiTextureRenderer<ColorRectTexture, RegisteredColorRectTextureRenderer> {
        @Override
        public Class<ColorRectTexture> type() {
            return ColorRectTexture.class;
        }

        @Override
        public void draw(ColorRectTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            ColorRectTextureRenderer.draw(texture, context, x, y, width, height);
        }
    }

    @LDLRegisterClient(name = "color_border_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredColorBorderTextureRenderer implements RegisteredGuiTextureRenderer<ColorBorderTexture, RegisteredColorBorderTextureRenderer> {
        @Override
        public Class<ColorBorderTexture> type() {
            return ColorBorderTexture.class;
        }

        @Override
        public void draw(ColorBorderTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            ColorBorderTextureRenderer.draw(texture, context, x, y, width, height);
        }
    }

    @LDLRegisterClient(name = "sdf_rect_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredSDFRectTextureRenderer implements RegisteredGuiTextureRenderer<SDFRectTexture, RegisteredSDFRectTextureRenderer> {
        @Override
        public Class<SDFRectTexture> type() {
            return SDFRectTexture.class;
        }

        @Override
        public void draw(SDFRectTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            SDFRectTextureRenderer.draw(texture, context, x, y, width, height);
        }
    }

    @LDLRegisterClient(name = "sprite_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredSpriteTextureRenderer implements RegisteredGuiTextureRenderer<SpriteTexture, RegisteredSpriteTextureRenderer> {
        @Override
        public Class<SpriteTexture> type() {
            return SpriteTexture.class;
        }

        @Override
        public void draw(SpriteTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            SpriteTextureRenderer.draw(texture, context, x, y, width, height);
        }
    }

    @LDLRegisterClient(name = "vanilla_sprite_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredVanillaSpriteTextureRenderer implements RegisteredGuiTextureRenderer<VanillaSpriteTexture, RegisteredVanillaSpriteTextureRenderer> {
        @Override
        public Class<VanillaSpriteTexture> type() {
            return VanillaSpriteTexture.class;
        }

        @Override
        public void draw(VanillaSpriteTexture texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            VanillaSpriteTextureRenderer.draw(texture, context, x, y, width, height);
        }
    }

    @LDLRegisterClient(name = "sprite_interpolated_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredSpriteTextureInterpolationRenderer implements RegisteredGuiTextureRenderer<SpriteTextureInterpolation, RegisteredSpriteTextureInterpolationRenderer> {
        @Override
        public Class<SpriteTextureInterpolation> type() {
            return SpriteTextureInterpolation.class;
        }

        @Override
        public void draw(SpriteTextureInterpolation texture, com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext context, float x, float y, float width, float height) {
            SpriteTextureInterpolationRenderer.draw(texture, context, x, y, width, height);
        }
    }
}
