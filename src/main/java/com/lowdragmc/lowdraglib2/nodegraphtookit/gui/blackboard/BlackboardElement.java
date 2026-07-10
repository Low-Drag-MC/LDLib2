package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.blackboard;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.ElementRenameColorCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IHasName;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;


public class BlackboardElement extends ModelElement {
    @Getter @Setter(AccessLevel.PROTECTED)
    private Blackboard blackboard;

    /** Inline edit field shown in place of a title label during a double-click rename. */
    @Nullable
    private TextField inlineRenameField;

    @Override
    protected void onSelectionChanged() {
        if (blackboard != null) {
            blackboard.onSelectionChanged();
        }
    }

    @Override
    public boolean canBeRegionSelected(Vector4f region) {
        return false;
    }

    /**
     * Wires a title {@code label} for double-click inline renaming: the label is swapped for a
     * {@link TextField} that commits on Enter / focus-loss and cancels on Escape. Renames go through
     * the undoable {@link ElementRenameColorCommands.RenameElementCommand}.
     */
    protected void enableInlineRename(Label label) {
        label.addEventListener(UIEvents.DOUBLE_CLICK, e -> {
            startInlineRename(label);
            e.stopPropagation();
        });
    }

    private void startInlineRename(Label label) {
        if (inlineRenameField != null) return;
        if (!(getModel() instanceof GraphElementModel model) || !model.isRenamable()
                || !(model instanceof IHasName named)) return;
        var parent = label.getParent();
        if (parent == null) return;
        var initial = named.getName();
        // Force-hide the label while the inline edit field is in place.
        Style.importantPipeline(label.getLayout(), l -> l.display(TaffyDisplay.NONE));
        var field = new TextField();
        inlineRenameField = field;
        field.setText(initial == null ? "" : initial, false);
        Style.defaultPipeline(field.getLayout(), l -> l.flex(1).height(10));

        final boolean[] done = {false};
        Runnable commit = () -> {
            if (done[0]) return;
            done[0] = true;
            var newName = field.getValue();
            if (newName != null && !newName.isBlank() && !newName.equals(initial)) {
                if (graphView != null) {
                    graphView.dispatchCommand(new ElementRenameColorCommands.RenameElementCommand(model, newName));
                } else {
                    named.setName(newName);
                }
            }
            endInlineRename(label);
        };
        Runnable cancel = () -> {
            if (done[0]) return;
            done[0] = true;
            endInlineRename(label);
        };

        field.addEventListener(UIEvents.KEY_DOWN, ev -> {
            if (ev.keyCode == GLFW.GLFW_KEY_ENTER || ev.keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                commit.run();
                ev.stopPropagation();
            } else if (ev.keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancel.run();
                ev.stopPropagation();
            }
        });
        field.addEventListener(UIEvents.BLUR, ev -> commit.run());

        // Insert where the label sat so the edit field lines up with the title.
        int index = parent.getChildren().indexOf(label);
        if (index < 0) {
            parent.addChild(field);
        } else {
            parent.addChildAt(field, index);
        }
        field.focus();
    }

    private void endInlineRename(Label label) {
        if (inlineRenameField != null) {
            inlineRenameField.removeSelf();
            inlineRenameField = null;
        }
        // Clear the IMPORTANT display override so the label is visible again, and re-sync its text.
        Style.importantPipeline(label.getLayoutStyle(), l -> l.display((TaffyDisplay) null));
        if (getModel() instanceof IHasName named) {
            label.setText(named.getName());
        }
    }
}
