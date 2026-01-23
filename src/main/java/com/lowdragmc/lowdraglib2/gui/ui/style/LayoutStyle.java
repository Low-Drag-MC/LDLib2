package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Grid;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridAuto;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplate;
import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplateAreas;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutConfigParser;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.GridAutoValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.GridTemplateAreasValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.GridTemplateValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.GridValue;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import dev.vfyjxf.taffy.style.*;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.numeric.FloatOptional;
import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.style.StyleSizeLength;
import org.appliedenergistics.yoga.style.YogaStyle;

import java.util.*;

@RemapPrefixForJS("kjs$")
public final class LayoutStyle extends Style {
    private static final Property<?>[] PROPERTIES;
    static {
        var properties = new ArrayList<Property<?>>();
        properties.add(LayoutProperties.DISPLAY);
        properties.add(LayoutProperties.LAYOUT_DIRECTION);
        properties.add(LayoutProperties.FLEX_BASIS);
        properties.add(LayoutProperties.FLEX);
        properties.add(LayoutProperties.FLEX_GROW);
        properties.add(LayoutProperties.FLEX_SHRINK);
        properties.add(LayoutProperties.FLEX_DIRECTION);
        properties.add(LayoutProperties.FLEX_WRAP);
        properties.add(LayoutProperties.POSITION);
        properties.addAll(Arrays.stream(LayoutProperties.POSITIONS).toList());
        properties.addAll(Arrays.stream(LayoutProperties.MARGINS).toList());
        properties.addAll(Arrays.stream(LayoutProperties.PADDINGS).toList());
        properties.addAll(Arrays.stream(LayoutProperties.GAPS).toList());
        properties.add(LayoutProperties.WIDTH);
        properties.add(LayoutProperties.HEIGHT);
        properties.addAll(Arrays.stream(LayoutProperties.MIN).toList());
        properties.addAll(Arrays.stream(LayoutProperties.MAX).toList());
        properties.add(LayoutProperties.ASPECT_RATE);
        properties.add(LayoutProperties.OVERFLOW);
        properties.add(LayoutProperties.ALIGN_ITEMS);
        properties.add(LayoutProperties.JUSTIFY_CONTENT);
        properties.add(LayoutProperties.ALIGN_SELF);
        properties.add(LayoutProperties.ALIGN_CONTENT);
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
        LayoutConfigParser.buildConfigurator(this, father);
    }

    public LayoutStyle setWidth(StyleSizeLength length) {
        set(LayoutProperties.WIDTH, length);
        return this;
    }

    public LayoutStyle width(StyleSizeLength length) {
        return setWidth(length);
    }

    public LayoutStyle setWidth(float width) {
        return setWidth(StyleSizeLength.points(width));
    }

    public LayoutStyle width(float width) {
        return setWidth(width);
    }

    public LayoutStyle setWidthPercent(float percent) {
        return setWidth(StyleSizeLength.percent(percent));
    }

    public LayoutStyle widthPercent(float percent) {
        return setWidthPercent(percent);
    }

    public LayoutStyle setWidthAuto() {
        return setWidth(StyleSizeLength.ofAuto());
    }

    public LayoutStyle widthAuto() {
        return setWidthAuto();
    }

    public LayoutStyle setWidthMaxContent() {
        return setWidth(StyleSizeLength.ofMaxContent());
    }

    public LayoutStyle widthMaxContent() {
        return setWidthMaxContent();
    }

    public LayoutStyle setWidthFitContent() {
        return setWidth(StyleSizeLength.ofFitContent());
    }

    public LayoutStyle widthFitContent() {
        return setWidthFitContent();
    }

    public LayoutStyle setWidthStretch() {
        return setWidth(StyleSizeLength.ofStretch());
    }

    public LayoutStyle widthStretch() {
        return setWidthStretch();
    }

    public LayoutStyle setMinWidth(StyleSizeLength length) {
        set(LayoutProperties.MIN[YogaDimension.WIDTH.ordinal()], length);
        return this;
    }

    public LayoutStyle minWidth(StyleSizeLength length) {
        return setMinWidth(length);
    }

    public LayoutStyle setMinWidth(float minWidth) {
        return setMinWidth(StyleSizeLength.points(minWidth));
    }

    public LayoutStyle minWidth(float minWidth) {
        return setMinWidth(minWidth);
    }

    public LayoutStyle setMinWidthPercent(float percent) {
        return setMinWidth(StyleSizeLength.percent(percent));
    }

    public LayoutStyle minWidthPercent(float percent) {
        return setMinWidthPercent(percent);
    }

    public LayoutStyle setMinWidthMaxContent() {
        return setMinWidth(StyleSizeLength.ofMaxContent());
    }

    public LayoutStyle minWidthMaxContent() {
        return setMinWidthMaxContent();
    }

    public LayoutStyle setMinWidthFitContent() {
        return setMinWidth(StyleSizeLength.ofFitContent());
    }

    public LayoutStyle minWidthFitContent() {
        return setMinWidthFitContent();
    }

    public LayoutStyle setMinWidthStretch() {
        return setMinWidth(StyleSizeLength.ofStretch());
    }

    public LayoutStyle minWidthStretch() {
        return setMinWidthStretch();
    }

    public LayoutStyle setMaxWidth(StyleSizeLength length) {
        set(LayoutProperties.MAX[YogaDimension.WIDTH.ordinal()], length);
        return this;
    }

    public LayoutStyle maxWidth(StyleSizeLength length) {
        return setMaxWidth(length);
    }

    public LayoutStyle setMaxWidth(float maxWidth) {
        return setMaxWidth(StyleSizeLength.points(maxWidth));
    }

    public LayoutStyle maxWidth(float maxWidth) {
        return setMaxWidth(maxWidth);
    }

    public LayoutStyle setMaxWidthPercent(float percent) {
        return setMaxWidth(StyleSizeLength.percent(percent));
    }

    public LayoutStyle maxWidthPercent(float percent) {
        return setMaxWidthPercent(percent);
    }

    public LayoutStyle setMaxWidthMaxContent() {
        return setMaxWidth(StyleSizeLength.ofMaxContent());
    }

    public LayoutStyle maxWidthMaxContent() {
        return setMaxWidthMaxContent();
    }

    public LayoutStyle setMaxWidthFitContent() {
        return setMaxWidth(StyleSizeLength.ofFitContent());
    }

    public LayoutStyle maxWidthFitContent() {
        return setMaxWidthFitContent();
    }

    public LayoutStyle setMaxWidthStretch() {
        return setMaxWidth(StyleSizeLength.ofStretch());
    }

    public LayoutStyle maxWidthStretch() {
        return setMaxWidthStretch();
    }

    /* Height properties */
    public LayoutStyle setHeight(StyleSizeLength length) {
        set(LayoutProperties.HEIGHT, length);
        return this;
    }

    public LayoutStyle height(StyleSizeLength length) {
        return setHeight(length);
    }

    public LayoutStyle setHeight(float height) {
        return setHeight(StyleSizeLength.points(height));
    }

    public LayoutStyle height(float height) {
        return setHeight(height);
    }

    public LayoutStyle setHeightPercent(float percent) {
        return setHeight(StyleSizeLength.percent(percent));
    }

    public LayoutStyle heightPercent(float percent) {
        return setHeightPercent(percent);
    }

    public LayoutStyle setHeightAuto() {
        return setHeight(StyleSizeLength.ofAuto());
    }

    public LayoutStyle heightAuto() {
        return setHeightAuto();
    }

    public LayoutStyle setHeightMaxContent() {
        return setHeight(StyleSizeLength.ofMaxContent());
    }

    public LayoutStyle heightMaxContent() {
        return setHeightMaxContent();
    }

    public LayoutStyle setHeightFitContent() {
        return setHeight(StyleSizeLength.ofFitContent());
    }

    public LayoutStyle heightFitContent() {
        return setHeightFitContent();
    }

    public LayoutStyle setHeightStretch() {
        return setHeight(StyleSizeLength.ofStretch());
    }

    public LayoutStyle heightStretch() {
        return setHeightStretch();
    }

    public LayoutStyle setMinHeight(StyleSizeLength length) {
        set(LayoutProperties.MIN[YogaDimension.HEIGHT.ordinal()], length);
        return this;
    }

    public LayoutStyle minHeight(StyleSizeLength length) {
        return setMinHeight(length);
    }

    public LayoutStyle setMinHeight(float minHeight) {
        return setMinHeight(StyleSizeLength.points(minHeight));
    }

    public LayoutStyle minHeight(float minHeight) {
        return setMinHeight(minHeight);
    }

    public LayoutStyle setMinHeightPercent(float percent) {
        return setMinHeight(StyleSizeLength.percent(percent));
    }

    public LayoutStyle minHeightPercent(float percent) {
        return setMinHeightPercent(percent);
    }

    public LayoutStyle setMinHeightMaxContent() {
        return setMinHeight(StyleSizeLength.ofMaxContent());
    }

    public LayoutStyle minHeightMaxContent() {
        return setMinHeightMaxContent();
    }

    public LayoutStyle setMinHeightFitContent() {
        return setMinHeight(StyleSizeLength.ofFitContent());
    }

    public LayoutStyle minHeightFitContent() {
        return setMinHeightFitContent();
    }

    public LayoutStyle setMinHeightStretch() {
        return setMinHeight(StyleSizeLength.ofStretch());
    }

    public LayoutStyle minHeightStretch() {
        return setMinHeightStretch();
    }

    public LayoutStyle setMaxHeight(StyleSizeLength length) {
        set(LayoutProperties.MAX[YogaDimension.HEIGHT.ordinal()], length);
        return this;
    }

    public LayoutStyle maxHeight(StyleSizeLength length) {
        return setMaxHeight(length);
    }

    public LayoutStyle setMaxHeight(float maxHeight) {
        return setMaxHeight(StyleSizeLength.points(maxHeight));
    }

    public LayoutStyle maxHeight(float maxHeight) {
        return setMaxHeight(maxHeight);
    }

    public LayoutStyle setMaxHeightPercent(float percent) {
        return setMaxHeight(StyleSizeLength.percent(percent));
    }

    public LayoutStyle maxHeightPercent(float percent) {
        return setMaxHeightPercent(percent);
    }

    public LayoutStyle setMaxHeightMaxContent() {
        return setMaxHeight(StyleSizeLength.ofMaxContent());
    }

    public LayoutStyle maxHeightMaxContent() {
        return setMaxHeightMaxContent();
    }

    public LayoutStyle setMaxHeightFitContent() {
        return setMaxHeight(StyleSizeLength.ofFitContent());
    }

    public LayoutStyle maxHeightFitContent() {
        return setMaxHeightFitContent();
    }

    public LayoutStyle setMaxHeightStretch() {
        return setMaxHeight(StyleSizeLength.ofStretch());
    }

    public LayoutStyle maxHeightStretch() {
        return setMaxHeightStretch();
    }

    /* Margin properties */
    public LayoutStyle setMargin(YogaEdge edge, StyleLength length) {
        set(LayoutProperties.MARGINS[edge.ordinal()], length);
        return this;
    }

    public LayoutStyle marginLeft(StyleLength length) {
        return setMargin(YogaEdge.LEFT, length);
    }

    public LayoutStyle marginTop(StyleLength length) {
        return setMargin(YogaEdge.TOP, length);
    }

    public LayoutStyle marginRight(StyleLength length) {
        return setMargin(YogaEdge.RIGHT, length);
    }

    public LayoutStyle marginBottom(StyleLength length) {
        return setMargin(YogaEdge.BOTTOM, length);
    }

    public LayoutStyle marginStart(StyleLength length) {
        return setMargin(YogaEdge.START, length);
    }

    public LayoutStyle marginEnd(StyleLength length) {
        return setMargin(YogaEdge.END, length);
    }

    public LayoutStyle marginHorizontal(StyleLength length) {
        return setMargin(YogaEdge.HORIZONTAL, length);
    }

    public LayoutStyle marginVertical(StyleLength length) {
        return setMargin(YogaEdge.VERTICAL, length);
    }

    public LayoutStyle marginAll(StyleLength length) {
        return setMargin(YogaEdge.ALL, length);
    }

    public LayoutStyle setMargin(YogaEdge edge, float margin) {
        return setMargin(edge, StyleLength.points(margin));
    }

    public LayoutStyle marginLeft(float margin) {
        return setMargin(YogaEdge.LEFT, margin);
    }

    public LayoutStyle marginTop(float margin) {
        return setMargin(YogaEdge.TOP, margin);
    }

    public LayoutStyle marginRight(float margin) {
        return setMargin(YogaEdge.RIGHT, margin);
    }

    public LayoutStyle marginBottom(float margin) {
        return setMargin(YogaEdge.BOTTOM, margin);
    }

    public LayoutStyle marginStart(float margin) {
        return setMargin(YogaEdge.START, margin);
    }

    public LayoutStyle marginEnd(float margin) {
        return setMargin(YogaEdge.END, margin);
    }

    public LayoutStyle marginHorizontal(float margin) {
        return setMargin(YogaEdge.HORIZONTAL, margin);
    }

    public LayoutStyle marginVertical(float margin) {
        return setMargin(YogaEdge.VERTICAL, margin);
    }

    public LayoutStyle marginAll(float margin) {
        return setMargin(YogaEdge.ALL, margin);
    }

    public LayoutStyle setMarginPercent(YogaEdge edge, float percent) {
        return setMargin(edge, StyleLength.percent(percent));
    }

    public LayoutStyle marginLeftPercent(float margin) {
        return setMarginPercent(YogaEdge.LEFT, margin);
    }

    public LayoutStyle marginTopPercent(float margin) {
        return setMarginPercent(YogaEdge.TOP, margin);
    }

    public LayoutStyle marginRightPercent(float margin) {
        return setMarginPercent(YogaEdge.RIGHT, margin);
    }

    public LayoutStyle marginBottomPercent(float margin) {
        return setMarginPercent(YogaEdge.BOTTOM, margin);
    }

    public LayoutStyle marginStartPercent(float margin) {
        return setMarginPercent(YogaEdge.START, margin);
    }

    public LayoutStyle marginEndPercent(float margin) {
        return setMarginPercent(YogaEdge.END, margin);
    }

    public LayoutStyle marginHorizontalPercent(float margin) {
        return setMarginPercent(YogaEdge.HORIZONTAL, margin);
    }

    public LayoutStyle marginVerticalPercent(float margin) {
        return setMarginPercent(YogaEdge.VERTICAL, margin);
    }

    public LayoutStyle marginAllPercent(float margin) {
        return setMarginPercent(YogaEdge.ALL, margin);
    }

    public LayoutStyle setMarginAuto(YogaEdge edge) {
        return setMargin(edge, StyleLength.ofAuto());
    }

    public LayoutStyle marginLeftAuto() {
        return setMarginAuto(YogaEdge.LEFT);
    }

    public LayoutStyle marginTopAuto() {
        return setMarginAuto(YogaEdge.TOP);
    }

    public LayoutStyle marginRightAuto() {
        return setMarginAuto(YogaEdge.RIGHT);
    }

    public LayoutStyle marginBottomAuto() {
        return setMarginAuto(YogaEdge.BOTTOM);
    }

    public LayoutStyle marginStartAuto() {
        return setMarginAuto(YogaEdge.START);
    }

    public LayoutStyle marginEndAuto() {
        return setMarginAuto(YogaEdge.END);
    }

    public LayoutStyle marginHorizontalAuto() {
        return setMarginAuto(YogaEdge.HORIZONTAL);
    }

    public LayoutStyle marginVerticalAuto() {
        return setMarginAuto(YogaEdge.VERTICAL);
    }

    public LayoutStyle marginAllAuto() {
        return setMarginAuto(YogaEdge.ALL);
    }
    /* Padding properties */

    public LayoutStyle setPadding(YogaEdge edge, StyleLength length) {
        set(LayoutProperties.PADDINGS[edge.ordinal()], length);
        return this;
    }

    public LayoutStyle paddingLeft(StyleLength length) {
        return setPadding(YogaEdge.LEFT, length);
    }

    public LayoutStyle paddingTop(StyleLength length) {
        return setPadding(YogaEdge.TOP, length);
    }

    public LayoutStyle paddingRight(StyleLength length) {
        return setPadding(YogaEdge.RIGHT, length);
    }

    public LayoutStyle paddingBottom(StyleLength length) {
        return setPadding(YogaEdge.BOTTOM, length);
    }

    public LayoutStyle paddingStart(StyleLength length) {
        return setPadding(YogaEdge.START, length);
    }

    public LayoutStyle paddingEnd(StyleLength length) {
        return setPadding(YogaEdge.END, length);
    }

    public LayoutStyle paddingHorizontal(StyleLength length) {
        return setPadding(YogaEdge.HORIZONTAL, length);
    }

    public LayoutStyle paddingVertical(StyleLength length) {
        return setPadding(YogaEdge.VERTICAL, length);
    }

    public LayoutStyle paddingAll(StyleLength length) {
        return setPadding(YogaEdge.ALL, length);
    }

    public LayoutStyle setPadding(YogaEdge edge, float padding) {
        return setPadding(edge, StyleLength.points(padding));
    }

    public LayoutStyle paddingLeft(float padding) {
        return setPadding(YogaEdge.LEFT, padding);
    }

    public LayoutStyle paddingTop(float padding) {
        return setPadding(YogaEdge.TOP, padding);
    }

    public LayoutStyle paddingRight(float padding) {
        return setPadding(YogaEdge.RIGHT, padding);
    }

    public LayoutStyle paddingBottom(float padding) {
        return setPadding(YogaEdge.BOTTOM, padding);
    }

    public LayoutStyle paddingStart(float padding) {
        return setPadding(YogaEdge.START, padding);
    }

    public LayoutStyle paddingEnd(float padding) {
        return setPadding(YogaEdge.END, padding);
    }

    public LayoutStyle paddingHorizontal(float padding) {
        return setPadding(YogaEdge.HORIZONTAL, padding);
    }

    public LayoutStyle paddingVertical(float padding) {
        return setPadding(YogaEdge.VERTICAL, padding);
    }

    public LayoutStyle paddingAll(float padding) {
        return setPadding(YogaEdge.ALL, padding);
    }

    public LayoutStyle setPaddingPercent(YogaEdge edge, float percent) {
        return setPadding(edge, StyleLength.percent(percent));
    }

    public LayoutStyle paddingLeftPercent(float padding) {
        return setPaddingPercent(YogaEdge.LEFT, padding);
    }

    public LayoutStyle paddingTopPercent(float padding) {
        return setPaddingPercent(YogaEdge.TOP, padding);
    }

    public LayoutStyle paddingRightPercent(float padding) {
        return setPaddingPercent(YogaEdge.RIGHT, padding);
    }

    public LayoutStyle paddingBottomPercent(float padding) {
        return setPaddingPercent(YogaEdge.BOTTOM, padding);
    }

    public LayoutStyle paddingStartPercent(float padding) {
        return setPaddingPercent(YogaEdge.START, padding);
    }

    public LayoutStyle paddingEndPercent(float padding) {
        return setPaddingPercent(YogaEdge.END, padding);
    }

    public LayoutStyle paddingHorizontalPercent(float padding) {
        return setPaddingPercent(YogaEdge.HORIZONTAL, padding);
    }

    public LayoutStyle paddingVerticalPercent(float padding) {
        return setPaddingPercent(YogaEdge.VERTICAL, padding);
    }

    public LayoutStyle paddingAllPercent(float padding) {
        return setPaddingPercent(YogaEdge.ALL, padding);
    }

    public LayoutStyle setPaddingAuto(YogaEdge edge) {
        return setPadding(edge, StyleLength.ofAuto());
    }

    public LayoutStyle paddingLeftAuto() {
        return setPaddingAuto(YogaEdge.LEFT);
    }

    public LayoutStyle paddingTopAuto() {
        return setPaddingAuto(YogaEdge.TOP);
    }

    public LayoutStyle paddingRightAuto() {
        return setPaddingAuto(YogaEdge.RIGHT);
    }

    public LayoutStyle paddingBottomAuto() {
        return setPaddingAuto(YogaEdge.BOTTOM);
    }

    public LayoutStyle paddingStartAuto() {
        return setPaddingAuto(YogaEdge.START);
    }

    public LayoutStyle paddingEndAuto() {
        return setPaddingAuto(YogaEdge.END);
    }

    public LayoutStyle paddingHorizontalAuto() {
        return setPaddingAuto(YogaEdge.HORIZONTAL);
    }

    public LayoutStyle paddingVerticalAuto() {
        return setPaddingAuto(YogaEdge.VERTICAL);
    }

    public LayoutStyle paddingAllAuto() {
        return setPaddingAuto(YogaEdge.ALL);
    }

    /* Position properties */
    @Deprecated(since = "26.1")
    public LayoutStyle setPositionType(YogaPositionType positionType) {
        return positionType(positionType);
    }

    @Deprecated(since = "26.1")
    @HideFromJS
    public LayoutStyle positionType(YogaPositionType positionType) {
        return positionType(switch (positionType) {
            case STATIC, RELATIVE -> TaffyPosition.RELATIVE;
            case ABSOLUTE -> TaffyPosition.ABSOLUTE;
        });
    }

    @HideFromJS
    public LayoutStyle positionType(TaffyPosition positionType) {
        set(LayoutProperties.POSITION, positionType);
        return this;
    }

    public LayoutStyle setPosition(YogaEdge edge, StyleLength length) {
        set(LayoutProperties.POSITIONS[edge.ordinal()], length);
        return this;
    }

    public LayoutStyle left(StyleLength length) {
        return setPosition(YogaEdge.LEFT, length);
    }

    public LayoutStyle top(StyleLength length) {
        return setPosition(YogaEdge.TOP, length);
    }

    public LayoutStyle right(StyleLength length) {
        return setPosition(YogaEdge.RIGHT, length);
    }

    public LayoutStyle bottom(StyleLength length) {
        return setPosition(YogaEdge.BOTTOM, length);
    }

    public LayoutStyle start(StyleLength length) {
        return setPosition(YogaEdge.START, length);
    }

    public LayoutStyle end(StyleLength length) {
        return setPosition(YogaEdge.END, length);
    }

    public LayoutStyle horizontal(StyleLength length) {
        return setPosition(YogaEdge.HORIZONTAL, length);
    }

    public LayoutStyle vertical(StyleLength length) {
        return setPosition(YogaEdge.VERTICAL, length);
    }

    public LayoutStyle all(StyleLength length) {
        return setPosition(YogaEdge.ALL, length);
    }

    public LayoutStyle setPosition(YogaEdge edge, float position) {
        return setPosition(edge, StyleLength.points(position));
    }

    public LayoutStyle left(float position) {
        return setPosition(YogaEdge.LEFT, position);
    }

    public LayoutStyle top(float position) {
        return setPosition(YogaEdge.TOP, position);
    }

    public LayoutStyle right(float position) {
        return setPosition(YogaEdge.RIGHT, position);
    }

    public LayoutStyle bottom(float position) {
        return setPosition(YogaEdge.BOTTOM, position);
    }

    public LayoutStyle start(float position) {
        return setPosition(YogaEdge.START, position);
    }

    public LayoutStyle end(float position) {
        return setPosition(YogaEdge.END, position);
    }

    public LayoutStyle horizontal(float position) {
        return setPosition(YogaEdge.HORIZONTAL, position);
    }

    public LayoutStyle vertical(float position) {
        return setPosition(YogaEdge.VERTICAL, position);
    }

    public LayoutStyle all(float position) {
        return setPosition(YogaEdge.ALL, position);
    }

    public LayoutStyle setPositionPercent(YogaEdge edge, float percent) {
        return setPosition(edge, StyleLength.percent(percent));
    }

    public LayoutStyle leftPercent(float percent) {
        return setPositionPercent(YogaEdge.LEFT, percent);
    }

    public LayoutStyle topPercent(float percent) {
        return setPositionPercent(YogaEdge.TOP, percent);
    }

    public LayoutStyle rightPercent(float percent) {
        return setPositionPercent(YogaEdge.RIGHT, percent);
    }

    public LayoutStyle bottomPercent(float percent) {
        return setPositionPercent(YogaEdge.BOTTOM, percent);
    }

    public LayoutStyle startPercent(float percent) {
        return setPositionPercent(YogaEdge.START, percent);
    }

    public LayoutStyle endPercent(float percent) {
        return setPositionPercent(YogaEdge.END, percent);
    }

    public LayoutStyle horizontalPercent(float percent) {
        return setPositionPercent(YogaEdge.HORIZONTAL, percent);
    }

    public LayoutStyle verticalPercent(float percent) {
        return setPositionPercent(YogaEdge.VERTICAL, percent);
    }

    public LayoutStyle allPercent(float percent) {
        return setPositionPercent(YogaEdge.ALL, percent);
    }

    public LayoutStyle setPositionAuto(YogaEdge edge) {
        return setPosition(edge, StyleLength.ofAuto());
    }

    public LayoutStyle leftAuto() {
        return setPositionAuto(YogaEdge.LEFT);
    }

    public LayoutStyle topAuto() {
        return setPositionAuto(YogaEdge.TOP);
    }

    public LayoutStyle rightAuto() {
        return setPositionAuto(YogaEdge.RIGHT);
    }

    public LayoutStyle bottomAuto() {
        return setPositionAuto(YogaEdge.BOTTOM);
    }

    public LayoutStyle startAuto() {
        return setPositionAuto(YogaEdge.START);
    }

    public LayoutStyle endAuto() {
        return setPositionAuto(YogaEdge.END);
    }

    public LayoutStyle horizontalAuto() {
        return setPositionAuto(YogaEdge.HORIZONTAL);
    }

    public LayoutStyle verticalAuto() {
        return setPositionAuto(YogaEdge.VERTICAL);
    }

    public LayoutStyle allAuto() {
        return setPositionAuto(YogaEdge.ALL);
    }

    /* Alignment properties */
    @Deprecated(since = "26.1")
    public LayoutStyle setAlignContent(YogaAlign alignContent) {
        return alignContent(alignContent);
    }

    @Deprecated(since = "26.1")
    @HideFromJS
    public LayoutStyle alignContent(YogaAlign alignContent) {
        return alignContent(switch (alignContent) {
            case AUTO, BASELINE -> null;
            case FLEX_START -> AlignContent.FLEX_START;
            case CENTER -> AlignContent.CENTER;
            case FLEX_END -> AlignContent.FLEX_END;
            case STRETCH -> AlignContent.STRETCH;
            case SPACE_BETWEEN -> AlignContent.SPACE_BETWEEN;
            case SPACE_AROUND -> AlignContent.SPACE_AROUND;
            case SPACE_EVENLY -> AlignContent.SPACE_EVENLY;
        });
    }

    @HideFromJS
    public LayoutStyle alignContent(AlignContent alignContent) {
        set(LayoutProperties.ALIGN_CONTENT, alignContent);
        return this;
    }

    @Deprecated(since = "26.1")
    public LayoutStyle setAlignItems(YogaAlign alignItems) {
        return alignItems(alignItems);
    }

    @Deprecated(since = "26.1")
    @HideFromJS
    public LayoutStyle alignItems(YogaAlign alignItems) {
        return alignItems(switch (alignItems) {
            case AUTO, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY -> null;
            case FLEX_START -> AlignItems.FLEX_START;
            case CENTER -> AlignItems.CENTER;
            case FLEX_END -> AlignItems.FLEX_END;
            case STRETCH -> AlignItems.STRETCH;
            case BASELINE -> AlignItems.BASELINE;
        });
    }

    public LayoutStyle alignItems(AlignItems alignItems) {
        set(LayoutProperties.ALIGN_ITEMS, alignItems);
        return this;
    }

    @Deprecated(since = "26.1")
    public LayoutStyle setAlignSelf(YogaAlign alignSelf) {
        return alignSelf(alignSelf);
    }

    @Deprecated(since = "26.1")
    @HideFromJS
    public LayoutStyle alignSelf(YogaAlign alignSelf) {
        return alignSelf(switch (alignSelf) {
            case AUTO, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY -> null;
            case FLEX_START -> AlignItems.FLEX_START;
            case CENTER -> AlignItems.CENTER;
            case FLEX_END -> AlignItems.FLEX_END;
            case STRETCH -> AlignItems.STRETCH;
            case BASELINE -> AlignItems.BASELINE;
        });
    }

    @HideFromJS
    public LayoutStyle alignSelf(AlignItems alignSelf) {
        set(LayoutProperties.ALIGN_SELF, alignSelf);
        return this;
    }

    /* Flex properties */
    public LayoutStyle setFlex(float flex) {
        set(LayoutProperties.FLEX, FloatOptional.of(flex));
        return this;
    }

    public LayoutStyle flex(float flex) {
        return setFlex(flex);
    }

    public LayoutStyle setFlexAuto() {
        set(LayoutProperties.FLEX, FloatOptional.of());
        return this;
    }

    public LayoutStyle flexAuto() {
        return setFlexAuto();
    }

    public LayoutStyle setFlexBasisAuto() {
        set(LayoutProperties.FLEX_BASIS, StyleSizeLength.ofAuto());
        return this;
    }

    public LayoutStyle flexBasisAuto() {
        return setFlexBasisAuto();
    }

    public LayoutStyle setFlexBasisPercent(float percent) {
        set(LayoutProperties.FLEX_BASIS, StyleSizeLength.percent(percent));
        return this;
    }

    public LayoutStyle flexBasisPercent(float percent) {
        return setFlexBasisPercent(percent);
    }

    public LayoutStyle setFlexBasis(float flexBasis) {
        set(LayoutProperties.FLEX_BASIS, StyleSizeLength.points(flexBasis));
        return this;
    }

    public LayoutStyle flexBasis(float flexBasis) {
        return setFlexBasis(flexBasis);
    }

    public LayoutStyle setFlexBasisMaxContent() {
        set(LayoutProperties.FLEX_BASIS, StyleSizeLength.ofMaxContent());
        return this;
    }

    public LayoutStyle flexBasisMaxContent() {
        return setFlexBasisMaxContent();
    }

    public LayoutStyle setFlexBasisFitContent() {
        set(LayoutProperties.FLEX_BASIS, StyleSizeLength.ofFitContent());
        return this;
    }

    public LayoutStyle flexBasisFitContent() {
        return setFlexBasisFitContent();
    }

    public LayoutStyle setFlexBasisStretch() {
        set(LayoutProperties.FLEX_BASIS, StyleSizeLength.ofStretch());
        return this;
    }

    public LayoutStyle flexBasisStretch() {
        return setFlexBasisStretch();
    }

    @Deprecated(since = "26.1")
    public LayoutStyle setFlexDirection(YogaFlexDirection direction) {
        return flexDirection(direction);
    }

    @Deprecated(since = "26.1")
    @HideFromJS
    public LayoutStyle flexDirection(YogaFlexDirection direction) {
        return flexDirection(switch (direction) {
            case COLUMN -> FlexDirection.COLUMN;
            case COLUMN_REVERSE -> FlexDirection.COLUMN_REVERSE;
            case ROW -> FlexDirection.ROW;
            case ROW_REVERSE -> FlexDirection.ROW_REVERSE;
        });
    }

    @HideFromJS
    public LayoutStyle flexDirection(FlexDirection flexDirection) {
        set(LayoutProperties.FLEX_DIRECTION, flexDirection);
        return this;
    }

    public LayoutStyle setFlexGrow(float flexGrow) {
        set(LayoutProperties.FLEX_GROW, FloatOptional.of(flexGrow));
        return this;
    }

    public LayoutStyle flexGrow(float flexGrow) {
        return setFlexGrow(flexGrow);
    }

    public LayoutStyle setFlexGrowAuto() {
        set(LayoutProperties.FLEX_GROW, FloatOptional.of());
        return this;
    }

    public LayoutStyle flexGrowAuto() {
        return setFlexGrowAuto();
    }

    public LayoutStyle setFlexShrink(float flexShrink) {
        set(LayoutProperties.FLEX_SHRINK, FloatOptional.of(flexShrink));
        return this;
    }

    public LayoutStyle flexShrink(float flexShrink) {
        return setFlexShrink(flexShrink);
    }

    public LayoutStyle setFlexShrinkAuto() {
        set(LayoutProperties.FLEX_SHRINK, FloatOptional.of());
        return this;
    }

    public LayoutStyle flexShrinkAuto() {
        return setFlexShrinkAuto();
    }

    /* Other properties */
    @Deprecated(since = "26.1")
    public LayoutStyle setJustifyContent(YogaJustify justifyContent) {
        return justifyContent(justifyContent);
    }

    @Deprecated(since = "26.1")
    @HideFromJS
    public LayoutStyle justifyContent(YogaJustify justifyContent) {
        return justifyContent(switch (justifyContent) {
            case FLEX_START -> AlignContent.FLEX_START;
            case CENTER -> AlignContent.CENTER;
            case FLEX_END -> AlignContent.FLEX_END;
            case SPACE_BETWEEN -> AlignContent.SPACE_BETWEEN;
            case SPACE_AROUND -> AlignContent.SPACE_AROUND;
            case SPACE_EVENLY -> AlignContent.SPACE_EVENLY;
        });
    }

    @HideFromJS
    public LayoutStyle justifyContent(AlignContent justifyContent) {
        set(LayoutProperties.JUSTIFY_CONTENT, justifyContent);
        return this;
    }

    public LayoutStyle justifyItems(AlignItems justifyItems) {
        set(LayoutProperties.JUSTIFY_ITEMS, justifyItems);
        return this;
    }

    public LayoutStyle justifySelf(AlignItems justifySelf) {
        set(LayoutProperties.JUSTIFY_SELF, justifySelf);
        return this;
    }

    @Deprecated(since = "26.1")
    public LayoutStyle setDirection(YogaDirection direction) {
        set(LayoutProperties.LAYOUT_DIRECTION, switch (direction) {
            case INHERIT -> TaffyDirection.INHERIT;
            case LTR -> TaffyDirection.LTR;
            case RTL -> TaffyDirection.RTL;
        });
        return this;
    }

    @Deprecated(since = "26.1")
    @HideFromJS
    public LayoutStyle direction(YogaDirection direction) {
        return setDirection(direction);
    }

    @HideFromJS
    public LayoutStyle direction(TaffyDirection direction) {
        set(LayoutProperties.LAYOUT_DIRECTION, direction);
        return this;
    }

    @Deprecated(since = "26.1")
    public LayoutStyle setWrap(YogaWrap wrap) {
        return wrap(wrap);
    }

    @HideFromJS
    public LayoutStyle wrap(YogaWrap wrap) {
        return wrap(switch (wrap) {
            case NO_WRAP -> FlexWrap.NO_WRAP;
            case WRAP -> FlexWrap.WRAP;
            case WRAP_REVERSE -> FlexWrap.WRAP_REVERSE;
        });
    }

    @HideFromJS
    public LayoutStyle wrap(FlexWrap wrap) {
        set(LayoutProperties.FLEX_WRAP, wrap);
        return this;
    }

    public LayoutStyle setAspectRatio(float aspectRatio) {
        set(LayoutProperties.ASPECT_RATE, FloatOptional.of(aspectRatio));
        return this;
    }

    public LayoutStyle aspectRatio(float aspectRatio) {
        return setAspectRatio(aspectRatio);
    }

    public LayoutStyle setAspectRatioAuto() {
        set(LayoutProperties.ASPECT_RATE, FloatOptional.of());
        return this;
    }

    public LayoutStyle aspectRatioAuto() {
        return setAspectRatioAuto();
    }

    public LayoutStyle setGap(YogaGutter gutter, StyleLength value) {
        set(LayoutProperties.GAPS[gutter.ordinal()], value);
        return this;
    }

    public LayoutStyle gapColumn(StyleLength value) {
        return setGap(YogaGutter.COLUMN, value);
    }

    public LayoutStyle gapRow(StyleLength value) {
        return setGap(YogaGutter.ROW, value);
    }

    public LayoutStyle gapAll(StyleLength value) {
        return setGap(YogaGutter.ALL, value);
    }

    public LayoutStyle setGap(YogaGutter gutter, float value) {
        return setGap(gutter, StyleLength.points(value));
    }

    public LayoutStyle gapColumn(float value) {
        return setGap(YogaGutter.COLUMN, value);
    }

    public LayoutStyle gapRow(float value) {
        return setGap(YogaGutter.ROW, value);
    }

    public LayoutStyle gapAll(float value) {
        return setGap(YogaGutter.ALL, value);
    }

    public LayoutStyle setGapPercent(YogaGutter gutter, float percent) {
        return setGap(gutter, StyleLength.percent(percent));
    }

    public LayoutStyle gapColumnPercent(float percent) {
        return setGapPercent(YogaGutter.COLUMN, percent);
    }

    public LayoutStyle gapRowPercent(float percent) {
        return setGapPercent(YogaGutter.ROW, percent);
    }

    public LayoutStyle gapAllPercent(float percent) {
        return setGapPercent(YogaGutter.ALL, percent);
    }

    @Deprecated(since = "26.1")
    public LayoutStyle setDisplay(YogaDisplay display) {
        set(LayoutProperties.DISPLAY, switch (display) {
            case FLEX, CONTENTS -> TaffyDisplay.FLEX;
            case NONE -> TaffyDisplay.NONE;
        });
        return this;
    }

    @HideFromJS
    @Deprecated(since = "26.1")
    public LayoutStyle display(YogaDisplay display) {
        return setDisplay(display);
    }

    @HideFromJS
    public LayoutStyle display(TaffyDisplay display) {
        set(LayoutProperties.DISPLAY, display);
        return this;
    }

    public LayoutStyle setOverflow(YogaOverflow overflow) {
        set(LayoutProperties.OVERFLOW, overflow);
        return this;
    }

    public LayoutStyle overflow(YogaOverflow overflow) {
        return setOverflow(overflow);
    }

    // grid
    public LayoutStyle girdTemplateRows(String gridTemplateRows) {
        set(LayoutProperties.GRID_TEMPLATE_ROWS, GridTemplateValue.parse(gridTemplateRows));
        return this;
    }

    public LayoutStyle gridTemplateRows(GridTemplate gridTemplateRows) {
        set(LayoutProperties.GRID_TEMPLATE_ROWS, gridTemplateRows);
        return this;
    }

    public LayoutStyle girdTemplateColumns(String girdTemplateColumns) {
        set(LayoutProperties.GRID_TEMPLATE_COLUMNS, GridTemplateValue.parse(girdTemplateColumns));
        return this;
    }

    public LayoutStyle gridTemplateColumns(GridTemplate girdTemplateColumns) {
        set(LayoutProperties.GRID_TEMPLATE_COLUMNS, girdTemplateColumns);
        return this;
    }

    public LayoutStyle girdTemplateAreas(String girdTemplateAreas) {
        set(LayoutProperties.GRID_TEMPLATE_AREAS, GridTemplateAreasValue.parse(girdTemplateAreas));
        return this;
    }

    public LayoutStyle girdTemplateAreas(GridTemplateAreas templateAreas) {
        set(LayoutProperties.GRID_TEMPLATE_AREAS, templateAreas);
        return this;
    }

    public LayoutStyle girdAutoRows(String gridAutoRows) {
        set(LayoutProperties.GRID_AUTO_ROWS, GridAutoValue.parse(gridAutoRows));
        return this;
    }

    public LayoutStyle gridAutoRows(GridAuto gridAutoRows) {
        set(LayoutProperties.GRID_AUTO_ROWS, gridAutoRows);
        return this;
    }

    public LayoutStyle girdAutoColumns(String girdAutoColumns) {
        set(LayoutProperties.GRID_AUTO_COLUMNS, GridAutoValue.parse(girdAutoColumns));
        return this;
    }

    public LayoutStyle girdAutoColumns(GridAuto girdAutoColumns) {
        set(LayoutProperties.GRID_AUTO_COLUMNS, girdAutoColumns);
        return this;
    }

    public LayoutStyle gridAutoFlow(GridAutoFlow gridAutoFlow) {
        set(LayoutProperties.GRID_AUTO_FLOW, gridAutoFlow);
        return this;
    }

    public LayoutStyle girdRow(String gridRow) {
        set(LayoutProperties.GRID_ROW, GridValue.parse(gridRow));
        return this;
    }

    public LayoutStyle gridRow(Grid gridRow) {
        set(LayoutProperties.GRID_ROW, gridRow);
        return this;
    }

    public LayoutStyle girdColumn(String girdColumn) {
        set(LayoutProperties.GRID_COLUMN, GridValue.parse(girdColumn));
        return this;
    }

    public LayoutStyle girdColumn(Grid girdColumn) {
        set(LayoutProperties.GRID_COLUMN, girdColumn);
        return this;
    }

    /* Getters */
    public YogaValue getWidth() {
        return getValueSave(LayoutProperties.WIDTH).asYogaValue();
    }

    public YogaValue getMinWidth() {
        return getValueSave(LayoutProperties.MIN[YogaDimension.WIDTH.ordinal()]).asYogaValue();
    }

    public YogaValue getMaxWidth() {
        return getValueSave(LayoutProperties.MAX[YogaDimension.WIDTH.ordinal()]).asYogaValue();
    }

    public YogaValue getHeight() {
        return getValueSave(LayoutProperties.HEIGHT).asYogaValue();
    }

    public YogaValue getMinHeight() {
        return getValueSave(LayoutProperties.MIN[YogaDimension.HEIGHT.ordinal()]).asYogaValue();
    }

    public YogaValue getMaxHeight() {
        return getValueSave(LayoutProperties.MAX[YogaDimension.HEIGHT.ordinal()]).asYogaValue();
    }

    public TaffyDirection getStyleDirection() {
        return getValueSave(LayoutProperties.LAYOUT_DIRECTION);
    }

    public FlexDirection getFlexDirection() {
        return getValueSave(LayoutProperties.FLEX_DIRECTION);
    }

    public AlignContent getJustifyContent() {
        return getValueSave(LayoutProperties.JUSTIFY_CONTENT);
    }

    public AlignItems getJustifyItems() {
        return getValueSave(LayoutProperties.JUSTIFY_ITEMS);
    }

    public AlignItems getJustifySelf() {
        return getValueSave(LayoutProperties.JUSTIFY_SELF);
    }

    public AlignItems getAlignItems() {
        return getValueSave(LayoutProperties.ALIGN_ITEMS);
    }

    public AlignItems getAlignSelf() {
        return getValueSave(LayoutProperties.ALIGN_SELF);
    }

    public AlignContent getAlignContent() {
        return getValueSave(LayoutProperties.ALIGN_CONTENT);
    }

    public TaffyPosition getPositionType() {
        return getValueSave(LayoutProperties.POSITION);
    }

    public float getFlexGrow() {
        return getValueSave(LayoutProperties.FLEX_GROW).unwrapOrDefault(YogaStyle.DEFAULT_FLEX_GROW);
    }

    public float getFlexShrink() {
        return getValueSave(LayoutProperties.FLEX_SHRINK).unwrapOrDefault(YogaStyle.DEFAULT_FLEX_SHRINK);
    }

    public YogaValue getFlexBasis() {
        return getValueSave(LayoutProperties.FLEX_BASIS).asYogaValue();
    }

    public float getAspectRatio() {
        return getValueSave(LayoutProperties.ASPECT_RATE).unwrapOrDefault(YogaConstants.UNDEFINED);
    }

    public YogaValue getMargin(YogaEdge edge) {
        return getValueSave(LayoutProperties.MARGINS[edge.ordinal()]).asYogaValue();
    }

    public YogaValue getPadding(YogaEdge edge) {
        return getValueSave(LayoutProperties.PADDINGS[edge.ordinal()]).asYogaValue();
    }

    public YogaValue getPosition(YogaEdge edge) {
        return getValueSave(LayoutProperties.POSITIONS[edge.ordinal()]).asYogaValue();
    }

    public YogaValue getGap(YogaGutter gutter) {
        return getValueSave(LayoutProperties.GAPS[gutter.ordinal()]).asYogaValue();
    }

    public YogaOverflow getOverflow() {
        return getValueSave(LayoutProperties.OVERFLOW);
    }
}
