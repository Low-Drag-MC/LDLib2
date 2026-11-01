package com.lowdragmc.lowdraglib2.editor.ui.menu;

import com.lowdragmc.lowdraglib2.editor.settings.AppearanceSettings;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.OffscreenSurface;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ViewMenu extends MenuTab {
    public ViewMenu(Editor editor) {
        super(editor);
    }

    @Override
    protected TreeBuilder.Menu createDefaultMenu() {
        var viewMenu = TreeBuilder.Menu.start().branch("ldlib.gui.editor.menu.view.window_size", menu -> {
            var window = editor.getWindow();
            if (window != null && window.isInOsWindow()) {
                // the editor's own window: scaled on its own, never the game's
                var current = AppearanceSettings.of(editor).map(AppearanceSettings::getScreenScale)
                        .orElse(OffscreenSurface.FOLLOW_GAME);
                var maxScale = Math.max(window.getOsWindow().getMaxGuiScale(), current);
                for (int i = OffscreenSurface.FOLLOW_GAME; i <= maxScale; i++) {
                    addScaleLeaf(menu, i, current == i);
                }
                return;
            }
            var minecraft = Minecraft.getInstance();
            var guiScale = minecraft.options.guiScale();
            var maxScale = minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode());
            for (int i = 0; i <= maxScale; i++) {
                addScaleLeaf(menu, i, guiScale.get() == i);
            }
        });
        return viewMenu;
    }

    private void addScaleLeaf(TreeBuilder.Menu menu, int scale, boolean current) {
        var label = scale < 0 ? "editor.window.scale.follow_game"
                : scale == 0 ? "options.guiScale.auto" : Integer.toString(scale);
        menu.leaf(current ? Icons.CHECK : IGuiTexture.EMPTY, label, () ->
                AppearanceSettings.of(editor).ifPresent(appearanceSettings -> {
                    appearanceSettings.setScreenScale(scale);
                    if (editor.getEditorSettings().isDirty()) {
                        editor.getEditorSettings().applyCurrentSettings();
                        editor.getEditorSettings().saveAllSettingsToFile();
                    }
                }));
    }

    @Override
    protected Component getComponent() {
        return Component.translatable("editor.view");
    }

}
