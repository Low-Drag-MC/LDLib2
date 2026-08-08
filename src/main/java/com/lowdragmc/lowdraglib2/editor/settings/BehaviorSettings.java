package com.lowdragmc.lowdraglib2.editor.settings;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class BehaviorSettings implements Settings {
    public static final Identifier ID = LDLib2.id("behavior");
    public static final int DEFAULT_RECENT_PROJECT_COUNT = 10;
    public static final Codec<BehaviorSettings> CODEC =
            PersistedParser.createCodec(BehaviorSettings::new);

    /**
     * The behavior settings of an editor, or a default instance when it has none registered, so that
     * callers do not each have to spell out the lookup and a fallback for every field they read.
     */
    public static BehaviorSettings of(Editor editor) {
        return editor.getEditorSettings().getSettings(ID)
                .filter(BehaviorSettings.class::isInstance)
                .map(BehaviorSettings.class::cast)
                .orElseGet(BehaviorSettings::new);
    }

    @Configurable @Getter @Setter private boolean shouldCloseOnEsc = false;
    @DefaultValue(booleanValue = true)
    @Configurable(name = "settings.ldlib2.behavior.restoreLayoutOnProjectOpen")
    @Getter @Setter private boolean restoreLayoutOnProjectOpen = true;
    @DefaultValue(booleanValue = true)
    @Configurable(name = "settings.ldlib2.behavior.restoreAssetBrowserPath")
    @Getter @Setter private boolean restoreAssetBrowserPath = true;
    @DefaultValue(numberValue = DEFAULT_RECENT_PROJECT_COUNT)
    @Configurable(name = "settings.ldlib2.behavior.recentProjectCount")
    @ConfigNumber(range = {0, 50}, type = ConfigNumber.Type.INTEGER)
    @Getter @Setter private int recentProjectCount = DEFAULT_RECENT_PROJECT_COUNT;

    private final UIEventListener onKeyDownListener = this::onKeyDown;

    // runtime
    @Nullable private Stylesheet currentStylesheet;

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public String getPath() {
        return "Behavior";
    }

    @Override
    public void onApply(Editor editor) {
        if (!editor.hasEventListener(UIEvents.KEY_DOWN, onKeyDownListener)) {
            editor.addEventListener(UIEvents.KEY_DOWN, onKeyDownListener);
            editor.setFocusable(true);
        }
    }

   /**
     * Handles keyboard shortcuts for the editor:
     * <br><br> &#064;-  ESC → closes editor if allowed
     * <br><br> &#064;-  Ctrl + Alt + S → open settings panel
     * <br><br> &#064;-  Ctrl + Shift + S → save project as
     * <br><br> &#064;-  Ctrl + S → save project
     */
   private void onKeyDown(UIEvent event) {
        if (shouldCloseOnEsc && event.keyCode == GLFW.GLFW_KEY_ESCAPE && event.currentElement instanceof Editor editor) {
            editor.close();
        }

       if (!(event.currentElement instanceof Editor editor)) return;

       // TODO Add Key Bindings system,
       // Ctrl + Alt + S → open settings panel
       if (UIElement.isAltDown()
               && event.keyCode == GLFW.GLFW_KEY_S) {
           editor.openSettingsPanel();
       }

       // Ctrl + SHIFT + S → save as project
       if (UIElement.isCtrlOrCmdDown()
               && UIElement.isShiftDown()
               && event.keyCode == GLFW.GLFW_KEY_S) {
           editor.saveAsProject(null); // pass null
       }
    }
}
