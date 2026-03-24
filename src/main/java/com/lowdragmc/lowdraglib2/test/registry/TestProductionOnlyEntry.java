package com.lowdragmc.lowdraglib2.test.registry;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;

@LDLRegister(name = "test_production_only", registry = "ldlib2:test_env_registry", environment = RegistrationEnvironment.PRODUCTION_ONLY)
public class TestProductionOnlyEntry implements ITestRegistryEntry {
}
