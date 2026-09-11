package com.lowdragmc.lowdraglib2.editor.keymap.ui;

import com.lowdragmc.lowdraglib2.editor.keymap.KeyChord;
import com.lowdragmc.lowdraglib2.editor.keymap.KeyNames;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.KeyState;
import com.lowdragmc.lowdraglib2.gui.util.UISoundUtils;
import dev.vfyjxf.taffy.style.AlignContent;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * The control a shortcut is assigned with: it shows a chord, and on click it listens for the next one.
 *
 * <p>Reads as a text field rather than a button, because that is what it is — a field holding a value
 * you replace. The stylesheets give it the same frame they give {@code text-field} by listing
 * {@link #FIELD_CLASS} alongside it, and the {@link #CAPTURING_CLASS} it carries while listening is what
 * a theme hangs its "waiting for you" look on.
 *
 * <p>While it is listening it {@link #ownsKey owns every key}, so nothing else in the editor acts on
 * what the user presses — assigning {@code Ctrl+S} must not also save the project.
 *
 * <p>Escape is assigned like any other key rather than cancelling, because "Escape closes the editor" is
 * a binding people want and there would otherwise be no way to enter it. Clicking elsewhere cancels;
 * Backspace and Delete clear the binding.
 */
public class KeyChordField extends UIElement {
    /** Styled as a text field by every stylesheet. */
    public static final String FIELD_CLASS = "__keymap_chord__";
    public static final String TEXT_CLASS = "__keymap_chord-text__";
    /** On the field while it waits for a chord. */
    public static final String CAPTURING_CLASS = "__capturing__";
    /** On the field when another action answers to the same chord. */
    public static final String CONFLICT_CLASS = "__conflict__";

    public final TextElement text;

    @Getter
    private KeyChord chord;
    @Setter
    @Nullable
    private Consumer<KeyChord> onChordChanged;
    @Getter
    private boolean capturing;

    public KeyChordField(KeyChord chord) {
        this.chord = chord;
        this.text = new TextElement();

        setFocusable(true);
        addClass(FIELD_CLASS);
        getLayout().height(12);
        getLayout().paddingHorizontal(1);
        getLayout().justifyContent(AlignContent.CENTER);
        style(style -> style.backgroundTexture(Sprites.RECT_SOLID));

        text.addClass(TEXT_CLASS);
        addChild(text
                .textStyle(textStyle -> {
                    // Fills the field and rolls on hover rather than sizing itself: a chord as long as
                    // Ctrl+Shift+Page Down would otherwise either widen the field past its share of the
                    // row or be cut off with no way to read the rest.
                    textStyle.adaptiveWidth(false);
                    textStyle.textWrap(TextWrap.HOVER_ROLL);
                    textStyle.fontSize(7);
                    textStyle.textShadow(false);
                    textStyle.textAlignHorizontal(Horizontal.CENTER);
                    textStyle.textAlignVertical(Vertical.CENTER);
                })
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                }));
        setOverflowVisible(false);

        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.KEY_DOWN, this::onKeyDown);
        addEventListener(UIEvents.BLUR, event -> cancelCapture());
        refreshText();

        // everything above is a default a stylesheet is free to replace
        moveInlineAsDefault();
        text.moveInlineAsDefault();
    }

    /** Marks the field as answering to a chord something else already uses. */
    public void setConflicting(boolean conflicting) {
        if (conflicting) {
            addClass(CONFLICT_CLASS);
        } else {
            removeClass(CONFLICT_CLASS);
        }
    }

    public void startCapture() {
        if (capturing) return;
        capturing = true;
        addClass(CAPTURING_CLASS);
        focus();
        refreshText();
    }

    public void cancelCapture() {
        if (!capturing) return;
        capturing = false;
        removeClass(CAPTURING_CLASS);
        refreshText();
    }

    /**
     * Everything, while listening. A field waiting for a chord is the one place where the keyboard
     * belongs to a single control and to nothing else.
     */
    @Override
    public boolean ownsKey(UIEvent event) {
        return capturing;
    }

    protected void onMouseDown(UIEvent event) {
        if (event.button != 0) return;
        UISoundUtils.playButtonClickSound();
        startCapture();
    }

    protected void onKeyDown(UIEvent event) {
        if (!capturing) return;
        // before anything else: the chord being assigned must not also run whatever it is bound to
        event.hasHandler = true;
        event.stopPropagation();
        if (KeyNames.isModifier(event.keyCode)) {
            // still waiting for the key the modifiers go with; show them as they are held
            refreshText();
            return;
        }
        if (isClear(event.keyCode)) {
            commit(KeyChord.UNBOUND);
            return;
        }
        var next = KeyChord.of(event.keyCode,
                KeyState.isCtrlDown(), KeyState.isShiftDown(), KeyState.isAltDown());
        // a key this library has no name for cannot be stored, so it is not accepted either
        if (!next.isBound()) return;
        commit(next);
    }

    private boolean isClear(int keyCode) {
        return (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE)
                && !KeyState.isCtrlDown() && !KeyState.isShiftDown() && !KeyState.isAltDown();
    }

    protected void commit(KeyChord chord) {
        this.chord = chord;
        cancelCapture();
        if (onChordChanged != null) {
            onChordChanged.accept(chord);
        }
    }

    protected void refreshText() {
        text.setText(currentText());
    }

    protected Component currentText() {
        if (capturing) {
            var held = new StringBuilder();
            if (KeyState.isCtrlDown()) held.append("Ctrl+");
            if (KeyState.isShiftDown()) held.append("Shift+");
            if (KeyState.isAltDown()) held.append("Alt+");
            return held.isEmpty()
                    ? Component.translatable("keymap.ldlib2.press_key")
                    : Component.literal(held + "…");
        }
        return chord.isBound()
                ? Component.literal(chord.toDisplayString())
                : Component.translatable("keymap.ldlib2.unbound");
    }
}
