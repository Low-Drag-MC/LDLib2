package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.layout.YogaNodeConfigParser;
import com.lowdragmc.lowdraglib2.gui.ui.layout.YogaProperties;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.style.StyleSizeLength;
import org.appliedenergistics.yoga.style.YogaStyle;

import java.util.*;

public final class LayoutStyle extends Style {
    private static final Property<?>[] PROPERTIES;
    static {
        var properties = new ArrayList<Property<?>>();
        properties.add(YogaProperties.DISPLAY);
        properties.add(YogaProperties.LAYOUT_DIRECTION);
        properties.add(YogaProperties.FLEX_BASIS);
        properties.add(YogaProperties.FLEX);
        properties.add(YogaProperties.FLEX_GROW);
        properties.add(YogaProperties.FLEX_SHRINK);
        properties.add(YogaProperties.FLEX_DIRECTION);
        properties.add(YogaProperties.FLEX_WRAP);
        properties.add(YogaProperties.POSITION);
        properties.addAll(Arrays.stream(YogaProperties.POSITIONS).toList());
        properties.addAll(Arrays.stream(YogaProperties.MARGINS).toList());
        properties.addAll(Arrays.stream(YogaProperties.PADDINGS).toList());
        properties.addAll(Arrays.stream(YogaProperties.GAPS).toList());
        properties.add(YogaProperties.WIDTH);
        properties.add(YogaProperties.HEIGHT);
        properties.addAll(Arrays.stream(YogaProperties.MIN).toList());
        properties.addAll(Arrays.stream(YogaProperties.MAX).toList());
        properties.add(YogaProperties.ASPECT_RATE);
        properties.add(YogaProperties.OVERFLOW);
        properties.add(YogaProperties.ALIGN_ITEMS);
        properties.add(YogaProperties.JUSTIFY_CONTENT);
        properties.add(YogaProperties.ALIGN_SELF);
        properties.add(YogaProperties.ALIGN_CONTENT);
        PROPERTIES = properties.toArray(new Property[0]);
    }

    public LayoutStyle(UIElement holder) {
        super(holder);
    }

    @Override
    protected Property<?>[] getProperties() {
        return PROPERTIES;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        YogaNodeConfigParser.buildConfigurator(this, father);
    }

    public LayoutStyle setWidth(StyleSizeLength length) {
        set(YogaProperties.WIDTH, length);
        return this;
    }

    public LayoutStyle setWidth(float width) {
        return setWidth(StyleSizeLength.points(width));
    }

    public LayoutStyle setWidthPercent(float percent) {
        return setWidth(StyleSizeLength.percent(percent));
    }

    public LayoutStyle setWidthAuto() {
        return setWidth(StyleSizeLength.ofAuto());
    }

    public LayoutStyle setWidthMaxContent() {
        return setWidth(StyleSizeLength.ofMaxContent());
    }

    public LayoutStyle setWidthFitContent() {
        return setWidth(StyleSizeLength.ofFitContent());
    }

    public LayoutStyle setWidthStretch() {
        return setWidth(StyleSizeLength.ofStretch());
    }

    public LayoutStyle setMinWidth(StyleSizeLength length) {
        set(YogaProperties.MIN[YogaDimension.WIDTH.ordinal()], length);
        return this;
    }

    public LayoutStyle setMinWidth(float minWidth) {
        return setMinWidth(StyleSizeLength.points(minWidth));
    }

    public LayoutStyle setMinWidthPercent(float percent) {
        return setMinWidth(StyleSizeLength.percent(percent));
    }

    public LayoutStyle setMinWidthMaxContent() {
        return setMinWidth(StyleSizeLength.ofMaxContent());
    }

    public LayoutStyle setMinWidthFitContent() {
        return setMinWidth(StyleSizeLength.ofFitContent());
    }

    public LayoutStyle setMinWidthStretch() {
        return setMinWidth(StyleSizeLength.ofStretch());
    }

    public LayoutStyle setMaxWidth(StyleSizeLength length) {
        set(YogaProperties.MAX[YogaDimension.WIDTH.ordinal()], length);
        return this;
    }

    public LayoutStyle setMaxWidth(float maxWidth) {
        return setMaxWidth(StyleSizeLength.points(maxWidth));
    }

    public LayoutStyle setMaxWidthPercent(float percent) {
        return setMaxWidth(StyleSizeLength.percent(percent));
    }

    public LayoutStyle setMaxWidthMaxContent() {
        return setMaxWidth(StyleSizeLength.ofMaxContent());
    }

    public LayoutStyle setMaxWidthFitContent() {
        return setMaxWidth(StyleSizeLength.ofFitContent());
    }

    public LayoutStyle setMaxWidthStretch() {
        return setMaxWidth(StyleSizeLength.ofStretch());
    }

    /* Height properties */
    public LayoutStyle setHeight(StyleSizeLength length) {
        set(YogaProperties.HEIGHT, length);
        return this;
    }

    public LayoutStyle setHeight(float height) {
        return setHeight(StyleSizeLength.points(height));
    }

    public LayoutStyle setHeightPercent(float percent) {
        return setHeight(StyleSizeLength.percent(percent));
    }

    public LayoutStyle setHeightAuto() {
        return setHeight(StyleSizeLength.ofAuto());
    }

    public LayoutStyle setHeightMaxContent() {
        return setHeight(StyleSizeLength.ofMaxContent());
    }

    public LayoutStyle setHeightFitContent() {
        return setHeight(StyleSizeLength.ofFitContent());
    }

    public LayoutStyle setHeightStretch() {
        return setHeight(StyleSizeLength.ofStretch());
    }

    public LayoutStyle setMinHeight(StyleSizeLength length) {
        set(YogaProperties.MIN[YogaDimension.HEIGHT.ordinal()], length);
        return this;
    }

    public LayoutStyle setMinHeight(float minHeight) {
        return setMinHeight(StyleSizeLength.points(minHeight));
    }

    public LayoutStyle setMinHeightPercent(float percent) {
        return setMinHeight(StyleSizeLength.percent(percent));
    }

    public LayoutStyle setMinHeightMaxContent() {
        return setMinHeight(StyleSizeLength.ofMaxContent());
    }

    public LayoutStyle setMinHeightFitContent() {
        return setMinHeight(StyleSizeLength.ofFitContent());
    }

    public LayoutStyle setMinHeightStretch() {
        return setMinHeight(StyleSizeLength.ofStretch());
    }

    public LayoutStyle setMaxHeight(StyleSizeLength length) {
        set(YogaProperties.MAX[YogaDimension.HEIGHT.ordinal()], length);
        return this;
    }

    public LayoutStyle setMaxHeight(float maxHeight) {
        return setMaxHeight(StyleSizeLength.points(maxHeight));
    }

    public LayoutStyle setMaxHeightPercent(float percent) {
        return setMaxHeight(StyleSizeLength.percent(percent));
    }

    public LayoutStyle setMaxHeightMaxContent() {
        return setMaxHeight(StyleSizeLength.ofMaxContent());
    }

    public LayoutStyle setMaxHeightFitContent() {
        return setMaxHeight(StyleSizeLength.ofFitContent());
    }

    public LayoutStyle setMaxHeightStretch() {
        return setMaxHeight(StyleSizeLength.ofStretch());
    }

    /* Margin properties */

    public LayoutStyle setMargin(YogaEdge edge, StyleLength length) {
        set(YogaProperties.MARGINS[edge.ordinal()], length);
        return this;
    }

    public LayoutStyle setMargin(YogaEdge edge, float margin) {
        return setMargin(edge, StyleLength.points(margin));
    }

    public LayoutStyle setMarginPercent(YogaEdge edge, float percent) {
        return setMargin(edge, StyleLength.percent(percent));
    }

    public LayoutStyle setMarginAuto(YogaEdge edge) {
        return setMargin(edge, StyleLength.ofAuto());
    }

    /* Padding properties */

    public LayoutStyle setPadding(YogaEdge edge, StyleLength length) {
        set(YogaProperties.PADDINGS[edge.ordinal()], length);
        return this;
    }

    public LayoutStyle setPadding(YogaEdge edge, float padding) {
        return setPadding(edge, StyleLength.points(padding));
    }

    public LayoutStyle setPaddingPercent(YogaEdge edge, float percent) {
        return setPadding(edge, StyleLength.percent(percent));
    }

    /* Position properties */

    public LayoutStyle setPositionType(YogaPositionType positionType) {
        set(YogaProperties.POSITION, positionType);
        return this;
    }

    public LayoutStyle setPosition(YogaEdge edge, StyleLength length) {
        set(YogaProperties.POSITIONS[edge.ordinal()], length);
        return this;
    }

    public LayoutStyle setPosition(YogaEdge edge, float position) {
        return setPosition(edge, StyleLength.points(position));
    }

    public LayoutStyle setPositionPercent(YogaEdge edge, float percent) {
        return setPosition(edge, StyleLength.percent(percent));
    }

    public LayoutStyle setPositionAuto(YogaEdge edge) {
        return setPosition(edge, StyleLength.ofAuto());
    }

    public LayoutStyle left(float left) {
        return setPosition(YogaEdge.LEFT, left);
    }

    public LayoutStyle leftPercent(float percent) {
        return setPositionPercent(YogaEdge.LEFT, percent);
    }

    public LayoutStyle leftAuto() {
        return setPositionAuto(YogaEdge.LEFT);
    }

    public LayoutStyle right(float right) {
        return setPosition(YogaEdge.RIGHT, right);
    }

    public LayoutStyle rightPercent(float percent) {
        return setPositionPercent(YogaEdge.RIGHT, percent);
    }

    public LayoutStyle rightAuto() {
        return setPositionAuto(YogaEdge.RIGHT);
    }

    public LayoutStyle top(float top) {
        return setPosition(YogaEdge.TOP, top);
    }

    public LayoutStyle topPercent(float percent) {
        return setPositionPercent(YogaEdge.TOP, percent);
    }


    public LayoutStyle topAuto() {
        return setPositionAuto(YogaEdge.TOP);
    }

    public LayoutStyle bottom(float bottom) {
        return setPosition(YogaEdge.BOTTOM, bottom);
    }

    public LayoutStyle bottomPercent(float percent) {
        return setPositionPercent(YogaEdge.BOTTOM, percent);
    }

    public LayoutStyle bottomAuto() {
        return setPositionAuto(YogaEdge.BOTTOM);
    }

    /* Alignment properties */

    public LayoutStyle setAlignContent(YogaAlign alignContent) {
        set(YogaProperties.ALIGN_CONTENT, alignContent);
        return this;
    }

    public LayoutStyle setAlignItems(YogaAlign alignItems) {
        set(YogaProperties.ALIGN_ITEMS, alignItems);
        return this;
    }

    public LayoutStyle setAlignSelf(YogaAlign alignSelf) {
        set(YogaProperties.ALIGN_SELF, alignSelf);
        return this;
    }

    /* Flex properties */
    public LayoutStyle setFlex(float flex) {
        set(YogaProperties.FLEX, FloatOptional.of(flex));
        return this;
    }

    public LayoutStyle setFlexAuto() {
        set(YogaProperties.FLEX, FloatOptional.of());
        return this;
    }

    public LayoutStyle setFlexBasisAuto() {
        set(YogaProperties.FLEX_BASIS, StyleSizeLength.ofAuto());
        return this;
    }

    public LayoutStyle setFlexBasisPercent(float percent) {
        set(YogaProperties.FLEX_BASIS, StyleSizeLength.percent(percent));
        return this;
    }

    public LayoutStyle setFlexBasis(float flexBasis) {
        set(YogaProperties.FLEX_BASIS, StyleSizeLength.points(flexBasis));
        return this;
    }

    public LayoutStyle setFlexBasisMaxContent() {
        set(YogaProperties.FLEX_BASIS, StyleSizeLength.ofMaxContent());
        return this;
    }

    public LayoutStyle setFlexBasisFitContent() {
        set(YogaProperties.FLEX_BASIS, StyleSizeLength.ofFitContent());
        return this;
    }

    public LayoutStyle setFlexBasisStretch() {
        set(YogaProperties.FLEX_BASIS, StyleSizeLength.ofStretch());
        return this;
    }

    public LayoutStyle setFlexDirection(YogaFlexDirection direction) {
        set(YogaProperties.FLEX_DIRECTION, direction);
        return this;
    }

    public LayoutStyle setFlexGrow(float flexGrow) {
        set(YogaProperties.FLEX_GROW, FloatOptional.of(flexGrow));
        return this;
    }

    public LayoutStyle setFlexShrink(float flexShrink) {
        set(YogaProperties.FLEX_SHRINK, FloatOptional.of(flexShrink));
        return this;
    }

    /* Other properties */

    public LayoutStyle setJustifyContent(YogaJustify justifyContent) {
        set(YogaProperties.JUSTIFY_CONTENT, justifyContent);
        return this;
    }

    public LayoutStyle setDirection(YogaDirection direction) {
        set(YogaProperties.LAYOUT_DIRECTION, direction);
        return this;
    }

    public LayoutStyle setWrap(YogaWrap wrap) {
        set(YogaProperties.FLEX_WRAP, wrap);
        return this;
    }

    public LayoutStyle setAspectRatio(float aspectRatio) {
        set(YogaProperties.ASPECT_RATE, FloatOptional.of(aspectRatio));
        return this;
    }

    public LayoutStyle setAspectRatioAuto() {
        set(YogaProperties.ASPECT_RATE, FloatOptional.of());
        return this;
    }


    public LayoutStyle setGap(YogaGutter gutter, StyleLength value) {
        set(YogaProperties.GAPS[gutter.ordinal()], value);
        return this;
    }

    public LayoutStyle setGap(YogaGutter gutter, float value) {
        return setGap(gutter, StyleLength.points(value));
    }

    public LayoutStyle setGapPercent(YogaGutter gutter, float percent) {
        return setGap(gutter, StyleLength.percent(percent));
    }

    public LayoutStyle setDisplay(YogaDisplay display) {
        set(YogaProperties.DISPLAY, display);
        return this;
    }

    public LayoutStyle setOverflow(YogaOverflow overflow) {
        set(YogaProperties.OVERFLOW, overflow);
        return this;
    }

    /* Getters */
    public YogaValue getWidth() {
        return getValueSave(YogaProperties.WIDTH).asYogaValue();
    }

    public YogaValue getMinWidth() {
        return getValueSave(YogaProperties.MIN[YogaDimension.WIDTH.ordinal()]).asYogaValue();
    }

    public YogaValue getMaxWidth() {
        return getValueSave(YogaProperties.MAX[YogaDimension.WIDTH.ordinal()]).asYogaValue();
    }

    public YogaValue getHeight() {
        return getValueSave(YogaProperties.HEIGHT).asYogaValue();
    }

    public YogaValue getMinHeight() {
        return getValueSave(YogaProperties.MIN[YogaDimension.HEIGHT.ordinal()]).asYogaValue();
    }

    public YogaValue getMaxHeight() {
        return getValueSave(YogaProperties.MAX[YogaDimension.HEIGHT.ordinal()]).asYogaValue();
    }

    public YogaDirection getStyleDirection() {
        return getValueSave(YogaProperties.LAYOUT_DIRECTION);
    }

    public YogaFlexDirection getFlexDirection() {
        return getValueSave(YogaProperties.FLEX_DIRECTION);
    }

    public YogaJustify getJustifyContent() {
        return getValueSave(YogaProperties.JUSTIFY_CONTENT);
    }

    public YogaAlign getAlignItems() {
        return getValueSave(YogaProperties.ALIGN_ITEMS);
    }

    public YogaAlign getAlignSelf() {
        return getValueSave(YogaProperties.ALIGN_SELF);
    }

    public YogaAlign getAlignContent() {
        return getValueSave(YogaProperties.ALIGN_CONTENT);
    }

    public YogaPositionType getPositionType() {
        return getValueSave(YogaProperties.POSITION);
    }

    public float getFlexGrow() {
        return getValueSave(YogaProperties.FLEX_GROW).unwrapOrDefault(YogaStyle.DEFAULT_FLEX_GROW);
    }

    public float getFlexShrink() {
        return getValueSave(YogaProperties.FLEX_SHRINK).unwrapOrDefault(YogaStyle.DEFAULT_FLEX_SHRINK);
    }

    public YogaValue getFlexBasis() {
        return getValueSave(YogaProperties.FLEX_BASIS).asYogaValue();
    }

    public float getAspectRatio() {
        return getValueSave(YogaProperties.ASPECT_RATE).unwrapOrDefault(YogaConstants.UNDEFINED);
    }

    public YogaValue getMargin(YogaEdge edge) {
        return getValueSave(YogaProperties.MARGINS[edge.ordinal()]).asYogaValue();
    }

    public YogaValue getPadding(YogaEdge edge) {
        return getValueSave(YogaProperties.PADDINGS[edge.ordinal()]).asYogaValue();
    }

    public YogaValue getPosition(YogaEdge edge) {
        return getValueSave(YogaProperties.POSITIONS[edge.ordinal()]).asYogaValue();
    }

    public YogaValue getGap(YogaGutter gutter) {
        return getValueSave(YogaProperties.GAPS[gutter.ordinal()]).asYogaValue();
    }
}
