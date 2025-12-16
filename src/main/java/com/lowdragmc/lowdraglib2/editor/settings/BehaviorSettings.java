package com.lowdragmc.lowdraglib2.editor.settings;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class BehaviorSettings implements Settings {
    public static final ResourceLocation ID = LDLib2.id("behavior");
    public static final Codec<BehaviorSettings> CODEC =
            PersistedParser.createCodec(BehaviorSettings::new);

    @Configurable @Getter @Setter private boolean shouldCloseOnEsc = false;

    private final UIEventListener onKeyDownListener = this::onKeyDown;

    // runtime
    @Nullable private Stylesheet currentStylesheet;

    @Override
    public ResourceLocation getId() {
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

       // Ctrl + Alt + S → open settings panel
       if ((event.modifiers & GLFW.GLFW_MOD_ALT) != 0
               && (event.modifiers & GLFW.GLFW_MOD_CONTROL) != 0
               && event.keyCode == GLFW.GLFW_KEY_S) {
           editor.openSettingsPanel();
       }

       // Ctrl + SHIFT + S → save as project
       if ((event.modifiers & GLFW.GLFW_MOD_CONTROL) != 0
               && (event.modifiers & GLFW.GLFW_MOD_SHIFT) != 0
               && event.keyCode == GLFW.GLFW_KEY_S) {
           editor.saveAsProject(null); // pass null
       }

       // Ctrl + S → save project
       if ((event.modifiers & GLFW.GLFW_MOD_CONTROL) != 0
               && event.keyCode == GLFW.GLFW_KEY_S) {
           editor.saveProject(null); // pass null
       }
    }
}
