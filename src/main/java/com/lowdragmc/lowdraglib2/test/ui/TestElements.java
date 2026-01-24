package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.NoArgsConstructor;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import org.appliedenergistics.yoga.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

@LDLRegisterClient(name = "ui_elements", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestElements implements IScreenTest {
    @Nullable
    private Block block = null;

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.width(250);
            layout.height(400);
            layout.paddingAll(10);
        }).setId("root");
        root.getStyle().backgroundTexture(Sprites.BORDER);
        root.addChildren(new TabView().addTab(new Tab().setText("element1"), new UIElement().layout(layout -> {
                            layout.widthPercent(100);
                        }).addChildren(
                                new Label()
                                        .setText("Hello World!!")
                                        .textStyle(style -> style
                                                .fontSize(20)
                                                .textAlignHorizontal(Horizontal.CENTER)
                                                .textAlignVertical(Vertical.CENTER))
                                        .layout(layout -> {
                                            layout.height(30);
                                        }).setId("header")
                                        .style(style -> style.backgroundTexture(Sprites.BORDER)),
                                new UIElement().layout(layout -> {
                                            layout.marginHorizontal(10);
                                            layout.marginBottom(10);
                                            layout.paddingAll(5);
                                            layout.gapRow(2);
                                        })
                                        .style(style -> style.backgroundTexture(Sprites.BORDER))
                                        .addChildren(
                                                new Button(),
                                                new Toggle(),
                                                new Selector<Direction.Axis>().setCandidates(Arrays.stream(Direction.Axis.values()).toList()),
                                                new Selector<Direction>().setCandidates(Arrays.stream(Direction.values()).toList()),
                                                new TextField(),
                                                new TextField().setResourceLocationOnly(),
                                                new TextField().setNumbersOnlyInt(23, 145),
                                                new Scroller.Horizontal(),
                                                new ScrollerView()
                                                        .addScrollViewChildren(new Button(), new Button().layout(layout -> layout.width(300)),
                                                                new TextField().setNumbersOnlyFloat(-3, 3),
                                                                new ScrollerView().addScrollViewChildren(new Button(), new Button(), new Button(), new Button(), new Button(), new Button(), new Button(), new Button(), new Button(), new Button())
                                                                        .layout(layout -> {
                                                                            layout.width(120);
                                                                            layout.height(120);
                                                                        }),
                                                                new Button(), new Button(), new Button(), new Button(), new Button())
                                                        .layout(layout -> layout.height(100)),
                                                new ProgressBar().label(label -> label.setText("30%")).setProgress(0.3f).barContainer(barContainer ->
                                                        barContainer.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                                                            if (barContainer.isMouseOverContent(event.x, event.y)) {
                                                                var percent = (Mth.clamp(event.x, barContainer.getContentX(),
                                                                        barContainer.getContentX() + barContainer.getContentWidth()) - barContainer.getContentX())
                                                                        / barContainer.getContentWidth();
                                                                if (barContainer.getParent() instanceof ProgressBar progressBar) {
                                                                    progressBar.setValue(percent);
                                                                }
                                                            }
                                                        }))))
                ).addTab(new Tab().setText("element2"), new UIElement().layout(layout -> {
                            layout.gapRow(2);
                        }).addChildren(
                                new ColorSelector().layout(layout -> {
                                    layout.width(60);
                                }),
                                new TagField(),
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
                                        block = value;
                                    }
                                }).setCandidateUIProvider(UIElementProvider.iconText(
                                        block -> new ItemStackTexture(block.asItem()),
                                        block -> Component.translatable(block.getDescriptionId())
                                )),
                                new TextArea().textAreaStyle(textAreaStyle -> textAreaStyle.fontSize(13))
                                        .setLines(List.of(
                                                "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ",
                                                "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. ",
                                                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. ",
                                                "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
                                        )),
                                new CodeEditor().setLanguage(Languages.LSS)
                        )
                ).addTab(new Tab().setText("transform"), new UIElement().layout(layout -> {
                            layout.gapRow(2);
                        }).addChildren(
                                new Button().transform(transform -> transform.translate(10, 0)),
                                new Button().transform(transform -> transform.translate(0, -5)),
                                new Button().transform(transform -> transform.scale(1.5f, 1)),
                                new Button().transform(transform -> transform.rotation(30)),
                                new Button().transform(transform -> transform.pivot(0, 0).rotation(30)),
                                new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW))
                                        .addChildren(
                                                new Button(),
                                                new Button().transform(transform -> transform.translate(0, -5)),
                                                new Button().transform(transform -> transform.scale(1.5f, 1)),
                                                new Button().transform(transform -> transform.rotation(30))
                                        ).transform(transform -> transform.translate(0, 10).rotation(15))
                        )
                )
        );
        // menu
        root.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 1) {
                root.addChildren(new Menu<>(TreeBuilder.Menu.start()
                        .leaf("item1", () -> {
                        })
                        .leaf(Icons.COPY, "item2", () -> {
                        })
                        .crossLine()
                        .branch("branch1", menu -> {
                            menu.leaf("item3", () -> {
                            });
                            menu.leaf("item4", () -> {
                            });
                        })
                        .leaf("item5", () -> {
                        })
                        .build(), TreeBuilder.Menu::uiProvider)
                        .layout(layout -> {
                            layout.left(e.x - root.getPositionX());
                            layout.top(e.y - root.getContentY());
                        }));
            }
        });
        return new ModularUI(UI.of(root));
    }
}
