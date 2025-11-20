package jellybeans;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Main <file-or-directory>");
            System.exit(1);
        }

        try {
            LinterRunner runner = new LinterRunner();
            runner.runOnPath(Path.of(args[0]));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }
}
