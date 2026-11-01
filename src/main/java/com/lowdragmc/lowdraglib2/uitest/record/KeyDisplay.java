package com.lowdragmc.lowdraglib2.uitest.record;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;

/**
 * What the key display shows, merged the way a viewer reads it: a chord as {@code Ctrl + Z}, typed text
 * as one growing key, a repeat as a count. Times are video time in nanoseconds.
 */
final class KeyDisplay {

    static final long APPEAR_NANOS = 120_000_000L;
    static final long LINGER_NANOS = 1_500_000_000L;
    static final long FADE_NANOS = 300_000_000L;
    static final long JOIN_NANOS = 1_200_000_000L;
    static final int MAX_KEYS = 4;
    static final int TYPED_TAIL = 28;

    enum Kind {
        /** Modifiers held with nothing pressed yet; becomes a {@link #CHORD} when something is. */
        MODIFIERS, CHORD, TYPED, SCROLL
    }

    static final class Key {
        final Kind kind;
        String label;
        int count = 1;
        final long createdAt;
        long updatedAt;
        /** Where the overlay last drew it, so a key slides rather than jumps when its neighbours change. */
        float x = Float.NaN;

        private Key(Kind kind, String label, long now) {
            this.kind = kind;
            this.label = label;
            this.createdAt = now;
            this.updatedAt = now;
        }

        String text() {
            if (kind == Kind.TYPED && label.length() > TYPED_TAIL) {
                return "…" + label.substring(label.length() - TYPED_TAIL + 1);
            }
            return count > 1 ? label + "  ×" + count : label;
        }

        /** How far it has popped in, 0 to 1. */
        float appear(long now) {
            return Math.min(1f, (now - createdAt) / (float) APPEAR_NANOS);
        }

        float alpha(long now) {
            long idle = now - updatedAt;
            return appear(now) * (idle < LINGER_NANOS ? 1f : Math.max(0f, 1f - (idle - LINGER_NANOS) / (float) FADE_NANOS));
        }
    }

    private final List<Key> keys = new ArrayList<>();
    private final List<String> heldModifiers = new ArrayList<>();
    private final IntFunction<String> layoutName;

    KeyDisplay() {
        this(keyCode -> {
            var name = GLFW.glfwGetKeyName(keyCode, 0);
            return name == null ? "Key " + keyCode : name.toUpperCase(Locale.ROOT);
        });
    }

    /** @param layoutName names a key this class has no name of its own for */
    KeyDisplay(IntFunction<String> layoutName) {
        this.layoutName = layoutName;
    }

    /** The keys still showing, oldest first. */
    List<Key> visible(long now) {
        keys.removeIf(key -> now - key.updatedAt >= LINGER_NANOS + FADE_NANOS);
        return keys;
    }

    void mouseDown(int button, long now) {
        var latest = latest();
        if (latest != null && latest.kind == Kind.MODIFIERS) {
            latest.label += " + " + buttonName(button);
            latest.updatedAt = now;
        }
    }

    void scroll(double amount, long now) {
        push(Kind.SCROLL, amount > 0 ? "Scroll ↑" : "Scroll ↓", true, now);
    }

    void keyDown(int keyCode, long now) {
        var modifier = modifierName(keyCode);
        if (modifier != null) {
            if (!heldModifiers.contains(modifier)) heldModifiers.add(modifier);
            var latest = latest();
            var label = String.join(" + ", orderedModifiers());
            if (latest != null && latest.kind == Kind.MODIFIERS && now - latest.updatedAt < JOIN_NANOS) {
                latest.label = label;
                latest.updatedAt = now;
            } else {
                push(Kind.MODIFIERS, label, false, now);
            }
            return;
        }
        var latest = latest();
        if (latest != null && latest.kind == Kind.MODIFIERS && !heldModifiers.isEmpty()) {
            keys.remove(latest);
        }
        var name = keyName(keyCode);
        push(Kind.CHORD, heldModifiers.isEmpty() ? name : String.join(" + ", orderedModifiers()) + " + " + name, true, now);
    }

    void keyUp(int keyCode) {
        var modifier = modifierName(keyCode);
        if (modifier != null) heldModifiers.remove(modifier);
    }

    void charTyped(char codePoint, long now) {
        var latest = latest();
        if (latest != null && latest.kind == Kind.TYPED && now - latest.updatedAt < JOIN_NANOS) {
            latest.label += codePoint;
            latest.updatedAt = now;
        } else {
            push(Kind.TYPED, String.valueOf(codePoint), false, now);
        }
    }

    private void push(Kind kind, String label, boolean counts, long now) {
        var latest = latest();
        if (counts && latest != null && latest.kind == kind && latest.label.equals(label) && now - latest.updatedAt < JOIN_NANOS) {
            latest.count++;
            latest.updatedAt = now;
            return;
        }
        keys.add(new Key(kind, label, now));
        while (keys.size() > MAX_KEYS) keys.removeFirst();
    }

    private Key latest() {
        return keys.isEmpty() ? null : keys.getLast();
    }

    private List<String> orderedModifiers() {
        var ordered = new ArrayList<String>(heldModifiers.size());
        for (var name : new String[]{"Ctrl", "Shift", "Alt", "Win"}) {
            if (heldModifiers.contains(name)) ordered.add(name);
        }
        return ordered;
    }

    private static String modifierName(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "Ctrl";
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> "Shift";
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> "Alt";
            case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> "Win";
            default -> null;
        };
    }

    private static String buttonName(int button) {
        return switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "Right Click";
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "Middle Click";
            default -> "Click";
        };
    }

    private String keyName(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> "Enter";
            case GLFW.GLFW_KEY_ESCAPE -> "Esc";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
            case GLFW.GLFW_KEY_DELETE -> "Delete";
            case GLFW.GLFW_KEY_INSERT -> "Insert";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_LEFT -> "←";
            case GLFW.GLFW_KEY_RIGHT -> "→";
            case GLFW.GLFW_KEY_UP -> "↑";
            case GLFW.GLFW_KEY_DOWN -> "↓";
            case GLFW.GLFW_KEY_HOME -> "Home";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_PAGE_UP -> "PgUp";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PgDn";
            default -> {
                if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F25) yield "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
                if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) yield String.valueOf((char) keyCode);
                if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) yield String.valueOf((char) keyCode);
                yield layoutName.apply(keyCode);
            }
        };
    }
}
