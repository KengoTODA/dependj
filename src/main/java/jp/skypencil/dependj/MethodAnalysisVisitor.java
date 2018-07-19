package jp.skypencil.dependj;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

class MethodAnalysisVisitor extends MethodVisitor {
  private Set<String> dependencies = new HashSet<>();
  private Consumer<Set<String>> callback;

  public MethodAnalysisVisitor(int api, Consumer<Set<String>> callback) {
    super(api);
    this.callback = Preconditions.checkNotNull(callback);
  }

  @Override
  public void visitEnd() {
    callback.accept(Collections.unmodifiableSet(dependencies));
  }

  @Override
  public AnnotationVisitor visitAnnotationDefault() {
    return new AnnotationAnalysisVisitor(api, dependencies::addAll);
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    handle(descriptor);
    return new AnnotationAnalysisVisitor(api, dependencies::addAll);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    handle(descriptor);
    return new AnnotationAnalysisVisitor(api, dependencies::addAll);
  }

  @Override
  public AnnotationVisitor visitParameterAnnotation(
      final int parameter, final String descriptor, final boolean visible) {
    handle(descriptor);
    return new AnnotationAnalysisVisitor(api, dependencies::addAll);
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    handle(type);
  }

  @Override
  public void visitFieldInsn(
      final int opcode, final String owner, final String name, final String descriptor) {
    handle(owner, descriptor);
  }

  @Override
  public void visitMethodInsn(
      final int opcode,
      final String owner,
      final String name,
      final String descriptor,
      final boolean isInterface) {
    handle(owner, descriptor);
  }

  @Override
  public void visitInvokeDynamicInsn(
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    handle(descriptor, bootstrapMethodHandle.getOwner(), bootstrapMethodHandle.getDesc());
  }

  @Override
  public AnnotationVisitor visitInsnAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    handle(descriptor);
    return new AnnotationAnalysisVisitor(api, dependencies::addAll);
  }

  @Override
  public void visitTryCatchBlock(
      final Label start, final Label end, final Label handler, final String type) {
    handle(type);
  }

  @Override
  public AnnotationVisitor visitTryCatchAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    handle(descriptor);
    return new AnnotationAnalysisVisitor(api, dependencies::addAll);
  }

  @Override
  public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
    handle(descriptor);
  }

  @Override
  public void visitLocalVariable(
      final String name,
      final String descriptor,
      final String signature,
      final Label start,
      final Label end,
      final int index) {
    handle(descriptor);
  }

  @Override
  public AnnotationVisitor visitLocalVariableAnnotation(
      final int typeRef,
      final TypePath typePath,
      final Label[] start,
      final Label[] end,
      final int[] index,
      final String descriptor,
      final boolean visible) {
    handle(descriptor);
    return new AnnotationAnalysisVisitor(api, dependencies::addAll);
  }

  private void handle(String... types) {
    for (String typeString : types) {
      if (typeString == null) {
        continue;
      }
      Type type = Type.getObjectType(typeString);
      String packageName = findPackage(type);
      if (packageName != null) {
        dependencies.add(packageName);
      }
    }
  }

  String findPackage(final Type type) {
    switch (type.getSort()) {
      case Type.ARRAY:
        return findPackage(type.getElementType());
      case Type.OBJECT:
        return findPackage(type.getClassName());
      default:
        return null;
    }
  }

  String findPackage(final String name) {
    String packageName;
    int lastIndex = name.lastIndexOf('/');
    if (lastIndex >= 0) {
      packageName = name.substring(0, lastIndex).replace('/', '.');
    } else {
      packageName = "";
    }
    return packageName;
  }
}
