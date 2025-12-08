package jellybeans;

import java.nio.file.Path;
import java.util.List;

/**
 * Main entry point for the Jellybeans linter.
 *
 * Responsible only for:
 *  - parsing command line arguments
 *  - wiring together the checks and reporter
 *  - invoking the LinterRunner
 */
public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -cp lib/asm-9.7.1.jar:out jellybeans.Main <file-or-directory>");
            System.exit(1);
        }

        Path root = Path.of(args[0]);

        Reporter reporter = new ConsoleReporter(System.out);

        // Register all checks (Strategy pattern).
        // Adding new checks in the future should only require updating this list
        // or introducing a more dynamic registration mechanism.
        List<LintCheck> checks = List.of(
            new NamingConventionCheck(),
            new NonPublicConstructorCheck(),
            new LongMethodCheck(50),
            new MagicNumberCheck(),
            new UnusedFieldCheck(),
            new NullReturnCheck()
        );

        LinterRunner runner = new LinterRunner(checks, reporter);

        try {
            runner.runOnPath(root);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }
}
