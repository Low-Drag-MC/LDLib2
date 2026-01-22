package com.lowdragmc.lowdraglib2.gui.ui.data;

import dev.vfyjxf.taffy.style.GridTemplateArea;

import java.util.List;

/**
 * Wrapper for CSS grid-template-areas that stores Taffy's GridTemplateArea objects.
 *
 * <p>The CSS syntax like:
 * <pre>
 * grid-template-areas:
 *   "header header header"
 *   "nav main aside"
 *   "footer footer footer";
 * </pre>
 *
 * <p>Is converted to a list of GridTemplateArea objects, where each unique area name
 * becomes one GridTemplateArea with its bounding row/column lines:
 * <ul>
 *   <li>GridTemplateArea("header", rowStart=1, rowEnd=2, columnStart=1, columnEnd=4)</li>
 *   <li>GridTemplateArea("nav", rowStart=2, rowEnd=3, columnStart=1, columnEnd=2)</li>
 *   <li>GridTemplateArea("main", rowStart=2, rowEnd=3, columnStart=2, columnEnd=3)</li>
 *   <li>GridTemplateArea("aside", rowStart=2, rowEnd=3, columnStart=3, columnEnd=4)</li>
 *   <li>GridTemplateArea("footer", rowStart=3, rowEnd=4, columnStart=1, columnEnd=4)</li>
 * </ul>
 */
public record GridTemplateAreas(List<GridTemplateArea> areas) {
    public static final GridTemplateAreas EMPTY = new GridTemplateAreas(List.of());
}
