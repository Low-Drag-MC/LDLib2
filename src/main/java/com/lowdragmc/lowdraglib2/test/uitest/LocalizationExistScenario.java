package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;

/**
 * {@link LocalizationUtils#exist} and {@link LocalizationUtils#format} on the client. 26.2 removed {@code I18n.exists},
 * which {@code exist} used to call by reflection, so it answered false for every key and an enum's
 * {@code <enum class>.<CONSTANT>} translations never applied. Both now ask {@code Language} directly.
 */
@LDLRegisterClient(name = "localization_exist", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class LocalizationExistScenario implements UIScenario {

    @Override
    public void define(ScenarioBuilder s) {
        s.checkEquals("a key the language file has exists", true,
                        ctx -> LocalizationUtils.exist("ldlib.gui.tips.confirm"))
                .checkEquals("a key no language file has does not", false,
                        ctx -> LocalizationUtils.exist("ldlib.uitest.no_such_key"))
                .checkEquals("a key is formatted with its substitutions", "\u00a79Amount: 1234",
                        ctx -> LocalizationUtils.format("ldlib.item.amount", 1234))
                .checkEquals("a key no language file has formats as itself", "ldlib.uitest.no_such_key",
                        ctx -> LocalizationUtils.format("ldlib.uitest.no_such_key"))
                // not a %d given a string: Language rewrites a file's %d to %s on load, so that one formats fine
                .checkEquals("a translation String.format can't parse is reported, not thrown", "Format error: ldlib.uitest.%q",
                        ctx -> LocalizationUtils.format("ldlib.uitest.%q"));
    }
}
