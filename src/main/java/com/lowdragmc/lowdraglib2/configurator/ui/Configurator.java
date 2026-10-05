package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@Accessors(chain = true)
public class Configurator extends UIElement {
    /**
     * The {@code configurator.change} is sent when a change is made by a configurator.
     * The {@link UIEvent#target} refers to the {@link Configurator} that triggered the change.
     */
    public static final String CHANGE_EVENT = "configurator.change";
    public final UIElement lineContainer;
    public final Label label;
    public final UIElement inlineContainer;
    public final UIElement tip;

    @Nullable
    protected Supplier<Supplier<?>> copyFunction;
    @Setter
    protected boolean copyDirect = false;
    @Nullable
    protected Predicate<Class<?>> canPaste;
    @Nullable
    protected Consumer<?> onPaste;
    /**
     * The field this row edits, when {@link com.lowdragmc.lowdraglib2.configurator.ConfiguratorParser}
     * made it from one — and, on a group made for a sub-configurable field, that field. What lets a
     * caller find the row's value in what the object saved
     * ({@link com.lowdragmc.lowdraglib2.utils.PersistedParser#persistedKey}): an editor marking the
     * rows whose value differs from a template's, say. Null for a row built by hand.
     */
    @Setter @Nullable
    private java.lang.reflect.Field sourceField;
    private final java.util.List<java.util.function.BiConsumer<Configurator, TreeBuilder.Menu>> menuContributors =
            new java.util.ArrayList<>();
    // runtime
    @Setter @Nullable
    private Component notifyName;
    private boolean overridden;
    /** The label as it was set, while {@link #setOverridden} draws it bold. */
    @Nullable
    private Component plainLabel;
    @Nullable
    private UIElement overriddenMark;

    public Configurator() {
        this("");
    }

    public Configurator(String name) {
        this.lineContainer = new UIElement().addClass("__configurator_line__");
        this.label = (Label) new Label().addClass("__configurator_label__");
        this.inlineContainer = new UIElement().addClass("__configurator_inline__");
        this.tip = new UIElement().addClass("__configurator_tip__");

        getLayout().gapAll(1);

        addChild(this.lineContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(2);
        }).addChildren(
                this.label.textStyle(textStyle -> {
                    textStyle.adaptiveWidth(true);
                    textStyle.textAlignVertical(Vertical.CENTER);
                }).setText(name).layout(layout -> {
                    layout.height(14);
                }),
                this.inlineContainer.layout(layout -> layout.flex(1)),
                this.tip.layout(layout -> {
                    layout.width(14);
                    layout.height(14);
                }).style(style -> style.backgroundTexture(Icons.HELP))));
        if (name.isEmpty()) {
            this.label.setDisplay(false);
        }
        this.tip.setDisplay(false);

        this.addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);

        this.lineContainer.moveInlineAsDefault();
        this.inlineContainer.moveInlineAsDefault();
        this.label.moveInlineAsDefault();
        this.tip.moveInlineAsDefault();
    }

    public Configurator setLabel(String name) {
        this.label.setText(name);
        this.label.setDisplay(!name.isEmpty());
        reapplyOverridden();
        return this;
    }

    public Configurator setLabel(Component name) {
        this.label.setText(name);
        this.label.setDisplay(!name.getString().isEmpty());
        reapplyOverridden();
        return this;
    }

    /** The label as it was set — without the bold {@link #setOverridden} draws it in. */
    public Component getLabel() {
        return overridden && plainLabel != null ? plainLabel : this.label.getText();
    }

    @Nullable
    public java.lang.reflect.Field getSourceField() {
        return sourceField;
    }

    // ---- overridden ----

    /** The bar Unity draws beside an overridden property of a prefab instance. */
    public static final int OVERRIDDEN_COLOR = 0xFF4A9EFF;

    public boolean isOverridden() {
        return overridden;
    }

    /**
     * Draws this row as holding a value that differs from where it came from — Unity's blue bar and
     * bold label on a prefab instance's overridden property. Only the look: what counts as different,
     * and what may be done about it ({@link #addMenuContributor}), is the caller's to say.
     */
    public Configurator setOverridden(boolean overridden) {
        if (this.overridden == overridden) {
            return this;
        }
        this.overridden = overridden;
        if (overridden) {
            this.plainLabel = null;
            reapplyOverridden();
        } else if (plainLabel != null) {
            this.label.setText(plainLabel);
            this.plainLabel = null;
        }
        if (overriddenMark == null) {
            overriddenMark = new UIElement().addClass("__configurator_overridden__").layout(layout -> {
                layout.width(2);
                layout.height(14);
            }).style(style -> style.backgroundTexture(new ColorRectTexture(OVERRIDDEN_COLOR)));
            this.lineContainer.addChildAt(overriddenMark, 0);
        }
        overriddenMark.setDisplay(overridden);
        return this;
    }

    /** The label just set, drawn bold again while this row is overridden. */
    private void reapplyOverridden() {
        if (overridden) {
            this.plainLabel = this.label.getText();
            this.label.setText(plainLabel.copy().withStyle(ChatFormatting.BOLD));
        }
    }

    // ---- the right-click menu, from outside ----

    /**
     * Adds entries to the right-click menu of this configurator <b>and of every configurator under
     * it</b>, after the row's own (copy, paste). Called with the configurator that was clicked, so
     * one contributor on a group serves every row in it — which is what an inspector that owns a
     * whole group needs: a "revert this to the template's value" beside every row it can say that of.
     */
    public Configurator addMenuContributor(java.util.function.BiConsumer<Configurator, TreeBuilder.Menu> contributor) {
        menuContributors.add(contributor);
        return this;
    }

    public Component getNotifyName() {
        return notifyName == null ? getLabel() : notifyName;
    }

    public Configurator setTips(String... tips) {
        this.tip.style(style -> style.appendTooltipsString(tips));
        this.tip.setDisplay(tips.length > 0);
        return this;
    }

    public Configurator setTips(Component... tips) {
        this.tip.style(style -> style.appendTooltips(tips));
        this.tip.setDisplay(tips.length > 0);
        return this;
    }

    public Configurator addInlineChild(UIElement child) {
        this.inlineContainer.addChild(child);
        return this;
    }

    public Configurator addInlineChildren(UIElement... children) {
        this.inlineContainer.addChildren(children);
        return this;
    }

    public Configurator addInlineChildAt(UIElement child, int index) {
        this.inlineContainer.addChildAt(child, index);
        return this;
    }

    @Override
    public Configurator addChildAt(@Nullable UIElement child, int index) {
        return (Configurator) super.addChildAt(child, index);
    }

    @Override
    public Configurator addChild(@Nullable UIElement child) {
        return (Configurator) super.addChild(child);
    }

    @Override
    public Configurator addChildren(UIElement... children) {
        return (Configurator) super.addChildren(children);
    }

    public void notifyChanges() {
        notifyChanges(this);
    }

    public final void notifyChanges(Configurator source) {
        var event = UIEvent.create(CHANGE_EVENT);
        event.target = source;
        UIEventDispatcher.dispatchEvent(event);
    }

    public Configurator setCopiable(Supplier<Supplier<?>> copyFunction) {
        this.copyFunction = copyFunction;
        return this;
    }

    public Configurator setCopiableDirect(Object value) {
        this.copyDirect = true;
        return setCopiable(() -> () -> value);
    }

    public Configurator setPastable(Predicate<Class<?>> canPaste, Consumer<?> onPaste) {
        this.canPaste = canPaste;
        this.onPaste = onPaste;
        return this;
    }

    public <T> Configurator setPastable(Class<T> canPaste, Consumer<T> onPaste) {
        return setPastable(canPaste::isAssignableFrom, onPaste);
    }

    /// Menu
    protected void onMouseDown(UIEvent event) {
        if (event.button == 1) {
            var menu = createMenu();
            if (menu != null) {
                contributeTo(menu);
            }
            if (menu != null && !menu.isEmpty()) {
                var mui = getModularUI();
                if (mui != null) {
                    var root = mui.ui.rootElement;
                    var layoutOffset = root.worldToLocalLayoutOffset(new Vector2f(event.x, event.y));
                    root.addChild(new Menu<>(menu.build(), TreeBuilder.Menu::uiProvider)
                            .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                            .setOnNodeClicked(TreeBuilder.Menu::handle)
                            .layout(layout -> {
                                layout.left(layoutOffset.x);
                                layout.top(layoutOffset.y);
                            })
                    );
                    // the row that opened a menu is the one that was clicked: the group around it
                    // would otherwise open a second one on top, of its own entries, from the same click
                    event.stopPropagation();
                }

            }
        }
    }

    /** Lets this configurator and every one around it add to this one's menu — see {@link #addMenuContributor}. */
    private void contributeTo(TreeBuilder.Menu menu) {
        for (UIElement element = this; element != null; element = element.getParent()) {
            if (element instanceof Configurator configurator) {
                for (var contributor : configurator.menuContributors) {
                    contributor.accept(this, menu);
                }
            }
        }
    }

    protected TreeBuilder.Menu createMenu() {
        var menu = TreeBuilder.Menu.start();
        if (copyFunction != null) {
            var copyValue = copyFunction.get().get();
            menu.leaf(Icons.COPY, Component.translatable("ldlib.gui.editor.menu.copy.type", copyValue.getClass().getSimpleName()), () -> {
                try {
                    if (copyDirect) {
                        ClipboardManager.INSTANCE.copyDirect(copyValue);
                    } else {
                        ClipboardManager.INSTANCE.copy(copyFunction.get());
                    }
                } catch (Exception ignored) {}
            });
        }
        if (canPaste != null && isActiveInHierarchy() && ClipboardManager.INSTANCE.getClipboardType() != null
                && canPaste.test(ClipboardManager.INSTANCE.getClipboardType())) {
            menu.leaf(Icons.PASTE, Component.translatable("ldlib.gui.editor.menu.paste.type", ClipboardManager.INSTANCE.getClipboardType().getSimpleName()), () -> {
                try {
                    var pasted = ClipboardManager.INSTANCE.paste();
                    if (pasted != null && onPaste != null) {
                        ((Consumer)onPaste).accept(pasted);
                    }
                } catch (Exception ignored) {}
            });
        }
        return menu;
    }

}
