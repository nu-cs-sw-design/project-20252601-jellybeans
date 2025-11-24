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

/**
 * MagicNumberCheck
 *
 * This check inspects:
 *  - There is no use of "magic numbers" in methods or class fields (except for constants like 0,1,-1, etc.)
 *  - Numeric values must be stored in a named variable/constant (not hardcoded directly within the code)
 * Implementation details:
 *  - Tracks constructor access modifiers by overriding visitMethod().
 *  - Evaluates the final condition in visitEnd(), after the whole class has been processed.
 */
class MagicNumberCheck extends ClassVisitor {

    //holds name of class curr visited
    private String simpleName;

    private static final java.util.Set<Number> ALLOWED_Values = java.util.Set.of(-1, 0, 1);

    //calls parent constructor for ASM API
    public MagicNumberCheck() {
        super(Opcodes.ASM9);
    }

    @Override
    public void visit(int version,
                      int access,
                      String name,
                      String signature,
                      String superName,
                      String[] interfaces) {
        this.simpleName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions){
        if (name.equals("<init>") || name.equals("<clinit>")) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        return new MethodVisitor(api, mv) {

            @Override
            public void visitLdcInsn(Object value) {
                if (value instanceof Number) {
                    Number num = (Number) value;
                    check(num, name);
                }
                super.visitLdcInsn(value);
            }

            @Override
            public void visitIntInsn(int opcode, int operand) {
                if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                    check(operand, name);
                }
                super.visitIntInsn(opcode, operand);
            }

            @Override
            public void visitInsn(int opcode) {
                Integer val = null;

                switch (opcode) {
                    case Opcodes.ICONST_M1:
                        val = -1;
                        break;
                    case Opcodes.ICONST_0:
                        val = 0;
                        break;
                    case Opcodes.ICONST_1:
                        val = 1;
                        break;
                    case Opcodes.ICONST_2:
                        val = 2;
                        break;
                    case Opcodes.ICONST_3:
                        val = 3;
                        break;
                    case Opcodes.ICONST_4:
                        val = 4;
                        break;
                    case Opcodes.ICONST_5:
                        val = 5;
                        break;
                    default:
                        // leave val = null
                        break;
                }

                if (val != null) {
                    check(val, name);
                }
                super.visitInsn(opcode);
            }

            private void check(Number num, String method) {
                if (!ALLOWED_Values.contains(num)) {
                    System.out.printf("Warning: Magic number %s found in %s.%s()%n", num, simpleName, method);
                }
            }
        };
    }

}
/**
 * UnusedFieldOrMethodCheck
 *
 * This check inspects that:
 *  - There are no fields or methods that are declared but never read/written/invoked within the class
 *
 * Rationale:
 *  - Unused fields/methods create unnecessary clutter and make maintainability harder
 * Implementation details:
 *  - During the class visit, all declared fields and methods are collected.
 *  - Each time a field or method is referenced (GETFIELD, PUTFIELD, INVOKEVIRTUAL, etc.),
 *    it is marked as "used".
 *  - At the end of the visit (visitEnd()), the check compares the sets of declared
 *     members and used members.
 *  - Any field or method that was never marked as used is reported as unused.
 */

class UnusedFieldOrMethodCheck extends ClassVisitor{

    private final java.util.Set<String> declaredFields = new java.util.HashSet<>();
    private final java.util.Set<String> declaredMethods = new java.util.HashSet<>();
    private final java.util.Set<String> usedFields = new java.util.HashSet<>();
    private final java.util.Set<String> usedMethods = new java.util.HashSet<>();

    private String currClassName;

    public UnusedFieldOrMethodCheck(int api) {
        super(api);
    }
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        // Extract simple class name from internal name
        int idx = name.lastIndexOf('/');
        currClassName = (idx >= 0) ? name.substring(idx + 1) : name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        declaredFields.add(name + ":" + descriptor);
        return super.visitField(access, name, descriptor, signature, value);
    }
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        declaredMethods.add(name + descriptor);

        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        return new MethodVisitor(api, mv) {
            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                usedFields.add(name + ":" + descriptor);
                super.visitFieldInsn(opcode, owner, name, descriptor);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                usedMethods.add(name + descriptor);
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        };
    }

    @Override
    public void visitEnd() {
        //only keep unused fields
        java.util.Set<String> unusedFields = new java.util.HashSet<>(declaredFields);
        unusedFields.removeAll(usedFields);

        //only keep unused methods
        java.util.Set<String> unusedMethods = new java.util.HashSet<>(declaredMethods);
        unusedMethods.removeAll(usedMethods);

        for (String field : unusedFields) {
            int idx = field.indexOf(':');
            String simpleName = (idx >= 0) ? field.substring(0, idx) : field;
            System.out.println("Warning: Unused field detected in " + currClassName + ": Method" + simpleName);
        }

        for (String method : unusedMethods) {
            int idx = method.indexOf('(');        // position of argument list
            String simpleName = (idx >= 0) ? method.substring(0, idx) : method;

            //skip compiler generated method names
            if (!simpleName.startsWith("<")) {
                System.out.println("Warning: Unused method detected in " + currClassName + ": Method" + simpleName + "()");
            }
        }
        super.visitEnd();
    }



}
/**
 * CheckNullReturn
 *
 * This check looks to see if methods to detect if they return null explicitly.
 *
 * Rationale:
 *  - Methods that return null can lead to exceptions if callers are not careful (default objects or Optional usage is preferred)
 *
 * Implementation details:
 *  - During method visits, the bytecode instructions are analyzed.
 *  - If a method contains a return of a null constant, it is flagged with a warning
 *  - void methods are ignored as well as compiler methods
 *  - Flagged methods are printed with a warning
 */
class NullReturnCheck extends ClassVisitor {

    private String currClassName;
    private final java.util.Set<String> flaggedMethods = new java.util.HashSet<>();

    public NullReturnCheck(int api) {
        super(api);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        int idx = name.lastIndexOf('/');
        currClassName = (idx >= 0) ? name.substring(idx + 1) : name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {

        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        return new MethodVisitor(api, mv) {
            @Override
            public void visitInsn(int opcode) {
                if (opcode == Opcodes.ARETURN) {
                    if (lastWasNull) {
                        flaggedMethods.add(name + descriptor);
                    }
                }
                lastWasNull = (opcode == Opcodes.ACONST_NULL);

                super.visitInsn(opcode);
            }

            private boolean lastWasNull = false;
        };
    }

    @Override
    public void visitEnd() {
        for (String method : flaggedMethods) {
            int idx = method.indexOf('(');
            String simpleName = (idx >= 0) ? method.substring(0, idx) : method;
            System.out.println("Warning: Method in " + currClassName + " returns null: " + simpleName + "()");
        }
        super.visitEnd();
    }
}