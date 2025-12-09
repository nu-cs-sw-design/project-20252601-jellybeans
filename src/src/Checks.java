package src;

import org.objectweb.asm.*;

import java.util.HashSet;
import java.util.Set;

/**
 * All check implementations live in this file for simplicity.
 * Each check:
 *   - implements LintCheck (Strategy pattern)
 *   - extends ClassVisitor to leverage ASM's Visitor API
 *   - reports findings via Reporter instead of printing directly
 */

/* ============================================================
   1. NamingConventionCheck
   ============================================================ */
/**
 * NamingConventionCheck
 *
 * Enforces simple naming conventions on classes, methods, and fields.
 *
 * This check inspects:
 *  - Class names: must start with an uppercase letter.
 *  - Method names: must start with a lowercase letter (constructors are ignored).
 *  - Field names: must start with a lowercase letter.
 */
class NamingConventionCheck extends ClassVisitor implements LintCheck {

    private Reporter reporter;
    private String simpleClassName;

    NamingConventionCheck() {
        super(Opcodes.ASM9);
    }

    @Override
    public String name() {
        return "NamingConvention";
    }

    @Override
    public void runOnClass(ClassReader reader, Reporter reporter) {
        this.reporter = reporter;
        this.simpleClassName = null;
        reader.accept(this, 0);
    }

    private void report(String message) {
        reporter.report(name(), simpleClassName, message);
    }

    @Override
    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {
        int idx = name.lastIndexOf('/');
        simpleClassName = (idx >= 0) ? name.substring(idx + 1) : name;

        if (!simpleClassName.isEmpty() && !Character.isUpperCase(simpleClassName.charAt(0))) {
            report("Class name should start with an uppercase letter.");
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String descriptor,
                                     String signature,
                                     String[] exceptions) {
        if (!name.equals("<init>") && !name.equals("<clinit>")) {
            if (!name.isEmpty() && !Character.isLowerCase(name.charAt(0))) {
                report("Method '" + name + "' should start with a lowercase letter.");
            }
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public FieldVisitor visitField(int access,
                                   String name,
                                   String descriptor,
                                   String signature,
                                   Object value) {
        if (!name.isEmpty() && !Character.isLowerCase(name.charAt(0))) {
            report("Field '" + name + "' should start with a lowercase letter.");
        }
        return super.visitField(access, name, descriptor, signature, value);
    }
}


/* ============================================================
   2. NonPublicConstructorCheck
   ============================================================ */
/**
 * NonPublicConstructorCheck
 *
 * Detects public classes that cannot be constructed publicly.
 *
 * A warning is issued when ALL of the following conditions are true:
 *  - The class is public.
 *  - The class is NOT abstract and NOT an interface.
 *  - The class declares at least one constructor.
 *  - None of its constructors are public or protected.
 */
class NonPublicConstructorCheck extends ClassVisitor implements LintCheck {

    private Reporter reporter;
    private String simpleClassName;
    private boolean isPublicClass;
    private boolean isAbstractOrInterface;
    private boolean hasAnyCtor;
    private boolean hasPublicOrProtectedCtor;

    NonPublicConstructorCheck() {
        super(Opcodes.ASM9);
    }

    @Override
    public String name() {
        return "NonPublicConstructor";
    }

    @Override
    public void runOnClass(ClassReader reader, Reporter reporter) {
        this.reporter = reporter;
        this.simpleClassName = null;
        this.isPublicClass = false;
        this.isAbstractOrInterface = false;
        this.hasAnyCtor = false;
        this.hasPublicOrProtectedCtor = false;
        reader.accept(this, 0);
    }

    private void report(String message) {
        reporter.report(name(), simpleClassName, message);
    }

    @Override
    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {
        int idx = name.lastIndexOf('/');
        simpleClassName = (idx >= 0) ? name.substring(idx + 1) : name;

        isPublicClass = (access & Opcodes.ACC_PUBLIC) != 0;
        boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        boolean isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
        isAbstractOrInterface = isInterface || isAbstract;

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String descriptor,
                                     String signature,
                                     String[] exceptions) {
        if (name.equals("<init>")) {
            hasAnyCtor = true;

            boolean isPublic = (access & Opcodes.ACC_PUBLIC) != 0;
            boolean isProtected = (access & Opcodes.ACC_PROTECTED) != 0;
            if (isPublic || isProtected) {
                hasPublicOrProtectedCtor = true;
            }
        }

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        if (isPublicClass && !isAbstractOrInterface && hasAnyCtor && !hasPublicOrProtectedCtor) {
            report("Public class has no public or protected constructors.");
        }
        super.visitEnd();
    }
}


/* ============================================================
   3. LongMethodCheck
   ============================================================ */
/**
 * LongMethodCheck
 *
 * Flags methods whose bytecode instruction count exceeds a configurable threshold.
 * Uses instruction count as a proxy for complexity/length.
 */
class LongMethodCheck extends ClassVisitor implements LintCheck {

    private Reporter reporter;
    private String simpleClassName;
    private final int maxInstructions;

    LongMethodCheck(int maxInstructions) {
        super(Opcodes.ASM9);
        this.maxInstructions = maxInstructions;
    }

    @Override
    public String name() {
        return "LongMethod";
    }

    @Override
    public void runOnClass(ClassReader reader, Reporter reporter) {
        this.reporter = reporter;
        this.simpleClassName = null;
        reader.accept(this, 0);
    }

    private void report(String methodName, int count) {
        String msg = "Method '" + methodName + "' has " + count
                + " bytecode instructions (limit " + maxInstructions + ").";
        reporter.report(name(), simpleClassName, msg);
    }

    @Override
    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {
        int idx = name.lastIndexOf('/');
        simpleClassName = (idx >= 0) ? name.substring(idx + 1) : name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String descriptor,
                                     String signature,
                                     String[] exceptions) {
        if (name.equals("<init>") || name.equals("<clinit>")) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);

        return new MethodVisitor(api, parent) {
            int instructionCount = 0;

            @Override public void visitInsn(int opcode) { instructionCount++; super.visitInsn(opcode); }
            @Override public void visitIntInsn(int opcode, int operand) { instructionCount++; super.visitIntInsn(opcode, operand); }
            @Override public void visitVarInsn(int opcode, int var) { instructionCount++; super.visitVarInsn(opcode, var); }
            @Override public void visitTypeInsn(int opcode, String type) { instructionCount++; super.visitTypeInsn(opcode, type); }
            @Override public void visitFieldInsn(int opcode, String owner, String name, String desc) { instructionCount++; super.visitFieldInsn(opcode, owner, name, desc); }
            @Override public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) { instructionCount++; super.visitMethodInsn(opcode, owner, name, desc, itf); }
            @Override public void visitJumpInsn(int opcode, Label label) { instructionCount++; super.visitJumpInsn(opcode, label); }
            @Override public void visitLdcInsn(Object value) { instructionCount++; super.visitLdcInsn(value); }

            @Override
            public void visitEnd() {
                if (instructionCount > maxInstructions) {
                    report(name, instructionCount);
                }
                super.visitEnd();
            }
        };
    }
}


/* ============================================================
   4. MagicNumberCheck
   ============================================================ */
/**
 * MagicNumberCheck
 *
 * Flags "magic numbers" appearing in code as literals, except for a small
 * whitelist of common values (-1, 0, 1).
 *
 * Currently checks:
 *  - BIPUSH/SIPUSH integer instructions
 *  - LDC with numeric constants
 */
class MagicNumberCheck extends ClassVisitor implements LintCheck {

    private Reporter reporter;
    private String simpleClassName;

    MagicNumberCheck() {
        super(Opcodes.ASM9);
    }

    @Override
    public String name() {
        return "MagicNumber";
    }

    @Override
    public void runOnClass(ClassReader reader, Reporter reporter) {
        this.reporter = reporter;
        this.simpleClassName = null;
        reader.accept(this, 0);
    }

    private void report(String methodName, Number value) {
        String msg = "Magic number " + value + " used in method '" + methodName
                + "'. Consider extracting a named constant.";
        reporter.report(name(), simpleClassName, msg);
    }

    private boolean isWhitelistedNumber(Number n) {
        if (n instanceof Integer || n instanceof Long) {
            long v = n.longValue();
            return v == -1L || v == 0L || v == 1L;
        }
        if (n instanceof Float || n instanceof Double) {
            double v = n.doubleValue();
            return v == 0.0 || v == 1.0;
        }
        return false;
    }

    @Override
    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {
        int idx = name.lastIndexOf('/');
        simpleClassName = (idx >= 0) ? name.substring(idx + 1) : name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String descriptor,
                                     String signature,
                                     String[] exceptions) {

        if (name.equals("<init>") || name.equals("<clinit>")) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);

        return new MethodVisitor(api, parent) {
            @Override
            public void visitIntInsn(int opcode, int operand) {
                if (!isWhitelistedNumber(operand)) {
                    report(name, operand);
                }
                super.visitIntInsn(opcode, operand);
            }

            @Override
            public void visitLdcInsn(Object value) {
                if (value instanceof Number) {
                    Number num = (Number) value;
                    if (!isWhitelistedNumber(num)) {
                        report(name, num);
                    }
                }
                super.visitLdcInsn(value);
            }
        };
    }
}


/* ============================================================
   5. UnusedFieldOrMethodCheck
   ============================================================ */
/**
 * UnusedFieldCheck
 *
 * Attempts to detect fields  that are declared but never used
 * within the same class.
 *
 * For simplicity and to reduce false positives, this implementation focuses
 * on private fields excluding constructors and
 * static initializers).
 */
class UnusedFieldCheck extends ClassVisitor implements LintCheck {

    private Reporter reporter;
    private String simpleClassName;
    private String classInternalName;

    private final Set<String> privateFields = new HashSet<>();
    private final Set<String> usedFields = new HashSet<>();

    UnusedFieldCheck() {
        super(Opcodes.ASM9);
    }

    @Override
    public String name() {
        return "UnusedField";
    }

    @Override
    public void runOnClass(ClassReader reader, Reporter reporter) {
        this.reporter = reporter;
        this.simpleClassName = null;
        this.classInternalName = null;

        privateFields.clear();
        usedFields.clear();

        reader.accept(this, 0);
    }

    private void report(String message) {
        reporter.report(name(), simpleClassName, message);
    }

    @Override
    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {
        classInternalName = name;
        int idx = name.lastIndexOf('/');
        simpleClassName = (idx >= 0) ? name.substring(idx + 1) : name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access,
                                   String name,
                                   String descriptor,
                                   String signature,
                                   Object value) {
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            privateFields.add(name);
        }
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String descriptor,
                                     String signature,
                                     String[] exceptions) {
        if (name.equals("<init>") || name.equals("<clinit>")) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);

        return new MethodVisitor(api, parent) {
            @Override
            public void visitFieldInsn(int opcode,
                                       String owner,
                                       String fieldName,
                                       String fieldDesc) {
                if (owner.equals(classInternalName)) {
                    usedFields.add(fieldName);
                }
                super.visitFieldInsn(opcode, owner, fieldName, fieldDesc);
            }
        };
    }

    @Override
    public void visitEnd() {
        for (String field : privateFields) {
            if (!usedFields.contains(field)) {
                report("Private field '" + field + "' appears to be unused.");
            }
        }
        super.visitEnd();
    }
}


/* ============================================================
   6. NullReturnCheck
   ============================================================ */
/**
 * NullReturnCheck
 *
 * Flags methods that return null directly (ACONST_NULL followed by ARETURN).
 * This is a conservative heuristic to detect potential nullability issues.
 */
class NullReturnCheck extends ClassVisitor implements LintCheck {

    private Reporter reporter;
    private String simpleClassName;

    NullReturnCheck() {
        super(Opcodes.ASM9);
    }

    @Override
    public String name() {
        return "NullReturn";
    }

    @Override
    public void runOnClass(ClassReader reader, Reporter reporter) {
        this.reporter = reporter;
        this.simpleClassName = null;
        reader.accept(this, 0);
    }

    private void report(String methodName) {
        String msg = "Method '" + methodName + "' returns null directly.";
        reporter.report(name(), simpleClassName, msg);
    }

    @Override
    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {
        int idx = name.lastIndexOf('/');
        simpleClassName = (idx >= 0) ? name.substring(idx + 1) : name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String descriptor,
                                     String signature,
                                     String[] exceptions) {

        if (name.equals("<init>") || name.equals("<clinit>")) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);

        return new MethodVisitor(api, parent) {
            boolean sawNullBeforeReturn = false;
            boolean foundNullReturn = false;

            @Override
            public void visitInsn(int opcode) {
                if (opcode == Opcodes.ACONST_NULL) {
                    sawNullBeforeReturn = true;
                } else if (opcode == Opcodes.ARETURN && sawNullBeforeReturn) {
                    foundNullReturn = true;
                } else {
                    // Any other instruction between ACONST_NULL and ARETURN
                    // cancels our simple "direct null" heuristic.
                    sawNullBeforeReturn = false;
                }
                super.visitInsn(opcode);
            }

            @Override
            public void visitEnd() {
                if (foundNullReturn) {
                    report(name);
                }
                super.visitEnd();
            }
        };
    }
}
