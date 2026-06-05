package com.lowdragmc.lowdraglib2.nodegraphtookit.api.port;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;

/**
 * Specifies the visual style of the connector used to represent a port in the UI.
 *
 * <p>Use this enum to define how the connector of a port appears visually.
 * The connector indicates the type or role of the port and can help users understand connection semantics.</p>
 */
public record PortConnectorUI(IGuiTexture unconnectedIcon, IGuiTexture connectedIcon) {
    public static final PortConnectorUI DEFAULT = new PortConnectorUI(Icons.RADIOBOX_BLANK, Icons.RADIOBOX_MARKED);
    public static final PortConnectorUI FLOW = new PortConnectorUI(Icons.PLAY_EMPTY, Icons.PLAY_FILL);

    public IGuiTexture getIcon(boolean isConnected) {
        return isConnected ? connectedIcon : unconnectedIcon;
    }

    public IGuiTexture getIcon(PortModel portModel) {
        return getIcon(portModel.isConnected());
    }
}
