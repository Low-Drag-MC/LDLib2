package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wiget;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.UISoundUtils;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.util.RenameColorConfigurableHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.StickyNoteModel;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.NotNull;

public class StickyNoteElement extends GraphElement<StickyNoteModel> {
    public static final String STICKY_NOTE_LAYER = "StickyNote";
    private static final float HEADER_HEIGHT = 14f;

    private UIElement header;
    private UIElement folderIcon;
    private TextArea textArea;

    public StickyNoteElement(StickyNoteModel model) {
        super(model);
        addClass("__sticky-note__");
    }

    @Override
    public String getLayerName() {
        return STICKY_NOTE_LAYER;
    }

    @Override
    protected void buildUI() {
        var model = getModel();
        // Position / width / background color come from the model — pin via IMPORTANT.
        Style.importantPipeline(getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE)
                .left(model.getPosition().x)
                .top(model.getPosition().y)
                .width(model.getSize().x));
        Style.importantPipeline(getStyle(), s -> s.background(SDFRectTexture.of(model.getElementColor())));

        // Header bar
        header = new UIElement().addClass("__sticky-note_header__");
        Style.defaultPipeline(header.getLayout(), l -> l.widthPercent(100).height(HEADER_HEIGHT)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER));

        folderIcon = new UIElement().addClass("__sticky-note_folder-icon__");
        Style.defaultPipeline(folderIcon.getLayout(), l -> l.width(8).height(8).marginAll(2));
        folderIcon.addEventListener(UIEvents.MOUSE_DOWN, this::onHeaderClick);
        header.addChild(folderIcon);

        addChild(header);

        // Text area
        textArea = new TextArea();
        textArea.addClass("__sticky-note_text-area__");
        textArea.setValue(model.getContent().split("\n", -1));
        Style.defaultPipeline(textArea.getLayout(), l -> l.widthPercent(100).flexGrow(1));
        textArea.textAreaStyle(style -> {
            Style.importantPipeline(style, s -> {
                s.textColor(0xFF000000);
                s.textShadow(false);
            });
            // fontSize is read from the model — data-driven.
            Style.importantPipeline(style, s -> s.fontSize(model.getFontSize()));
        });
        Style.importantPipeline(textArea.contentView.getStyle(), s -> s.background(IGuiTexture.EMPTY));
        textArea.setLinesResponder(lines -> {
            var content = String.join("\n", lines);
            model.setContent(content);
        });
        addChild(textArea);

        applyCollapsedState(model.isCollapsed());
    }

    private void onHeaderClick(UIEvent event) {
        if (event.button == 0) {
            var model = getModel();
            model.setCollapsed(!model.isCollapsed());
            applyCollapsedState(model.isCollapsed());
            UISoundUtils.playButtonClickSound();
            event.stopPropagation();
        }
    }

    private void applyCollapsedState(boolean collapsed) {
        var model = getModel();
        // Arrow icon, text-area visibility, and height all flip with collapse state — pin via IMPORTANT.
        Style.importantPipeline(folderIcon.getStyle(), s -> s.backgroundTexture(collapsed
                ? Icons.RIGHT_ARROW_NO_BAR_S_LIGHT
                : Icons.DOWN_ARROW_NO_BAR_S_LIGHT));
        Style.importantPipeline(textArea.getLayout(), l -> l.display(collapsed ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
        Style.importantPipeline(getLayout(), l -> l.height(collapsed ? HEADER_HEIGHT : model.getSize().y));
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        var model = getModel();
        if (visitor.hasHint(ChangeHint.LAYOUT)) {
            Style.importantPipeline(getLayout(), l -> l.left(model.getPosition().x)
                    .top(model.getPosition().y)
                    .width(model.getSize().x));
            applyCollapsedState(model.isCollapsed());
        }
        if (visitor.hasHint(ChangeHint.STYLE)) {
            Style.importantPipeline(getStyle(), s -> s.background(SDFRectTexture.of(model.getElementColor())));
            if (textArea != null) {
                textArea.textAreaStyle(style -> Style.importantPipeline(style, s -> s.fontSize(model.getFontSize())));
            }
        }
        if (visitor.hasHint(ChangeHint.DATA)) {
            if (textArea != null) {
                var currentContent = String.join("\n", textArea.getValue());
                if (!currentContent.equals(model.getContent())) {
                    textArea.setValue(model.getContent().split("\n", -1));
                }
            }
        }
    }

    @Override
    protected void onSelectionInspect(GraphInspector inspector) {
        super.onSelectionInspect(inspector);
        if (graphView != null) inspector.setHistoryStack(graphView.getHistoryStack());
        // StickyNote isn't renamable (its content is the body, not a name) — helper handles that
        // automatically: only the color row will be added because Capabilities.RENAMABLE is absent.
        inspector.inspect(RenameColorConfigurableHelper.build(getModel(), graphView));
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
        super.drawBackgroundOverlay(guiContext);
    }
}
