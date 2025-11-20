package jellybeans;

import java.nio.file.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    public static List<Path> collectClassFiles(Path root) throws IOException {
        List<Path> result = new ArrayList<>();

        if (Files.isRegularFile(root) && root.toString().endsWith(".class")) {
            result.add(root);
            return result;
        }

        if (Files.isDirectory(root)) {
            try (var stream = Files.walk(root)) {
                stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".class"))
                      .forEach(result::add);
            }
        }
        return result;
    }
}
