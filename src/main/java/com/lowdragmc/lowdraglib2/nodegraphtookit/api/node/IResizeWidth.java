package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

/**
 * Marker interface for graph element models that expose a user-tunable minimum width.
 *
 * <p>The element's actual width remains driven by its children (auto-layout); {@code minWidth}
 * acts as a floor — when the computed width would otherwise fall below this value, the layout
 * engine grows the element. Default {@code 0} disables the floor.</p>
 *
 * <p>Capability gate: {@link com.lowdragmc.lowdraglib2.nodegraphtookit.model.Capabilities#RESIZABLE}.
 * If the capability is off, the inspector hides the field and the layout pipeline skips
 * applying {@code minWidth}.</p>
 */
public interface IResizeWidth {
    float getMinWidth();

    void setMinWidth(float value);
}
