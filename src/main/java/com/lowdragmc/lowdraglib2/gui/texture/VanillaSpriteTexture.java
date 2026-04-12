package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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

    @OnlyIn(Dist.CLIENT)
    private SearchComponentConfigurator.ISearchConfigurator<Identifier> searchSprites() {
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
