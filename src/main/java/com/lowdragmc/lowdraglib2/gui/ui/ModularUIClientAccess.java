package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.holder.DebugScreen;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ModularUIClientAccess {
    private ModularUIClientAccess() {
    }

    static ModularUIClientState getState(ModularUI modularUI) {
        if (!(modularUI.clientState instanceof ModularUIClientState)) {
            var state = new ModularUIClientState(modularUI);
            modularUI.clientState = state;
            return state;
        }
        return (ModularUIClientState) modularUI.clientState;
    }

    public static void setScreenAndInit(ModularUI modularUI, Screen screen) {
        var state = getState(modularUI);
        state.screen = screen;
        modularUI.init(screen.width, screen.height);
    }

    public static ModularUIWidget getWidget(ModularUI modularUI) {
        return getState(modularUI).getWidget();
    }

    @Nullable
    public static Screen getScreen(ModularUI modularUI) {
        return getState(modularUI).screen;
    }

    @Nullable
    public static HoverTooltips getHoverTooltips(ModularUI modularUI) {
        return getState(modularUI).hoverTooltips;
    }

    public static void setHoverTooltip(ModularUI modularUI, @Nullable HoverTooltips hoverTooltips) {
        getState(modularUI).hoverTooltips = hoverTooltips;
    }

    public static void cleanTooltip(ModularUI modularUI) {
        getState(modularUI).hoverTooltips = null;
    }

    public static List<Rect2i> getGuiExtraAreas(ModularUI modularUI) {
        var state = getState(modularUI);
        if (state.extraAreas.isEmpty()) {
            state.extraAreas.clear();
            UIElementClientAccess.appendExtraAreas(modularUI.ui.rootElement, state.extraAreas);
        }
        return state.extraAreas;
    }

    public static void enableDebugger(ModularUI modularUI, boolean debugMode) {
        if (modularUI.isDebugMode() == debugMode) {
            return;
        }
        modularUI.setDebugMode(debugMode);
        if (!debugMode) {
            return;
        }
        var state = getState(modularUI);
        if (state.uiDebuggerCache == null) {
            state.uiDebuggerCache = new UIDebugger(modularUI);
        }
        Minecraft.getInstance().gui.pushScreenLayer(new DebugScreen(state.uiDebuggerCache));
    }
}
