package com.lowdragmc.lowdraglib2.integration.xei.jei;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.utils.IModularUIProvider;
import com.lowdragmc.lowdraglib2.integration.xei.jei.handler.JEIRecipeSlotHandler;

import java.time.Duration;
import java.util.List;
import java.util.function.IntSupplier;

final class CachedRecipeUI<T> {
    private final LoadingCache<T, Entry> uiCache;

    CachedRecipeUI(IModularUIProvider<T> uiProvider, IntSupplier width, IntSupplier height) {
        uiCache = CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(10))
                .maximumSize(10)
                .removalListener((RemovalNotification<T, Entry> notification) -> {
                    var value = notification.getValue();
                    if (value != null) {
                        value.onRemoved();
                    }
                })
                .build(new CacheLoader<>() {
                    @Override
                    public Entry load(T key) {
                        return Entry.create(
                                uiProvider.createModularUI(key),
                                width.getAsInt(),
                                height.getAsInt()
                        );
                    }
                });
    }

    Entry get(T recipe) {
        return uiCache.getUnchecked(recipe);
    }

    record Entry(
            ModularUI modularUI,
            List<JEIRecipeSlotHandler.Binding> bindings
    ) {
        Entry {
            bindings = List.copyOf(bindings);
        }

        private static Entry create(ModularUI modularUI, int width, int height) {
            modularUI.setAllowDebugMode(false);
            modularUI.setDrawTooltips(false);
            modularUI.init(width, height);
            return new Entry(modularUI, JEIRecipeSlotHandler.collectBindings(modularUI));
        }

        private void onRemoved() {
            modularUI.onRemoved();
        }
    }
}
