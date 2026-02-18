package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import dev.vfyjxf.taffy.style.TaffyDirection;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

public class PortConnectorWithIconElement extends PortConnectorElement {
    // runtime
    @Getter
    protected UIElement portIcon;
    @Nullable
    protected TypeHandle lastTypeHandle;

    public PortConnectorWithIconElement(PortModel portModel) {
        super(portModel);
    }

    @Override
    public Stream<? extends UIElement> getWireDragParts() {
        return Stream.concat(super.getWireDragParts(), Stream.of(portIcon));
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        portIcon = new UIElement();
        portIcon.getLayout().aspectRatio(1).width(9);
        addChildAt(portIcon, 1);
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        if (!Objects.equals(lastTypeHandle, portModel.getDataTypeHandle())) {
            this.lastTypeHandle = portModel.getDataTypeHandle();
            var icon = portModel.getDataTypeHandle().getIcon();
            portIcon.getStyle().background(icon);
            portIcon.setDisplay(icon != IGuiTexture.EMPTY);
        }
        if (visitor.hasHint(ChangeHint.DATA)) {
            getLayout().direction(portModel.getDirection() == PortDirection.INPUT ? TaffyDirection.LTR :
                    portModel.getDirection() == PortDirection.OUTPUT ? TaffyDirection.RTL : TaffyDirection.INHERIT);
        }
    }
}
