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
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
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
        getLayout().flexGrow(1).marginAll(2);

        icon.getLayout().aspectRatio(1).height(9);
        label.getTextStyle().adaptiveWidth(true);
        collapseToggle.getLayout().height(9);
        collapseToggle.noText().setOnToggleChanged(this::setCollapsed);
        collapseToggle.setOn(isCollapsed, false).toggleStyle(toggleStyle -> toggleStyle
                .baseTexture(IGuiTexture.EMPTY)
                .hoverTexture(IGuiTexture.EMPTY)
                .markTexture(Icons.RIGHT_ARROW_NO_BAR_S_WHITE)
                .unmarkTexture(Icons.DOWN_ARROW_NO_BAR_S_WHITE));
        titleBar.getLayout().flexDirection(FlexDirection.ROW)
                .paddingAll(4)
                .gapAll(2)
                .alignItems(AlignItems.CENTER);
        titleBar.getStyle().background(Sprites.RECT_LIGHT);
        titleBar.addChildren(icon, label, collapseToggle);

        typeLabel.setText("graph.type");
        typeLabel.getTextStyle().adaptiveWidth(true);

        typeSearchComponent.getLayout().flexGrow(1).minWidth(55);
        typeSearchComponent.setSearchUI(this);
        typeSearchComponent.setCandidateUIProvider(UIElementProvider.text(value -> value == null ?
                Component.translatable("text_field.empty").withColor(ColorPattern.LIGHT_GRAY.color) :
                Component.translatable(value.getFriendlyName())));

        valueFieldInspector.setFieldName(Component.translatable("graph.default_value"));

        contentContainer.setDisplay(false);
        contentContainer.getLayout().paddingAll(3).gapAll(2);
        contentContainer.getStyle().background(Sprites.RECT_SOLID);
        contentContainer.addChildren(new UIElement().layout(layout -> layout
                        .alignItems(AlignItems.CENTER).flexDirection(FlexDirection.ROW).gapAll(2))
                .addChildren(typeLabel, typeSearchComponent), valueFieldInspector);

    }

    @Override
    protected void buildUI() {
        super.buildUI();
        addChildren(
                new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW))
                        .addChild(titleBar),
                contentContainer
        );
        internalSetup();
    }

    public void setCollapsed(boolean isCollapsed) {
        if (this.isCollapsed == isCollapsed) return;
        this.isCollapsed = isCollapsed;
        contentContainer.setDisplay(!isCollapsed);
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
        var types = List.copyOf(graphModel.getSupportTypes());
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
                    () -> getModel().getModifiers() == ModifierFlags.NONE ? VariableType.NONE : VariableType.SUB_GRAPH_PORT,
                    type -> {
                        if (graphView == null) return;
                        graphView.dispatchCommand(new VariableDeclarationCommands.ChangeVariableModifiersCommand(
                                List.of(getModel()),
                                type == VariableType.NONE ? ModifierFlags.NONE : ModifierFlags.READ
                        ));
                    },
                    VariableType.NONE,
                    true,
                    Arrays.stream(VariableType.values()).toList(),
                    VariableType::getSerializedName,
                    (type, configuratorGroup) -> {
                        if (type == VariableType.SUB_GRAPH_PORT) {
                            configuratorGroup.addConfigurator(EnumAccessor.create(
                                    "graph.flow_direction",
                                    Arrays.stream(SubGraphPort.values()).toList(),
                                    () -> getModel().getModifiers() == ModifierFlags.READ ? SubGraphPort.INPUT : SubGraphPort.OUTPUT,
                                    io -> {
                                        if (graphView == null) return;
                                        graphView.dispatchCommand(new VariableDeclarationCommands.ChangeVariableModifiersCommand(
                                                List.of(getModel()),
                                                io == SubGraphPort.INPUT ? ModifierFlags.READ : ModifierFlags.WRITE
                                        ));
                                    },
                                    SubGraphPort.INPUT,
                                    true,
                                    SubGraphPort::getIcon
                            ));
                        }
                    }
            );
            group.addConfigurators(rename, defaultValue, subGraphConfigurator);
        });
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
        NONE,
        SUB_GRAPH_PORT;

        @Override
        public String getSerializedName() {
            return this == NONE ? "none" : "sub graph port";
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
