package com.lowdragmc.lowdraglib.editor.configurator;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Size;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Function;

/**
 * @author KilaBash
 * @date 2022/12/3
 * @implNote WrapperConfigurator
 */
@Accessors(chain = true)
public class WrapperConfigurator extends Configurator {
    public final Widget inner;
    @Setter
    public boolean removeTitleBar;

    public WrapperConfigurator(Widget widget) {
        this("", widget, true);
    }

    public WrapperConfigurator(Function<WrapperConfigurator, Widget> widgetSupplier) {
        this("", widgetSupplier, true);
    }

    public WrapperConfigurator(String name, Widget widget) {
        this(name, widget, false);
    }

    public WrapperConfigurator(String name, Function<WrapperConfigurator, Widget> widgetSupplier) {
        this(name, widgetSupplier, false);
    }

    public WrapperConfigurator(String name, Widget widget, boolean removeTitleBar) {
        this(name, configurator -> widget, removeTitleBar);
    }

    public WrapperConfigurator(String name, Function<WrapperConfigurator, Widget> widgetSupplier, boolean removeTitleBar) {
        super(name);
        this.inner = widgetSupplier.apply(this);
        this.removeTitleBar = removeTitleBar;
    }

    @Override
    public void computeHeight() {
        super.computeHeight();
        if (removeTitleBar) {
            setSize(Size.of(getSize().width, inner.getSize().height + 4));
        } else {
            setSize(Size.of(getSize().width, inner.getSize().height + 19));
        }
    }

    @Override
    public void init(int width) {
        super.init(width);
        Size size = inner.getSize();
        inner.setSelfPosition(Position.of((width - size.width) / 2, removeTitleBar ? 2 : 17));
        addWidget(inner);
    }
}
