package com.lowdragmc.lowdraglib2.editor.keymap;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Something the editor can do, and the keys it answers to by default.
 *
 * <p>An action is the unit the keymap and its settings page deal in: it has a stable {@link #id()} that
 * a saved binding refers to, a {@link #category()} to group it under, and a {@link #handler()} that does
 * the work. The chords here are only the <em>defaults</em> — what the user actually pressed to get here
 * is {@link Keymap#bindingsOf}.
 */
public record EditorAction(Identifier id,
                           String category,
                           Component displayName,
                           KeyChord defaultPrimary,
                           KeyChord defaultSecondary,
                           KeyContext when,
                           int priority,
                           Handler handler) {

    /**
     * Runs the action.
     *
     * @return true if the action did something. Returning false passes the chord to the next action
     *         that wants it, which is how one key can serve several panels without each of them
     *         knowing about the others.
     */
    @FunctionalInterface
    public interface Handler {
        boolean run(KeymapContext context);
    }

    public static Builder builder(Identifier id) {
        return new Builder(id);
    }

    /** The translation key the settings page shows this action under, unless one was set explicitly. */
    public static Component defaultDisplayName(Identifier id) {
        return Component.translatable(id.toLanguageKey("keymap"));
    }

    public static final class Builder {
        private final Identifier id;
        private String category = KeymapCategories.GENERAL;
        @Nullable
        private Component displayName;
        private KeyChord defaultPrimary = KeyChord.UNBOUND;
        private KeyChord defaultSecondary = KeyChord.UNBOUND;
        private KeyContext when = KeyContext.global();
        private int priority;
        @Nullable
        private Handler handler;

        private Builder(Identifier id) {
            this.id = id;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder displayName(Component displayName) {
            this.displayName = displayName;
            return this;
        }

        /** The chord this action answers to out of the box. */
        public Builder defaultChord(KeyChord chord) {
            this.defaultPrimary = chord;
            return this;
        }

        /** A second chord for the same action, for the users of the other convention. */
        public Builder defaultAlternative(KeyChord chord) {
            this.defaultSecondary = chord;
            return this;
        }

        public Builder when(KeyContext when) {
            this.when = when;
            return this;
        }

        /** Breaks a tie between two actions of equal specificity. Higher runs first. */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /** For an action that always applies once its context holds. */
        public Builder onAction(Runnable action) {
            return onAction(context -> {
                action.run();
                return true;
            });
        }

        public Builder onAction(Handler handler) {
            this.handler = handler;
            return this;
        }

        public EditorAction build() {
            if (handler == null) {
                throw new IllegalStateException("Key action " + id + " has no handler");
            }
            return new EditorAction(id, category,
                    displayName == null ? defaultDisplayName(id) : displayName,
                    defaultPrimary, defaultSecondary, when, priority, handler);
        }
    }
}
