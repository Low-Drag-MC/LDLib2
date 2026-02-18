package com.lowdragmc.lowdraglib2.gui.holder;

import com.lowdragmc.lowdraglib2.gui.sync.IUISyncManagerHolder;
import com.lowdragmc.lowdraglib2.gui.sync.UISyncManager;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import org.jetbrains.annotations.Nullable;

public interface IModularUIHolder extends IUISyncManagerHolder {
    @Nullable
    ModularUI getModularUI();

    default boolean hasModularUI() {
        return getModularUI() != null;
    }

    @Override
    @Nullable
    default UISyncManager getSyncManager() {
        var modularUI = getModularUI();
        if (modularUI == null) return null;
        return modularUI.syncManager;
    }
}
