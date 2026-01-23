package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.NoArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.appliedenergistics.yoga.YogaJustify;

import java.util.concurrent.atomic.AtomicInteger;

@LDLRegisterClient(name="doc", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestDoc implements IScreenTest{
    @Override
    public ModularUI createUI(Player entityPlayer) {
//        return step12();
//        return step3();
//        return step4();
//        return step5();
//        return step6();
//        return step6_2();
//        return step6_3();
        var element = new UIElement();
        // many ways to set the layout
        // set the layout directly
        element.getLayout()
                .flexDirection(FlexDirection.ROW)
                .width(150)
                .heightPercent(100)
                .marginAll(10)
                .paddingAll(10);
        // set the layout with chaining methods
        element.layout(layout -> layout
                .flexDirection(FlexDirection.ROW)
                .width(150)
                .heightPercent(100)
                .marginAll(10)
                .paddingAll(10)
        );
        // set the layout by stylesheet
        element.lss("flex-direction", "row");
        element.lss("width", 150);
        element.lss("height-percent", 100);
        element.lss("margin-all", 10);
        element.lss("padding-all", 10);
        return step7();
    }

    private ModularUI step12() {
        // create a root element
        var root = new UIElement();
        root.addChildren(
                // add a label to display text
                new Label().setText("My First UI"),
                // add a button with text
                new Button().setText("Click Me!"),
                // add an element to display an image based on a resource location
                new UIElement().layout(layout -> layout.width(80).height(80))
                        .style(style -> style.background(
                                SpriteTexture.of("ldlib2:textures/gui/icon.png"))
                        )
        ).style(style -> style.background(Sprites.BORDER)); // set a background for the root element
        // create a UI
        var ui = UI.of(root);
        // return a modular UI for runtime instance
        return new ModularUI(ui);
    }

    private ModularUI step3() {
        // create a root element
        var root = new UIElement();
        root.addChildren(
                // add a label to display text
                new Label().setText("My First UI")
                        // center align text
                        .textStyle(textStyle -> textStyle.textAlignHorizontal(Horizontal.CENTER)),
                // add a button with text
                new Button().setText("Click Me!"),
                // add an element to display an image based on a resource location
                new UIElement().layout(layout -> layout.width(80).height(80))
                        .style(style -> style.background(
                                SpriteTexture.of("ldlib2:textures/gui/icon.png"))
                        )
        ).style(style -> style.background(Sprites.BORDER)); // set a background for the root element
        // set padding and gap for children elements
        root.layout(layout -> layout.paddingAll(7).gapAll(5));
        // create a UI
        var ui = UI.of(root);
        // return a modular UI for runtime instance
        return new ModularUI(ui);
    }

    private ModularUI step4() {
        // create a root element
        var root = new UIElement();
        // add an element to display an image based on a resource location
        var image = new UIElement().layout(layout -> layout.width(80).height(80))
                .style(style -> style.background(
                        SpriteTexture.of("ldlib2:textures/gui/icon.png"))
                );
        root.addChildren(
                // add a label to display text
                new Label().setText("Interaction")
                        // center align text
                        .textStyle(textStyle -> textStyle.textAlignHorizontal(Horizontal.CENTER)),
                image,
                // add a container with the row flex direction
                new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW)).addChildren(
                        // a button to rotate the image -45°
                        new Button().setText("-45°")
                                .setOnClick(e -> image.transform(transform ->
                                        transform.rotation(transform.rotation()-45))),
                        new UIElement().layout(layout -> layout.flex(1)), // occupies the remaining space
                        // a button to rotate the image 45°
                        new Button().setText("+45°")
                                .setOnClick(e -> image.transform(transform ->
                                        transform.rotation(transform.rotation() + 45)))
                )
        ).style(style -> style.background(Sprites.BORDER)); // set a background for the root element
        // set padding and gap for children elements
        root.layout(layout -> layout.paddingAll(7).gapAll(5));
        // create a UI
        var ui = UI.of(root);
        // return a modular UI for runtime instance
        return new ModularUI(ui);
    }

    private ModularUI step5() {
        // create a root element
        var root = new UIElement();
        // add an element to display an image based on a resource location
        var image = new UIElement().layout(layout -> layout.width(80).height(80))
                .style(style -> style.background(
                        SpriteTexture.of("ldlib2:textures/gui/icon.png"))
                );
        root.addChildren(
                // add a label to display text
                new Label().setText("Interaction")
                        // center align text
                        .textStyle(textStyle -> textStyle.textAlignHorizontal(Horizontal.CENTER)),
                image,
                // add a container with the row flex direction
                new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW)).addChildren(
                        // implement the button by using ui events
                        new UIElement().addChild(new Label().setText("-45°").textStyle(textStyle -> textStyle.adaptiveWidth(true)))
                                .layout(layout -> layout.justifyContent(AlignContent.CENTER).paddingHorizontal(3))
                                .style(style -> style.background(Sprites.BORDER1))
                                .addEventListener(UIEvents.MOUSE_DOWN, e -> image.transform(transform ->
                                        transform.rotation(transform.rotation()-45)))
                                .addEventListener(UIEvents.MOUSE_ENTER, e ->
                                        e.currentElement.style(style -> style.background(Sprites.BORDER1_DARK)), true)
                                .addEventListener(UIEvents.MOUSE_LEAVE, e ->
                                        e.currentElement.style(style -> style.background(Sprites.BORDER1)), true),
                        new UIElement().layout(layout -> layout.flex(1)), // occupies the remaining space
                        // a button to rotate the image 45°
                        new Button().setText("+45°")
                                .setOnClick(e -> image.transform(transform ->
                                        transform.rotation(transform.rotation() + 45)))
                )
        ).style(style -> style.background(Sprites.BORDER)); // set a background for the root element
        // set padding and gap for children elements
        root.layout(layout -> layout.paddingAll(7).gapAll(5));
        // create a UI
        var ui = UI.of(root);
        // return a modular UI for runtime instance
        return new ModularUI(ui);
    }

    private ModularUI step6() {
        var root = new UIElement();
        root.addChildren(
                new Label().setText("LSS example")
                        .lss("horizontal-align", "center"),
                new Button().setText("Click Me!"),
                new UIElement()
                        .lss("width", 80)
                        .lss("height", 80)
                        .lss("background", "sprite(ldlib2:textures/gui/icon.png)")

        );
        root.lss("background", "built-in(ui-gdp:BORDER)");
        root.lss("padding-all", 7);
        root.lss("gap-all", 5);
        var ui = UI.of(root);
        return new ModularUI(ui);
    }

    private ModularUI step6_2() {
        // set root with an ID
        var root = new UIElement().setId("root");
        root.addChildren(
                new Label().setText("LSS example"),
                new Button().setText("Click Me!"),
                // set the element with a class
                new UIElement().addClass("image")
        );
        var lss = """
            // id selector
            #root {
                background: built-in(ui-gdp:BORDER);
                padding-all: 7;
                gap-all: 5;
            }
            
            // class selector
            .image {
                width: 80;
                height: 80;
                background: sprite(ldlib2:textures/gui/icon.png);
            }
            
            // element selector
            #root label {
                horizontal-align: center;
            }
            """;
        var stylesheet = Stylesheet.parse(lss);
        // add to ui
        var ui = UI.of(root, stylesheet);
        return new ModularUI(ui);
    }

    private ModularUI step6_3() {
        var root = new UIElement();
        root.layout(layout -> layout.width(100));
        root.addChildren(
                new Label().setText("Stylesheets"),
                new Button().setText("Click Me!"),
                new ProgressBar().setProgress(0.5f).label(label -> label.setText("Progress")),
                new Toggle().setText("Toggle"),
                new TextField().setText("Text Field"),
                new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW)).addChildren(
                        new ItemSlot().setItem(Items.APPLE.getDefaultInstance()),
                        new FluidSlot().setFluid(new FluidStack(Fluids.WATER, 1000))
                ),
                // list all stylesheets
                new Selector<ResourceLocation>()
                        .setSelected(StylesheetManager.GDP, false)
                        .setCandidates(StylesheetManager.INSTANCE.getAllPackStylesheets().stream().toList())
                        .setOnValueChanged(selected -> {
                            // switch to the selected stylesheet
                            var mui = root.getModularUI();
                            if (mui != null) {
                                mui.getStyleEngine().clearAllStylesheets();
                                mui.getStyleEngine().addStylesheet(StylesheetManager.INSTANCE.getStylesheetSafe(selected));
                            }
                        })
        );
        root.addClass("panel_bg");
        // use GDP stylesheets by default
        var ui = UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP));
        return new ModularUI(ui);
    }

    private ModularUI step7() {
        // a value holder
        var valueHolder = new AtomicInteger(0);

        var root = new UIElement();
        root.addChildren(
                new Label().setText("Data Bindings")
                        .textStyle(textStyle -> textStyle.textAlignHorizontal(Horizontal.CENTER)),
                new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW)).addChildren(
                        // button to decrease the value
                        new Button().setText("-")
                                .setOnClick(e -> {
                                    if (valueHolder.get() > 0) {
                                        valueHolder.decrementAndGet();
                                    }
                                }),
                        new TextField()
                                .setNumbersOnlyInt(0, 100)
                                .setValue(String.valueOf(valueHolder.get()))
                                // bind an Observer to update the value holder
                                .bindObserver(value -> valueHolder.set(Integer.parseInt(value)))
                                // bind a DataSource to notify the value changes
                                .bindDataSource(SupplierDataSource.of(() -> String.valueOf(valueHolder.get())))
                                .layout(layout -> layout.flex(1)),
                        // button to increase the value
                        new Button().setText("+")
                                .setOnClick(e -> {
                                    if (valueHolder.get() < 100) {
                                        valueHolder.incrementAndGet();
                                    }
                                })
                ),
                // bind a DataSource to notify the value changes for label and progress bar
                new Label().bindDataSource(SupplierDataSource.of(() -> Component.literal("Binding: ").append(String.valueOf(valueHolder.get())))),
                new ProgressBar()
                        .setProgress(valueHolder.get() / 100f)
                        .bindDataSource(SupplierDataSource.of(() -> valueHolder.get() / 100f))
                        .label(label -> label.bindDataSource(SupplierDataSource.of(() -> Component.literal("Progress: ").append(String.valueOf(valueHolder.get())))))
        ).style(style -> style.background(Sprites.BORDER));
        root.layout(layout -> layout.width(100).paddingAll(7).gapAll(5));
        return new ModularUI(UI.of(root));
    }
}
