package com.lowdragmc.lowdraglib.editor_outdated.configurator;

import com.lowdragmc.lowdraglib.editor_outdated.IConfiguratorContainer;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.math.Size;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote Configurator
 */
public class Configurator extends WidgetGroup {
    @Nullable
    @Getter
    protected IConfiguratorContainer configuratorContainer;
    protected String[] tips = new String[0];
    @Getter
    protected String name;
    @Getter
    protected int leftWidth, rightWidth, width = -1;
    @Getter
    @Nullable
    protected LabelWidget nameWidget;
    @Getter
    protected final List<Consumer<Configurator>> listeners = new ArrayList<>();

    public Configurator(String name) {
        super(0, 0, 200, 15);
        this.name = name;
        setClientSideWidget();
        if (!name.isEmpty()) {
            this.addWidget(nameWidget = new LabelWidget(3, 3, name));
            leftWidth = Minecraft.getInstance().font.width(LocalizationUtils.format(name)) + 6;
        } else {
            leftWidth = 3;
        }
    }

    public Configurator() {
        this("");
    }

    public void setConfiguratorContainer(@Nullable IConfiguratorContainer configuratorContainer) {
        this.configuratorContainer = configuratorContainer;
    }

    public void computeLayout() {
        if (configuratorContainer != null) {
            configuratorContainer.computeLayout();
        }
    }

    public void setTips(String... tips) {
        this.tips = tips;
        rightWidth = tips.length > 0 ? 13 : 0;
    }

    public boolean isInit() {
        return width > -1;
    }

    public void computeHeight() {

    }

    public void init(int width) {
        this.width = width;
        setSize(Size.of(width, getSize().height));
        if (tips.length > 0) {
            this.addWidget(new ImageWidget(width - 12, 2, 9, 9, Icons.HELP).setHoverTooltips(tips));
        }
    }

    /**
     * Add a listener to this configurator
     */
    public void addListener(Consumer<Configurator> listener) {
        listeners.add(listener);
    }

    public final void notifyChanges() {
        notifyChanges(this);
    }

    public void notifyChanges(Configurator source) {
        listeners.forEach(listener -> listener.accept(source));
        if (parent instanceof Configurator configurator) {
            configurator.notifyChanges(source);
        }
    }

}
