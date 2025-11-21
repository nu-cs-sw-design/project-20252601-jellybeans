1. Main parses the command-line argument (examples/out) and calls LinterRunner.runOnPath(...).

2. LinterRunner asks FileUtils for all .class files under that path.

3. For each .class file:

   1. It reads the bytes.
   2. Constructs an org.objectweb.asm.ClassReader.
   3. Calls reader.accept(visitor, 0) once for each of your checks.

4. ASM then walks through the bytecode of the class and calls methods like:

   1. visit(...) once for the class

   2. visitField(...) for each field

   3. visitMethod(...) for each method

   4. And inside each method, callbacks for each bytecode instruction
