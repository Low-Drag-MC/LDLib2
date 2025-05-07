package com.lowdragmc.lowdraglib.gui.ui.style;

import java.util.Set;

public class InheritableProperties {
    // 可继承属性白名单
    public static final Set<String> INHERITABLE = Set.of(
        "font-family", 
        "font-size",
        "color",
        "line-height",
        "text-align",
        "visibility"
    );
    
    public static boolean isInheritable(String property) {
        return INHERITABLE.contains(property);
    }
}