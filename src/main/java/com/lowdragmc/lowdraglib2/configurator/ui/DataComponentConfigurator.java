package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.mojang.serialization.DataResult;
import dev.vfyjxf.taffy.style.TaffyDirection;
import lombok.Getter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.appliedenergistics.yoga.YogaDirection;
import org.appliedenergistics.yoga.YogaOverflow;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings({"unchecked", "rawtypes"})
public class DataComponentConfigurator extends ConfiguratorGroup {
    public final Button resetButton = new Button();
    @Getter
    private DataComponentMap prototype;
    private final Supplier<DataComponentPatch> supplier;
    private final Consumer<DataComponentPatch> consumer;
    private final boolean forceUpdate;

    public DataComponentConfigurator(DataComponentMap prototype, Supplier<DataComponentPatch> supplier, Consumer<DataComponentPatch> consumer, boolean forceUpdate) {
        super("configurator.data_component");
        this.supplier = supplier;
        this.consumer = consumer;
        this.forceUpdate = forceUpdate;
        setPrototype(prototype);
        this.resetButton.setText("configurator.reset_prototype").setOnClick(e -> {
            resetPrototype();
            e.stopPropagation();
        });
        this.resetButton.text.layout(layout -> layout.widthPercent(100));
        this.resetButton.text.textStyle(textStyle -> textStyle.adaptiveWidth(false).textWrap(TextWrap.HOVER_ROLL)).setOverflowVisible(false);
        this.inlineContainer.layout(layout -> layout.direction(TaffyDirection.RTL));
        this.inlineContainer.addChild(resetButton);
    }

    public void resetPrototype() {
        var builder = DataComponentPatch.builder();
        for (var typedDataComponent : prototype) {
            DataComponentType type = typedDataComponent.type();
            var value = prototype.get(type);
            if (value == null) builder.remove(type);
            else builder.set(type, value);
        }
        consumer.accept(builder.build());
        notifyChanges();
    }

    public DataComponentConfigurator setPrototype(DataComponentMap prototype) {
        if (this.prototype == prototype) return this;
        this.prototype = prototype;
        removeAllConfigurators();
        var opWithRegistry = Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE);
        // registered data components
        for (var typedDataComponent : prototype) {
            DataComponentType type = typedDataComponent.type();
            if (type.isTransient()) continue;
            var key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (key == null) continue;
            var tagConfigurator = new TagConfigurator(key.getPath(), () -> {
                var valueOpt = supplier.get().get(type);
                var value = valueOpt == null ? prototype.get(type) : valueOpt.orElse(null);
                if (value == null) return EndTag.INSTANCE;
                DataResult<Tag> result = type.codec().encodeStart(opWithRegistry, value);
                return result.result().orElse(EndTag.INSTANCE);
            }, tag -> {
                var value = type.codec().parse(opWithRegistry, tag).result().orElse(null);
                var patch = supplier.get();
                var builder = DataComponentPatch.builder();
                for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
                    if (entry.getKey() == type) continue;
                    var t = (DataComponentType)entry.getKey();
                    var v = entry.getValue().orElse(null);
                    if (v == null) builder.remove(t);
                    else builder.set(t, v);
                }
                if (value == null) builder.remove(type);
                else builder.set(type, value);
                consumer.accept(builder.build());
            }, EndTag.INSTANCE, forceUpdate);
            tagConfigurator.tagField.setTagValidator(tag -> type.codec().parse(opWithRegistry, tag).isSuccess());
            addConfigurator(tagConfigurator);
        }
        // additional data component
        var typeValues = new ArrayList<TypedDataComponent>();
        for (var entry : supplier.get().entrySet()) {
            if (prototype.has(entry.getKey())) continue;
            typeValues.add(TypedDataComponent.createUnchecked(entry.getKey(), entry.getValue().orElse(null)));
        }
        var arrayGroup = new ArrayConfiguratorGroup<>("configurator.additional", true,
                () -> typeValues,
                (getter, setter) ->
                        new TypedDataComponentConfigurator("", getter, setter, getter.get(), forceUpdate), forceUpdate);
        arrayGroup.setTips("configurator.additional.tips");
        arrayGroup.setAddDefault(() -> {
            var type = BuiltInRegistries.DATA_COMPONENT_TYPE.stream()
                    .filter(t -> !t.isTransient() &&
                            supplier.get().entrySet().stream().noneMatch(entry -> entry.getKey() == t))
                    .findAny();
            return TypedDataComponent.createUnchecked(type.orElse(null), null);
        });
        arrayGroup.setOnUpdate(list -> {
            if (typeValues.equals(list)) return;
            typeValues.clear();
            typeValues.addAll(list);
            var builder = DataComponentPatch.builder();
            for (Map.Entry<DataComponentType<?>, Optional<?>> entry : supplier.get().entrySet()) {
                if (prototype.has(entry.getKey())) {
                    var value = entry.getValue().orElse(null);
                    if (value == null) builder.remove(entry.getKey());
                    else builder.set((DataComponentType)entry.getKey(), value);
                }
            }
            typeValues.forEach(component -> {
                if (component.type() != null) {
                    if (component.value() == null) builder.remove(component.type());
                    else builder.set(component.type(), component.value());
                }
            });
            consumer.accept(builder.build());
        });
//        arrayGroup.addEventListener(UIEvents.TICK, event -> {
//            var count = 0;
//            var dirty = false;
//            for (var entry : supplier.get().entrySet()) {
//                if (prototype.has(entry.getKey())) continue;
//                if (!typeValues.contains(TypedDataComponent.createUnchecked(entry.getKey(), entry.getValue().orElse(null)))) {
//                    dirty = true;
//                    break;
//                }
//                count++;
//            }
//            dirty |= count != typeValues.size();
//            if (dirty) {
//                typeValues.clear();
//                for (var entry : supplier.get().entrySet()) {
//                    if (prototype.has(entry.getKey())) continue;
//                    typeValues.add(TypedDataComponent.createUnchecked(entry.getKey(), entry.getValue().orElse(null)));
//                }
//            }
//        });
        addConfigurator(arrayGroup);
        return this;
    }



}
