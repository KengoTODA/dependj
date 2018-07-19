package jp.skypencil.dependj;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

class DependencyAnalysisVisitorTest {
  @Test
  void testCountClass() throws IOException {
    DependencyAnalysisVisitor visitor = new DependencyAnalysisVisitor(Opcodes.ASM6);
    ClassReader reader = new ClassReader("java.lang.Object");
    reader.accept(visitor, 0);
    assertEquals(visitor.getClassCount().get("java.lang").intValue(), 1);
    assertNull(visitor.getInterfaceCount().get("java.lang"));
  }

  @Test
  void testCountInterace() throws IOException {
    DependencyAnalysisVisitor visitor = new DependencyAnalysisVisitor(Opcodes.ASM6);
    ClassReader reader = new ClassReader("java.lang.Comparable");
    reader.accept(visitor, 0);
    assertNull(visitor.getClassCount().get("java.lang"));
    assertEquals(visitor.getInterfaceCount().get("java.lang").intValue(), 1);
  }

  @Test
  void testFindPackage() throws IOException {
    DependencyAnalysisVisitor visitor = new DependencyAnalysisVisitor(Opcodes.ASM6);
    assertEquals("", visitor.findPackage("A"));
    assertEquals("java.lang", visitor.findPackage("java/lang/Object"));
    assertEquals("java.util", visitor.findPackage("java/util/EnumSet$SerializationProxy"));
  }

  @Test
  void smokeTest() throws IOException {
    Set<Path> jarFiles = new HashSet<>();
    Path lib = Paths.get(System.getProperty("user.home"), ".m2", "repository", "com", "google");
    Files.walkFileTree(
        lib,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            String name = file.getFileName().toString();
            if (name.endsWith(".jar")
                && !name.endsWith("-sources.jar")
                && !name.endsWith("-javadoc.jar")) {
              jarFiles.add(file);
            }
            return FileVisitResult.CONTINUE;
          }
        });

    DependencyAnalysisVisitor visitor = new DependencyAnalysisVisitor(Opcodes.ASM6);
    jarFiles
        .stream()
        .map(Path::toFile)
        .map(
            jar -> {
              try {
                return new JarFile(jar);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            })
        .flatMap(
            jar ->
                jar.stream()
                    .map(
                        entry -> {
                          try {
                            if (entry.getName().endsWith(".class")) {
                              return jar.getInputStream(entry);
                            } else {
                              return null;
                            }
                          } catch (IOException e) {
                            throw new UncheckedIOException(e);
                          }
                        }))
        .filter(Objects::nonNull)
        .map(
            input -> {
              try {
                return new ClassReader(input);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            })
        .forEach(
            reader -> {
              reader.accept(visitor, 0);
            });
    System.out.println(visitor.getDependency());
  }
}
