package com.lowdragmc.lowdraglib2.client.window;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_EventFilter;
import org.lwjgl.sdl.SDL_WindowEvent;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Takes every SDL event addressed to one of our windows out of the process-wide queue before vanilla
 * sees it.
 *
 * <p>This is not optional. SDL delivers the events of every window through the one queue that
 * {@code SDLEventHandler} drains, and while vanilla's keyboard and mouse handlers check which window
 * an event is for, {@code Window#handleEvent} does not: a close request on a second window would
 * quit the game, and its size events would resize the game's own framebuffer.
 *
 * <p>An event filter rather than a mixin on the poll loop, for two reasons. It runs as the event is
 * queued, so nothing vanilla does to the queue afterwards — {@code SDLEventHandler#flushInputEvents}
 * throws every pending input event away when the game pumps during loading — can lose an event meant
 * for us. And it needs no hook into vanilla code at all.
 *
 * <p>Two windows of time need more than the filter, because the window's id is not known to it yet
 * or no longer: the events SDL queues while it creates a window, which {@link #register} sweeps out of
 * the queue once the id is known, and the ones it queues while destroying it — a focus loss, the
 * destroyed notice — which {@link #retire} catches by remembering the id afterwards. Both kinds are
 * exactly the ones that would otherwise land on the game's window.
 *
 * <p>SDL has room for one filter. If something had already installed one it is chained, not
 * replaced: every event that is not ours is handed to it and its answer returned. It is called
 * through {@link JNI} rather than wrapped as an LWJGL callback, because a filter installed from
 * native code is not an LWJGL callback and cannot be wrapped as one. Once installed ours stays for
 * the rest of the session, so an id retired a moment ago is still recognised.
 */
final class OsWindowEvents {

    /**
     * Windows by SDL window id. Written only on the render thread; read from the filter, which SDL may
     * call on whichever thread queues an event — for window events that is the render thread too,
     * which is where the platform's event pump runs, but the collections are synchronized rather than
     * trusting it.
     */
    private static final Int2ObjectMap<OsWindow> WINDOWS = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());
    /** Ids of windows already destroyed, whose stragglers are still ours to swallow. SDL never reuses one. */
    private static final IntSet RETIRED = IntSets.synchronize(new IntOpenHashSet());

    @Nullable
    private static SDL_EventFilter filter;
    @Nullable
    private static SDL_EventFilter sweep;
    private static long previousFilter;
    private static long previousUserdata;

    private OsWindowEvents() {
    }

    /**
     * Puts the filter in place. Called before a window is created, so nothing SDL queues for it during
     * creation can reach the game unseen.
     */
    static void install() {
        RenderSystem.assertOnRenderThread();
        if (filter != null) return;
        try (var stack = MemoryStack.stackPush()) {
            var previous = stack.mallocPointer(1);
            var userdata = stack.mallocPointer(1);
            if (SDLEvents.SDL_GetEventFilter(previous, userdata)) {
                previousFilter = previous.get(0);
                previousUserdata = userdata.get(0);
                LDLib2.LOGGER.debug("[os-window] chaining the SDL event filter that was already installed");
            }
        }
        filter = SDL_EventFilter.create(OsWindowEvents::filter);
        sweep = SDL_EventFilter.create((userdata, eventAddress) -> !routeIfOurs(eventAddress));
        SDLEvents.SDL_SetEventFilter(filter, MemoryUtil.NULL);
    }

    /**
     * Starts routing {@code window}'s events, including any SDL queued while creating it.
     */
    static void register(OsWindow window) {
        RenderSystem.assertOnRenderThread();
        install();
        WINDOWS.put(window.id(), window);
        sweepQueue();
    }

    /**
     * Stops delivering to {@code window} and swallows its events from now on — including those its
     * destruction has just queued.
     */
    static void retire(OsWindow window) {
        RenderSystem.assertOnRenderThread();
        RETIRED.add(window.id());
        WINDOWS.remove(window.id());
        sweepQueue();
    }

    /** Runs the queue past {@link #routeIfOurs}, which takes ours out and leaves the rest in place. */
    private static void sweepQueue() {
        var current = sweep;
        if (current != null) {
            SDLEvents.SDL_FilterEvents(current, MemoryUtil.NULL);
        }
    }

    /**
     * @return whether SDL should queue the event: false for ours, which are consumed here
     */
    private static boolean filter(long userdata, long eventAddress) {
        if (routeIfOurs(eventAddress)) return false;
        var previous = previousFilter;
        return previous == MemoryUtil.NULL || JNI.invokePPZ(previousUserdata, eventAddress, previous);
    }

    /**
     * Hands the event to the window it is addressed to, if that window is one of ours.
     *
     * @return whether it was ours — delivered, or a straggler of a window already destroyed
     */
    private static boolean routeIfOurs(long eventAddress) {
        var id = windowIdOf(eventAddress);
        if (id == 0) return false;
        var window = WINDOWS.get(id);
        if (window != null) {
            try {
                window.accept(SDL_Event.create(eventAddress));
            } catch (Throwable throwable) {
                // An exception must not unwind into native code.
                LDLib2.LOGGER.error("[os-window] failed to record an event", throwable);
            }
            return true;
        }
        return RETIRED.contains(id);
    }

    /**
     * The id of the window an event is addressed to, or 0 for one addressed to none.
     *
     * <p>Read from the event itself rather than through {@code SDL_GetWindowFromEvent}, which looks the
     * window up and so answers nothing for a window being destroyed. Every event SDL addresses to a
     * window — window, keyboard, text, mouse and drop events alike — carries the id at the same
     * offset, straight after the type and timestamp; the device events interleaved with them
     * (keymap changed, a mouse plugged in) carry none and are left alone.
     */
    private static int windowIdOf(long eventAddress) {
        var type = SDL_Event.ntype(eventAddress);
        var addressed = (type >= SDLEvents.SDL_EVENT_WINDOW_FIRST && type <= SDLEvents.SDL_EVENT_WINDOW_LAST)
                || (type >= SDLEvents.SDL_EVENT_KEY_DOWN && type <= SDLEvents.SDL_EVENT_TEXT_INPUT)
                || type == SDLEvents.SDL_EVENT_TEXT_EDITING_CANDIDATES
                || (type >= SDLEvents.SDL_EVENT_MOUSE_MOTION && type <= SDLEvents.SDL_EVENT_MOUSE_WHEEL)
                || (type >= SDLEvents.SDL_EVENT_DROP_FILE && type <= SDLEvents.SDL_EVENT_DROP_POSITION);
        return addressed ? SDL_WindowEvent.nwindowID(eventAddress) : 0;
    }
}
