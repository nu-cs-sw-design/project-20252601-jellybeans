package jellybeans;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * LinterRunner
 *
 * Orchestrates running a set of LintCheck strategies on all .class files
 * under a given root path. It does not know the details of any specific
 * check, or how results are presented to the user.
 */
public class LinterRunner {

    private final List<LintCheck> checks;
    private final Reporter reporter;

    public LinterRunner(List<LintCheck> checks, Reporter reporter) {
        this.checks = checks;
        this.reporter = reporter;
    }

    public void runOnPath(Path root) throws IOException {
        List<Path> classFiles = FileUtils.collectClassFiles(root);
        if (classFiles.isEmpty()) {
            System.out.println("No .class files found under " + root);
            return;
        }

        for (Path classFile : classFiles) {
            byte[] bytes = Files.readAllBytes(classFile);
            ClassReader reader = new ClassReader(bytes);

            reporter.startFile(classFile);
            for (LintCheck check : checks) {
                check.runOnClass(reader, reporter);
            }
            reporter.endFile(classFile);
        }
    }
}
