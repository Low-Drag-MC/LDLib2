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
import dev.vfyjxf.taffy.style.*;

import java.util.*;

//@RemapPrefixForJS("kjs$")
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
        properties.add(LayoutProperties.LEFT);
        properties.add(LayoutProperties.RIGHT);
        properties.add(LayoutProperties.TOP);
        properties.add(LayoutProperties.BOTTOM);
        properties.add(LayoutProperties.MARGIN_LEFT);
        properties.add(LayoutProperties.MARGIN_RIGHT);
        properties.add(LayoutProperties.MARGIN_TOP);
        properties.add(LayoutProperties.MARGIN_BOTTOM);
        properties.add(LayoutProperties.MARGIN_VERTICAL);
        properties.add(LayoutProperties.MARGIN_HORIZONTAL);
        properties.add(LayoutProperties.MARGIN_ALL);
        properties.add(LayoutProperties.MARGIN);
        properties.add(LayoutProperties.PADDING_LEFT);
        properties.add(LayoutProperties.PADDING_RIGHT);
        properties.add(LayoutProperties.PADDING_TOP);
        properties.add(LayoutProperties.PADDING_BOTTOM);
        properties.add(LayoutProperties.PADDING_VERTICAL);
        properties.add(LayoutProperties.PADDING_HORIZONTAL);
        properties.add(LayoutProperties.PADDING_ALL);
        properties.add(LayoutProperties.PADDING);
        properties.add(LayoutProperties.GAP_ROW);
        properties.add(LayoutProperties.GAP_COLUMN);
        properties.add(LayoutProperties.GAP_ALL);
        properties.add(LayoutProperties.GAP);
        properties.add(LayoutProperties.WIDTH);
        properties.add(LayoutProperties.HEIGHT);
        properties.add(LayoutProperties.MIN_WIDTH);
        properties.add(LayoutProperties.MIN_HEIGHT);
        properties.add(LayoutProperties.MAX_WIDTH);
        properties.add(LayoutProperties.MAX_HEIGHT);
        properties.add(LayoutProperties.ASPECT_RATE);
        properties.add(LayoutProperties.ALIGN_ITEMS);
        properties.add(LayoutProperties.ALIGN_CONTENT);
        properties.add(LayoutProperties.ALIGN_SELF);
        properties.add(LayoutProperties.JUSTIFY_CONTENT);
        properties.add(LayoutProperties.JUSTIFY_ITEMS);
        properties.add(LayoutProperties.JUSTIFY_SELF);
        properties.add(LayoutProperties.GRID_TEMPLATE_ROWS);
        properties.add(LayoutProperties.GRID_TEMPLATE_COLUMNS);
        properties.add(LayoutProperties.GRID_TEMPLATE_AREAS);
        properties.add(LayoutProperties.GRID_AUTO_ROWS);
        properties.add(LayoutProperties.GRID_AUTO_COLUMNS);
        properties.add(LayoutProperties.GRID_AUTO_FLOW);
        properties.add(LayoutProperties.GRID_ROW);
        properties.add(LayoutProperties.GRID_COLUMN);
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

    public LayoutStyle setWidth(TaffyDimension dimension) {
        set(LayoutProperties.WIDTH, dimension);
        return this;
    }

    public LayoutStyle width(float width) {
        set(LayoutProperties.WIDTH, TaffyDimension.length(width));
        return this;
    }

    public LayoutStyle widthPercent(float percent) {
        set(LayoutProperties.WIDTH, TaffyDimension.percent(percent / 100f));
        return this;
    }

    public LayoutStyle widthAuto() {
        set(LayoutProperties.WIDTH, TaffyDimension.auto());
        return this;
    }

    public LayoutStyle widthMaxContent() {
        set(LayoutProperties.WIDTH, TaffyDimension.maxContent());
        return this;
    }

    public LayoutStyle widthMinContent() {
        set(LayoutProperties.WIDTH, TaffyDimension.minContent());
        return this;
    }

    public LayoutStyle widthFitContent() {
        set(LayoutProperties.WIDTH, TaffyDimension.fitContent());
        return this;
    }

    public LayoutStyle widthStretch() {
        set(LayoutProperties.WIDTH, TaffyDimension.stretch());
        return this;
    }

    public LayoutStyle setMinWidth(TaffyDimension dimension) {
        set(LayoutProperties.MIN_WIDTH, dimension);
        return this;
    }

    public LayoutStyle minWidth(float minWidth) {
        set(LayoutProperties.MIN_WIDTH, TaffyDimension.length(minWidth));
        return this;
    }

    public LayoutStyle minWidthPercent(float percent) {
        set(LayoutProperties.MIN_WIDTH, TaffyDimension.percent(percent / 100f));
        return this;
    }

    public LayoutStyle minWidthAuto() {
        set(LayoutProperties.MIN_WIDTH, TaffyDimension.auto());
        return this;
    }

    public LayoutStyle minWidthMaxContent() {
        set(LayoutProperties.MIN_WIDTH, TaffyDimension.maxContent());
        return this;
    }

    public LayoutStyle minWidthMinContent() {
        set(LayoutProperties.MIN_WIDTH, TaffyDimension.minContent());
        return this;
    }

    public LayoutStyle minWidthFitContent() {
        set(LayoutProperties.MIN_WIDTH, TaffyDimension.fitContent());
        return this;
    }

    public LayoutStyle minWidthStretch() {
        set(LayoutProperties.MIN_WIDTH, TaffyDimension.stretch());
        return this;
    }

    public LayoutStyle setMaxWidth(TaffyDimension dimension) {
        set(LayoutProperties.MAX_WIDTH, dimension);
        return this;
    }

    public LayoutStyle maxWidth(float maxWidth) {
        set(LayoutProperties.MAX_WIDTH, TaffyDimension.length(maxWidth));
        return this;
    }

    public LayoutStyle maxWidthPercent(float percent) {
        set(LayoutProperties.MAX_WIDTH, TaffyDimension.percent(percent / 100f));
        return this;
    }

    public LayoutStyle maxWidthAuto() {
        set(LayoutProperties.MAX_WIDTH, TaffyDimension.auto());
        return this;
    }

    public LayoutStyle maxWidthMaxContent() {
        set(LayoutProperties.MAX_WIDTH, TaffyDimension.maxContent());
        return this;
    }

    public LayoutStyle maxWidthMinContent() {
        set(LayoutProperties.MAX_WIDTH, TaffyDimension.minContent());
        return this;
    }

    public LayoutStyle maxWidthFitContent() {
        set(LayoutProperties.MAX_WIDTH, TaffyDimension.fitContent());
        return this;
    }

    public LayoutStyle maxWidthStretch() {
        set(LayoutProperties.MAX_WIDTH, TaffyDimension.stretch());
        return this;
    }

    /* Height properties */
    public LayoutStyle setHeight(TaffyDimension dimension) {
        set(LayoutProperties.HEIGHT, dimension);
        return this;
    }

    public LayoutStyle height(float height) {
        set(LayoutProperties.HEIGHT, TaffyDimension.length(height));
        return this;
    }

    public LayoutStyle heightPercent(float percent) {
        set(LayoutProperties.HEIGHT, TaffyDimension.percent(percent / 100f));
        return this;
    }

    public LayoutStyle heightAuto() {
        set(LayoutProperties.HEIGHT, TaffyDimension.auto());
        return this;
    }

    public LayoutStyle heightMaxContent() {
        set(LayoutProperties.HEIGHT, TaffyDimension.maxContent());
        return this;
    }

    public LayoutStyle heightMinContent() {
        set(LayoutProperties.HEIGHT, TaffyDimension.minContent());
        return this;
    }

    public LayoutStyle heightFitContent() {
        set(LayoutProperties.HEIGHT, TaffyDimension.fitContent());
        return this;
    }

    public LayoutStyle heightStretch() {
        set(LayoutProperties.HEIGHT, TaffyDimension.stretch());
        return this;
    }

    public LayoutStyle setMinHeight(TaffyDimension dimension) {
        set(LayoutProperties.MIN_HEIGHT, dimension);
        return this;
    }

    public LayoutStyle minHeight(float minHeight) {
        set(LayoutProperties.MIN_HEIGHT, TaffyDimension.length(minHeight));
        return this;
    }

    public LayoutStyle minHeightPercent(float percent) {
        set(LayoutProperties.MIN_HEIGHT, TaffyDimension.percent(percent / 100f));
        return this;
    }

    public LayoutStyle minHeightAuto() {
        set(LayoutProperties.MIN_HEIGHT, TaffyDimension.auto());
        return this;
    }

    public LayoutStyle minHeightMaxContent() {
        set(LayoutProperties.MIN_HEIGHT, TaffyDimension.maxContent());
        return this;
    }

    public LayoutStyle minHeightMinContent() {
        set(LayoutProperties.MIN_HEIGHT, TaffyDimension.minContent());
        return this;
    }

    public LayoutStyle minHeightFitContent() {
        set(LayoutProperties.MIN_HEIGHT, TaffyDimension.fitContent());
        return this;
    }

    public LayoutStyle minHeightStretch() {
        set(LayoutProperties.MIN_HEIGHT, TaffyDimension.stretch());
        return this;
    }

    public LayoutStyle setMaxHeight(TaffyDimension dimension) {
        set(LayoutProperties.MAX_HEIGHT, dimension);
        return this;
    }

    public LayoutStyle maxHeight(float maxHeight) {
        set(LayoutProperties.MAX_HEIGHT, TaffyDimension.length(maxHeight));
        return this;
    }

    public LayoutStyle maxHeightPercent(float percent) {
        set(LayoutProperties.MAX_HEIGHT, TaffyDimension.percent(percent / 100f));
        return this;
    }

    public LayoutStyle maxHeightAuto() {
        set(LayoutProperties.MAX_HEIGHT, TaffyDimension.auto());
        return this;
    }

    public LayoutStyle maxHeightMaxContent() {
        set(LayoutProperties.MAX_HEIGHT, TaffyDimension.maxContent());
        return this;
    }

    public LayoutStyle maxHeightMinContent() {
        set(LayoutProperties.MAX_HEIGHT, TaffyDimension.minContent());
        return this;
    }

    public LayoutStyle maxHeightFitContent() {
        set(LayoutProperties.MAX_HEIGHT, TaffyDimension.fitContent());
        return this;
    }

    public LayoutStyle maxHeightStretch() {
        set(LayoutProperties.MAX_HEIGHT, TaffyDimension.stretch());
        return this;
    }

    /* Margin properties */
    public LayoutStyle marginLeft(float margin) {
        set(LayoutProperties.MARGIN_LEFT, LengthPercentageAuto.length(margin));
        return this;
    }

    public LayoutStyle marginTop(float margin) {
        set(LayoutProperties.MARGIN_TOP, LengthPercentageAuto.length(margin));
        return this;
    }

    public LayoutStyle marginRight(float margin) {
        set(LayoutProperties.MARGIN_RIGHT, LengthPercentageAuto.length(margin));
        return this;
    }

    public LayoutStyle marginBottom(float margin) {
        set(LayoutProperties.MARGIN_BOTTOM, LengthPercentageAuto.length(margin));
        return this;
    }

    public LayoutStyle marginHorizontal(float margin) {
        set(LayoutProperties.MARGIN_HORIZONTAL, LengthPercentageAuto.length(margin));
        return this;
    }

    public LayoutStyle marginVertical(float margin) {
        set(LayoutProperties.MARGIN_VERTICAL, LengthPercentageAuto.length(margin));
        return this;
    }

    public LayoutStyle marginAll(float margin) {
        set(LayoutProperties.MARGIN_ALL, LengthPercentageAuto.length(margin));
        return this;
    }

    public LayoutStyle marginLeftPercent(float margin) {
        set(LayoutProperties.MARGIN_LEFT, LengthPercentageAuto.percent(margin / 100f));
        return this;
    }

    public LayoutStyle marginTopPercent(float margin) {
        set(LayoutProperties.MARGIN_TOP, LengthPercentageAuto.percent(margin / 100f));
        return this;
    }

    public LayoutStyle marginRightPercent(float margin) {
        set(LayoutProperties.MARGIN_RIGHT, LengthPercentageAuto.percent(margin / 100f));
        return this;
    }

    public LayoutStyle marginBottomPercent(float margin) {
        set(LayoutProperties.MARGIN_BOTTOM, LengthPercentageAuto.percent(margin / 100f));
        return this;
    }

    public LayoutStyle marginHorizontalPercent(float margin) {
        set(LayoutProperties.MARGIN_HORIZONTAL, LengthPercentageAuto.percent(margin / 100f));
        return this;
    }

    public LayoutStyle marginVerticalPercent(float margin) {
        set(LayoutProperties.MARGIN_VERTICAL, LengthPercentageAuto.percent(margin / 100f));
        return this;
    }

    public LayoutStyle marginAllPercent(float margin) {
        set(LayoutProperties.MARGIN_ALL, LengthPercentageAuto.percent(margin / 100f));
        return this;
    }

    public LayoutStyle marginLeftAuto() {
        set(LayoutProperties.MARGIN_LEFT, LengthPercentageAuto.auto());
        return this;
    }

    public LayoutStyle marginTopAuto() {
        set(LayoutProperties.MARGIN_TOP, LengthPercentageAuto.auto());
        return this;
    }

    public LayoutStyle marginRightAuto() {
        set(LayoutProperties.MARGIN_RIGHT, LengthPercentageAuto.auto());
        return this;
    }

    public LayoutStyle marginBottomAuto() {
        set(LayoutProperties.MARGIN_BOTTOM, LengthPercentageAuto.auto());
        return this;
    }

    public LayoutStyle marginHorizontalAuto() {
        set(LayoutProperties.MARGIN_HORIZONTAL, LengthPercentageAuto.auto());
        return this;
    }

    public LayoutStyle marginVerticalAuto() {
        set(LayoutProperties.MARGIN_VERTICAL, LengthPercentageAuto.auto());
        return this;
    }

    public LayoutStyle marginAllAuto() {
        set(LayoutProperties.MARGIN_ALL, LengthPercentageAuto.auto());
        return this;
    }

    /* Padding properties */
    public LayoutStyle paddingLeft(float padding) {
        set(LayoutProperties.PADDING_LEFT, LengthPercentageAuto.length(padding));
        return this;
    }

    public LayoutStyle paddingTop(float padding) {
        set(LayoutProperties.PADDING_TOP, LengthPercentageAuto.length(padding));
        return this;
    }

    public LayoutStyle paddingRight(float padding) {
        set(LayoutProperties.PADDING_RIGHT, LengthPercentageAuto.length(padding));
        return this;
    }

    public LayoutStyle paddingBottom(float padding) {
        set(LayoutProperties.PADDING_BOTTOM, LengthPercentageAuto.length(padding));
        return this;
    }

    public LayoutStyle paddingHorizontal(float padding) {
        set(LayoutProperties.PADDING_HORIZONTAL, LengthPercentageAuto.length(padding));
        return this;
    }

    public LayoutStyle paddingVertical(float padding) {
        set(LayoutProperties.PADDING_VERTICAL, LengthPercentageAuto.length(padding));
        return this;
    }

    public LayoutStyle paddingAll(float padding) {
        set(LayoutProperties.PADDING_ALL, LengthPercentageAuto.length(padding));
        return this;
    }

    public LayoutStyle paddingLeftPercent(float padding) {
        set(LayoutProperties.PADDING_LEFT, LengthPercentageAuto.percent(padding / 100f));
        return this;
    }

    public LayoutStyle paddingTopPercent(float padding) {
        set(LayoutProperties.PADDING_TOP, LengthPercentageAuto.percent(padding / 100f));
        return this;
    }

    public LayoutStyle paddingRightPercent(float padding) {
        set(LayoutProperties.PADDING_RIGHT, LengthPercentageAuto.percent(padding / 100f));
        return this;
    }

    public LayoutStyle paddingBottomPercent(float padding) {
        set(LayoutProperties.PADDING_BOTTOM, LengthPercentageAuto.percent(padding / 100f));
        return this;
    }

    public LayoutStyle paddingHorizontalPercent(float padding) {
        set(LayoutProperties.PADDING_HORIZONTAL, LengthPercentageAuto.percent(padding / 100f));
        return this;
    }

    public LayoutStyle paddingVerticalPercent(float padding) {
        set(LayoutProperties.PADDING_VERTICAL, LengthPercentageAuto.percent(padding / 100f));
        return this;
    }

    public LayoutStyle paddingAllPercent(float padding) {
        set(LayoutProperties.PADDING_ALL, LengthPercentageAuto.percent(padding / 100f));
        return this;
    }

    /* Position properties */
    public LayoutStyle positionType(TaffyPosition positionType) {
        set(LayoutProperties.POSITION, positionType);
        return this;
    }

    public LayoutStyle left(float position) {
        set(LayoutProperties.LEFT, LengthPercentageAuto.length(position));
        return this;
    }

    public LayoutStyle top(float position) {
        set(LayoutProperties.TOP, LengthPercentageAuto.length(position));
        return this;
    }

    public LayoutStyle right(float position) {
        set(LayoutProperties.RIGHT, LengthPercentageAuto.length(position));
        return this;
    }

    public LayoutStyle bottom(float position) {
        set(LayoutProperties.BOTTOM, LengthPercentageAuto.length(position));
        return this;
    }

    public LayoutStyle leftPercent(float percent) {
        set(LayoutProperties.LEFT, LengthPercentageAuto.percent(percent / 100f));
        return this;
    }

    public LayoutStyle topPercent(float percent) {
        set(LayoutProperties.TOP, LengthPercentageAuto.percent(percent / 100f));
        return this;
    }

    public LayoutStyle rightPercent(float percent) {
        set(LayoutProperties.RIGHT, LengthPercentageAuto.percent(percent / 100f));
        return this;
    }

    public LayoutStyle bottomPercent(float percent) {
        set(LayoutProperties.BOTTOM, LengthPercentageAuto.percent(percent / 100f));
        return this;
    }

    public LayoutStyle leftAuto() {
        set(LayoutProperties.LEFT, LengthPercentageAuto.auto());
        return this;
    }

    public LayoutStyle topAuto() {
        set(LayoutProperties.TOP, LengthPercentageAuto.auto());
        return this;
    }

    public LayoutStyle rightAuto() {
        set(LayoutProperties.RIGHT, LengthPercentageAuto.auto());
        return this;
    }

    public LayoutStyle bottomAuto() {
        set(LayoutProperties.BOTTOM, LengthPercentageAuto.auto());
        return this;
    }

    /* Alignment properties */
    public LayoutStyle alignContent(AlignContent alignContent) {
        set(LayoutProperties.ALIGN_CONTENT, alignContent);
        return this;
    }

    public LayoutStyle alignItems(AlignItems alignItems) {
        set(LayoutProperties.ALIGN_ITEMS, alignItems);
        return this;
    }

    public LayoutStyle alignSelf(AlignItems alignSelf) {
        set(LayoutProperties.ALIGN_SELF, alignSelf);
        return this;
    }

    /* Flex properties */
    public LayoutStyle flex(float flex) {
        set(LayoutProperties.FLEX, flex);
        return this;
    }

    public LayoutStyle flexAuto() {
        set(LayoutProperties.FLEX, Float.NaN);
        return this;
    }


    public LayoutStyle flexBasisAuto() {
        set(LayoutProperties.FLEX_BASIS, TaffyDimension.auto());
        return this;
    }

    public LayoutStyle flexBasisPercent(float percent) {
        set(LayoutProperties.FLEX_BASIS, TaffyDimension.percent(percent / 100f));
        return this;
    }

    public LayoutStyle flexBasis(float flexBasis) {
        set(LayoutProperties.FLEX_BASIS, TaffyDimension.length(flexBasis));
        return this;
    }

    public LayoutStyle flexBasisMaxContent() {
        set(LayoutProperties.FLEX_BASIS, TaffyDimension.maxContent());
        return this;
    }

    public LayoutStyle flexBasisMinContent() {
        set(LayoutProperties.FLEX_BASIS, TaffyDimension.minContent());
        return this;
    }

    public LayoutStyle flexBasisFitContent() {
        set(LayoutProperties.FLEX_BASIS, TaffyDimension.fitContent());
        return this;
    }

    public LayoutStyle flexBasisStretch() {
        set(LayoutProperties.FLEX_BASIS, TaffyDimension.stretch());
        return this;
    }

    public LayoutStyle flexDirection(FlexDirection flexDirection) {
        set(LayoutProperties.FLEX_DIRECTION, flexDirection);
        return this;
    }

    public LayoutStyle setFlexGrow(float flexGrow) {
        set(LayoutProperties.FLEX_GROW, flexGrow);
        return this;
    }

    public LayoutStyle flexGrow(float flexGrow) {
        return setFlexGrow(flexGrow);
    }

    public LayoutStyle setFlexGrowAuto() {
        set(LayoutProperties.FLEX_GROW, Float.NaN);
        return this;
    }

    public LayoutStyle flexGrowAuto() {
        return setFlexGrowAuto();
    }

    public LayoutStyle setFlexShrink(float flexShrink) {
        set(LayoutProperties.FLEX_SHRINK, flexShrink);
        return this;
    }

    public LayoutStyle flexShrink(float flexShrink) {
        return setFlexShrink(flexShrink);
    }

    public LayoutStyle setFlexShrinkAuto() {
        set(LayoutProperties.FLEX_SHRINK, Float.NaN);
        return this;
    }

    public LayoutStyle flexShrinkAuto() {
        return setFlexShrinkAuto();
    }

    /* Other properties */
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

    public LayoutStyle direction(TaffyDirection direction) {
        set(LayoutProperties.LAYOUT_DIRECTION, direction);
        return this;
    }

    public LayoutStyle wrap(FlexWrap wrap) {
        set(LayoutProperties.FLEX_WRAP, wrap);
        return this;
    }

    public LayoutStyle flexWrap(FlexWrap wrap) {
        set(LayoutProperties.FLEX_WRAP, wrap);
        return this;
    }

    public LayoutStyle setAspectRatio(float aspectRatio) {
        set(LayoutProperties.ASPECT_RATE, aspectRatio);
        return this;
    }

    public LayoutStyle aspectRatio(float aspectRatio) {
        return setAspectRatio(aspectRatio);
    }

    public LayoutStyle setAspectRatioAuto() {
        set(LayoutProperties.ASPECT_RATE, Float.NaN);
        return this;
    }

    public LayoutStyle aspectRatioAuto() {
        return setAspectRatioAuto();
    }

    public LayoutStyle gapColumn(float value) {
        set(LayoutProperties.GAP_COLUMN, LengthPercentageAuto.length(value));
        return this;
    }

    public LayoutStyle gapRow(float value) {
        set(LayoutProperties.GAP_ROW, LengthPercentageAuto.length(value));
        return this;
    }

    public LayoutStyle gapAll(float value) {
        set(LayoutProperties.GAP_ALL, LengthPercentageAuto.length(value));
        return this;
    }

    public LayoutStyle gapColumnPercent(float percent) {
        set(LayoutProperties.GAP_COLUMN, LengthPercentageAuto.percent(percent / 100f));
        return this;
    }

    public LayoutStyle gapRowPercent(float percent) {
        set(LayoutProperties.GAP_ROW, LengthPercentageAuto.percent(percent / 100f));
        return this;
    }

    public LayoutStyle gapAllPercent(float percent) {
        set(LayoutProperties.GAP_ALL, LengthPercentageAuto.percent(percent / 100f));
        return this;
    }

    public LayoutStyle display(TaffyDisplay display) {
        set(LayoutProperties.DISPLAY, display);
        return this;
    }

    // grid
    public LayoutStyle gridTemplateRows(String gridTemplateRows) {
        set(LayoutProperties.GRID_TEMPLATE_ROWS, GridTemplateValue.parse(gridTemplateRows));
        return this;
    }

    public LayoutStyle gridTemplateRows(GridTemplate gridTemplateRows) {
        set(LayoutProperties.GRID_TEMPLATE_ROWS, gridTemplateRows);
        return this;
    }

    public LayoutStyle gridTemplateColumns(String gridTemplateColumns) {
        set(LayoutProperties.GRID_TEMPLATE_COLUMNS, GridTemplateValue.parse(gridTemplateColumns));
        return this;
    }

    public LayoutStyle gridTemplateColumns(GridTemplate gridTemplateColumns) {
        set(LayoutProperties.GRID_TEMPLATE_COLUMNS, gridTemplateColumns);
        return this;
    }

    public LayoutStyle gridTemplateAreas(String gridTemplateAreas) {
        set(LayoutProperties.GRID_TEMPLATE_AREAS, GridTemplateAreasValue.parse(gridTemplateAreas));
        return this;
    }

    public LayoutStyle gridTemplateAreas(GridTemplateAreas templateAreas) {
        set(LayoutProperties.GRID_TEMPLATE_AREAS, templateAreas);
        return this;
    }

    public LayoutStyle gridAutoRows(String gridAutoRows) {
        set(LayoutProperties.GRID_AUTO_ROWS, GridAutoValue.parse(gridAutoRows));
        return this;
    }

    public LayoutStyle gridAutoRows(GridAuto gridAutoRows) {
        set(LayoutProperties.GRID_AUTO_ROWS, gridAutoRows);
        return this;
    }

    public LayoutStyle gridAutoColumns(String gridAutoColumns) {
        set(LayoutProperties.GRID_AUTO_COLUMNS, GridAutoValue.parse(gridAutoColumns));
        return this;
    }

    public LayoutStyle gridAutoColumns(GridAuto gridAutoColumns) {
        set(LayoutProperties.GRID_AUTO_COLUMNS, gridAutoColumns);
        return this;
    }

    public LayoutStyle gridAutoFlow(GridAutoFlow gridAutoFlow) {
        set(LayoutProperties.GRID_AUTO_FLOW, gridAutoFlow);
        return this;
    }

    public LayoutStyle gridRow(String gridRow) {
        set(LayoutProperties.GRID_ROW, GridValue.parse(gridRow));
        return this;
    }

    public LayoutStyle gridRow(Grid gridRow) {
        set(LayoutProperties.GRID_ROW, gridRow);
        return this;
    }

    public LayoutStyle gridColumn(String gridColumn) {
        set(LayoutProperties.GRID_COLUMN, GridValue.parse(gridColumn));
        return this;
    }

    public LayoutStyle gridColumn(Grid gridColumn) {
        set(LayoutProperties.GRID_COLUMN, gridColumn);
        return this;
    }

    /* Getters */
    public TaffyDimension getWidth() {
        return getValueSave(LayoutProperties.WIDTH);
    }

    public TaffyDimension getMinWidth() {
        return getValueSave(LayoutProperties.MIN_WIDTH);
    }

    public TaffyDimension getMaxWidth() {
        return getValueSave(LayoutProperties.MAX_WIDTH);
    }

    public TaffyDimension getHeight() {
        return getValueSave(LayoutProperties.HEIGHT);
    }

    public TaffyDimension getMinHeight() {
        return getValueSave(LayoutProperties.MIN_HEIGHT);
    }

    public TaffyDimension getMaxHeight() {
        return getValueSave(LayoutProperties.MAX_HEIGHT);
    }

    public TaffyDimension getFlexBasis() {
        return getValueSave(LayoutProperties.FLEX_BASIS);
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
        return getValueSave(LayoutProperties.FLEX_GROW);
    }

    public float getFlexShrink() {
        return getValueSave(LayoutProperties.FLEX_SHRINK);
    }

    public float getAspectRatio() {
        return getValueSave(LayoutProperties.ASPECT_RATE);
    }
}
