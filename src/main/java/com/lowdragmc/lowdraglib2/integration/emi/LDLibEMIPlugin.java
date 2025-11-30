package com.lowdragmc.lowdraglib2.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class LDLibEMIPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addGenericExclusionArea(ModularUIEMIHandlers.EXCLUSION_AREA);
        registry.addGenericStackProvider(ModularUIEMIHandlers.STACK_PROVIDER);
    }
}
