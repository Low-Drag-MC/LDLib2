package com.lowdragmc.lowdraglib2.gui.ui.debugger;

import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.client.font.LDFonts;
import com.lowdragmc.lowdraglib2.gui.editor.view.UIHierarchy;
import com.lowdragmc.lowdraglib2.gui.editor.view.UITreeNode;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.HistoryStack;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class UIDebugger extends UIElement {
    public final UIElement titleBar;
    /**
     * Moves the debugger between its two hosts: layered over the game, or in an OS window of its own.
     * Hidden when the platform will not give us a second window at all.
     */
    public final Toggle windowModeToggle;
    public final Label title;
    public final UIElement container;
    public final ModularUI modularUI;
    public final UIHierarchy hierarchy;
    public final Inspector inspector;
    public final TabView tabView;
    public final ScrollerView computedView;
    public final CodeEditor codeEditor;
    public final HistoryStack historyStack = new HistoryStack();
    public final LayoutPanel margin = new LayoutPanel(0x80646669);
    public final LayoutPanel border = new LayoutPanel(0x8000ff00);
    public final LayoutPanel padding = new LayoutPanel(0x800000ff);
    public final Label content = new Label();

    // runtime
    @Nullable
    private ISubscription codeEditorSubscription;
    @Getter @Setter
    private boolean focusMode;
    @Getter @Setter
    private boolean renderUIShaping = true;
    /**
     * Whether the debugger floats over the UI it inspects, or fills a host of its own.
     *
     * <p>Floating is how it opens: a 200x200 panel the user drags around by its title and resizes by
     * its edges, laid out over the very UI being inspected. Hosted in an OS window none of that
     * applies — the window is moved and resized by <em>its</em> edges, and a second set of handles
     * inside the client area only fights them.
     */
    @Getter
    private boolean floating = true;
    protected boolean isResizing = false;

    public UIDebugger(ModularUI modularUI) {
        this.modularUI = modularUI;
        applyHostLayout();

        this.title = new Label();
        this.title.getLayout().flexGrow(1).heightPercent(100);
        this.title.getTextStyle().textAlignVertical(Vertical.CENTER);
        this.title.setText("Debugger").setOverflowVisible(false);
        this.windowModeToggle = buildWindowModeToggle();
        this.titleBar = new UIElement().layout(layout -> layout.paddingAll(4).alignItems(AlignItems.CENTER).flexDirection(FlexDirection.ROW))
                .addChildren(
                        title,
                        // right
                        new UIElement().layout(layout -> {
                            layout.flexDirection(FlexDirection.ROW);
                            layout.justifyContent(AlignContent.FLEX_END);
                            layout.gapAll(2);
                            layout.height(14);
                        }).addChildren(
                                // focus mode toggle
                                new Toggle()
                                        .setText("")
                                        .setOn(isFocusMode(), false)
                                        .toggleButton(button -> button.layout(layout -> {
                                            layout.widthPercent(100);
                                            layout.heightPercent(100);
                                        }))
                                        .setOnToggleChanged(this::setFocusMode)
                                        .toggleStyle(style -> {
                                            style.setPipelineState(StyleOrigin.DEFAULT);
                                            style.baseTexture(Sprites.BORDER1_RT1_DARK);
                                            style.hoverTexture(Sprites.BORDER1_RT1);
                                            style.setPipelineState(StyleOrigin.INLINE);
                                            style.unmarkTexture(Icons.PAGE_FIT.copy().setColor(ColorPattern.GRAY.color).scale(0.8f));
                                            style.markTexture(Icons.PAGE_FIT.copy().scale(0.8f));
                                        })
                                        .bindDataSource(SupplierDataSource.of(this::isFocusMode))
                                        .layout(layout -> {
                                            layout.paddingAll(0);
                                            layout.heightPercent(100);
                                            layout.setAspectRatio(1f);
                                        })
                                        .style(style -> style.tooltips("debugger.focus_mode.0", "debugger.focus_mode.1")),
                                // render ui shaping toggle
                                new Toggle()
                                        .setText("")
                                        .setOn(isRenderUIShaping(), false)
                                        .toggleButton(button -> button.layout(layout -> {
                                            layout.widthPercent(100);
                                            layout.heightPercent(100);
                                        }))
                                        .setOnToggleChanged(this::setRenderUIShaping)
                                        .toggleStyle(style -> {
                                            style.setPipelineState(StyleOrigin.DEFAULT);
                                            style.baseTexture(Sprites.BORDER1_RT1_DARK);
                                            style.hoverTexture(Sprites.BORDER1_RT1);
                                            style.setPipelineState(StyleOrigin.INLINE);
                                            style.unmarkTexture(Icons.INFORMATION.copy().setColor(ColorPattern.GRAY.color).scale(0.8f));
                                            style.markTexture(Icons.INFORMATION.copy().scale(0.8f));
                                        })
                                        .bindDataSource(SupplierDataSource.of(this::isRenderUIShaping))
                                        .layout(layout -> {
                                            layout.paddingAll(0);
                                            layout.heightPercent(100);
                                            layout.setAspectRatio(1f);
                                        })
                                        .style(style -> style.tooltips("debugger.ui_shaping.0")),
                                windowModeToggle
                        )
                );
        this.titleBar.addClass("__editor_top__");
        this.titleBar.getStyle().background(Sprites.BORDER);

        this.container = new UIElement();
        this.container.getLayout().flex(1);
        this.container.getStyle().background(SDFRectTexture.of(0xdd2c2c34)
                .setRadius(new Vector4f(0, 0, 4, 4)));

        this.hierarchy = new UIHierarchy();
        this.hierarchy.loadUI(modularUI.ui);
        this.hierarchy.setOnSelectedChanged(this::onNodeSelected);

        this.inspector = new Inspector();
        this.inspector.setHistoryStack(historyStack);
        this.inspector.layout(layout -> {
            layout.heightPercent(100);
            layout.widthPercent(100);
        });
        this.inspector.addClass("__ui-editor-view_inspector__").moveInlineAsDefault();

        margin.getStyle().tooltips("margin");
        border.getStyle().tooltips("border");
        padding.getStyle().tooltips("padding");
        content.setText("- X -");
        content.getTextStyle().fontSize(4.5f).textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER);
        content.getLayout().widthPercent(100).heightPercent(100);
        tabView = new TabView();
        tabView.getLayout().widthPercent(100).heightPercent(100);
        tabView.tabContentContainer.getLayout().flex(1);
        this.container.addChildren(new SplitView.Horizontal()
                .setPercentage(35)
                .left(new UIElement().layout(layout -> layout.widthPercent(100).heightPercent(100).flexDirection(FlexDirection.ROW))
                        .addChildren(hierarchy.layout(layout -> layout.widthAuto().flex(1)), new UIElement()
                                .layout(layout -> layout.width(2).heightPercent(100))
                                .style(style -> style.background(SDFRectTexture.of(ColorPattern.T_WHITE.color).setRadius(1f)))
                        )
                )
                .right(tabView));

        // inline settings
        tabView.addTab(new Tab().setText("inline"), new UIElement()
                .layout(layout -> layout.widthPercent(100).heightPercent(100).paddingAll(4))
                .addClass("panel_bg")
                .addChild(new SplitView.Vertical().setPercentage(30).top(new UIElement()
                                .layout(layout -> layout.widthPercent(100).heightPercent(100).paddingAll(2))
                                .addChild(margin.addCenter(border.addCenter(padding.addCenter(new UIElement()
                                        .layout(l -> l.widthPercent(100).heightPercent(100)
                                                .alignItems(AlignItems.CENTER).justifyItems(AlignItems.CENTER))
                                        .style(s -> s.tooltips("size")
                                                .background(new ColorRectTexture(ColorPattern.T_GRAY.color)))
                                        .addChild(content)))))).bottom(inspector)
                        .selfCall(e -> ((SplitView.Vertical)e).first
                                .addClass("panel_bg")
                                .layout(l -> l.minHeight(68)))
                ));
        tabView.addTab(new Tab().setText("computed"), computedView = (ScrollerView) new ScrollerView()
                .layout(layout -> layout.widthPercent(100).heightPercent(100)));
        tabView.addTab(new Tab().setText("local lss"), codeEditor = (CodeEditor) new CodeEditor()
                        .layout(layout -> layout.widthPercent(100).heightPercent(100)));
        codeEditor.setLanguage(Languages.LSS);

        // Gated on the predicates rather than only installed when floating: the host can change while
        // the debugger element lives on, and a listener already added cannot be taken back off.
        WindowDragHelper.setDragMove(this.title, this, e -> floating, null);
        WindowDragHelper.setBorderResize(this, this, 4, new Vector2f(40), new Vector2f(Float.MAX_VALUE),
                e -> floating, (e, handle) -> {
                    isResizing = true;
                    return true;
                }, e -> isResizing = false);

        addChildren(titleBar, container);

        internalSetup();
    }

    /**
     * The control that moves the debugger between its two hosts.
     *
     * <p>Built here rather than in {@code UIDebuggerWindow} because it has to be reachable from both:
     * the point of it is that you can leave the window as easily as you entered it.
     */
    private Toggle buildWindowModeToggle() {
        return (Toggle) new Toggle()
                .setText("")
                .setOn(false, false)
                .toggleButton(button -> button.layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                }))
                // Requested, not applied: the switch reparents this toggle's own ancestor, which is
                // not something to do from inside its click dispatch. See ModularUIClientAccess.
                .setOnToggleChanged(windowed -> ModularUIClientAccess.requestDebuggerWindowed(modularUI, windowed))
                .toggleStyle(style -> {
                    style.setPipelineState(StyleOrigin.DEFAULT);
                    style.baseTexture(Sprites.BORDER1_RT1_DARK);
                    style.hoverTexture(Sprites.BORDER1_RT1);
                    style.setPipelineState(StyleOrigin.INLINE);
                    // The icon says where clicking takes you, not where you are.
                    style.unmarkTexture(Icons.FLOAT.copy().setColor(ColorPattern.GRAY.color).scale(0.8f));
                    style.markTexture(Icons.SCREEN.copy().scale(0.8f));
                })
                .bindDataSource(SupplierDataSource.of(() -> ModularUIClientAccess.isDebuggerWindowed(modularUI)))
                .layout(layout -> {
                    layout.paddingAll(0);
                    layout.heightPercent(100);
                    layout.setAspectRatio(1f);
                })
                .style(style -> style.tooltips("debugger.window_mode.0", "debugger.window_mode.1"));
    }

    /**
     * Switches between floating over the inspected UI and filling a host of its own.
     *
     * @see #isFloating()
     */
    public UIDebugger setFloating(boolean floating) {
        if (this.floating == floating) return this;
        this.floating = floating;
        applyHostLayout();
        return this;
    }

    private void applyHostLayout() {
        if (floating) {
            getLayout().positionType(TaffyPosition.ABSOLUTE).width(200).height(200);
        } else {
            // left/top back to auto, not zero: a previous floating session moved them, and an inset on
            // a relative box offsets it out of its host rather than being ignored.
            getLayout().positionType(TaffyPosition.RELATIVE).leftAuto().topAuto()
                    .widthPercent(100).heightPercent(100);
        }
    }

    private void onNodeSelected(Set<UITreeNode> uiTreeNodes) {
        inspector.clear();
        if (uiTreeNodes.size() == 1) {
            var element = uiTreeNodes.iterator().next().key;
            inspector.inspect(element);
            loadComputedView(element);
            loadLocalLSS(element);
        }
    }

    private void loadComputedView(UIElement element) {
        computedView.clearAllScrollViewChildren();
        for (Style style : element.getStyles()) {
            for (Property<?> property : style.getPropertiesList()) {
                computedView.addScrollViewChild(new Label()
                        .bindDataSource(SupplierDataSource.of(() ->
                                Component.literal(property.name).append(": ").append(style.getValueSave(property).toString()))
                        )
                        .textStyle(textStyle -> textStyle.adaptiveWidth(true))
                );
            }
        }
    }

    private void loadLocalLSS(UIElement element) {
        if (codeEditorSubscription != null) {
            codeEditorSubscription.unsubscribe();
            codeEditorSubscription = null;
        }
        codeEditor.setValue(null, false);
        var current = new AtomicReference<Stylesheet>(null);
        codeEditorSubscription = codeEditor.registerValueListener(lines -> {
            var newStylesheet = Stylesheet.parse(String.join("\n", lines));
            newStylesheet.setName("DEBUG_LOCAL");
            if (current.get() != null) {
                element.removeLocalStylesheet(current.get());
            }
            element.addLocalStylesheet(newStylesheet);
            current.set(newStylesheet);
        });
        for (var stylesheet : element.getLocalStylesheets()) {
            if (stylesheet.getName().equals("DEBUG_LOCAL") && stylesheet.getRawLss() != null) {
                var rawLss = stylesheet.getRawLss();
                current.set(stylesheet);
                codeEditor.setValue(rawLss.split("\n"), false);
                break;
            }
        }
    }

    public void focusElement(UIElement element) {
        if (hierarchy.treeList.getRoot() != null) {
            var node = findElementNode(hierarchy.treeList.getRoot(), element);
            if (node != null) {
                hierarchy.treeList.expandNodeAlongPath(node);
                hierarchy.treeList.setSelected(List.of(node), true);
            }
        }
    }

    private @Nullable UITreeNode findElementNode(UITreeNode node, UIElement element) {
        if (node.key == element) return node;
        for (UITreeNode child : node.getChildren()) {
            var result = findElementNode(child, element);
            if (result != null) return result;
        }
        return null;
    }

    // ------------------------------------------------------------------ drawing into the inspected UI

    /**
     * Selects whatever the pointer is over in the inspected UI, if focus mode is on.
     *
     * <p>Called from the inspected UI's own click handling — which is the only place that knows the
     * pointer landed there — before anything else sees the press. That press is swallowed by the
     * caller either way, which is why nothing is reported back: picking an element must not also
     * press the button it picked, and that has to hold when the pointer is over nothing selectable
     * too.
     */
    public void pickHovered() {
        if (!focusMode) return;
        var hovered = modularUI.getLastHoveredElement();
        if (hovered != null) {
            focusElement(hovered);
        }
    }

    /**
     * The element whose box the overlay is outlining, or {@code null} for none.
     *
     * <p>Focus mode follows the pointer over the inspected UI; otherwise it follows the hierarchy
     * row the pointer is over, so hovering a row in the tree lights the element up on screen.
     */
    @Nullable
    public UIElement getShapingElement() {
        if (focusMode) {
            return modularUI.getLastHoveredElement();
        }
        if (renderUIShaping && hierarchy.treeList.getHoveredNode() != null) {
            return hierarchy.treeList.getHoveredNode().key;
        }
        return null;
    }

    /**
     * Draws the debugger's overlay — the box model of the highlighted element, its vitals, and in
     * focus mode a crosshair — into whichever host is showing the inspected UI.
     *
     * <p>This runs as part of the <em>inspected</em> UI's frame, not the debugger's own, because the
     * two need not be in the same window any more: the debugger can sit in an OS window of its own
     * while its target is drawn in the game window or in a third one. Coordinates here are that
     * host's, which is exactly what the element transforms and the mouse position already are.
     *
     * <p>No depth juggling, unlike 1.21: the gui renderer is deferred, so these outlines are appended
     * to the host's render state after the UI content it already contributed and sort above it by
     * construction. There is no depth state left switched off for the tooltips and drag ghost that
     * follow to trip over.
     */
    public void renderHostOverlay(GUIContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        var shaping = getShapingElement();
        // Nothing highlighted means nothing to draw — including the crosshair, which in focus mode is
        // shown exactly when there is something under the pointer to pick.
        if (shaping == null) return;

        ModularUIClientAccess.getWidget(modularUI).renderUISpacing(context, shaping, graphics);
        var font = LDFonts.font();
        // Only while picking: outside focus mode the inspected UI is live and fully clickable, and
        // a crosshair over it would be noise rather than an affordance.
        if (focusMode) {
            DrawerHelperClient.drawSolidRect(context, 0, mouseY - 1, modularUI.getScreenWidth(), 1, 0xffff0000);
            DrawerHelperClient.drawSolidRect(context, mouseX - 1, 0, 1, modularUI.getScreenHeight(), 0xffff0000);
            LDFonts.drawText(context, font, "pos(%d, %d)".formatted(mouseX, mouseY),
                    mouseX, Math.max(0, mouseY - 10), ColorPattern.YELLOW.color, true);
        }
        var y = 0;
        for (var info : shaping.getDebugInfo()) {
            LDFonts.drawText(context, font, info, 0, y, -1, true);
            y += 10;
        }
    }

    @Override
    public boolean isEditorVisible() {
        return false;
    }

    @Override
    public void screenTick() {
        super.screenTick();
        // Re-checked rather than set once: whether a second window can be opened depends on native
        // fullscreen, which the user can turn on and off with the debugger already open.
        windowModeToggle.setDisplay(OsWindowManager.isAvailable() || ModularUIClientAccess.isDebuggerWindowed(modularUI));
        hierarchy.getSelectedOne().ifPresentOrElse(e -> {
            var layout = e.getTaffyLayout();
            margin.setValue(layout.margin());
            border.setValue(layout.border());
            padding.setValue(layout.padding());
            content.setText("%.1f X %.1f".formatted(layout.size().width, layout.size().height));
        }, () -> {
            margin.setValue(null);
            border.setValue(null);
            padding.setValue(null);
            content.setText("- X -");
        });
    }


    @Override
    protected void drawBackgroundAdditional(@NotNull IGUIContext guiContext) {
        if (!(guiContext instanceof GUIContext context)) return;
        super.drawBackgroundAdditional(context);

        if (floating && isSelfOrChildHover() && !isResizing) {
            WindowDragHelper.drawResizeIcon(context, this, 4);
        }
    }
}
