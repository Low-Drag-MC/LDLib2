package com.lowdragmc.lowdraglib2.nodegraphtookit.api;

/**
 * Interface for logging errors, warnings, and informational messages in the graph system.
 *
 * <p>Implement this interface to provide logging functionality for the graph editor.
 * Messages logged through this interface can be displayed in the console and/or as visual markers
 * on graph elements.</p>
 */
public interface IErrorsAndWarnings {

    /**
     * Logs an error message.
     *
     * @param message the error message to display
     * @param context optional context object to associate with the error (typically a node)
     */
    void logError(Object message, Object context);

    /**
     * Logs a warning message.
     *
     * @param message the warning message to display
     * @param context optional context object to associate with the warning (typically a node)
     */
    void logWarning(Object message, Object context);

    /**
     * Logs an informational message.
     *
     * @param message the message to display
     * @param context optional context object to associate with the message (typically a node)
     */
    void log(Object message, Object context);
}
