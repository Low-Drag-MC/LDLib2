package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import lombok.Getter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class ModularUIClientState {
    @Nullable
    Screen screen;
    @Nullable
    HoverTooltips hoverTooltips;
    @Nullable
    UIDebugger uiDebuggerCache;
    /** A queued {@link ModularUIClientAccess#setDebuggerWindowed} — see {@code applyPendingDebuggerHost}. */
    @Nullable
    Boolean pendingDebuggerWindowed;
    @Getter
    private final ModularUIWidget widget;
    final List<Rect2i> extraAreas = new ArrayList<>();

    ModularUIClientState(ModularUI modularUI) {
        this.widget = new ModularUIWidget(modularUI);
    }
}
