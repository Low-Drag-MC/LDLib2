package com.lowdragmc.lowdraglib2.editor.ui.menu;

import com.lowdragmc.lowdraglib2.editor.settings.AppearanceSettings;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
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
            var minecraft = Minecraft.getInstance();
            var guiScale = minecraft.options.guiScale();
            var maxScale = minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode());
            for (int i = 0; i <= maxScale; i++) {
                var finalI = i;
                menu.leaf(guiScale.get() == i ? Icons.CHECK : IGuiTexture.EMPTY, i == 0 ? "options.guiScale.auto" : i + "", () -> {
                    editor.getEditorSettings().getSettings(AppearanceSettings.ID).ifPresent(settings -> {
                        if (settings instanceof AppearanceSettings appearanceSettings) {
                            appearanceSettings.setScreenScale(finalI);
                            if (editor.getEditorSettings().isDirty()) {
                                editor.getEditorSettings().applyCurrentSettings();
                                editor.getEditorSettings().saveAllSettingsToFile();
                            }
                        }
                    });
                });
            }
        });
        return viewMenu;
    }

    @Override
    protected Component getComponent() {
        return Component.translatable("editor.view");
    }

}
