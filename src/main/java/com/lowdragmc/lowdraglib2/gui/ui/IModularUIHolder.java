package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.sync.IUISyncManagerHolder;
import com.lowdragmc.lowdraglib2.gui.sync.UISyncManager;

import javax.annotation.Nonnull;

public interface IModularUIHolder extends IUISyncManagerHolder {
    @Nonnull ModularUI getModularUI();

    @Override
    default UISyncManager getSyncManager() {
        return getModularUI().syncManager;
    }
}
