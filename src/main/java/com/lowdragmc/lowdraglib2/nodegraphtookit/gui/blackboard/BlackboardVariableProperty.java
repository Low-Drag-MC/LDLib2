package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.blackboard;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.EnumAccessor;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorSelectorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.FieldValueInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.VariableDeclarationCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlackboardVariableProperty extends BlackboardElement implements SearchComponent.ISearchUI<TypeHandle> {
    public final UIElement titleBar = new UIElement();
    public final UIElement icon = new UIElement();
    public final Label label = new Label();
    public final Toggle collapseToggle = new Toggle();

    public final UIElement contentContainer = new UIElement();
    public final Label typeLabel = new Label();
    public final SearchComponent<TypeHandle> typeSearchComponent = new SearchComponent<>();
    public final FieldValueInspector valueFieldInspector = new FieldValueInspector();

    //runtime
    @Getter
    private boolean isCollapsed = true;
    @Nullable
    private TypeHandle lastTypeHandle;
    @Nullable
    private IConfigurable variableConfigurable;

    public BlackboardVariableProperty(VariableDeclarationModelBase variableModel) {
        setModel(variableModel);
        addClass("__blackboard-var-prop__");
        Style.defaultPipeline(getLayout(), l -> l.flexGrow(1).marginAll(2));

        icon.addClass("__blackboard-var-prop_icon__");
        Style.defaultPipeline(icon.getLayout(), l -> l.aspectRatio(1).height(9));
        label.addClass("__blackboard-var-prop_label__");
        Style.defaultPipeline(label.getTextStyle(), s -> s.adaptiveWidth(true));
        collapseToggle.addClass("__blackboard-var-prop_collapse-toggle__");
        collapseToggle.getLayout().height(9);
        collapseToggle.noText().setOnToggleChanged(this::setCollapsed);
        collapseToggle.setOn(isCollapsed, false).toggleStyle(toggleStyle -> toggleStyle
                .baseTexture(IGuiTexture.EMPTY)
                .hoverTexture(IGuiTexture.EMPTY)
                .markTexture(Icons.RIGHT_ARROW_NO_BAR_S_WHITE)
                .unmarkTexture(Icons.DOWN_ARROW_NO_BAR_S_WHITE));
        titleBar.addClass("__blackboard-var-prop_title-bar__");
        Style.defaultPipeline(titleBar.getLayout(), l -> l.flexDirection(FlexDirection.ROW)
                .paddingAll(4)
                .gapAll(2)
                .alignItems(AlignItems.CENTER));
        Style.defaultPipeline(titleBar.getStyle(), s -> s.background(Sprites.RECT_LIGHT));
        titleBar.addChildren(icon, label, collapseToggle);

        typeLabel.addClass("__blackboard-var-prop_type-label__");
        typeLabel.setText("graph.type");
        Style.defaultPipeline(typeLabel.getTextStyle(), s -> s.adaptiveWidth(true));

        typeSearchComponent.addClass("__blackboard-var-prop_type-search__");
        Style.defaultPipeline(typeSearchComponent.getLayout(), l -> l.flexGrow(1).minWidth(55));
        typeSearchComponent.setSearchUI(this);
        typeSearchComponent.setCandidateUIProvider(UIElementProvider.text(value -> value == null ?
                Component.translatable("text_field.empty").withColor(ColorPattern.LIGHT_GRAY.color) :
                Component.translatable(value.getFriendlyName())));

        valueFieldInspector.addClass("__blackboard-var-prop_value-inspector__");
        valueFieldInspector.setFieldName(Component.translatable("graph.default_value"));

        contentContainer.addClass("__blackboard-var-prop_content__");
        // Initial collapsed state hides the content panel — pin via IMPORTANT because it is state-driven.
        Style.importantPipeline(contentContainer.getLayout(), l -> l.display(TaffyDisplay.NONE));
        Style.defaultPipeline(contentContainer.getLayout(), l -> l.paddingAll(3).gapAll(2));
        Style.defaultPipeline(contentContainer.getStyle(), s -> s.background(Sprites.RECT_SOLID));

        var typeRow = new UIElement().addClass("__blackboard-var-prop_type-row__");
        Style.defaultPipeline(typeRow.getLayout(), l -> l.alignItems(AlignItems.CENTER).flexDirection(FlexDirection.ROW).gapAll(2));
        typeRow.addChildren(typeLabel, typeSearchComponent);
        contentContainer.addChildren(typeRow, valueFieldInspector);
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        var titleRow = new UIElement().addClass("__blackboard-var-prop_title-row__");
        Style.defaultPipeline(titleRow.getLayout(), l -> l.flexDirection(FlexDirection.ROW));
        titleRow.addChild(titleBar);
        addChildren(titleRow, contentContainer);
    }

    public void setCollapsed(boolean isCollapsed) {
        if (this.isCollapsed == isCollapsed) return;
        this.isCollapsed = isCollapsed;
        // Collapse state drives content panel visibility.
        Style.importantPipeline(contentContainer.getLayout(), l -> l.display(isCollapsed ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
    }

    @Override
    public VariableDeclarationModelBase getModel() {
        return (VariableDeclarationModelBase) super.getModel();
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);

        if (visitor.hasHint(ChangeHint.DATA)) {
            label.setText(getModel().getName());
        }

        TypeHandle typeHandle = null;
        if (getModel().getInitializationModel() != null) {
            typeHandle = getModel().getDataTypeHandle();
        }
        if (!Objects.equals(lastTypeHandle, typeHandle)) {
            // there is a change here, update ui
            if (graphView != null) valueFieldInspector.setHistoryStack(graphView.getHistoryStack());
            valueFieldInspector.loadValueField(getModel());
            lastTypeHandle = typeHandle;
            typeSearchComponent.setValue(lastTypeHandle == null ? TypeHandles.UNKNOWN : lastTypeHandle, false);
        }
    }

    @Override
    public String resultText(TypeHandle value) {
        return value.getFriendlyName();
    }

    @Override
    public void onResultSelected(@Nullable TypeHandle value) {
        if (value != null) {
            getModel().setDataTypeHandle(value);
            valueFieldInspector.loadValueField(getModel());
            // update inspector if necessary
            if (graphView != null && graphView.inspector.getInspectedConfigurable() == getVariableConfigurable()) {
                onSelectionInspect(graphView.inspector);
            }
        }
    }

    @Override
    public void search(String word, IResultHandler<TypeHandle> searchHandler) {
        var graphModel = getModel().getGraphModel();
        if (graphModel == null) return;
        var types = List.copyOf(graphModel.getVariableSupportTypes());
        var lowerWord = word.toLowerCase();
        for (var type : types) {
            if (Thread.interrupted()) return;
            if (type.getIdentification().toLowerCase().contains(lowerWord)
                    || LocalizationUtils.format(type.getFriendlyName()).toLowerCase().contains(lowerWord)) {
                searchHandler.accept(type);
            }
        }
    }

    protected IConfigurable createVariableConfigurable() {
        return IConfigurable.create(group -> {
            var rename = new StringConfigurator("graph.variable_name", () -> getModel().getName(),
                    name -> getModel().setName(name),  getModel().getName(), true);
            var defaultValue = new ConfiguratorGroup("graph.default_value").setCollapse(false);
            getModel().buildConfigurator(defaultValue);
            var subGraphConfigurator = new ConfiguratorSelectorConfigurator<>(
                    "graph.variable_type",
                    () -> getModel().getModifiers() == ModifierFlags.NONE ? VariableType.INTERNAL : VariableType.EXTERNAL,
                    type -> {
                        if (graphView == null) return;
                        graphView.dispatchCommand(new VariableDeclarationCommands.ChangeVariableModifiersCommand(
                                List.of(getModel()),
                                type == VariableType.INTERNAL ? ModifierFlags.NONE : getDefaultSubgraphPortModifier()
                        ));
                    },
                    VariableType.INTERNAL,
                    true,
                    getVariableTypeCandidates(),
                    VariableType::getSerializedName,
                    (type, configuratorGroup) -> {
                        if (type == VariableType.EXTERNAL) {
                            var portCandidates = getSubGraphPortCandidates();
                            configuratorGroup.addConfigurator(EnumAccessor.create(
                                    "graph.flow_direction",
                                    portCandidates,
                                    () -> getSelectedSubGraphPort(portCandidates),
                                    io -> {
                                        if (graphView == null) return;
                                        graphView.dispatchCommand(new VariableDeclarationCommands.ChangeVariableModifiersCommand(
                                                List.of(getModel()),
                                                toModifier(io)
                                        ));
                                    },
                                    portCandidates.isEmpty() ? SubGraphPort.INPUT : portCandidates.get(0),
                                    true,
                                    SubGraphPort::getIcon
                            ));
                        }
                    }
            );
            group.addConfigurators(rename, defaultValue, subGraphConfigurator);
        });
    }

    private List<VariableType> getVariableTypeCandidates() {
        if (getSubGraphPortCandidates().isEmpty()) {
            return List.of(VariableType.INTERNAL);
        }
        return Arrays.stream(VariableType.values()).toList();
    }

    private List<SubGraphPort> getSubGraphPortCandidates() {
        var graphModel = getModel().getGraphModel();
        if (graphModel == null) return List.of();
        var supportedKinds = graphModel.getSupportedSubgraphVariableKinds();
        var candidates = new ArrayList<SubGraphPort>();
        if (supportedKinds.contains(VariableKind.INPUT)) candidates.add(SubGraphPort.INPUT);
        if (supportedKinds.contains(VariableKind.OUTPUT)) candidates.add(SubGraphPort.OUTPUT);
        return candidates;
    }

    private ModifierFlags getDefaultSubgraphPortModifier() {
        var candidates = getSubGraphPortCandidates();
        if (candidates.isEmpty()) return ModifierFlags.NONE;
        return toModifier(candidates.get(0));
    }

    private SubGraphPort getSelectedSubGraphPort(List<SubGraphPort> candidates) {
        var modifiers = getModel().getModifiers();
        var selected = modifiers.hasFlag(ModifierFlags.WRITE) && !modifiers.hasFlag(ModifierFlags.READ)
                ? SubGraphPort.OUTPUT : SubGraphPort.INPUT;
        if (candidates.contains(selected)) return selected;
        return candidates.isEmpty() ? SubGraphPort.INPUT : candidates.get(0);
    }

    private ModifierFlags toModifier(SubGraphPort io) {
        return io == SubGraphPort.INPUT ? ModifierFlags.READ : ModifierFlags.WRITE;
    }

    public IConfigurable getVariableConfigurable() {
        if (variableConfigurable == null)
            variableConfigurable = createVariableConfigurable();
        return variableConfigurable;
    }

    @Override
    protected void onSelectionInspect(GraphInspector inspector) {
        inspector.inspect(getVariableConfigurable());
    }

    private enum VariableType implements StringRepresentable {
        INTERNAL,
        EXTERNAL;

        @Override
        public String getSerializedName() {
            return this == INTERNAL ? "graph.variable_type.internal" : "graph.variable_type.external";
        }
    }

    private enum SubGraphPort implements StringRepresentable {
        INPUT(new TextTexture("I")),
        OUTPUT(new TextTexture("O"));

        @Getter
        public final IGuiTexture icon;

        SubGraphPort(IGuiTexture icon) {
            this.icon = icon;
        }

        @Override
        public String getSerializedName() {
            return this == INPUT ? "input" : "output";
        }

    }
}
