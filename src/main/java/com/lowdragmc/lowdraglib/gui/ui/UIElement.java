package com.lowdragmc.lowdraglib.gui.ui;

import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.ui.style.StyleContext;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaConstants;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaNode;
import org.appliedenergistics.yoga.YogaProps;
import org.appliedenergistics.yoga.numeric.FloatOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;

/**
 * The base class for all UI elements.
 * <br>
 * LDLib uses Yoga for layout. please refer to the see <a href="https://www.yogalayout.dev/">Yoga Documentation</a> for more information.
 *
 */
@RemapPrefixForJS("kjs$")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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
    @Getter
    private final List<UIElement> children = new ArrayList<>();
    @Getter
    private final List<UIElement> waitToRemoved = new ArrayList<>();
    @Getter
    private final List<UIElement> waitToAdded = new ArrayList<>();
    // style
    @Getter @Setter
    @Accessors(chain = true)
    private String id = "";
    @Getter
    private final List<String> classes = new ArrayList<>();
    @Getter
    private final StyleContext styleContext = createStyleContext();
    @Getter @Setter
    @Accessors(chain = true)
    private boolean drawBackgroundWhenHover = true;
    @Getter @Setter
    @Accessors(chain = true)
    private IGuiTexture backgroundTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    @Accessors(chain = true)
    private IGuiTexture borderTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    @Accessors(chain = true)
    private IGuiTexture overlayTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    @Accessors(chain = true)
    private IGuiTexture hoverTexture = IGuiTexture.EMPTY;
    @Getter @Setter
    @Accessors(chain = true)
    private boolean isVisible = true;
    @Getter @Setter
    @Accessors(chain = true)
    private boolean isActive = true;
    // runtime
    private FloatOptional positionX = FloatOptional.of();
    private FloatOptional positionY = FloatOptional.of();

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
        if (this.modularUI == gui) return;
        this.modularUI = gui;
        for (var child : children) {
            child._setModularUIInternal(gui);
        }
    }

    /// Layout
    public YogaProps getLayout() {
        return layoutNode;
    }

    public UIElement layout(Consumer<YogaProps> layout) {
        layout.accept(layoutNode);
        return this;
    }

    /**
     * Calculate the layout of the element and its children.
     * Call it to update layout manually if the node {@link YogaNode#hasNewLayout()},
     */
    public void calculateLayout() {
        if (layoutNode.hasNewLayout()) {
            layoutNode.calculateLayout(YogaConstants.UNDEFINED, YogaConstants.UNDEFINED);
        }
        positionX = FloatOptional.of();
        positionY = FloatOptional.of();
        for (var child : children) {
            child.positionX = FloatOptional.of();
            child.positionY = FloatOptional.of();
        }
    }

    /**
     * The X offset relative to the border box of the node's parent, along with dimensions, and the resolved values for margin, border, and padding for each physical edge.
     */
    public final float getLayoutX() {
        return parent == null ? modularUI == null ? 0 : modularUI.getLeftPos() : layoutNode.getLayoutX();
    }

    /**
     * The Y offset relative to the border box of the node's parent, along with dimensions, and the resolved values for margin, border, and padding for each physical edge.
     */
    public final float getLayoutY() {
        return parent == null ? modularUI == null ? 0 : modularUI.getTopPos() : layoutNode.getLayoutY();
    }

    /**
     * The absolute X offset relative to the screen.
     */
    public final float getPositionX() {
        if (positionX.isUndefined()) {
            positionX = FloatOptional.of(getLayoutX() + (parent == null ? 0 : parent.getPositionX()));
        }
        return positionX.getValue();
    }

    /**
     * The absolute Y offset relative to the screen.
     */
    public final float getPositionY() {
        if (positionY.isUndefined()) {
            positionY = FloatOptional.of(getLayoutY() + (parent == null ? 0 : parent.getPositionY()));
        }
        return positionY.getValue();
    }

    public final float getSizeWidth() {
        return layoutNode.getLayoutWidth();
    }

    public final float getSizeHeight() {
        return layoutNode.getLayoutHeight();
    }

    /// Structure
    @Nullable
    public UIElement getParent() {
        return parent;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public boolean hasChild(UIElement child) {
        return children.contains(child);
    }

    public UIElement addChildAt(@Nullable UIElement child, int index) {
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

    public UIElement addChild(@Nullable UIElement child) {
        return addChildAt(child, children.size());
    }

    public UIElement addChildren(UIElement... children) {
        Arrays.stream(children).forEach(this::addChild);
        return this;
    }

    public boolean removeChild(@Nullable UIElement child) {
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

    public void clearAllChildren() {
        for (var element : new ArrayList<>(this.children)) {
            removeChild(element);
        }
        synchronized (waitToRemoved) {
            if (!waitToRemoved.isEmpty()) {
                waitToRemoved.clear();
            }
        }
        synchronized (waitToAdded) {
            if (!waitToAdded.isEmpty()) {
                waitToAdded.clear();
            }
        }
    }

    public void init(int screenWidth, int screenHeight) {
        for (var child : children) {
            child.init(screenWidth, screenHeight);
        }
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

    /// Interaction
    public boolean isMouseOverElement(double mouseX, double mouseY) {
        return isMouseOver(getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), mouseX, mouseY);
    }

    @Nullable
    public UIElement getHoverElement(double mouseX, double mouseY) {
        if (!isDisplayed() || !isVisible()) return null;
        for (int i = getChildren().size() - 1; i >= 0; i--) {
            var hovered = children.get(i).getHoverElement(mouseX, mouseY);
            if (hovered != null) {
                return hovered;
            }
        }
        if (isMouseOver(getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), mouseX, mouseY)) {
            return this;
        }
        return null;
    }

    public static boolean isMouseOver(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && x + width > mouseX && y + height > mouseY;
    }

    /// Logic
    public void screenTick() {
        for (var child : children) {
            if (child.isActive()) {
                child.screenTick();
            }
        }
        synchronized (waitToRemoved) {
            if (!waitToRemoved.isEmpty()) {
                waitToRemoved.forEach(this::removeChild);
                waitToRemoved.clear();
            }
        }
        synchronized (waitToAdded) {
            if (!waitToAdded.isEmpty()) {
                waitToAdded.forEach(this::addChild);
                waitToAdded.clear();
            }
        }
    }
    
    /// Rendering
    public boolean isDisplayed() {
        return layoutNode.getDisplay() != YogaDisplay.NONE;
    }

    /**
     * Renders the graphical user interface (GUI) element in Background.
     * Render phases are:
     * <li> 1. Background
     * <li> 2. Background Additional
     * <li> 3. Overlay
     * <li> 4. Children
     */
    public void drawInBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var display = layoutNode.getDisplay();
        if (display == YogaDisplay.NONE) {
            return;
        }
        if (display == YogaDisplay.FLEX) {
            drawBackgroundTexture(guiGraphics, mouseX, mouseY, partialTick);
            drawBackgroundAdditional(guiGraphics, mouseX, mouseY, partialTick);
            drawOverlayTexture(guiGraphics, mouseX, mouseY, partialTick);
        }
        children.forEach(child -> {
            if (child.isVisible()) {
                child.drawInBackground(guiGraphics, mouseX, mouseY, partialTick);
            }
        });
    }

    /**
     * Renders the background texture of the GUI element.
     */
    public void drawBackgroundTexture(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var border = getBorderTexture();
        if (border != IGuiTexture.EMPTY) {
            // TODO border rendering
            border.draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        }
        var isHovered = isMouseOverElement(mouseX, mouseY);
        var bg = getBackgroundTexture();
        var hover = getHoverTexture();
        if (bg != IGuiTexture.EMPTY && (!isHovered || drawBackgroundWhenHover)) {
            bg.draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        }
        if (hover != IGuiTexture.EMPTY && isHovered && isActive()) {
            hover.draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
        }
    }

    /**
     * Renders the additional background of the GUI element.
     */
    public void drawBackgroundAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

    }

    /**
     * Renders the overlay texture of the GUI element.
     */
    public void drawOverlayTexture(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var overlay = getOverlayTexture();
        if (overlay != IGuiTexture.EMPTY) {
            overlay.draw(graphics, mouseX, mouseY, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), partialTicks);
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

    public List<Component> getDebugInfo() {
        var info = new ArrayList<Component>();
        info.add(Component.literal("[type: %s, pos: (%.1f %.1f), size: (%.1f, %.1f), children: %d]".formatted(
                getElementName(), getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(), children.size())));
        info.add(Component.literal("[id: %s, class: \"%s\"]".formatted(getId().isEmpty() ? "empty" : getId(), String.join(" ", classes))));
        return info;
    }

}
