package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.DeclarationModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IPlaceHolder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.PlaceholderModelHelper;

public class PortalDeclarationPlaceholder extends DeclarationModel implements IPlaceHolder {
    public PortalDeclarationPlaceholder() {
        PlaceholderModelHelper.setPlaceholderCapabilities(this);
    }
}
