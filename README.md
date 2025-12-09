# Project: Well-designed Linter

## Contributors

Janelys Graciano, Ellie Li

## Dependencies

- JDK 17+
- ASM 9.7.1

## Build Instructions

HOW TO BUILD YOUR PROJECT.

Compile the linter
javac -cp lib/asm-9.7.1.jar -d out src/\*.java

Compile example classes to analyze
mkdir -p examples/out
javac -d examples/out examples/\*.java

Run the linter (Mac/Linux)
java -cp lib/asm-9.7.1.jar:out jellybeans.Main examples/out

Run the linter (Windows)
java -cp "lib\asm-9.7.1.jar;out" jellybeans.Main examples\out
