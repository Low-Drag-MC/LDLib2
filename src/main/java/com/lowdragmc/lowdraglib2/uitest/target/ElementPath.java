package com.lowdragmc.lowdraglib2.uitest.target;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A stable, human-readable identifier for an element, of the form
 * <pre>/UIElement#root:0/UIElement#hud_top:0/button#restart_button.primary:2</pre>
 * — one segment per ancestor, each {@code name#id.class1.class2:siblingIndex}.
 *
 * <p>Reports and tree snapshots need an id that means the same thing across runs, JVMs and rebuilds,
 * so that a diff points at "this element changed" rather than "everything changed". That rules out
 * {@code System.identityHashCode} (new every run), {@code UIElement#toString()} (collides for
 * siblings without ids) and the Taffy {@code NodeId} (depends on allocation order).
 *
 * <p>State classes ({@code __hovered__}, {@code __focused__}, …) are excluded: they change as the
 * test drives the UI, and a path that churns is not an identity.
 */
public final class ElementPath {

    private ElementPath() {
    }

    public static String of(UIElement element) {
        var builder = new StringBuilder();
        for (var node : element.getStructurePath()) {
            builder.append('/').append(node.getElementName());
            if (!node.getId().isEmpty()) {
                builder.append('#').append(node.getId());
            }
            for (var styleClass : stableClasses(node)) {
                builder.append('.').append(styleClass);
            }
            builder.append(':').append(Math.max(node.getSiblingIndex(), 0));
        }
        return builder.toString();
    }

    /** A filesystem-safe form of {@link #of}, for screenshot file names. */
    public static String slug(UIElement element) {
        return slug(of(element));
    }

    public static String slug(String path) {
        var slug = path.replaceAll("[^A-Za-z0-9_-]+", "_").replaceAll("_+", "_");
        if (slug.startsWith("_")) slug = slug.substring(1);
        // Windows caps a path component at 255 chars; deep trees blow past that.
        return slug.length() <= 120 ? slug : slug.substring(slug.length() - 120);
    }

    /**
     * Walks a path produced by {@link #of} back to a live element, so a failing report can be
     * replayed against a running UI.
     *
     * @return the element, or {@code null} if the tree no longer has that shape
     */
    @Nullable
    public static UIElement resolve(ModularUI modularUI, String path) {
        var segments = path.split("/");
        UIElement current = null;
        for (var segment : segments) {
            if (segment.isEmpty()) continue;
            int index = siblingIndexOf(segment);
            if (current == null) {
                // The first segment is the root itself, not one of its children.
                current = modularUI.ui.rootElement;
                if (!segment.equals(segmentOf(current))) return null;
                continue;
            }
            var children = current.getChildren();
            if (index < 0 || index >= children.size()) return null;
            var child = children.get(index);
            if (!segment.equals(segmentOf(child))) return null;
            current = child;
        }
        return current;
    }

    private static String segmentOf(UIElement element) {
        var builder = new StringBuilder(element.getElementName());
        if (!element.getId().isEmpty()) {
            builder.append('#').append(element.getId());
        }
        for (var styleClass : stableClasses(element)) {
            builder.append('.').append(styleClass);
        }
        return builder.append(':').append(Math.max(element.getSiblingIndex(), 0)).toString();
    }

    private static int siblingIndexOf(String segment) {
        int colon = segment.lastIndexOf(':');
        if (colon < 0) return -1;
        try {
            return Integer.parseInt(segment.substring(colon + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Declared classes only, sorted for determinism; runtime {@code __state__} classes dropped. */
    public static List<String> stableClasses(UIElement element) {
        var result = new ArrayList<String>();
        for (var styleClass : element.getClasses()) {
            if (isStateClass(styleClass)) continue;
            result.add(styleClass);
        }
        result.sort(String::compareTo);
        return result;
    }

    /** The runtime pseudo-class convention: {@code __hovered__}, {@code __focused__}, {@code __root__}. */
    public static boolean isStateClass(String styleClass) {
        return styleClass.length() > 4 && styleClass.startsWith("__") && styleClass.endsWith("__");
    }
}
