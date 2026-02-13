package com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph;

import java.util.UUID;

/**
 * Provides functionality needed to access, and perform operations on, graph assets.
 *
 * <p>The {@code GraphDatabase} class is similar to an asset database, but tailored for graph-based tools.
 * Use this class to create, load, and save {@link Graph} instances and their associated assets.</p>
 *
 * <p>This API supports typical asset workflows such as creating new graph assets, accessing graphs by path or ID,
 * and ensuring changes to graph data are saved.</p>
 *
 * <p><strong>Note:</strong> The internal persistence implementation is left as TODO for users to implement
 * according to their platform's requirements.</p>
 */
public final class GraphDatabase {

    private GraphDatabase() {
        // Static utility class - no instantiation
    }

    /**
     * The persistence provider used for loading and saving graphs.
     * TODO: Implement this interface according to your persistence requirements.
     */
    private static GraphPersistenceProvider persistenceProvider;

    /**
     * Sets the persistence provider used for graph operations.
     *
     * @param provider the persistence provider to use
     */
    public static void setPersistenceProvider(GraphPersistenceProvider provider) {
        persistenceProvider = provider;
    }

    /**
     * Gets the current persistence provider.
     *
     * @return the persistence provider, or {@code null} if not set
     */
    public static GraphPersistenceProvider getPersistenceProvider() {
        return persistenceProvider;
    }

    /**
     * Creates a new graph asset of the specified type at the given path.
     *
     * <p>Use this method to programmatically create a new graph asset of the specified type at a specific location.
     * If an asset already exists at the specified path, this method may overwrite it depending on the
     * persistence provider implementation.</p>
     *
     * @param <T> the type of graph to create (must have a public no-arg constructor)
     * @param graphType the class of the graph type to create
     * @param assetPath the path for the new asset
     * @return the created graph instance
     * @throws IllegalStateException if no persistence provider is configured
     * @throws IllegalArgumentException if the path or type is invalid
     */
    public static <T extends Graph> T createGraph(Class<T> graphType, String assetPath) {
        checkPersistenceProvider();
        return persistenceProvider.createGraph(graphType, assetPath);
    }

    /**
     * Loads a graph of the specified type from the asset at the given path.
     *
     * <p>Use this method to load a graph asset of the specified type from a given asset path.
     * This method returns the graph object currently loaded in memory, which might differ from the version
     * on disk if the asset was modified or opened in an editor.</p>
     *
     * @param <T> the type of graph to load
     * @param graphType the class of the graph type to load
     * @param assetPath the path to the graph asset
     * @return the loaded graph instance, or {@code null} if no matching graph is found
     * @throws IllegalStateException if no persistence provider is configured
     */
    public static <T extends Graph> T loadGraph(Class<T> graphType, String assetPath) {
        checkPersistenceProvider();
        return persistenceProvider.loadGraph(graphType, assetPath);
    }

    /**
     * Saves the asset of the specified graph to disk if it has unsaved changes.
     *
     * <p>Use this method to persist any pending modifications made to a graph instance.
     * It prevents data loss by ensuring the asset on disk reflects the in-memory graph state.
     * This method only performs a save if the graph is marked dirty.</p>
     *
     * @param graph the graph to save
     * @throws IllegalStateException if no persistence provider is configured
     */
    public static void saveGraphIfDirty(Graph graph) {
        checkPersistenceProvider();
        persistenceProvider.saveGraphIfDirty(graph);
    }

    /**
     * Retrieves the globally unique identifier (UUID) for the asset associated with the specified graph.
     *
     * <p>Use this method to get a persistent identifier for a graph asset. The UUID allows reliable tracking,
     * referencing, and linking to graph assets across different sessions.</p>
     *
     * @param graph the graph whose asset UUID you want to retrieve
     * @return the UUID of the graph asset, or {@code null} if not available
     */
    public static UUID getGraphAssetId(Graph graph) {
        if (persistenceProvider == null) {
            return null;
        }
        return persistenceProvider.getGraphAssetId(graph);
    }

    /**
     * Retrieves the file path of the asset associated with the specified graph.
     *
     * <p>Use this method to get the path of the graph asset within the project.</p>
     *
     * @param graph the graph whose asset path you want to retrieve
     * @return the asset's file path, or an empty string if not available
     */
    public static String getGraphAssetPath(Graph graph) {
        if (persistenceProvider == null) {
            return "";
        }
        return persistenceProvider.getGraphAssetPath(graph);
    }

    /**
     * Checks that a persistence provider is configured.
     *
     * @throws IllegalStateException if no persistence provider is configured
     */
    private static void checkPersistenceProvider() {
        if (persistenceProvider == null) {
            throw new IllegalStateException(
                    "No GraphPersistenceProvider configured. " +
                    "Call GraphDatabase.setPersistenceProvider() before using GraphDatabase operations."
            );
        }
    }

    /**
     * Interface for graph persistence operations.
     *
     * <p>Implement this interface to provide platform-specific persistence functionality.
     * TODO: Implement this interface according to your persistence requirements.</p>
     */
    public interface GraphPersistenceProvider {

        /**
         * Creates a new graph of the specified type at the given path.
         *
         * @param <T> the type of graph
         * @param graphType the class of the graph type
         * @param assetPath the path for the new asset
         * @return the created graph instance
         */
        <T extends Graph> T createGraph(Class<T> graphType, String assetPath);

        /**
         * Loads a graph of the specified type from the given path.
         *
         * @param <T> the type of graph
         * @param graphType the class of the graph type
         * @param assetPath the path to the graph asset
         * @return the loaded graph, or {@code null} if not found
         */
        <T extends Graph> T loadGraph(Class<T> graphType, String assetPath);

        /**
         * Saves the graph if it has unsaved changes.
         *
         * @param graph the graph to save
         */
        void saveGraphIfDirty(Graph graph);

        /**
         * Gets the unique identifier for a graph asset.
         *
         * @param graph the graph
         * @return the asset UUID, or {@code null} if not available
         */
        UUID getGraphAssetId(Graph graph);

        /**
         * Gets the file path for a graph asset.
         *
         * @param graph the graph
         * @return the asset path, or an empty string if not available
         */
        String getGraphAssetPath(Graph graph);
    }
}
