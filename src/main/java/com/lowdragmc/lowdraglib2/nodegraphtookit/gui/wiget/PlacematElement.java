package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wiget;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.ElementRenameColorCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.util.RenameColorConfigurableHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.PlacematModel;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

public class PlacematElement extends GraphElement<PlacematModel> {
    public static final String PLACEMAT_LAYER = "Placemat";
    private static final float RESIZE_BORDER = 5f;
    private static final Vector2f MIN_SIZE = new Vector2f(80, 50);
    private static final Vector2f MAX_SIZE = new Vector2f(4000, 4000);

    private Label titleLabel;
    /** Inline edit field shown in place of the title label during rename. */
    private TextField inlineRenameField;

    public PlacematElement(PlacematModel model) {
        super(model);
        addClass("__placemat__");
    }

    @Override
    public String getLayerName() {
        return PLACEMAT_LAYER;
    }

    @Override
    protected void buildUI() {
        var model = getModel();
        // Position / size / background color come from the model — pin via IMPORTANT.
        Style.importantPipeline(getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE)
                .left(model.getPosition().x)
                .top(model.getPosition().y)
                .width(model.getSize().x)
                .height(model.getSize().y));
        Style.importantPipeline(getStyle(), s -> s.background(SDFRectTexture.of(model.getElementColor())));

        titleLabel = new Label();
        titleLabel.addClass("__placemat_title__");
        titleLabel.setText(Component.literal(model.getName()));
        Style.defaultPipeline(titleLabel.getLayout(), l -> l.widthPercent(100).height(14).marginAll(2));
        Style.defaultPipeline(titleLabel.getTextStyle(), s -> s.textColor(0xFFFFFFFF));
        addChild(titleLabel);

        // Inline rename on title double-click — placemats are always renamable per Capabilities.
        if (model.isRenamable()) {
            titleLabel.addEventListener(UIEvents.DOUBLE_CLICK, e -> {
                startInlineRename();
                e.stopPropagation();
            });
        }

        // Border resize support
        WindowDragHelper.setBorderResize(this, this, RESIZE_BORDER, MIN_SIZE, MAX_SIZE,
                // only resize on left-click
                event -> event.button == 0,
                // allow all resize drags
                null,
                // on finish: sync new layout back to model
                event -> {
                    model.setPosition(new Vector2f(getLayoutX(), getLayoutY()));
                    model.setSize(new Vector2f(getSizeWidth(), getSizeHeight()));
                });
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        var model = getModel();
        if (visitor.hasHint(ChangeHint.LAYOUT)) {
            Style.importantPipeline(getLayout(), l -> l.left(model.getPosition().x)
                    .top(model.getPosition().y)
                    .width(model.getSize().x)
                    .height(model.getSize().y));
        }
        if (visitor.hasHint(ChangeHint.STYLE)) {
            Style.importantPipeline(getStyle(), s -> s.background(SDFRectTexture.of(model.getElementColor())));
        }
        if (visitor.hasHint(ChangeHint.DATA)) {
            if (titleLabel != null) {
                titleLabel.setText(Component.literal(model.getName()));
            }
        }
    }

    @Override
    protected void onSelectionInspect(GraphInspector inspector) {
        super.onSelectionInspect(inspector);
        if (graphView != null) inspector.setHistoryStack(graphView.getHistoryStack());
        inspector.inspect(RenameColorConfigurableHelper.build(getModel(), graphView));
    }

    /**
     * Replaces the title label with a {@link TextField}. Enter / focus loss commit; Escape cancels.
     * Public so the right-click menu can trigger the same flow.
     */
    public void startInlineRename() {
        if (inlineRenameField != null) return;
        if (!getModel().isRenamable()) return;
        var initial = getModel().getName();
        // Force-hide the title label while the inline edit field is in place.
        Style.importantPipeline(titleLabel.getLayout(), l -> l.display(TaffyDisplay.NONE));
        inlineRenameField = new TextField();
        inlineRenameField.addClass("__placemat_title-rename__");
        inlineRenameField.setText(initial == null ? "" : initial);
        Style.defaultPipeline(inlineRenameField.getLayout(), l -> l.widthPercent(100).height(14).marginAll(2));

        final boolean[] done = {false};
        Runnable commit = () -> {
            if (done[0]) return;
            done[0] = true;
            var newName = inlineRenameField.getValue();
            var gv = getFirstAncestorOfType(GraphView.class);
            if (newName != null && !newName.equals(initial)) {
                if (gv != null) {
                    gv.dispatchCommand(new ElementRenameColorCommands.RenameElementCommand(getModel(), newName));
                } else {
                    getModel().setName(newName);
                }
            }
            endInlineRename();
        };
        Runnable cancel = () -> {
            if (done[0]) return;
            done[0] = true;
            endInlineRename();
        };

        inlineRenameField.addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == GLFW.GLFW_KEY_ENTER || e.keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                commit.run();
                e.stopPropagation();
            } else if (e.keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancel.run();
                e.stopPropagation();
            }
        });
        inlineRenameField.addEventListener(UIEvents.BLUR, e -> commit.run());

        addChild(inlineRenameField);
        inlineRenameField.focus();
    }

    private void endInlineRename() {
        if (inlineRenameField != null) {
            inlineRenameField.removeSelf();
            inlineRenameField = null;
        }
        // Clear the IMPORTANT display override applied in startInlineRename so the title is visible again.
        if (titleLabel != null) {
            Style.importantPipeline(titleLabel.getLayoutStyle(), l -> l.display((TaffyDisplay) null));
        }
    }

    @Override
    public void drawBackgroundOverlay(@NotNull GUIContext guiContext) {
        if (isSelected()) {
            guiContext.drawTexture(ColorPattern.BLUE.borderTexture(1),
                    getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        } else {
            var isHover = isSelfOrChildHover() || isUnderRegionSelection();
            if (isHover) {
                guiContext.drawTexture(ColorPattern.BLUE.borderTexture(1).setColor(0xaaffffff),
                        getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
            }
        }
        // Draw resize cursor hint
        if (isSelfOrChildHover()) {
            WindowDragHelper.drawResizeIcon(guiContext, this, RESIZE_BORDER);
        }
        super.drawBackgroundOverlay(guiContext);
    }
}
