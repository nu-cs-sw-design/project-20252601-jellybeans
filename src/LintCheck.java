package jellybeans;

import org.objectweb.asm.ClassReader;

/**
 * LintCheck
 *
 * Strategy interface for all linter checks.
 * Each check can be run on a single class and reports results
 * through the Reporter abstraction.
 */
public interface LintCheck {

    /**
     * @return a short human-readable name for this check
     *         (used in reporting).
     */
    String name();

    /**
     * Run this check on a single compiled class.
     *
     * @param reader   ASM ClassReader for the target class.
     * @param reporter Reporter for recording any violations.
     */
    void runOnClass(ClassReader reader, Reporter reporter);
}
