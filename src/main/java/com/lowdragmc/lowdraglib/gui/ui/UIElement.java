package com.lowdragmc.lowdraglib.gui.ui;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.ui.style.StyleContext;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaNode;
import org.appliedenergistics.yoga.YogaProps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/**
 * The base class for all UI elements.
 * <br>
 * LDLib uses Yoga for layout. please refer to the see <a href="https://www.yogalayout.dev/">Yoga Documentation</a> for more information.
 *
 */
@RemapPrefixForJS("kjs$")
public class UIElement {
    // core ui
    @Getter
    protected final YogaNode layoutNode;
    @Getter
    @Nullable
    private ModularUI modularUI;
    // structure
    @Nullable
    private UIElement parent;
    private final List<UIElement> children = new ArrayList<>();
    // style
    @Getter
    private final String id = "";
    @Getter
    private final List<String> classes = new ArrayList<>();
    @Getter
    private final StyleContext styleContext = createStyleContext();
    private IGuiTexture backgroundTexture = IGuiTexture.EMPTY;
    private IGuiTexture borderTexture = ColorPattern.GRAY.borderTexture(1);

    @Getter
    @Configurable(name = "ldlib.gui.editor.name.hover_tips", tips = "ldlib.gui.editor.tips.hover_tips")
    protected final List<Component> tooltipTexts = new ArrayList<>();

    public UIElement() {
        layoutNode = new YogaNode();
        layoutNode.setContext(this);
    }

    /**
     * Set the Modular UI for this element. In general, this method should only be called automatically.
     * You should not call this method manually.
     */
    protected void _setModularUIInternal(ModularUI gui) {
        this.modularUI = gui;
    }

    /// Layout
    public YogaProps getLayout() {
        return layoutNode;
    }

    public UIElement layout(Consumer<YogaProps> layout) {
        layout.accept(layoutNode);
        return this;
    }

    /// Structure
    @Nullable
    public UIElement getParent() {
        return parent;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public List<UIElement> getChildren() {
        return children;
    }

    public boolean hasChild(UIElement child) {
        return children.contains(child);
    }

    public UIElement addChildAt(UIElement child, int index) {
        if (child == null) {
            return this;
        }
        if (child == this) {
            throw new IllegalArgumentException("Cannot add self as a child");
        }
        if (hasChild(child)) {
            throw new IllegalArgumentException("Cannot add the same child twice");
        }
        if (child.hasParent()) {
            assert child.getParent() != null;
            child.getParent().removeChild(child);
        }
        child.parent = this;
        if (this.modularUI != null) {
            child.modularUI = modularUI;
        }
        children.add(index, child);
        layoutNode.addChildAt(child.layoutNode, index);
        return this;
    }

    public UIElement addChild(UIElement child) {
        addChildAt(child, children.size());
        return this;
    }

    public UIElement addChildren(UIElement... children) {
        Arrays.stream(children).forEach(this::addChild);
        return this;
    }

    public boolean removeChild(UIElement child) {
        if (child == null) {
            return false;
        }
        if (!hasChild(child)) {
            return false;
        }
        children.remove(child);
        layoutNode.removeChild(child.layoutNode);
        child.parent = null;
        return true;
    }

    /// Style
    public boolean hasClass(String identifier) {
        return classes.contains(identifier);
    }

    public String getElementName() {
        // TODO use LDLRegister instead
        return getClass().getSimpleName();
    }

    protected StyleContext createStyleContext() {
        return new StyleContext(this, getInlineStyleValues());
    }

    protected Map<String, StyleValue<?>> getInlineStyleValues() {
        return new HashMap<>();
    }

    public boolean supportStyle(String name) {
        return true;
    }

    /**
     * Apply a style to the element. it will be triggered by the {@link StyleContext}.
     * Apply the actual logic of the style to the element.
     * @param key the style key
     * @param value the style value. if null, it means the style is removed.
     */
    public void applyStyle(String key, @Nullable StyleValue<?> value) {
    }

    /// Tooltip
    @HideFromJS
    public UIElement setHoverTooltips(String... tooltipText) {
        tooltipTexts.clear();
        appendHoverTooltips(tooltipText);
        return this;
    }

    @HideFromJS
    public UIElement setHoverTooltips(Component... tooltipText) {
        tooltipTexts.clear();
        appendHoverTooltips(tooltipText);
        return this;
    }

    @HideFromJS
    public UIElement setHoverTooltips(List<Component> tooltipText) {
        tooltipTexts.clear();
        appendHoverTooltips(tooltipText);
        return this;
    }

    public UIElement appendHoverTooltips(String... tooltipText) {
        Arrays.stream(tooltipText).filter(Objects::nonNull).filter(s->!s.isEmpty()).map(
                Component::translatable).forEach(tooltipTexts::add);
        return this;
    }

    public UIElement appendHoverTooltips(Component... tooltipText) {
        Arrays.stream(tooltipText).filter(Objects::nonNull).forEach(tooltipTexts::add);
        return this;
    }

    public UIElement appendHoverTooltips(List<Component> tooltipText) {
        tooltipTexts.addAll(tooltipText);
        return this;
    }

    public UIElement kjs$setHoverTooltips(Component... tooltipText) {
        tooltipTexts.clear();
        Arrays.stream(tooltipText).filter(Objects::nonNull).forEach(tooltipTexts::add);
        return this;
    }

    /// Rendering
    /**
     * Renders the graphical user interface (GUI) element in Background.
     */
    public void drawInBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var display = layoutNode.getDisplay();
        if (display == YogaDisplay.NONE) {
            return;
        }
        if (display == YogaDisplay.FLEX) {
            drawBackgroundTexture(guiGraphics, mouseX, mouseY, partialTick);
        }
        children.forEach(child -> child.drawInBackground(guiGraphics, mouseX, mouseY, partialTick));
    }

    public void drawBackgroundTexture(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (backgroundTexture != IGuiTexture.EMPTY) {
            backgroundTexture.draw(graphics, mouseX, mouseY, layoutNode.getLayoutX(), layoutNode.getLayoutY(),
                    layoutNode.getLayoutWidth(), layoutNode.getLayoutHeight(), partialTicks);
        }
    }

    /**
     * Renders the graphical user interface (GUI) element in Foreground.
     */
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public String toString() {
        return getElementName() + "{" + id + "}";
    }
}
