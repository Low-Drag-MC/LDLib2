package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import org.w3c.dom.Element;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public abstract class SplitView extends UIElement {
    private static final Object DRAGGING = new Object();
    public final UIElement first = new UIElement();
    public final UIElement second = new UIElement();
    @Getter @Setter
    private float borderSize = 2;
    @Getter @Setter
    private float minPercentage = 5;
    @Getter @Setter
    private float maxPercentage = 95;

    public SplitView() {
        getLayout().widthPercent(100).heightPercent(100).flex(1);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);

        first.addClass("__split_view_first__");
        second.addClass("__split_view_second__");

        addChildren(first, second);
    }

    protected abstract boolean isHoverDragging(float mouseX, float mouseY);

    protected abstract SpriteTexture getDraggingIcon();

    protected abstract void onDragSourceUpdate(UIEvent event);

    public abstract SplitView setPercentage(float percentage);

    public abstract float getPercentage();

    private static float getPercentValue(TaffyDimension dimension) {
        if (!dimension.isPercent()) return 0;
        var value = dimension.getValue();
        return value <= 1 ? value * 100 : value;
    }

    public SplitView first(UIElement first) {
        this.first.clearAllChildren();
        this.first.addChild(first);
        return this;
    }

    public SplitView second(UIElement second) {
        this.second.clearAllChildren();
        this.second.addChild(second);
        return this;
    }

    protected void onMouseDown(UIEvent event) {
        // use int mouse coordinates to avoid issues with floating point precision
        if (event.button == 0 && isHoverDragging((int) event.x, (int) event.y)){
            var icon = getDraggingIcon();
            var width = icon.spriteSize.width;
            var height = icon.spriteSize.height;
            startDrag(DRAGGING, icon).setDragTexture(- width / 2f, -height / 2f, width, height);
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        if (isHoverDragging(guiContext.mouseX, guiContext.mouseY)) {
            guiContext.postRendering(ctx -> {
                var icon = getDraggingIcon();
                var width = icon.spriteSize.width;
                var height = icon.spriteSize.height;
                ctx.drawTexture(icon,
                        ctx.localMouseX - width / 2f,
                        ctx.localMouseY - height / 2f,
                        width,
                        height);
            });
        }
    }

    /// Editor + Xml
    @Override
    protected void additionalConfigurators(ConfiguratorGroup father) {
        father.addConfigurator(new NumberConfigurator("percentage",
                this::getPercentage, number -> setPercentage(number.floatValue()),
                50, true)
                .setRange(0f, 100f)
                .setType(ConfigNumber.Type.FLOAT));
    }

    @Override
    public void loadXml(Element element) {
        // percentage
        if (element.hasAttribute("percentage")) {
            setPercentage(Mth.clamp(XmlUtils.getAsFloat(element, "percentage", 50), 0f, 100f));
        }
        super.loadXml(element);
    }

    @Override
    protected void parseXmlChildElement(Element childElement) {
        if (childElement.getTagName().equals("first")) {
            first.loadXml(childElement);
        } else if (childElement.getTagName().equals("second")) {
            second.loadXml(childElement);
        }
    }

    @KJSBindings("SplitViewHorizontal")
    @LDLRegister(name = "split-view-horizontal", group = "container", registry = "ldlib2:ui_element")
    public static class Horizontal extends SplitView {
        public Horizontal() {
            getLayout().flexDirection(FlexDirection.ROW);
            first.getLayout().widthPercent(50);
            first.getLayout().heightPercent(100);
            second.getLayout().flex(1);
            second.getLayout().heightPercent(100);
            internalSetup();
        }

        @Override
        protected boolean isHoverDragging(float mouseX, float mouseY) {
            return isMouseOver(getPositionX() + first.getSizeWidth() - getBorderSize(), getPositionY(), getBorderSize(), getSizeHeight(), mouseX, mouseY);
        }

        @Override
        protected SpriteTexture getDraggingIcon() {
            return Icons.ARROW_LEFT_RIGHT;
        }

        public Horizontal left(UIElement left) {
            first(left);
            return this;
        }

        public Horizontal right(UIElement right) {
            second(right);
            return this;
        }


        @Override
        protected void onDragSourceUpdate(UIEvent event) {
            if (event.target != this || event.dragHandler.getDraggingObject() != DRAGGING) {
                return; // only handle drag events for this window
            }
            var width = getSizeWidth();
            if (width <= 0) {
                return; // prevent division by zero
            }
            var localMouse = getLocalMouse(event.x, event.y);
            setPercentage((localMouse.x - getPositionX()) / width * 100);
        }

        @Override
        public Horizontal setPercentage(float percentage) {
            first.getLayout().widthPercent(Mth.clamp(percentage, getMinPercentage(), getMaxPercentage()));
            return this;
        }

        @Override
        public float getPercentage() {
            var width = first.getStyleBag().computeCandidate(LayoutProperties.WIDTH);
            if (width == null) {
                width = first.getLayout().getWidth();
            }
            return getPercentValue(width);
        }
    }

    @KJSBindings("SplitViewVertical")
    @LDLRegister(name = "split-view-vertical", group = "container", registry = "ldlib2:ui_element")
    public static class Vertical extends SplitView {
        public Vertical() {
            first.getLayout().widthPercent(100);
            first.getLayout().heightPercent(50);
            second.getLayout().flex(1);
            second.getLayout().widthPercent(100);
            internalSetup();
        }

        @Override
        protected boolean isHoverDragging(float mouseX, float mouseY) {
            return isMouseOver(getPositionX(), getPositionY() + first.getSizeHeight() - getBorderSize(), getSizeWidth(), getBorderSize(), mouseX, mouseY);
        }

        @Override
        protected SpriteTexture getDraggingIcon() {
            return Icons.ARROW_UP_DOWN;
        }

        public Vertical top(UIElement top) {
            first(top);
            return this;
        }

        public Vertical bottom(UIElement bottom) {
            second(bottom);
            return this;
        }

        @Override
        protected void onDragSourceUpdate(UIEvent event) {
            if (event.target != this || event.dragHandler.getDraggingObject() != DRAGGING) {
                return; // only handle drag events for this window
            }
            var height = getSizeHeight();
            if (height <= 0) {
                return; // prevent division by zero
            }
            var localMouse = getLocalMouse(event.x, event.y);
            setPercentage((localMouse.y - getPositionY()) / height * 100);
        }

        @Override
        public Vertical setPercentage(float percentage) {
            first.getLayout().heightPercent(Mth.clamp(percentage, getMinPercentage(), getMaxPercentage()));
            return this;
        }

        @Override
        public float getPercentage() {
            var height = first.getStyleBag().computeCandidate(LayoutProperties.HEIGHT);
            if (height == null) {
                height = first.getLayout().getHeight();
            }
            return getPercentValue(height);
        }
    }
}
