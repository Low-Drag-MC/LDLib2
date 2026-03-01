package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import lombok.EqualsAndHashCode;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class StyleRule {
    private final static AtomicInteger SOURCE_ID_COUNTER = new AtomicInteger(0);
    public final HierarchicalStyleMatcher matcher;
    public final Map<Property<?>, StyleValue<?>> properties;
    @EqualsAndHashCode.Exclude
    public final int sourceOrder;

    public StyleRule(HierarchicalStyleMatcher matcher, Map<Property<?>, StyleValue<?>> properties) {
        this.sourceOrder = SOURCE_ID_COUNTER.getAndIncrement();
        this.matcher = matcher;
        this.properties = properties;
    }

    public StyleValue<?> getProperty(Property<?> property) {
        return properties.get(property);
    }

    public boolean matches(UIElement element) {
        return matcher.matches(element);
    }

    public int getSpecificity() {
        return matcher.getSpecificity();
    }
}