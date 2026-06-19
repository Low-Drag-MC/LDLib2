package com.lowdragmc.lowdraglib2.gui.sync.bindings.impl;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.gui.util.ITickable;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@Data(staticConstructor = "of")
@KJSBindings
public final class ScrollDataSource<T> implements IDataProvider<T>, ITickable, IPausable {
    @Getter
    private final List<T> data;
    private final List<Consumer<T>> listeners = new ArrayList<>();
    private volatile T lastValue;
    @Setter @Getter @Accessors(chain = true, fluent = true)
    private int frequency = 20;
    // runtime
    @Getter
    private boolean paused = false;
    @Nullable
    private T current;
    private int counter = 0;

    private ScrollDataSource(List<T> data) {
        this.data = data;
        this.current = data.isEmpty() ? null : data.getFirst();
    }

    public <D> ScrollDataSource<D> map(Function<T, D> mapper) {
        return ScrollDataSource.of(data.stream().map(mapper).toList());
    }

    @Override
    public ISubscription registerListener(Consumer<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public T getValue() {
        return current;
    }

    public void checkUpdate() {
        T currentValue = getValue();
        if (!Objects.equals(lastValue, currentValue)) {
            lastValue = currentValue;
            listeners.forEach(l -> l.accept(currentValue));
        }
    }

    @Override
    public void tick() {
        if (paused) return;
        counter++;
        if (frequency > 1) {
            if (counter % frequency != 0) return;
        }

        if (data.isEmpty()) {
            current = null;
        } else {
            int step = counter / frequency;
            current = data.get(step % data.size());
        }
        checkUpdate();

        if (counter > 1_000_000_000) {
            counter = 0;
        }
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
    }
}
