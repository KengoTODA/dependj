package jp.skypencil.dependj;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

class AnnotationAnalysisVisitor extends AnnotationVisitor {
  private Set<String> dependencies = new HashSet<>();
  private Consumer<Set<String>> callback;

  AnnotationAnalysisVisitor(int api, Consumer<Set<String>> callback) {
    super(api);
    this.callback = Preconditions.checkNotNull(callback);
  }

  @Override
  public void visit(final String name, final Object value) {
    if (value == null || !(value instanceof Type)) {
      return;
    }
    Type type = (Type) value;
    String packageName = findPackage(type);
    if (packageName != null) {
      dependencies.add(packageName);
    }
  }

  @Override
  public void visitEnum(final String name, final String descriptor, final String value) {
    if (descriptor == null) {
      return;
    }
    Type type = Type.getType(descriptor);
    String packageName = findPackage(type);
    if (packageName != null) {
      dependencies.add(packageName);
    }
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
    if (descriptor == null) {
      return this;
    }
    Type type = Type.getType(descriptor);
    String packageName = findPackage(type);
    if (packageName != null) {
      dependencies.add(packageName);
    }
    return this;
  }

  @Override
  public void visitEnd() {
    callback.accept(Collections.unmodifiableSet(dependencies));
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
