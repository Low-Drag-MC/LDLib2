package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.collect.Sets;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinPath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.UIResource;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleRule;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
//import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.w3c.dom.Element;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
//@RemapPrefixForJS("kjs$")
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "template", registry = "ldlib2:ui_element")
public class UITemplateElement extends UIElement {
    private static final ThreadLocal<Set<UITemplate>> LOADDINGS = ThreadLocal.withInitial(Sets::newHashSet);
    @Nullable
    @Getter
    private IResourcePath path;
    // runtime
    private boolean isTemplateLoading = false;
    @Nullable
    @Getter
    private UITemplate template;
    private List<StyleRule> styleRules = new ArrayList<>();

    public UITemplateElement() {
        this(null);
    }

    public UITemplateElement(@Nullable IResourcePath templatePath) {
        setTemplate(templatePath);
    }

    public UITemplateElement setTemplate(@Nullable IResourcePath templatePath) {
        if (path != templatePath) {
            path = templatePath;
            this.template = templatePath == null ? null : UIResource.INSTANCE.getResourceInstance().getResource(templatePath);
            loadTemplate();
        }
        return this;
    }

    protected void loadTemplate() {
        clearAllChildren();
        getStyleBag().removeCandidates(slot -> slot.origin() == StyleOrigin.DEFAULT);
        this.removeStyleRules(styleRules);
        styleRules.clear();
        if (template != null) {
            if (LOADDINGS.get().contains(template)) {
                LDLib2.LOGGER.error("Circular template loading detected: {}", path);
                return;
            }
            isTemplateLoading = true;
            LOADDINGS.get().add(template);
            template.initUI(this);
            for (var stylesheet : template.getAllStylesheets()) {
                styleRules.addAll(stylesheet.calculateValues(this));
            }
            // apply rules
            addStyleRules(styleRules);
            Stack<UIElement> elements = new Stack<>();
            this.getChildren().forEach(elements::push);
            while (!elements.isEmpty()) {
                var peek = elements.pop();
                if (peek.getId().equals("1")) {
                    System.out.println();
                }
                peek.getChildren().forEach(elements::push);
                for (var stylesheet : template.getAllStylesheets()) {
                    peek.addStyleRules(stylesheet.calculateValues(peek));
                }
            }
            internalSetup();
            LOADDINGS.get().remove(template);
            isTemplateLoading = false;
        }
    }

    /// Editor + Xml
    @Override
    public void serialize(ValueOutput output) {
        if (path != null) {
            super.serialize(output);
            output.store("path", IResourcePath.CODEC, path);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        if (isTemplateLoading) {
            super.deserialize(input);
        } else {
            input.read("path", IResourcePath.CODEC).ifPresent(path -> {
                setTemplate(path);
                isTemplateLoading = true;
                super.deserialize(input);
                isTemplateLoading = false;
            });
        }
    }

    @Override
    public void beforeDeserialize() {
        super.beforeDeserialize();
        if (!isTemplateLoading) {
            setTemplate(null);
        }
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        father.addConfigurator(
                new ConfiguratorSelectorConfigurator<>("ui_template", () -> path, this::setTemplate,
                        new BuiltinPath(""), true,
                        UIResource.INSTANCE.getResourceInstance().listAllResources().stream().map(Map.Entry::getKey).toList(),
                        IResourcePath::getResourceName,
                        (path, group) -> super.buildConfigurator(group))
        );
    }

    @Override
    public void loadXml(Element element) {
        // template
        if (element.hasAttribute("path")) {
            var path = IResourcePath.parse(element.getAttribute("path"));
            if (path != null) {
                setTemplate(path);
            }
        }
        super.loadXml(element);
    }
}
