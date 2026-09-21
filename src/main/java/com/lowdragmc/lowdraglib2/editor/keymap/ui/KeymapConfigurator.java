package com.lowdragmc.lowdraglib2.editor.keymap.ui;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.editor.keymap.EditorAction;
import com.lowdragmc.lowdraglib2.editor.keymap.KeyChord;
import com.lowdragmc.lowdraglib2.editor.settings.KeymapSettings;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The keymap settings page: every action the editor knows, grouped by category, with the two chords it
 * answers to and a way to change them.
 *
 * <p>Built out of {@link ConfiguratorGroup} and {@link Configurator} rather than plain elements so that
 * it looks like the rest of the settings in every stylesheet — the group frame, the row background, the
 * label colour and the hover are all the ones a theme already defines for a configurator.
 *
 * <p>Edits go into the {@link KeymapSettings} instance, not into the live keymap — the settings dialog
 * applies on OK and throws the instance away on Cancel, and a keymap that had already been rewritten
 * would survive that.
 */
public class KeymapConfigurator extends ConfiguratorGroup {
    public static final String PAGE_CLASS = "__keymap_page__";
    public static final String SEARCH_CLASS = "__keymap_search__";
    public static final String CATEGORY_CLASS = "__keymap_category__";
    public static final String ROW_CLASS = "__keymap_row__";
    public static final String RESET_CLASS = "__keymap_reset__";

    /** How much of a row the chords get against the action's name — 2 means two thirds of it. */
    private static final float LABEL_TO_CHORDS = 2f;
    /** How the chord area is split between the main chord and the optional second one. */
    private static final float PRIMARY_SHARE = 3f;
    private static final float SECONDARY_SHARE = 2f;

    private final KeymapSettings settings;
    private final TextField search;

    public KeymapConfigurator(KeymapSettings settings) {
        super("", false);
        this.settings = settings;
        hideTitle();
        setCanCollapse(false);
        addClass(PAGE_CLASS);
        // the page is a list, not a labelled control: it takes the whole width the settings pane gives it
        layout(layout -> layout.widthPercent(100));

        this.search = new TextField();
        this.search.setAnyString();
        this.search.setText("");
        this.search.setTextResponder(text -> rebuild());
        this.search.addClass(SEARCH_CLASS);
        this.search.textFieldStyle(style -> style.placeholder(
                Component.translatable("keymap.ldlib2.search_hint")));
        this.search.layout(layout -> layout.flex(1).height(12));

        rebuild();
    }

    /**
     * Rebuilds the list. Cheap enough to do on every change, and it keeps one piece of code responsible
     * for what a row shows — including the conflict marks, which a single rebind can change on rows
     * other than the one that was edited.
     */
    protected void rebuild() {
        // the search box is kept across rebuilds: it holds the focus and the caret while typing in it
        var configurators = new ArrayList<>(getConfigurators());
        configurators.forEach(this::removeConfigurator);

        var searchRow = new Configurator();
        searchRow.setLabel(Component.translatable("keymap.ldlib2.search"));
        searchRow.inlineContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
        });
        searchRow.addInlineChild(search);
        addConfigurator(searchRow);

        var query = search.getValue().trim().toLowerCase(Locale.ROOT);
        var byCategory = new LinkedHashMap<String, List<EditorAction>>();
        for (var action : settings.getActions()) {
            if (!matches(action, query)) continue;
            byCategory.computeIfAbsent(action.category(), key -> new ArrayList<>()).add(action);
        }
        if (byCategory.isEmpty()) {
            var empty = new Configurator();
            empty.setLabel(Component.translatable("keymap.ldlib2.no_match")
                    .withStyle(Style.EMPTY.withItalic(true)));
            addConfigurator(empty);
            return;
        }
        byCategory.forEach((category, actions) -> {
            var group = new ConfiguratorGroup(Component.translatable(category).getString(), false);
            group.addClass(CATEGORY_CLASS);
            for (var action : actions) {
                group.addConfigurator(createRow(action));
            }
            addConfigurator(group);
        });
    }

    /** Matches the action's name, its category and what it is bound to, so "ctrl+s" finds it too. */
    protected boolean matches(EditorAction action, String query) {
        if (query.isEmpty()) return true;
        var bindings = settings.bindingsOf(action);
        return contains(action.displayName().getString(), query)
                || contains(Component.translatable(action.category()).getString(), query)
                || contains(action.id().toString(), query)
                || contains(bindings.primary().toDisplayString(), query)
                || contains(bindings.secondary().toDisplayString(), query);
    }

    private static boolean contains(String text, String query) {
        return text.toLowerCase(Locale.ROOT).contains(query);
    }

    protected Configurator createRow(EditorAction action) {
        var bindings = settings.bindingsOf(action);
        var row = new Configurator();
        row.setLabel(action.displayName());
        row.addClass(ROW_CLASS);
        // A name longer than its column stays on its line and scrolls when hovered, rather than
        // growing into the chords: this list is read by scanning the left edge.
        row.label.textStyle(textStyle -> {
            textStyle.adaptiveWidth(false);
            textStyle.textWrap(TextWrap.HOVER_ROLL);
        });
        row.label.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
        });
        // and it is cut off at the edge of its column rather than drawn over the chord next to it
        row.label.setOverflowVisible(false);
        // the id, for the reader who wants to know which action a row actually is
        row.label.style(style -> style.appendTooltips(action.displayName(),
                Component.literal(action.id().toString()).withStyle(Style.EMPTY.withItalic(true))));
        row.inlineContainer.layout(layout -> {
            // A configurator's inline area is a column by default; the two chords and the reset button
            // belong on one line. It also takes twice the row rather than the usual half: the chords are
            // what this page is for, and "Ctrl+Shift+Page Down" has to fit in the width it gets.
            layout.flexDirection(FlexDirection.ROW);
            layout.flex(LABEL_TO_CHORDS);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
        });

        row.addInlineChildren(
                createField(action, bindings.primary(), PRIMARY_SHARE,
                        chord -> settings.setBindings(action, settings.bindingsOf(action).withPrimary(chord))),
                createField(action, bindings.secondary(), SECONDARY_SHARE,
                        chord -> settings.setBindings(action, settings.bindingsOf(action).withSecondary(chord))),
                createResetButton(action));
        return row;
    }

    private Button createResetButton(EditorAction action) {
        var reset = new Button();
        reset.noText().addPreIcon(Icons.HISTORY)
                .setOnClick(event -> {
                    settings.reset(action);
                    rebuild();
                })
                .addClasses(RESET_CLASS, "__white_icon__")   // white glyph: themes with light buttons re-tint it
                .layout(layout -> {
                    layout.width(12);
                    layout.height(12);
                    layout.flexShrink(0);
                })
                .style(style -> style.appendTooltips(Component.translatable("keymap.ldlib2.reset")));
        reset.setActive(settings.isOverridden(action));
        return reset;
    }

    private KeyChordField createField(EditorAction action, KeyChord chord, float share,
                                      Consumer<KeyChord> apply) {
        var field = new KeyChordField(chord);
        field.layout(layout -> {
            // Shares of the chord area rather than fixed widths, so the fields grow with whatever the
            // settings pane is. The main chord takes the larger share: it is the one that is always set,
            // and the one that has to hold "Ctrl+Shift+Page Down".
            layout.flex(share);
            layout.minWidth(28);
        });
        markConflicts(action, chord, field);
        field.setOnChordChanged(newChord -> {
            apply.accept(newChord);
            rebuild();
        });
        return field;
    }

    /**
     * Marks a chord another action already answers to. Only marked, never refused: the same key in two
     * panels is a normal thing to want, and only the user knows whether a particular overlap is one.
     */
    protected void markConflicts(EditorAction action, KeyChord chord, KeyChordField field) {
        var conflicts = settings.conflictsWith(action, chord);
        if (conflicts.isEmpty()) return;
        field.setConflicting(true);
        var tooltips = new ArrayList<Component>();
        tooltips.add(Component.translatable("keymap.ldlib2.conflict", chord.toDisplayString()));
        for (var other : conflicts) {
            tooltips.add(Component.literal("- ").append(other.displayName()));
        }
        field.style(style -> style.appendTooltips(tooltips.toArray(Component[]::new)));
    }
}
