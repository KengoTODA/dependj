package jp.skypencil.dependj;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

class FieldAnalysisVisitor extends FieldVisitor {
  private Set<String> dependencies = new HashSet<>();
  private Consumer<Set<String>> callback;

  FieldAnalysisVisitor(int api, Consumer<Set<String>> callback) {
    super(api);
    this.callback = Preconditions.checkNotNull(callback);
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
  public void visitEnd() {
    callback.accept(Collections.unmodifiableSet(dependencies));
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
