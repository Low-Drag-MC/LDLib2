package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import lombok.NoArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaJustify;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@LDLRegisterClient(name="doc", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestDoc implements IScreenTest{
    @Override
    public ModularUI createUI(Player entityPlayer) {
//        return step12();
//        return step3();
//        return step4();
        return step5();
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
                new UIElement().layout(layout -> layout.flexDirection(YogaFlexDirection.ROW)).addChildren(
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
                new UIElement().layout(layout -> layout.flexDirection(YogaFlexDirection.ROW)).addChildren(
                        // implement the button by using ui events
                        new UIElement().addChild(new Label().setText("-45°").textStyle(textStyle -> textStyle.adaptiveWidth(true)))
                                .layout(layout -> layout.justifyItems(YogaJustify.CENTER).paddingHorizontal(3))
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

    private ModularUI stepx() {
        var valueHolder = new AtomicReference<>("value");

        // create a root element
        var root = new UIElement();
        root.addChildren(
                // add a label to display text
                new Label().setText("Interaction")
                        // center align text
                        .textStyle(textStyle -> textStyle.textAlignHorizontal(Horizontal.CENTER)),
                // add a container with the row flex direction
                new UIElement().layout(layout -> layout.flexDirection(YogaFlexDirection.ROW)).addChildren(
                        new TextField()
                                .setTextResponder(valueHolder::set)
                                .bindDataSource(SupplierDataSource.of(valueHolder::get))
                                .layout(layout -> layout.flex(1)),
                        // a button to clear the text field
                        new Button().setText("clear")
                                .setOnClick(e -> valueHolder.set(""))
                ),
                new Label().bindDataSource(SupplierDataSource.of(() -> Component.literal(valueHolder.get()))),
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
}
