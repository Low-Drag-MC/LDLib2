package com.lowdragmc.lowdraglib2.editor.keymap;

import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * The actions one editor knows about and the chords they answer to.
 *
 * <p>Registration is per editor instance, so an editor, a view and a project can each add their own —
 * see {@link #register}. What the user changed lives in {@link #getOverrides()} and is persisted by the
 * keymap settings page; defaults stay in the {@link EditorAction}s, so a later version can change a
 * default without stepping on a user who never touched it.
 *
 * <p>Free of any UI type on purpose: everything the dispatch needs to know about the editor arrives
 * through {@link KeymapContext}.
 */
public class Keymap {

    /** The chords one action answers to: the main one, and an optional second. */
    public record Bindings(KeyChord primary, KeyChord secondary) {
        public static final Bindings UNBOUND = new Bindings(KeyChord.UNBOUND, KeyChord.UNBOUND);

        public static Bindings of(KeyChord primary, KeyChord secondary) {
            return new Bindings(primary, secondary);
        }

        public boolean matches(KeyChord chord) {
            return chord.isBound() && (primary.equals(chord) || secondary.equals(chord));
        }

        public boolean isUnbound() {
            return !primary.isBound() && !secondary.isBound();
        }

        public Bindings withPrimary(KeyChord chord) {
            return new Bindings(chord, secondary);
        }

        public Bindings withSecondary(KeyChord chord) {
            return new Bindings(primary, chord);
        }
    }

    /** Actions that answer to the same chord. Allowed — they may well be in different contexts. */
    public record Conflict(KeyChord chord, List<EditorAction> actions) {}

    /**
     * The grey chord a menu entry carries, or nothing when the action is unbound.
     *
     * <p>Menus used to spell their shortcut out in the translation, which stopped being true the moment
     * a shortcut could be changed.
     */
    public Component shortcutHint(ResourceLocation id) {
        var chord = bindingsOf(id).primary();
        return chord.isBound()
                ? Component.literal(" " + chord.toDisplayString()).withStyle(ChatFormatting.GRAY)
                : Component.empty();
    }

    /** The action's name with its chord after it, for a menu entry. */
    public Component menuLabel(ResourceLocation id, String translationKey) {
        return Component.translatable(translationKey).append(shortcutHint(id));
    }

    private record Registration(EditorAction action, int sequence) {}

    private final Map<ResourceLocation, Registration> registrations = new LinkedHashMap<>();
    private final Map<ResourceLocation, Bindings> overrides = new LinkedHashMap<>();
    private int sequence;

    /**
     * Adds an action, replacing any earlier one with the same id — which is how a subclass or a project
     * takes over a built-in action rather than fighting it for the same chord.
     *
     * @return a handle that removes this action again. Removing is a no-op once something else has
     *         registered the same id, so a view being disposed cannot unregister its successor.
     */
    public ISubscription register(EditorAction action) {
        var registration = new Registration(action, sequence++);
        registrations.put(action.id(), registration);
        return () -> registrations.remove(action.id(), registration);
    }

    /** Registers several actions at once; the returned handle removes all of them. */
    public ISubscription registerAll(EditorAction... actions) {
        ISubscription subscription = () -> {};
        for (var action : actions) {
            subscription = subscription.andThen(register(action));
        }
        return subscription;
    }

    public void unregister(ResourceLocation id) {
        registrations.remove(id);
    }

    public Collection<EditorAction> getActions() {
        return registrations.values().stream().map(Registration::action).toList();
    }

    public Optional<EditorAction> getAction(ResourceLocation id) {
        var registration = registrations.get(id);
        return registration == null ? Optional.empty() : Optional.of(registration.action());
    }

    /** The action ids in registration order, which is the order the settings page lists them in. */
    public List<ResourceLocation> getActionIds() {
        return List.copyOf(registrations.keySet());
    }

    /** What this action answers to right now: the user's override if there is one, else its defaults. */
    public Bindings bindingsOf(ResourceLocation id) {
        var override = overrides.get(id);
        if (override != null) return override;
        return getAction(id)
                .map(action -> new Bindings(action.defaultPrimary(), action.defaultSecondary()))
                .orElse(Bindings.UNBOUND);
    }

    public Bindings defaultBindingsOf(ResourceLocation id) {
        return getAction(id)
                .map(action -> new Bindings(action.defaultPrimary(), action.defaultSecondary()))
                .orElse(Bindings.UNBOUND);
    }

    public boolean isOverridden(ResourceLocation id) {
        return overrides.containsKey(id);
    }

    /**
     * Rebinds an action. Setting it back to its defaults drops the override rather than storing a copy,
     * so the binding keeps following the default if that default ever changes.
     */
    public void setBindings(ResourceLocation id, Bindings bindings) {
        if (bindings.equals(defaultBindingsOf(id)) && getAction(id).isPresent()) {
            overrides.remove(id);
        } else {
            overrides.put(id, bindings);
        }
    }

    /** Forgets the user's binding for this action, whatever it was. */
    public void resetBindings(ResourceLocation id) {
        overrides.remove(id);
    }

    public void resetAllBindings() {
        overrides.clear();
    }

    /**
     * The user's bindings, by action id. Includes ids this keymap has no action for: they belong to
     * another editor that shares the settings file, and dropping them here would wipe that editor's
     * keymap the next time this one saved.
     */
    public Map<ResourceLocation, Bindings> getOverrides() {
        return new LinkedHashMap<>(overrides);
    }

    public void setOverrides(Map<ResourceLocation, Bindings> overrides) {
        this.overrides.clear();
        this.overrides.putAll(overrides);
    }

    /**
     * Every action that answers to this chord, most specific first: context specificity, then priority,
     * then the most recently registered — so a project's action takes precedence over the editor's.
     *
     * <p>Context is not consulted here; {@link #dispatch} does that, because a candidate whose context
     * does not hold must not hide the one behind it.
     */
    public List<EditorAction> candidates(KeyChord chord) {
        if (!chord.isBound()) return List.of();
        var candidates = new ArrayList<Registration>();
        for (var registration : registrations.values()) {
            if (bindingsOf(registration.action().id()).matches(chord)) {
                candidates.add(registration);
            }
        }
        candidates.sort(Comparator
                .comparingInt((Registration r) -> r.action().when().specificity()).reversed()
                .thenComparing(Comparator.comparingInt((Registration r) -> r.action().priority()).reversed())
                .thenComparing(Comparator.comparingInt(Registration::sequence).reversed()));
        return candidates.stream().map(Registration::action).toList();
    }

    /**
     * Runs the first action that wants the chord and is in context.
     *
     * <p>An action whose handler returns false is treated as not having run at all, and the next
     * candidate gets its turn — that is how {@code Delete} reaches the asset browser when the graph
     * editor has nothing selected.
     *
     * @return the action that ran, or null if none did.
     */
    @Nullable
    public EditorAction dispatch(KeymapContext context) {
        for (var action : candidates(context.chord())) {
            if (!action.when().test(context)) continue;
            if (action.handler().run(context)) {
                return action;
            }
        }
        return null;
    }

    /**
     * Chords more than one action answers to, for the settings page to point at.
     *
     * <p>Reported rather than prevented: two actions in different panels sharing a key is normal and
     * useful, and only the user can say whether a particular overlap is a mistake.
     */
    public List<Conflict> conflicts() {
        var byChord = new LinkedHashMap<KeyChord, List<EditorAction>>();
        for (var registration : registrations.values()) {
            var action = registration.action();
            var bindings = bindingsOf(action.id());
            for (var chord : List.of(bindings.primary(), bindings.secondary())) {
                if (!chord.isBound()) continue;
                byChord.computeIfAbsent(chord, __ -> new ArrayList<>()).add(action);
            }
        }
        var conflicts = new ArrayList<Conflict>();
        byChord.forEach((chord, actions) -> {
            if (actions.size() > 1) {
                conflicts.add(new Conflict(chord, List.copyOf(actions)));
            }
        });
        return conflicts;
    }

    /** Whether this chord is claimed by an action other than the given one — what the editor UI warns about. */
    public List<EditorAction> conflictsWith(ResourceLocation id, KeyChord chord) {
        if (!chord.isBound()) return List.of();
        return candidates(chord).stream().filter(action -> !action.id().equals(id)).toList();
    }

    /**
     * The same question against bindings that are not (yet) this keymap's own — what the settings page
     * asks while the user is still editing, since nothing it does takes effect until Apply.
     */
    public static List<EditorAction> conflictsWith(Collection<EditorAction> actions,
                                                   Function<EditorAction, Bindings> bindings,
                                                   ResourceLocation id, KeyChord chord) {
        if (!chord.isBound()) return List.of();
        return actions.stream()
                .filter(action -> !action.id().equals(id))
                .filter(action -> bindings.apply(action).matches(chord))
                .toList();
    }
}
