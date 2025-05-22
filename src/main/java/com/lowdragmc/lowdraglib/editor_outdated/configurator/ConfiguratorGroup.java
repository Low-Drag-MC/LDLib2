package com.lowdragmc.lowdraglib.editor_outdated.configurator;

import com.lowdragmc.lowdraglib.editor_outdated.ColorPattern;
import com.lowdragmc.lowdraglib.editor_outdated.IConfiguratorContainer;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Size;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote ConfiguratorGroup
 */
public class ConfiguratorGroup extends Configurator {
    @Setter
    protected boolean canCollapse = true;
    @Getter
    protected boolean isCollapse;
    @Getter
    protected List<Configurator> configurators = new ArrayList<>();

    public ConfiguratorGroup(String name) {
        this(name, true);
    }

    public ConfiguratorGroup(String name, boolean isCollapse) {
        super(name);
        this.isCollapse = isCollapse;
    }

    @Override
    public void setConfiguratorContainer(IConfiguratorContainer configuratorContainer) {
        super.setConfiguratorContainer(configuratorContainer);
        for (Configurator configurator : configurators) {
            configurator.setConfiguratorContainer(configuratorContainer);
        }
    }

    protected void clickName(ClickData clickData) {
        if (!canCollapse) return;
        setCollapse(!isCollapse);
    }

    public void setCollapse(boolean collapse) {
        isCollapse = collapse;
        for (Configurator configurator : configurators) {
            configurator.setActive(!isCollapse);
            configurator.setVisible(!isCollapse);
        }
        computeLayout();
    }

    public void addConfigurator(int index, Configurator configurator) {
        configurator.setConfiguratorContainer(configuratorContainer);
        configurator.setActive(!isCollapse);
        configurator.setVisible(!isCollapse);
        this.configurators.add(index, configurator);
        addWidget(index, configurator);
        if (isInit()) {
            configurator.init(Math.max(0, width - 5));
            computeLayout();
        }
    }

    public void removeConfigurator(Configurator configurator) {
        if (configurators.remove(configurator)) {
            removeWidget(configurator);
        }
    }

    public void removeAllConfigurators() {
        for (Configurator configurator : configurators) {
            removeWidget(configurator);
        }
        configurators.clear();
    }

    public void addConfigurators(Configurator... configurators) {
        for (Configurator configurator : configurators) {
            configurator.setConfiguratorContainer(configuratorContainer);
            configurator.setActive(!isCollapse);
            configurator.setVisible(!isCollapse);
            this.configurators.add(configurator);
            addWidget(configurator);
        }
        if (isInit()) {
            for (Configurator configurator : configurators) {
                configurator.init(Math.max(0, width - 5));
            }
            computeLayout();
        }
    }

    @Override
    public void computeHeight() {
        int height = 15;
        if (!isCollapse) {
            for (Configurator configurator : configurators) {
                configurator.computeHeight();
                configurator.setSelfPosition(Position.of(5, height));
                height += configurator.getSize().height;
            }
        }
        setSize(Size.of(getSize().width, height));
    }

    @Override
    public void init(int width) {
        super.init(width);
        this.addWidget(new ButtonWidget(0, 0, leftWidth + 9, 15, IGuiTexture.EMPTY, this::clickName));
        for (Configurator configurator : configurators) {
            configurator.init(Math.max(0, width - 5));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull @Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        Position pos = getPosition();
        Size size = getSize();
        if (isCollapse) {
            Icons.RIGHT.setColor(-1).draw(graphics, mouseX, mouseY, pos.x + leftWidth, pos.y + 3, 9, 9);
        } else {
            Icons.DOWN.setColor(-1).draw(graphics, mouseX, mouseY, pos.x + leftWidth, pos.y + 3, 9, 9);
            if (configurators.size() > 0) {
                DrawerHelper.drawSolidRect(graphics, pos.x + 2, pos.y + 17, 1, size.height - 19, ColorPattern.T_WHITE.color);
            }
        }
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }
}
