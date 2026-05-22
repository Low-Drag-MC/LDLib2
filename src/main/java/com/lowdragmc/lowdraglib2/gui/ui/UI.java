package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import lombok.Data;
import net.minecraft.resources.ResourceLocation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@Data(staticConstructor = "of")
@KJSBindings
public final class UI {
    private static final UI EMPTY = UI.of(new UIElement());

    public static UI empty() {
        return EMPTY;
    }

    @FunctionalInterface
    public interface DynamicSizeProvider extends Function<Size, Size> {
        /**
         * Applies a transformation to the given screen size and returns a new {@code Size} object.
         *
         * @param screenSize the input size representing the dimensions of the screen
         * @return a new {@code Size} object representing the result of the transformation
         */
        @Override
        Size apply(Size screenSize);
    }
    public final UIElement rootElement;
    public final List<Stylesheet> stylesheets;
    @Nullable
    public final DynamicSizeProvider dynamicSize;

    public static UI of(UIElement rootElement) {
        return of(rootElement, Collections.emptyList(), null);
    }

    public static UI of(UIElement rootElement, List<Stylesheet> stylesheets) {
        return of(rootElement, stylesheets, null);
    }

    public static UI of(UIElement rootElement, Stylesheet... stylesheets) {
        return of(rootElement, Arrays.stream(stylesheets).toList(), null);
    }

    public static UI of(UIElement rootElement, ResourceLocation... stylesheets) {
        return of(rootElement, Arrays.stream(stylesheets).map(StylesheetManager.INSTANCE::getStylesheet).filter(Objects::nonNull).toList(), null);
    }

    public static UI of(UIElement rootElement, String... stylesheets) {
        return of(rootElement, Arrays.stream(stylesheets).map(StylesheetManager.INSTANCE::getMergedStylesheets).filter(Objects::nonNull).toList(), null);
    }

    public static UI of(UIElement rootElement, @Nullable DynamicSizeProvider dynamicSize) {
        return of(rootElement, Collections.emptyList(), dynamicSize);
    }

    public static UI of(Document xml) {
        var rootElement = xml.getDocumentElement();
        var nodes = rootElement.getChildNodes();
        var root = new UIElement();
        var stylesheets = new ArrayList<Stylesheet>();
        for (int i = 0; i < nodes.getLength(); i++) {
            var node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node instanceof Element element) {
                var tagName = element.getTagName();
                if (tagName.equals("root")) {
                    root.loadXml(element);
                } else if (tagName.equals("style")) {
                    var content = element.getTextContent();
                    if (content != null && !content.isBlank()) {
                        stylesheets.add(Stylesheet.parse(content));
                    }
                } else if (tagName.equals("stylesheet")) {
                    var location = XmlUtils.getAsString(element, "location", "");
                    if (!location.isEmpty()) {
                        var rs = ResourceLocation.tryParse(location);
                        if (rs != null) {
                            var stylesheet = StylesheetManager.INSTANCE.getStylesheet(rs);
                            if (stylesheet != null) {
                                stylesheets.add(stylesheet);
                            }
                        }
                    }
                }
            }
        }
        return of(root, stylesheets);
    }

    public static UI of() {
        return of(new UIElement());
    }

    public UITemplate toTemplate() {
        return UITemplate.of(rootElement);
    }

    public Stream<UIElement> select(String selector) {
        return rootElement.select(selector);
    }

    public <T> Stream<T> select(String selector, Class<T> type) {
        return rootElement.select(selector, type);
    }

    public Stream<UIElement> selectRegex(String regex) {
        return rootElement.selectRegex(regex);
    }

    public <T> Stream<T> selectRegex(String regex, Class<T> type) {
        return rootElement.selectRegex(regex, type);
    }

    public Stream<UIElement> selectId(String id) {
        return rootElement.selectId(id);
    }

    public <T> Stream<T> selectId(String id, Class<T> type) {
        return rootElement.selectId(id, type);
    }
}
