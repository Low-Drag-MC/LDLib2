package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

/** {@link SceneTranslucentOrderFboScenario} with improved transparency: the glass and the chest go through the scene's OIT phases. */
@LDLRegisterClient(name = "scene_translucent_order_fbo_oit", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class SceneTranslucentOrderFboOitScenario extends SceneTranslucentOrderFboScenario {

    @Override
    protected boolean improvedTransparency() {
        return true;
    }
}
