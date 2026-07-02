package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.UIResource;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.LDLibFonts;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.TagBuilder;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@LDLRegisterClient(name="component_examples", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestComponentExamples implements IScreenTest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var ui = Optional.ofNullable(UIResource.INSTANCE.getResourceInstance()
                .getResource(new FilePath(LDLib2.id("resources/examples/example_layout.ui.nbt"))))
                .map(UITemplate::createUI)
                .orElseGet(UI::empty);
        toggleStylesheets(ui, "#gdp-toggle", StylesheetManager.GDP);
        toggleStylesheets(ui, "#ore-toggle", StylesheetManager.ORE);
        toggleStylesheets(ui, "#mc-toggle", StylesheetManager.MC);
        toggleStylesheets(ui, "#modern-toggle", StylesheetManager.MODERN);
        var scrollerView = ui.select("#example-list", ScrollerView.class).findFirst().orElseThrow();
        var rightContainer = ui.select("#right_container", UIElement.class).findFirst().orElseThrow();
        rightContainer.layout(layout -> layout.paddingAll(6));
        var toggleGroup = new Toggle.ToggleGroup();

        BiConsumer<String, Supplier<UIElement>> addExample = (name, exampleSupplier) -> {
            var toggle = new Toggle();
            toggle.setToggleGroup(toggleGroup);
            toggle.noText();
            toggle.toggleStyle(style -> {
                style.markTexture(ColorPattern.WHITE.borderTexture(1));
                style.unmarkTexture(IGuiTexture.EMPTY);
            });
            toggle.toggleButton.getLayout().aspectRatioAuto().widthPercent(100);
            toggle.markIcon.addChild(new Label().setText(name)
                    .textStyle(style -> style.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL))
                    .layout(layout -> layout.heightPercent(100).marginHorizontal(5))
                    .setOverflowVisible(false));
            scrollerView.addScrollViewChild(toggle.setOnToggleChanged(isON -> {
                if (isON) {
                    rightContainer.clearAllChildren();
                    rightContainer.addChild(exampleSupplier.get());
                }
            }));
        };

        addExample.accept("button", this::buttonExample);
        addExample.accept("label", this::labelExample);
        addExample.accept("text-area", this::textAreaExample);
        addExample.accept("progress-bar", this::progressBarExample);
        addExample.accept("item / fluid slots", this::slotsExample);
        addExample.accept("toggle", this::toggleExample);
        addExample.accept("switch", this::switchExample);
        addExample.accept("selector", this::selectorExample);
        addExample.accept("scroller", this::scrollerExample);
        addExample.accept("text-field", this::textFieldExample);
        addExample.accept("tag-field", this::tagFieldExample);
        addExample.accept("structured-tag-editor", this::structuredTagEditorExample);
        addExample.accept("tree-list", this::treeListExample);
        addExample.accept("code-editor", this::codeEditorExample);
        addExample.accept("scene", this::sceneExample);
        addExample.accept("search-component", this::searchComponentExample);
        addExample.accept("color-selector", this::colorSelectorExample);
        addExample.accept("tab-view", this::tabViewExample);
        addExample.accept("scroller-view", this::scrollerViewExample);
        addExample.accept("graph-view", this::graphViewExample);
        addExample.accept("split-view", this::splitViewExample);
        return new ModularUI(ui);
    }

    private void toggleStylesheets(UI ui, String selector, ResourceLocation stylesheet) {
        ui.select(selector, Toggle.class).findFirst().ifPresent(toggle -> toggle.setOnToggleChanged(isOn -> {
            // switch to the selected stylesheet
            var mui = toggle.getModularUI();
            if (isOn && mui != null) {
                mui.getStyleEngine().clearAllStylesheets();
                mui.getStyleEngine().addStylesheet(StylesheetManager.INSTANCE.getStylesheetSafe(stylesheet));
            }
        }));
    }

    private UIElement buttonExample() {
        return new UIElement().layout(layout -> layout.gapAll(2)).addChildren(
                new Button(),
                new Button().setText("disabled").disabled(),
                new Button().setText("pre icon").addPreIcon(SpriteTexture.of("ldlib2:textures/gui/icon.png")),
                new Button().setText("post icon").addPostIcon(SpriteTexture.of("ldlib2:textures/gui/icon.png"))
        );
    }

    private UIElement labelExample() {
        return new UIElement().layout(layout -> layout.gapAll(2)).addChildren(
                new Label().setText("1. Hello World!"),
                new Label().setText("2. horizontal center")
                        .textStyle(textStyle -> textStyle.textAlignHorizontal(Horizontal.CENTER)),
                new Label().setText("3. This is a super long text!  This is a super long text! This is a super long text!"),
                new Label().setText("4. with overflow invisible! This is a super long text! This is a super long text!").setOverflowVisible(false),
                new Label().setText("5. with wrap! This is a super long text! This is a super long text!")
                        .textStyle(textStyle -> textStyle.textWrap(TextWrap.WRAP).adaptiveHeight(true)),
                new Label().setText("6. with roll! This is a super long text! This is a super long text!")
                        .textStyle(textStyle -> textStyle.textWrap(TextWrap.ROLL)).setOverflowVisible(false),
                new Label().setText("7. font, color and size!")
                        .textStyle(textStyle -> textStyle
                                .font(LDLibFonts.JETBRAINS_MONO_BOLD).fontSize(18).textColor(0xff3fff32)
                                .textWrap(TextWrap.WRAP)
                                .adaptiveHeight())
        );
    }

    private UIElement textAreaExample() {
        return new UIElement().layout(layout -> layout.gapAll(2)).addChildren(
                new TextArea().setValue("""
                        LDLib2 provides powerful UI library based on the yoga layout engine. If you are struggling with GUI development, LDLib2 is definitely one of your best choice. Compared with the LDLib, LDLib2 has undergone a complete refactoring:
                        
                        1. modern UI layout system
                        2. modern UI event system
                        3. data binding system (support data synchronization and rpc event between server <-> remote)
                        4. stylesheet system
                        5. massive plug-and-play components
                        6. xml support + in-game UI visual editor
                        xei / kjs supports
                        7. completed document and usage examples
                        
                        """.split("\n")),
                new TextArea().setLines(List.of("disabled")).disabled(),
                new TextArea().setValue("""
                        with styles
                        """.split("\n"))
                        .textAreaStyle(style -> style.fontSize(14).textColor(ColorPattern.LIME.color))
        );
    }

    private UIElement progressBarExample() {
        return new UIElement().layout(layout -> layout.gapAll(2)).addChildren(
                new ProgressBar().setValue(0.5f),
                new ProgressBar().setValue(0.5f).label(label -> label.setDisplay(false)),
                new ProgressBar().label(label -> label.setText("Progress: 30%")).setValue(0.3f)
        );
    }

    private UIElement slotsExample() {
        return new UIElement().layout(layout -> layout.gapAll(2)).addChildren(
                new ItemSlot(),
                new ItemSlot().setItem(Items.APPLE.getDefaultInstance()),
                new FluidSlot(),
                new FluidSlot().setFluid(new FluidStack(Fluids.LAVA, 1000))
        );
    }

    private UIElement toggleExample() {
        return new UIElement().layout(layout -> layout.gapAll(2)).addChildren(
                new Toggle().setText("my toggle"),
                new Toggle().setText("disabled").disabled(),
                new ToggleGroupElement().addChildren(
                        new Label().setText("Toggle Group"),
                        new Toggle().setText("toggle 1"),
                        new Toggle().setText("toggle 2"),
                        new Toggle().setText("toggle 3")
                ).addClass("panel_bg")
        );
    }

    private UIElement switchExample() {
        return new UIElement().layout(layout -> layout.gapAll(2)).addChildren(
                new Switch(),
                new Switch().disabled()
        );
    }

    private UIElement selectorExample() {
        return new UIElement().layout(layout -> layout.gapAll(2)).addChildren(
                new Selector<Direction>().setCandidates(Arrays.stream(Direction.values()).toList()),
                new Selector<Item>()
                        .setSelected(Items.APPLE)
                        .setCandidates(List.of(Items.APPLE, Items.STONE, Items.CHEST))
                        .setCandidateUIProvider(UIElementProvider.iconText(ItemStackTexture::new, Item::getDescription))
        );
    }

    private UIElement scrollerExample() {
        return new UIElement().layout(layout -> layout.gapAll(2).heightPercent(100)).addChildren(
                new Scroller.Horizontal().setValue(0.3f),
                new Scroller.Vertical().setValue(0.6f).layout(layout -> layout.flex(1))
        );
    }

    private UIElement textFieldExample() {
        return new UIElement().layout(layout -> layout.gapAll(2)).addChildren(
                new TextField(),
                new TextField().setText("disabled").disabled(),
                new TextField().setText("editable").textFieldStyle(style -> style.fontSize(13)).layout(l -> l.height(18))
        );
    }

    private UIElement tagFieldExample() {
        return new UIElement().layout(layout -> layout.gapAll(2)).addChildren(
                new Label().setText("any tag"),
                new TagField(),
                new Label().setText("compound tag only"),
                new TagField().setCompoundTagOnly().setValue(TagBuilder.compound().add("a", 123)
                        .add("b", "test").build()),
                new Label().setText("list tag only"),
                new TagField().setListOnly().setValue(new ListTag())
        );
    }

    private UIElement structuredTagEditorExample() {
        var root = new CompoundTag();
        root.putString("name", "LDLib2");
        root.putInt("count", 3);
        root.put("bytes", new ByteArrayTag(new byte[] {1, 2, 3}));
        root.put("ints", new IntArrayTag(new int[] {10, 20, 30}));
        var list = new ListTag();
        list.add(StringTag.valueOf("first"));
        list.add(StringTag.valueOf("second"));
        root.put("list", list);
        return new StructuredTagEditor()
                .setValue(root, false)
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                });
    }

    private UIElement treeListExample() {
        return new UIElement().layout(layout -> layout.gapAll(2)).addChildren(
                new TreeList<>(TreeBuilder.start("root")
                        .leaf("leaf 1", null)
                        .leaf("leaf 2", null)
                        .branch("branch 3", branch -> branch.leaf("leaf 3-1", null))
                        .leaf("leaf 4", null)
                        .branch("branch 5", branch -> branch
                                .leaf("leaf 5-1", null)
                                .branch("branch 5-2", branch2 ->
                                        branch2.leaf("leaf 5-2-1", null)
                                                .leaf("leaf 5-2-2", null)))
                        .build())
        );
    }

    private UIElement codeEditorExample() {
        return new UIElement().layout(layout -> layout.gapAll(2)).addChildren(
                new Label().setText("Xml Editor"),
                new CodeEditor().setLanguage(Languages.XML).setValue("""
                        <?xml version="1.0" encoding="UTF-8" ?>
                        <ldlib2-ui xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                   xsi:noNamespaceSchemaLocation="../../../../../ldlib2-ui.xsd">
                            <stylesheet location="ldlib2:lss/mc.lss"/>
                            <root class="panel_bg" style="width: 150; height: 300">
                                <button text="click me!"/>
                            </root>
                        </ldlib2-ui>
                        """.split("\n")),
                new Label().setText("LSS Editor"),
                new CodeEditor().setLanguage(Languages.LSS).setValue("""
                        label:host {
                          vertical-align: center;
                          horizontal-align: center;
                        }
                        .text-auto {
                          adaptive-width: true;
                          height: 10;
                        }
                        .button-icon {
                          height: 100%;
                          aspect-rate: 1;
                        }
                        .flex-1 {
                          flex: 1;
                        }
                        """.split("\n")),
                new Label().setText("JS Editor"),
                new CodeEditor().setLanguage(Languages.JAVASCRIPT).setValue("""
                        function add(a, b) {
                          return a + b;
                        }
                        """.split("\n"))
        );
    }

    private UIElement sceneExample() {
        var dummyWorld = TestScene.createTestScene();
        var dummyWorld2 = TestScene.createTestScene();
        return new UIElement().layout(layout -> layout.gapAll(2).widthPercent(100).heightPercent(100)).addChildren(
                new Label().setText("Prospective"),
                new Scene().createScene(dummyWorld)
                        .setTickWorld(true)
                        .setRenderedCore(dummyWorld.getFilledBlocks().longStream().mapToObj(BlockPos::of).toList())
                        .useCacheBuffer()
                        .layout(layout -> layout.widthPercent(100).flex(1).paddingAll(3))
                        .addClass("panel_bg"),
                new Label().setText("Ortho"),
                new Scene().createScene(dummyWorld2)
                        .useOrtho()
                        .setOrthoRange(.8f)
                        .setTickWorld(true)
                        .setRenderedCore(dummyWorld2.getFilledBlocks().longStream().mapToObj(BlockPos::of).toList())
                        .useCacheBuffer()
                        .layout(layout -> layout.widthPercent(100).flex(1).paddingAll(3))
                        .addClass("panel_bg")
        );
    }

    private UIElement searchComponentExample() {
        return new UIElement().layout(layout -> layout.gapAll(2).widthPercent(100).heightPercent(100)).addChildren(
                new SearchComponent<>(new SearchComponent.ISearchUI<Block>() {
                    @Override
                    public void search(String word, IResultHandler<Block> searchHandler) {
                        var lowerWord = word.toLowerCase();
                        for (var key : BuiltInRegistries.BLOCK.keySet()) {
                            if (Thread.currentThread().isInterrupted()) return;
                            if (key.toString().toLowerCase().contains(lowerWord)) {
                                searchHandler.acceptResult(BuiltInRegistries.BLOCK.get(key));
                            }
                        }
                    }

                    @Override
                    @Nonnull
                    public String resultText(@NotNull Block value) {
                        return BuiltInRegistries.BLOCK.getKey(value).toString();
                    }

                    @Override
                    public void onResultSelected(@Nullable Block value) {
                        // do noting
                    }
                }).setCandidateUIProvider(UIElementProvider.iconText(
                        block -> new ItemStackTexture(block.asItem()),
                        block -> Component.translatable(block.getDescriptionId())
                ))
        );
    }

    private UIElement colorSelectorExample() {
        return new UIElement().layout(layout -> layout.gapAll(2).widthPercent(40)).addChildren(
                new ColorSelector()
        );
    }

    private UIElement tabViewExample() {
        return new UIElement().layout(layout -> layout.gapAll(2).widthPercent(100)).addChildren(
                new TabView().addTab(new Tab().setText("Tab1"),
                        new UIElement().layout(layout ->
                                layout.height(150)).addChildren(new Label().setText("Tab1 Content")
                        )).addTab(new Tab().setText("Tab2"),
                        new UIElement().layout(layout ->
                                layout.height(150)).addChildren(new Label().setText("Tab2 Content")
                        ))

        );
    }

    private UIElement scrollerViewExample() {
        return new UIElement().layout(layout -> layout.gapAll(2).widthPercent(100).heightPercent(100)).addChildren(
                new ScrollerView()
                        .viewContainer(view -> view.getLayout().gapAll(2))
                        .addScrollViewChildren(
                                new Button(),
                                new Switch(),
                                new TextField(),
                                new Button(),
                                new Switch(),
                                new TextField(),
                                new Button(),
                                new Switch(),
                                new TextField(),
                                new Button(),
                                new Switch(),
                                new TextField(),
                                new Button(),
                                new Switch(),
                                new TextField(),
                                new Button(),
                                new Switch(),
                                new TextField(),
                                new Button(),
                                new Switch(),
                                new TextField(),
                                new Button(),
                                new Switch(),
                                new TextField()
                        )
                        .layout(layout -> layout.widthPercent(100).heightPercent(100))
        );
    }

    private UIElement graphViewExample() {
        return new UIElement().layout(layout -> layout.gapAll(2).widthPercent(100).heightPercent(100)).addChildren(
                new GraphView()
                        .addContentChild(new Button().layout(layout -> layout
                                .positionType(TaffyPosition.ABSOLUTE)
                                .left(0)
                                .top(0)
                        ).transform(transform2D -> transform2D.rotation(-45)))
                        .addContentChild(new Button().layout(layout -> layout
                                .positionType(TaffyPosition.ABSOLUTE)
                                .left(15)
                                .top(50)
                        ))
                        .addContentChild(new TextField().layout(layout -> layout
                                .positionType(TaffyPosition.ABSOLUTE)
                                .width(150)
                                .left(30)
                                .top(20)
                        ))
                        .addContentChild(new UIElement().layout(layout -> layout
                                .positionType(TaffyPosition.ABSOLUTE)
                                .width(100)
                                .height(100)
                                .left(100)
                                .top(40)
                        ).style(style -> style.background(new SpriteTexture())).transform(transform2D -> transform2D.rotation(45)))
                        .layout(layout -> layout.widthPercent(100).heightPercent(100))
        );
    }

    private UIElement splitViewExample() {
        return new UIElement().layout(layout -> layout.gapAll(2).widthPercent(100).heightPercent(100)).addChildren(
                new SplitView.Horizontal().setPercentage(50)
                        .left(new UIElement().layout(layout -> layout.height(100))
                                .addChildren(new Label().setText("Left"))
                                .addClass("panel_bg"))
                        .right(new UIElement().layout(layout -> layout.height(100))
                                .addChildren(new Label().setText("right"))
                                .addClass("panel_bg"))
                        .layout(layout -> layout.flex(1)),
                new SplitView.Vertical().setPercentage(50)
                        .top(new UIElement().layout(layout -> layout.widthPercent(100).heightPercent(100))
                                .addChildren(new Label().setText("top"))
                                .addClass("panel_bg"))
                        .bottom(new UIElement().layout(layout -> layout.widthPercent(100).heightPercent(100))
                                .addChildren(new Label().setText("bottom"))
                                .addClass("panel_bg"))
                        .layout(layout -> layout.flex(1))
        );
    }
}
