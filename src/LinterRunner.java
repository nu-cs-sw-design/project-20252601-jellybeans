package jellybeans;

import org.objectweb.asm.ClassReader;
import java.nio.file.*;
import java.io.IOException;
import java.util.List;

public class LinterRunner {

    public void runOnPath(Path root) throws IOException {
        List<Path> classFiles = FileUtils.collectClassFiles(root);
        if (classFiles.isEmpty()) {
            System.out.println("No .class files found under " + root);
            return;
        }

        for (Path classFile : classFiles) {
            byte[] bytes = Files.readAllBytes(classFile);
            ClassReader reader = new ClassReader(bytes);

            System.out.println("=== Analyzing " + classFile + " ===");
            // ellie's checks
            reader.accept(new NamingConventionCheck(), 0);
            reader.accept(new NonPublicConstructorCheck(), 0);
            reader.accept(new LongMethodCheck(50), 0);
            // JJ' checks
        }
    }
}
