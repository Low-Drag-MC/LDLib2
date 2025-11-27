package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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
    @Nullable
    protected Predicate<Class<?>> canPaste;
    @Nullable
    protected Consumer<?> onPaste;
    // runtime
    @Setter @Nullable
    private Component notifyName;

    public Configurator() {
        this("");
    }

    public Configurator(String name) {
        this.lineContainer = new UIElement().addClass("__configurator_line__");
        this.label = (Label) new Label().addClass("__configurator_label__");
        this.inlineContainer = new UIElement().addClass("__configurator_inline__");
        this.tip = new UIElement().addClass("__configurator_tip__");

        getLayout().setGap(YogaGutter.ALL, 1);

        addChild(this.lineContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setGap(YogaGutter.ALL, 2);
        }).addChildren(
                this.label.textStyle(textStyle -> {
                    textStyle.adaptiveWidth(true);
                    textStyle.textAlignVertical(Vertical.CENTER);
                }).setText(name).layout(layout -> {
                    layout.setHeight(14);
                }),
                this.inlineContainer.layout(layout -> layout.setFlex(1)),
                this.tip.layout(layout -> {
                    layout.setWidth(14);
                    layout.setHeight(14);
                }).style(style -> style.backgroundTexture(Icons.HELP))));
        if (name.isEmpty()) {
            this.label.setDisplay(YogaDisplay.NONE);
        }
        this.tip.setDisplay(YogaDisplay.NONE);

        this.addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);

        this.lineContainer.moveInlineAsDefault();
        this.inlineContainer.moveInlineAsDefault();
        this.label.moveInlineAsDefault();
        this.tip.moveInlineAsDefault();
    }

    public Configurator setLabel(String name) {
        this.label.setText(name);
        this.label.setDisplay(name.isEmpty() ? YogaDisplay.NONE : YogaDisplay.FLEX);
        return this;
    }

    public Configurator setLabel(Component name) {
        this.label.setText(name);
        this.label.setDisplay(name.getString().isEmpty() ? YogaDisplay.NONE : YogaDisplay.FLEX);
        return this;
    }

    public Component getLabel() {
        return this.label.getText();
    }

    public Component getNotifyName() {
        return notifyName == null ? getLabel() : notifyName;
    }

    public Configurator setTips(String... tips) {
        this.tip.style(style -> style.appendTooltipsString(tips));
        this.tip.setDisplay(tips.length > 0 ? YogaDisplay.FLEX : YogaDisplay.NONE);
        return this;
    }

    public Configurator setTips(Component... tips) {
        this.tip.style(style -> style.appendTooltips(tips));
        this.tip.setDisplay(tips.length > 0 ? YogaDisplay.FLEX : YogaDisplay.NONE);
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
            if (menu != null && !menu.isEmpty()) {
                var mui = getModularUI();
                if (mui != null) {
                    var root = mui.ui.rootElement;
                    root.addChild(new Menu<>(menu.build(), TreeBuilder.Menu::uiProvider)
                            .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                            .setOnNodeClicked(TreeBuilder.Menu::handle)
                            .layout(layout -> {
                                layout.setPosition(YogaEdge.LEFT, event.x - root.getContentX());
                                layout.setPosition(YogaEdge.TOP, event.y - root.getContentY());
                            })
                    );
                }

            }
        }
    }

    protected TreeBuilder.Menu createMenu() {
        var menu = TreeBuilder.Menu.start();
        if (copyFunction != null) {
            menu.leaf(Icons.COPY, Component.translatable("ldlib.gui.editor.menu.copy.type", copyFunction.get().get().getClass().getSimpleName()), () -> {
                try {
                    ClipboardManager.INSTANCE.copy(copyFunction.get());
                } catch (Exception ignored) {}
            });
        }
        if (canPaste != null && ClipboardManager.INSTANCE.getClipboardType() != null && canPaste.test(ClipboardManager.INSTANCE.getClipboardType())) {
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
