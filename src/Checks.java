package jellybeans;

import org.objectweb.asm.*;


/**
 * NamingConventionCheck
 * This check inspects:
 *  - Class names: must start with an uppercase letter.
 *  - Method names: must start with a lowercase letter (constructors are ignored).
 *  - Field names: must start with a lowercase letter.
 */

class NamingConventionCheck extends ClassVisitor {

    private String simpleName;

    public NamingConventionCheck() {
        super(Opcodes.ASM9);
    }

    @Override
    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {

        int idx = name.lastIndexOf('/');
        simpleName = (idx >= 0) ? name.substring(idx + 1) : name;

        // Class name must start with uppercase
        if (!simpleName.isEmpty() && !Character.isUpperCase(simpleName.charAt(0))) {
            System.out.printf("[Naming] Class '%s' should start with uppercase.%n", simpleName);
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String descriptor,
                                     String signature,
                                     String[] exceptions) {
        // Skip constructors
        if (!name.equals("<init>") && !name.equals("<clinit>")) {
            if (!name.isEmpty() && !Character.isLowerCase(name.charAt(0))) {
                System.out.printf("[Naming] Method '%s' in class '%s' should start with lowercase.%n",
                        name, simpleName);
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
            System.out.printf("[Naming] Field '%s' in class '%s' should start with lowercase.%n",
                    name, simpleName);
        }

        return super.visitField(access, name, descriptor, signature, value);
    }
}


/**
 * NonPublicConstructorCheck
 *
 * A warning is issued when ALL of the following conditions are true:
 *  - The class is public.
 *  - The class is NOT abstract and NOT an interface.
 *  - The class declares at least one constructor.
 *  - None of its constructors are public or protected (all are private or package-private).
 *
 * Rationale:
 *  - A public class with only private constructors may indicate an accidental access-level mistake,
 *    making the class impossible to instantiate from outside its package.
 *
 * Implementation details:
 *  - Tracks constructor access modifiers by overriding visitMethod().
 *  - Evaluates the final condition in visitEnd(), after the whole class has been processed.
 */

class NonPublicConstructorCheck extends ClassVisitor {

    private String simpleName;
    private boolean isPublicClass;
    private boolean isAbstractOrInterface;
    private boolean hasAnyCtor = false;
    private boolean hasPublicOrProtectedCtor = false;

    public NonPublicConstructorCheck() {
        super(Opcodes.ASM9);
    }

    @Override
    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {

        int idx = name.lastIndexOf('/');
        simpleName = (idx >= 0) ? name.substring(idx + 1) : name;

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
            System.out.printf("[Constructor] Public class '%s' has no public/protected constructors.%n",
                    simpleName);
        }
        super.visitEnd();
    }
}


/**
 * LongMethodCheck
 *
 * This check measures:
 *  - Total number of bytecode instructions in each non-constructor method.
 *  - If the count exceeds `maxInstructions`, a warning is printed.
 *
 * Implementation details:
 *  - Overrides a set of MethodVisitor instruction callbacks (visitInsn, visitVarInsn, etc.).
 *  - Increments an internal counter for each instruction encountered.
 *  - Final check is performed in visitEnd() once the method is fully visited.
 */

class LongMethodCheck extends ClassVisitor {

    private String simpleClassName;
    private final int maxInstructions;

    public LongMethodCheck(int maxInstructions) {
        super(Opcodes.ASM9);
        this.maxInstructions = maxInstructions;
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

            @Override
            public void visitEnd() {
                if (instructionCount > maxInstructions) {
                    System.out.printf("[LongMethod] Method '%s' in class '%s' has %d instructions (limit %d).%n",
                            name, simpleClassName, instructionCount, maxInstructions);
                }
                super.visitEnd();
            }
        };
    }
}
