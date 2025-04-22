package com.lowdragmc.lowdraglib.gui.core;

public class UIStyle {
    // 尺寸
    public Dimension width = Dimension.AUTO;
    public Dimension height = Dimension.AUTO;
    public Dimension minWidth = Dimension.AUTO;
    public Dimension minHeight = Dimension.AUTO;
    public Dimension maxWidth = Dimension.NONE;
    public Dimension maxHeight = Dimension.NONE;
    
    // 边距
    public Spacing margin = Spacing.ZERO;
    
    // 内边距
    public Spacing padding = Spacing.ZERO;
    
    // 边框
    public Spacing borderWidth = Spacing.ZERO;
    
    // 弹性容器
    public FlexDirection flexDirection = FlexDirection.ROW;
    public FlexWrap flexWrap = FlexWrap.NOWRAP;
    public AlignItems alignItems = AlignItems.STRETCH;
    public AlignContent alignContent = AlignContent.STRETCH;
    public JustifyContent justifyContent = JustifyContent.FLEX_START;
    
    // 弹性子项
    public Flex flex = Flex.of(0, 1, Dimension.AUTO);
    public AlignSelf alignSelf = AlignSelf.AUTO;
    
    // 定位
    //public Position position = Position.RELATIVE;
    
    // 枚举类型
    public enum FlexDirection { ROW, ROW_REVERSE, COLUMN, COLUMN_REVERSE }
    public enum FlexWrap { NOWRAP, WRAP, WRAP_REVERSE }
    public enum AlignItems { AUTO, FLEX_START, FLEX_END, CENTER, STRETCH, BASELINE }
    public enum AlignContent { STRETCH, FLEX_START, FLEX_END, CENTER, SPACE_BETWEEN, SPACE_AROUND }
    public enum JustifyContent { FLEX_START, FLEX_END, CENTER, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY }
    public enum AlignSelf { AUTO, FLEX_START, FLEX_END, CENTER, STRETCH, BASELINE }
}
