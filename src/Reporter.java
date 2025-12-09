package jellybeans;

import java.nio.file.Path;

/**
 * Reporter
 *
 * Abstraction for how lint results are reported.
 * This decouples checks from specific output formats
 * (console, GUI, JSON, rating system, etc.).
 */
public interface Reporter {

    /**
     * Called before processing a new .class file.
     */
    void startFile(Path classFile);

    /**
     * Record a single lint message.
     *
     * @param checkName  name of the check producing this message
     * @param className  simple name of the class being analyzed
     * @param message    human-readable details about the issue
     */
    void report(String checkName, String className, String message);

    /**
     * Called after finishing processing a .class file.
     */
    void endFile(Path classFile);
}
