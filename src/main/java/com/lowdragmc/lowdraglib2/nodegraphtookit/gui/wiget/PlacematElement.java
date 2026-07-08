package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wiget;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.ElementRenameColorCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.GraphCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.util.RenameColorConfigurableHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.PlacematModel;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

public class PlacematElement extends GraphElement<PlacematModel> {
    public static final String PLACEMAT_LAYER = "Placemat";
    private static final float RESIZE_BORDER = 5f;
    /** Title font size — doubled from the default 9 so the placemat label reads as a heading. */
    private static final float TITLE_FONT_SIZE = 18f;
    private static final float TITLE_HEIGHT = 24f;
    private static final Vector2f MIN_SIZE = new Vector2f(120, 60);
    private static final Vector2f MAX_SIZE = new Vector2f(4000, 4000);

    private Label titleLabel;
    /** Inline edit field shown in place of the title label during rename. */
    private TextField inlineRenameField;
    /** True while a border/corner resize drag is in progress (suppresses the hover move icon). */
    private boolean isResizing = false;

    public PlacematElement(PlacematModel model) {
        super(model);
        addClass("__placemat__");
    }

    @Override
    public String getLayerName() {
        return PLACEMAT_LAYER;
    }

    // Placemats opt out of GraphView's body-wide select+drag wiring: the body must stay transparent
    // to mouse-down so clicks/drags on it fall through to the graph view for region selection. Move is
    // wired onto the title bar (drag handle) in buildUI instead.
    @Override
    public boolean wantsDefaultMouseWiring() {
        return false;
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
        Style.inlinePipeline(titleLabel.getLayout(), l -> l.height(TITLE_HEIGHT));
        Style.defaultPipeline(titleLabel.getLayout(), l -> l.widthPercent(100)
                .marginTop(RESIZE_BORDER).marginLeft(RESIZE_BORDER).marginRight(RESIZE_BORDER));
        Style.defaultPipeline(titleLabel.getTextStyle(), s -> s.textColor(0xFFFFFFFF)
                .fontSize(TITLE_FONT_SIZE)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
        addChild(titleLabel);

        // Title bar is the drag handle: click selects the placemat, drag moves it (and contained
        // nodes). Reuses GraphView's shared select+drag logic from the title bar sub-element.
        titleLabel.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            var gv = getGraphView();
            if (gv != null) gv.onGraphElementMouseDown(this, e);
        });

        // Inline rename on title double-click — placemats are always renamable per Capabilities.
        if (model.isRenamable()) {
            titleLabel.addEventListener(UIEvents.DOUBLE_CLICK, e -> {
                startInlineRename();
                e.stopPropagation();
            });
        }

        // Border/corner resize. Registered as a CAPTURE-phase listener so that mouse-downs on the
        // body interior (no handle hit) are NOT counted as bubble listeners — that lets them fall
        // through to GraphView#onGraphViewMouseDown to start a region selection. On a border hit it
        // starts the resize drag and stops propagation. Applied via IMPORTANT to override the
        // model-pinned layout (a plain INLINE write would lose to the IMPORTANT pin above).
        addEventListener(UIEvents.MOUSE_DOWN, this::onResizeMouseDown, true);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onResizeDragUpdate);
        addEventListener(UIEvents.DRAG_END, this::onResizeDragEnd);
    }

    private void onResizeMouseDown(UIEvent e) {
        // only resize on left-click over a border/corner handle
        if (e.button != 0) return;
        var handle = WindowDragHelper.detectResizeHandle(this, e.x, e.y, RESIZE_BORDER);
        if (handle == null) return;
        var icon = handle.icon;
        var width = icon.spriteSize.width;
        var height = icon.spriteSize.height;
        startDrag(new WindowDragHelper.DragResize(
                        getLayoutX(), getLayoutY(), getSizeWidth(), getSizeHeight(), handle), icon)
                .setDragTexture(-width / 2f, -height / 2f, width, height);
        isResizing = true;
        e.stopPropagation();
    }

    private void onResizeDragUpdate(UIEvent e) {
        if (!(e.dragHandler.draggingObject instanceof WindowDragHelper.DragResize dragResize)) return;
        var d = getLocalMouseNormal(e.x - e.dragStartX, e.y - e.dragStartY);
        var rect = computeSnappedResizeRect(dragResize, d.x, d.y);
        // Live feedback — override the model-pinned layout via IMPORTANT.
        Style.importantPipeline(getLayout(), l -> l.left(rect.x).top(rect.y).width(rect.z).height(rect.w));
    }

    private void onResizeDragEnd(UIEvent e) {
        if (!(e.dragHandler.draggingObject instanceof WindowDragHelper.DragResize dragResize)) return;
        isResizing = false;
        var d = getLocalMouseNormal(e.x - e.dragStartX, e.y - e.dragStartY);
        var rect = computeSnappedResizeRect(dragResize, d.x, d.y);
        var newPos = new Vector2f(rect.x, rect.y);
        var newSize = new Vector2f(rect.z, rect.w);
        var model = getModel();
        // Skip no-op resizes (e.g. a click on the border without actually dragging) and just restore
        // the model-pinned layout so we don't push an empty undo step.
        if (newPos.equals(new Vector2f(dragResize.startX(), dragResize.startY()))
                && newSize.equals(new Vector2f(dragResize.startW(), dragResize.startH()))) {
            Style.importantPipeline(getLayout(), l -> l.left(model.getPosition().x).top(model.getPosition().y)
                    .width(model.getSize().x).height(model.getSize().y));
            return;
        }
        var gv = getGraphView();
        if (gv != null) {
            // Undoable via the command's before/after NBT snapshot.
            gv.dispatchCommand(new GraphCommands.ResizeElementCommand(model, newPos, newSize));
        } else {
            model.setPosition(newPos);
            model.setSize(newSize);
        }
    }

    /**
     * Computes the resized rect for the given drag delta, snapping the resulting edges to the graph
     * grid when snap-to-grid is enabled (keeps resize consistent with drag-move snapping).
     */
    private Vector4f computeSnappedResizeRect(WindowDragHelper.DragResize dragResize, float dx, float dy) {
        var rect = WindowDragHelper.computeResizeRect(dragResize, dx, dy, MIN_SIZE, MAX_SIZE);
        if (graphView != null && graphView.isSnapToGrid()) {
            var topLeft = graphView.snapPosition(new Vector2f(rect.x, rect.y));
            var bottomRight = graphView.snapPosition(new Vector2f(rect.x + rect.z, rect.y + rect.w));
            rect.set(topLeft.x, topLeft.y,
                    Math.max(MIN_SIZE.x, bottomRight.x - topLeft.x),
                    Math.max(MIN_SIZE.y, bottomRight.y - topLeft.y));
        }
        return rect;
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
     * A placemat is region-selected only when the selection rectangle fully contains it, so sweeping a
     * box across the canvas selects the nodes on top of it rather than the placemat itself.
     * Coordinates mirror {@link com.lowdragmc.lowdraglib2.gui.ui.UIElement#isOverlapping} so this stays
     * in the same space as the base region-selection check.
     */
    @Override
    public boolean canBeRegionSelected(Vector4f region) {
        var x = getPositionX();
        var y = getPositionY();
        var w = getSizeWidth();
        var h = getSizeHeight();
        return x >= region.x && y >= region.y
                && (x + w) <= (region.x + region.z)
                && (y + h) <= (region.y + region.w);
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
        // Match the title bar's inset + enlarged font so editing lines up with the displayed heading.
        // Height via INLINE to override TextField's constructor height(14) (INLINE beats DEFAULT).
        Style.inlinePipeline(inlineRenameField.getLayout(), l -> l.height(TITLE_HEIGHT));
        Style.defaultPipeline(inlineRenameField.getLayout(), l -> l.widthPercent(100)
                .marginTop(RESIZE_BORDER).marginLeft(RESIZE_BORDER).marginRight(RESIZE_BORDER));
        inlineRenameField.getTextFieldStyle().fontSize(TITLE_FONT_SIZE);

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
        boolean regionActive = graphView != null && graphView.getDragRegionSelection() != null;
        if (isSelected()) {
            guiContext.drawTexture(ColorPattern.BLUE.borderTexture(1),
                    getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        } else {
            // While a region selection is being dragged, reflect only true (full-containment)
            // candidacy — not hover — so the highlight doesn't flicker as the box sweeps the body.
            var isHighlighted = isUnderRegionSelection() || (!regionActive && isSelfOrChildHover());
            if (isHighlighted) {
                guiContext.drawTexture(ColorPattern.BLUE.borderTexture(1).setColor(0xaaffffff),
                        getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
            }
        }
        // Cursor hints (suppressed during a region drag): resize icon over a border/corner,
        // otherwise a move icon over the title bar.
        if (!isResizing && !regionActive && isSelfOrChildHover()) {
            var handle = WindowDragHelper.detectResizeHandle(this, guiContext.mouseX, guiContext.mouseY, RESIZE_BORDER);
            if (handle != null) {
                WindowDragHelper.drawResizeIcon(guiContext, this, RESIZE_BORDER);
            } else if (inlineRenameField == null && titleLabel != null && titleLabel.isSelfOrChildHover()) {
                drawMoveIcon(guiContext);
            }
        }
        super.drawBackgroundOverlay(guiContext);
    }

    /** Draws the move/grab icon at the cursor in screen space (mirrors {@link WindowDragHelper#drawResizeIcon}). */
    private void drawMoveIcon(GUIContext guiContext) {
        guiContext.postRendering(ctx -> {
            ctx.pose.pushPose();
            ctx.pose.setIdentity();
            var icon = Icons.MOVE;
            ctx.drawTexture(icon,
                    ctx.mouseX - 6,
                    ctx.mouseY - 6,
                    12,
                    12);
            ctx.pose.popPose();
        });
    }
}
