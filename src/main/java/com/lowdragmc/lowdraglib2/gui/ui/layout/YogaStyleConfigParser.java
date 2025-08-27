package com.lowdragmc.lowdraglib2.gui.ui.layout;

import com.lowdragmc.lowdraglib2.configurator.accessors.EnumAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.FloatOptionalConfigurator;
import org.appliedenergistics.yoga.YogaDirection;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaNode;
import org.appliedenergistics.yoga.numeric.FloatOptional;

public class YogaStyleConfigParser {

    public static void buildConfigurator(YogaNode yogaNode, ConfiguratorGroup father) {
        var style = yogaNode.getStyle();
        father.addConfigurator(EnumAccessor.create("LayoutDirection",
                YogaDirection.class, style::getDirection, yogaNode::setDirection,
                YogaDirection.INHERIT, true).setTips("LayoutDirection.tips"));

        // flex
        father.addConfigurator(EnumAccessor.create("FlexDirection",
                YogaFlexDirection.class, style::getFlexDirection, yogaNode::setFlexDirection,
                YogaFlexDirection.COLUMN, true).setTips("FlexDirection.tips"));

        father.addConfigurator(new FloatOptionalConfigurator("Flex", style::getFlex, value -> {
            style.setFlex(value);
            yogaNode.markDirtyAndPropagate();
        }, FloatOptional.of(), true).setTips("Flex.tips"));

        father.addConfigurator(new FloatOptionalConfigurator("FlexGrow", style::getFlexGrow, value -> {
            style.setFlexGrow(value);
            yogaNode.markDirtyAndPropagate();
        }, FloatOptional.of(), true).setTips("FlexGrow.tips"));

        father.addConfigurator(new FloatOptionalConfigurator("FlexShrink", style::getFlexShrink, value -> {
            style.setFlexShrink(value);
            yogaNode.markDirtyAndPropagate();
        }, FloatOptional.of(), true).setTips("FlexShrink.tips"));
    }
}
