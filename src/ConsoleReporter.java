package jellybeans;

import java.io.PrintStream;
import java.nio.file.Path;

/**
 * ConsoleReporter
 *
 * Simple Reporter implementation that prints results to the console.
 * This keeps printing concerns separate from analysis logic, and can be
 * swapped out later for a GUI-based or rating-based reporter.
 */
public class ConsoleReporter implements Reporter {

    private final PrintStream out;

    public ConsoleReporter(PrintStream out) {
        this.out = out;
    }

    @Override
    public void startFile(Path classFile) {
        out.println("=== Analyzing " + classFile + " ===");
    }

    @Override
    public void report(String checkName, String className, String message) {
        out.printf("[%s] In class '%s': %s%n", checkName, className, message);
    }

    @Override
    public void endFile(Path classFile) {
        // no summary for now, but this hook allows future extensions
    }
}
