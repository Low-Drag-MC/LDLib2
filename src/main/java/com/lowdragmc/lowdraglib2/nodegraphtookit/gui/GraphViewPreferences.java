package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wire.WireRouteStyle;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How a graph editor was last set up, remembered per graph type — which one a user wants depends
 * on what they are editing, so an execution flow and a dense material graph can each keep their own.
 *
 * <p>Deliberately none of the other three places this could live:</p>
 * <ul>
 *     <li><b>Not the graph file.</b> These are not part of the document. Writing them there would
 *     make opening a graph and looking at it dirty the file, put a view setting into undo history,
 *     and turn "I prefer curved wires" into a change that follows the asset to everyone else.</li>
 *     <li><b>Not the client config.</b> That is one value for everything; this is one per graph
 *     type, which is the whole point.</li>
 *     <li><b>Not the editor settings file.</b> A graph view is not always inside an editor.</li>
 * </ul>
 *
 * <p>Keyed by the graph's class name. Renaming a graph class quietly resets its preferences, which
 * is the right trade for not needing a registry of graph types that does not otherwise exist.</p>
 */
public final class GraphViewPreferences {

    /**
     * One graph type's remembered view setup.
     *
     * <p><b>Nothing in here may reference the enclosing class.</b> {@link #CODEC} below mentions
     * {@link #CODEC Entry.CODEC}, so the two initialise each other — and which one wins depends on
     * which is touched first. A client touches {@link #DEFAULTS} first, through
     * {@link GraphView}'s field initialisers; the enclosing class then initialises <em>during</em>
     * this record's own initialiser, reads an {@code Entry.CODEC} that is still {@code null}, and
     * every write afterwards dies with a null element codec. Keeping this side of the pair
     * self-contained is what makes the order stop mattering.</p>
     */
    public record Entry(boolean snapToGrid, float gridSnapSize, boolean snapToElements, WireRouteStyle wireStyle) {

        public static final Entry DEFAULTS = new Entry(true, 16f, true, WireRouteStyle.DEFAULT);

        /**
         * Stored by name, and an unknown name reads as the default rather than as a parse failure —
         * a style removed in a later build must not take the rest of the file down with it.
         */
        private static final Codec<WireRouteStyle> WIRE_STYLE = Codec.STRING.xmap(
                name -> {
                    for (var style : WireRouteStyle.values()) {
                        if (style.name().equals(name)) return style;
                    }
                    return WireRouteStyle.DEFAULT;
                },
                Enum::name);

        /**
         * Every field optional, falling back to the default: a file written by an older build is
         * missing the fields added since, and a preferences file is never worth failing to open an
         * editor over.
         *
         * <p>Optional fields are also <em>omitted</em> when they match the default, so a file only
         * ever names what somebody actually changed. The consequence worth knowing: changing a
         * default here moves everyone who had left that setting alone, which is the intent — a
         * preference nobody set is not a preference.</p>
         */
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("snap_to_grid", DEFAULTS.snapToGrid()).forGetter(Entry::snapToGrid),
                Codec.FLOAT.optionalFieldOf("grid_size", DEFAULTS.gridSnapSize()).forGetter(Entry::gridSnapSize),
                Codec.BOOL.optionalFieldOf("snap_to_elements", DEFAULTS.snapToElements()).forGetter(Entry::snapToElements),
                WIRE_STYLE.optionalFieldOf("wire_style", DEFAULTS.wireStyle()).forGetter(Entry::wireStyle)
        ).apply(instance, Entry::new));
    }

    static final Codec<Map<String, Entry>> CODEC = Codec.unboundedMap(Codec.STRING, Entry.CODEC);

    /** Resolved on first use, never in a static initialiser: {@link FMLLoader} has no game path in a unit test. */
    @Nullable
    private static Path file;
    @Nullable
    private static Map<String, Entry> cache;

    private GraphViewPreferences() {
    }

    /** Points the store at another file and forgets what it had read. For tests. */
    public static void setFile(@Nullable Path path) {
        file = path;
        cache = null;
    }

    public static Path getFile() {
        if (file == null) {
            file = FMLLoader.getGamePath().resolve("config").resolve(LDLib2.MOD_ID).resolve("graph_view.json");
        }
        return file;
    }

    public static Entry get(Class<?> graphType) {
        return entries().getOrDefault(keyOf(graphType), Entry.DEFAULTS);
    }

    /** Records this type's setup and writes the file straight away. */
    public static void put(Class<?> graphType, Entry entry) {
        var entries = entries();
        var key = keyOf(graphType);
        if (entry.equals(entries.get(key))) return;
        entries.put(key, entry);
        write(entries);
    }

    static String keyOf(Class<?> graphType) {
        return graphType.getName();
    }

    /**
     * The whole store, read from disk the first time anything asks.
     *
     * <p>Read once and then kept, rather than re-read per editor open: every write goes through
     * {@link #put} and updates both, so the copy in memory is the authority for this process and
     * the file only has to be believed at startup.</p>
     */
    private static Map<String, Entry> entries() {
        if (cache == null) cache = read();
        return cache;
    }

    private static Map<String, Entry> read() {
        var path = getFile();
        if (!Files.exists(path)) return new LinkedHashMap<>();
        try (var reader = Files.newBufferedReader(path)) {
            return CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                    .resultOrPartial(error -> LDLib2.LOGGER.error("Failed to read {}: {}", path, error))
                    .<Map<String, Entry>>map(LinkedHashMap::new)
                    .orElseGet(LinkedHashMap::new);
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to read graph view preferences from {}", path, e);
            return new LinkedHashMap<>();
        }
    }

    private static void write(Map<String, Entry> entries) {
        var path = getFile();
        try {
            var parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            var json = CODEC.encodeStart(JsonOps.INSTANCE, entries)
                    .resultOrPartial(error -> LDLib2.LOGGER.error("Failed to encode {}: {}", path, error));
            if (json.isPresent()) Files.writeString(path, json.get().toString());
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to write graph view preferences to {}", path, e);
        }
    }
}
