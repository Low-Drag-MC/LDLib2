package com.lowdragmc.lowdraglib2.editor.settings;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;

public class AppearanceSettings implements Settings {
    public static final Codec<AppearanceSettings> CODEC = PersistedParser.createCodec(AppearanceSettings::new);

    @Configurable
    @ConfigSearch(searchConfiguratorMethod = "searchStyles")
    @Getter @Setter
    private ResourceLocation stylesheet = StylesheetManager.GDP;

    // runtime
    @Nullable
    private Stylesheet currentStylesheet;

    @Override
    public ResourceLocation getId() {
        return LDLib2.id("appearance");
    }

    @Override
    public String getPath() {
        return "Appearance";
    }

    @Override
    public void onApply(Editor editor) {
        var mui = editor.getModularUI();
        var stylesheet = StylesheetManager.INSTANCE.getStylesheet(this.stylesheet);
        if (stylesheet != null) {
            if (mui != null) {
                if (currentStylesheet != null) {
                    mui.getStyleEngine().removeStylesheet(currentStylesheet);
                }
                mui.getStyleEngine().addStylesheet(stylesheet);
            } else {
                editor.addEventListener(UIEvents.MUI_CHANGED, postEventHandler(editor));
            }
            currentStylesheet = stylesheet;
        }
    }

    private @NotNull UIEventListener postEventHandler(Editor editor) {
        AtomicReference<UIEventListener> ref = new AtomicReference<>();
        UIEventListener postHandler = event -> {
            if (currentStylesheet != null) {
                var modularUI = event.target.getModularUI();
                if (modularUI != null) {
                    modularUI.getStyleEngine().addStylesheet(currentStylesheet);
                    if (ref.get() != null) {
                        editor.removeEventListener(UIEvents.MUI_CHANGED, ref.get());
                        ref.set(null);
                    }
                }
            }
        };
        ref.set(postHandler);
        return postHandler;
    }

    private SearchComponentConfigurator.ISearchConfigurator<ResourceLocation> searchStyles() {
        return new SearchComponentConfigurator.ISearchConfigurator<>() {
            @Override
            @Nonnull
            public ResourceLocation defaultValue() {
                return StylesheetManager.GDP;
            }

            @Override
            public void search(String word, IResultHandler<ResourceLocation> searchHandler) {
                var lowerWord = word.toLowerCase();
                for (var key : StylesheetManager.INSTANCE.getAllPackStylesheets()) {
                    if (Thread.currentThread().isInterrupted()) return;
                    if (key.toString().toLowerCase().contains(lowerWord)) {
                        searchHandler.acceptResult(key);
                    }
                }
            }

            @Override
            @Nonnull
            public String resultText(@NotNull ResourceLocation value) {
                return value.toString();
            }

            @Override
            public UIElementProvider<ResourceLocation> candidateUIProvider() {
                return UIElementProvider.text(res -> Component.literal(res.toString()));
            }
        };
    }
}
