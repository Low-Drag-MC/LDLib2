package com.lowdragmc.lowdraglib2.editor.keymap;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Looking a binding up from somewhere in the element tree, for anything that wants to <em>show</em> a
 * shortcut — a menu entry, a button's tooltip.
 *
 * <p>Written down in one place because the alternative is what this replaced: the chord spelled out in a
 * translation string, which stopped being true the moment shortcuts became configurable, and which no
 * one remembers to update.
 *
 * <p>Separate from {@link Keymap} so that stays free of UI types and therefore testable without a game.
 */
public final class Keymaps {

    private Keymaps() {}

    /** The keymap of the editor this element sits in, or null when it is not in one. */
    @Nullable
    public static Keymap of(@Nullable UIElement element) {
        if (element == null) return null;
        var editor = element.getFirstAncestorOfType(Editor.class);
        return editor == null ? null : editor.getKeymap();
    }

    /**
     * A tooltip for a control that runs an action: the action's own chord where there is a keymap, and
     * {@code fallback} where there is none — a graph view outside an editor still answers to the UI's
     * built-in chords, so that is what its buttons must advertise.
     *
     * @return the chord as text, or {@code fallback} when no keymap has an opinion, or an empty
     *         component when the action is deliberately unbound.
     */
    public static Component shortcutTooltip(@Nullable UIElement element, Identifier actionId,
                                            String fallback) {
        var keymap = of(element);
        if (keymap == null) return Component.literal(fallback);
        var bindings = keymap.bindingsOf(actionId);
        if (!bindings.primary().isBound() && !bindings.secondary().isBound()) {
            return Component.empty();
        }
        var text = bindings.primary().isBound() ? bindings.primary().toDisplayString() : "";
        if (bindings.secondary().isBound()) {
            text = text.isEmpty() ? bindings.secondary().toDisplayString()
                    : text + " / " + bindings.secondary().toDisplayString();
        }
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }
}
