package com.lowdragmc.lowdraglib2.editor.settings;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.SelectorConfigurator;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class AppearanceSettings implements Settings {
    public static final ResourceLocation ID = LDLib2.id("appearance");
    public static final Codec<AppearanceSettings> CODEC = PersistedParser.createCodec(AppearanceSettings::new);

    @Configurable
    @ConfigSearch(searchConfiguratorMethod = "searchStyles")
    @Getter @Setter
    private ResourceLocation stylesheet = StylesheetManager.ORE_MERGED;
    @Persisted(key = "windowSize")
    @Getter @Setter
    private int screenScale = -1;

    // runtime
    @Nullable
    private Stylesheet currentStylesheet;
    @Nullable
    private WeakReference<ModularUI> appliedModularUI;
    private final UIEventListener onMuiChangedListener = this::onModularUIChanged;

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public String getPath() {
        return "Appearance";
    }

    @Override
    public void onApply(Editor editor) {
        if (!editor.hasEventListener(UIEvents.MUI_CHANGED, onMuiChangedListener)) {
            editor.addEventListener(UIEvents.MUI_CHANGED, onMuiChangedListener);
        }
        applyStylesheet(editor);
        // screenScale
        var minecraft = Minecraft.getInstance();
        var guiScale = minecraft.options.guiScale();
        var maxScale =  minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode());
        if (screenScale > maxScale) {
            screenScale = maxScale;
        }
        if (guiScale.get() != screenScale) {
            guiScale.set(screenScale);
            Minecraft.getInstance().resizeDisplay();
        }
    }

    private void onModularUIChanged(UIEvent event) {
        if (event.currentElement instanceof Editor editor) {
            applyStylesheet(editor);
        }
    }

    private void applyStylesheet(Editor editor) {
        var stylesheet = StylesheetManager.INSTANCE.getStylesheet(this.stylesheet);
        if (stylesheet == null) {
            return;
        }

        var mui = editor.getModularUI();
        if (mui == null) {
            appliedModularUI = null;
            currentStylesheet = stylesheet;
            return;
        }

        var previousMui = appliedModularUI == null ? null : appliedModularUI.get();
        if (previousMui == mui && currentStylesheet == stylesheet) {
            return;
        }

        if (previousMui != null && currentStylesheet != null) {
            previousMui.getStyleEngine().removeStylesheet(currentStylesheet);
        }
        mui.getStyleEngine().addStylesheet(stylesheet);
        appliedModularUI = new WeakReference<>(mui);
        currentStylesheet = stylesheet;
    }

    private SearchComponentConfigurator.ISearchConfigurator<ResourceLocation> searchStyles() {
        return new SearchComponentConfigurator.ISearchConfigurator<>() {
            @Override
            @Nonnull
            public ResourceLocation defaultValue() {
                return StylesheetManager.ORE_MERGED;
            }

            @Override
            public void search(String word, IResultHandler<ResourceLocation> searchHandler) {
                var lowerWord = word.toLowerCase();
                for (var key : StylesheetManager.INSTANCE.getAllPackStylesheets()) {
                    if (Thread.currentThread().isInterrupted()) return;
                    if (key.toString().toLowerCase().contains(lowerWord)) {
                        if (key.getPath().endsWith(StylesheetManager.PATH)) {
                            key = key.withPath(key.getPath().substring(0, key.getPath().length() - StylesheetManager.PATH.length() - 1));
                        }
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

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        Settings.super.buildConfigurator(father);
        // scale
        var minecraft = Minecraft.getInstance();
        var guiScale = minecraft.options.guiScale();
        var maxScale =  minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode());
        var scales = new ArrayList<Integer>(maxScale + 1);
        for (int i = 0; i <= maxScale; i++) {
            scales.add(i);
        }
        father.addConfiguratorAt(new SelectorConfigurator<>("ldlib.gui.editor.menu.view.window_size", () -> screenScale, scale -> {
            guiScale.set(scale);
            setScreenScale(scale);
        }, -1, true, scales, scale -> scale == 0 ? "options.guiScale.auto" : scale + ""), 1);
    }
}
