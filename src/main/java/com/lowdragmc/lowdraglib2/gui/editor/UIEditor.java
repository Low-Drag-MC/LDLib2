package com.lowdragmc.lowdraglib2.gui.editor;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.*;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import javax.annotation.Nonnull;

public class UIEditor extends Editor {
    public final static Identifier WINDOW_ID = LDLib2.id("ui_editor");

    /**
     * Asks the server to open the editor's menu, as {@code /ldlib2_ui_editor} does.
     *
     * @return {@code false} off the client, or outside singleplayer, where that command does not exist
     */
    public static boolean requestMenu() {
        return LDLib2.isClient() && ClientSupport.requestMenu();
    }

    static final class ClientSupport {
        private ClientSupport() {
        }

        static boolean requestMenu() {
            var minecraft = Minecraft.getInstance();
            var connection = minecraft.getConnection();
            if (connection == null || !minecraft.hasSingleplayerServer()) return false;
            connection.sendCommand("ldlib2_ui_editor");
            return true;
        }
    }

    public UIEditor() {
        this.leftWindow.setDisplay(false);
        this.leftWindow.getParentWindow().removeSplitWindow(this.leftWindow);
        initResources();
    }

    @Override
    protected @Nonnull Editor createNewEditorInstance() {
        return new UIEditor();
    }

    private void initResources() {
        this.resourceView.clear();
        this.resourceView.loadResources(Resources.of(
                UIResource.INSTANCE,
                ColorsResource.INSTANCE,
                TexturesResource.INSTANCE
        ));
    }

    @Override
    protected void initMenus() {
        super.initMenus();
        fileMenu.addProjectProvider(UIXmlProjectType.TYPE);
    }

    @Override
    protected void closeCurrentProject() {
        super.closeCurrentProject();
        initResources();
    }
}
