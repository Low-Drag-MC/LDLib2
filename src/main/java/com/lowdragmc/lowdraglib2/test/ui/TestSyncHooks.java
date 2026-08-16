package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.function.Consumers;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Exercises the sync lifecycle hooks on {@link DataBindingBuilder}, plus the initial data pack's
 * framing. Driven by {@code SyncHookScenario}.
 *
 * <p>Two bindings carry the hooks, one per direction:
 * <ul>
 *     <li>{@code counter} is S2C only. The button bumps it <b>on the server</b>, so the server is the
 *         sending side and the client is the receiving side.</li>
 *     <li>{@code flag} is two-way. Flipping the switch makes the <b>client</b> the sending side.</li>
 * </ul>
 *
 * <p>A third group — the {@code poison} value and the three probes after it — guards the initial
 * data pack instead. Every hook bumps a {@link Tally} rather than only logging, so the scenario can
 * assert which side fired what. Client and server each build their own instance of this class through
 * {@code PlayerUIMenuType}; {@link #CLIENT} / {@link #SERVER} hand the scenario both of them.
 */
@LDLRegister(name = "ui_sync_hooks", registry = "ldlib2:menu_test")
@ParametersAreNonnullByDefault
public class TestSyncHooks implements IMenuTest {
    /** The instance the client built for the currently open menu. */
    @Nullable
    public static volatile TestSyncHooks CLIENT = null;
    /** The instance the server built for the currently open menu. */
    @Nullable
    public static volatile TestSyncHooks SERVER = null;

    /** One counter per hook kind, so an assertion can name the side it expects to have fired. */
    public static final class Tally {
        public final AtomicInteger beforeSync = new AtomicInteger();
        public final AtomicInteger afterSync = new AtomicInteger();
        public final AtomicInteger received = new AtomicInteger();
        public final AtomicInteger remoteReceived = new AtomicInteger();
        public final AtomicInteger serverReceived = new AtomicInteger();

        /** Installs one counting hook of every kind. */
        <V> DataBindingBuilder<V> install(DataBindingBuilder<V> builder) {
            return builder
                    .onBeforeSync(value -> beforeSync.incrementAndGet())
                    .onAfterSync(value -> afterSync.incrementAndGet())
                    .onSyncReceived(value -> received.incrementAndGet())
                    .onRemoteSyncReceived(value -> remoteReceived.incrementAndGet())
                    .onServerSyncReceived(value -> serverReceived.incrementAndGet());
        }
    }

    public final Tally counterHooks = new Tally();
    public final Tally flagHooks = new Tally();

    private int counter = 0;
    private boolean flag = false;
    // Initial-data probes. The server seeds distinctive values in init(); the client starts at the
    // defaults below and only ever gets them through the initial data pack.
    private int probeA = 0;
    private String probeB = "";
    private int probeC = 0;

    public int getCounter() {
        return counter;
    }

    public boolean isFlag() {
        return flag;
    }

    public int getProbeA() {
        return probeA;
    }

    public String getProbeB() {
        return probeB;
    }

    public int getProbeC() {
        return probeC;
    }

    @Override
    public void init(Player player) {
        if (player.level().isClientSide()) {
            CLIENT = this;
        } else {
            SERVER = this;
            probeA = 7;
            probeB = "hello";
            probeC = 4242;
        }
    }

    /**
     * A sync value with no widget behind it, mirrored into a field on arrival.
     *
     * <p>The mirror is not optional bookkeeping: on the client {@code bind()} installs the element
     * itself as the data source, so the builder's getter / setter only ever run on the server and
     * the field would otherwise stay at its default forever.
     */
    private <V> UIElement carrier(DataBindingBuilder<V> builder, Consumer<V> mirror) {
        return new BindableValue<V>().bind(builder.onRemoteSyncReceived(mirror).build());
    }

    @Override
    public ModularUI createUI(Player player) {
        var root = new UIElement().setId("root");
        root.layout(layout -> {
            layout.width(200);
            layout.height(120);
            layout.paddingAll(10);
        });
        root.getStyle().backgroundTexture(Sprites.BORDER);

        var counterLabel = new Button().setText("counter: ?");
        counterLabel.setId("counter_label");

        root.addChildren(
                counterLabel,
                // A C2S-only value, registered ahead of the probes below. The server has nothing to
                // say about it and the client refuses to accept it, so it must not appear in the
                // initial data pack at all - if it does, everything after it misparses.
                new BindableValue<String>().bind(DataBindingBuilder
                        .string(() -> "poison", Consumers.nop())
                        .name("poison")
                        .s2cStrategy(SyncStrategy.NONE)
                        .build()),
                // The probes. Their only job is to arrive intact.
                carrier(DataBindingBuilder.intValS2C(() -> probeA).name("probe_a"),
                        value -> probeA = value == null ? -1 : value),
                carrier(DataBindingBuilder.stringS2C(() -> probeB).name("probe_b"),
                        value -> probeB = value == null ? "<null>" : value),
                carrier(DataBindingBuilder.intValS2C(() -> probeC).name("probe_c"),
                        value -> probeC = value == null ? -1 : value),

                carrier(counterHooks.install(DataBindingBuilder.intValS2C(() -> counter).name("hook_counter")),
                        value -> {
                            counter = value == null ? 0 : value;
                            counterLabel.setText("counter: " + counter);
                        }),
                new Button().setText("counter +1")
                        .setOnServerClick(e -> counter++)
                        .setId("bump_button"),
                new Switch().bind(flagHooks
                        .install(DataBindingBuilder.bool(() -> flag, value -> flag = value).name("hook_flag"))
                        // Same reason as carrier()'s mirror, for the other direction: on the client the
                        // Switch is the data source, so this is what keeps isFlag() meaningful there.
                        .onBeforeSync(value -> flag = Boolean.TRUE.equals(value))
                        .build()).setId("flag_switch")
        );

        return new ModularUI(UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))), player);
    }
}
