package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneObject;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.TransformRef;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.math.Transform;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TransformRefConfigurator extends ValueConfigurator<TransformRef> {
    public final TextField textField;

    public TransformRefConfigurator(String name, Supplier<TransformRef> supplier, Consumer<TransformRef> onUpdate, @Nonnull TransformRef defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);

        if (value == null) value = defaultValue;
        inlineContainer.addChild(textField = new TextField());
        textField.setTextValidator(v -> {
            try {
                UUID.fromString(v);
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        textField.setTextResponder(v -> {
            try {
                updateValueActively(new TransformRef(UUID.fromString(v)));
            } catch (Exception ignored) {}
        });
        textField.setText(value.toString(), false);
        textField.setFocusable(false);
    }

    @Override
    protected boolean canDropObject(@Nonnull Object object) {
        return object instanceof Transform || object instanceof UUID || object instanceof ISceneObject || super.canDropObject(object);
    }

    @Override
    protected void onDropObject(@NotNull Object object) {
        switch (object) {
            case Transform transform -> {
                onValueUpdatePassively(new TransformRef(transform));
                updateValue();
            }
            case UUID uuid -> {
                onValueUpdatePassively(new TransformRef(uuid));
                updateValue();
            }
            case ISceneObject sceneObject -> {
                onValueUpdatePassively(new TransformRef(sceneObject.id()));
                updateValue();
            }
            default -> super.onDropObject(object);
        }
    }

    @Override
    protected void onValueUpdatePassively(TransformRef newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        textField.setText(newValue.toString(), false);
    }

}
