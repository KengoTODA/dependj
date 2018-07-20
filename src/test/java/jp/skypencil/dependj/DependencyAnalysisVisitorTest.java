package jp.skypencil.dependj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
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
}
