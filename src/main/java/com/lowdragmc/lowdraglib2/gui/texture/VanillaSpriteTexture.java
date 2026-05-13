package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * The sprite Identifier should point to a registered vanilla sprite
 * (e.g. from a GUI atlas), not a raw texture file.
 */
@KJSBindings
@LDLRegisterClient(name = "vanilla_sprite_texture", registry = "ldlib2:gui_texture")
@Accessors(chain = true)
public class VanillaSpriteTexture extends TransformTexture {

    @Configurable(name = "ldlib.gui.editor.name.resource")
    @ConfigSearch(searchConfiguratorMethod = "searchSprites")
    @Getter
    @Setter
    private Identifier sprite = Identifier.withDefaultNamespace("toast/recipe_book");

    @Configurable
    @ConfigColor
    @Getter
    @Setter
    private int color = -1;

    public VanillaSpriteTexture() {
    }

    public VanillaSpriteTexture(Identifier sprite) {
        this.sprite = sprite;
    }

    public static VanillaSpriteTexture of(Identifier sprite) {
        return new VanillaSpriteTexture(sprite);
    }

    public static VanillaSpriteTexture of(String sprite) {
        return of(Identifier.parse(sprite));
    }

    @Override
    public VanillaSpriteTexture copy() {
        var copied = new VanillaSpriteTexture(sprite);
        copied.color = color;
        copied.copyTransform(this);
        return copied;
    }

    private SearchComponentConfigurator.ISearchConfigurator<Identifier> searchSprites() {
        return VanillaSpriteTextureClientSupport.createSearchSprites();
    }

    public static final class VanillaSpriteTextureClientSupport {
        private VanillaSpriteTextureClientSupport() {
        }

        public static SearchComponentConfigurator.ISearchConfigurator<Identifier> createSearchSprites() {
            return new SearchComponentConfigurator.ISearchConfigurator<>() {
                @Override
                @NotNull
                public Identifier defaultValue() {
                    return Identifier.withDefaultNamespace("toast/recipe_book");
                }

                @Override
                public void search(String word, IResultHandler<Identifier> searchHandler) {
                    var lowerWord = word.toLowerCase();
                    var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI);
                    for (var key : atlas.getTextures().keySet()) {
                        if (Thread.currentThread().isInterrupted()) return;
                        if (key.toString().toLowerCase().contains(lowerWord)) {
                            searchHandler.acceptResult(key);
                        }
                    }
                }

                @Override
                @NotNull
                public String resultText(@NotNull Identifier value) {
                    return value.toString();
                }

                @Override
                public UIElementProvider<Identifier> candidateUIProvider() {
                    return UIElementProvider.iconText(VanillaSpriteTexture::of, res -> Component.literal(res.toString()));
                }
            };
        }
    }

    @LDLRegisterClient(name = "vanilla_sprite_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredVanillaSpriteTextureRenderer implements RegisteredGuiTextureRenderer<VanillaSpriteTexture, RegisteredVanillaSpriteTextureRenderer> {
        @Override
        public Class<VanillaSpriteTexture> type() {
            return VanillaSpriteTexture.class;
        }

        @Override
        public void draw(VanillaSpriteTexture texture, GUIContext context, float x, float y, float width, float height) {
            TransformTextureRenderer.draw(texture, context, x, y, width, height, this::drawInternal);
        }

        private void drawInternal(VanillaSpriteTexture texture, GUIContext context, float x, float y, float width, float height) {
            var sprite = texture.getSprite();
            if (sprite == null || width <= 0 || height <= 0) {
                return;
            }
            context.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, texture.getColor());
        }
    }
}
