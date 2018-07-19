package jp.skypencil.dependj;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

class DependencyAnalysisVisitor extends ClassVisitor {
  private Map<String, AtomicInteger> classCount = new ConcurrentHashMap<>();
  private Map<String, AtomicInteger> abstractClassCount = new ConcurrentHashMap<>();
  private Map<String, AtomicInteger> interfaceCount = new ConcurrentHashMap<>();
  private Multimap<String, String> dependency = HashMultimap.create();
  private String packageName;

  DependencyAnalysisVisitor(int api) {
    super(api);
  }

  @Override
  public void visit(
      final int version,
      final int access,
      final String name,
      final String signature,
      final String superName,
      final String[] interfaces) {
    packageName = findPackage(name);

    if ((access & Opcodes.ACC_INTERFACE) > 0) {
      interfaceCount.computeIfAbsent(packageName, n -> new AtomicInteger()).incrementAndGet();
    }
    if ((access & Opcodes.ACC_ABSTRACT) > 0) {
      abstractClassCount.computeIfAbsent(packageName, n -> new AtomicInteger()).incrementAndGet();
    } else {
      classCount.computeIfAbsent(packageName, n -> new AtomicInteger()).incrementAndGet();
    }

    if (superName != null) {
      dependency.put(packageName, findPackage(superName));
    }
    if (interfaces != null) {
      for (String interfaceName : interfaces) {
        dependency.put(packageName, findPackage(interfaceName));
      }
    }
    //    if (signature != null) {
    //      Type type = Type.getObjectType(signature);
    //      String signaturePackage = findPackage(type);
    //      if (signaturePackage != null) {
    //        dependency.put(packageName, signaturePackage);
    //      }
    //    }
  }

  @Override
  public void visitInnerClass(
      final String name, final String outerName, final String innerName, final int access) {
    if ((access & Opcodes.ACC_INTERFACE) > 0) {
      interfaceCount.computeIfAbsent(packageName, n -> new AtomicInteger()).incrementAndGet();
    }
    if ((access & Opcodes.ACC_ABSTRACT) > 0) {
      abstractClassCount.computeIfAbsent(packageName, n -> new AtomicInteger()).incrementAndGet();
    } else {
      classCount.computeIfAbsent(packageName, n -> new AtomicInteger()).incrementAndGet();
    }
  }

  @Override
  public FieldVisitor visitField(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final Object value) {
    handle(descriptor);

    return new FieldAnalysisVisitor(
        api,
        dependencies -> {
          dependency.putAll(packageName, dependencies);
        });
  }

  @Override
  public MethodVisitor visitMethod(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final String[] exceptions) {
    handle(descriptor);
    handle(exceptions);

    return new MethodAnalysisVisitor(
        api,
        dependencies -> {
          dependency.putAll(packageName, dependencies);
        });
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    handle(descriptor);

    return new AnnotationAnalysisVisitor(
        api,
        dependencies -> {
          dependency.putAll(packageName, dependencies);
        });
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    handle(descriptor);

    return new AnnotationAnalysisVisitor(
        api,
        dependencies -> {
          dependency.putAll(packageName, dependencies);
        });
  }

  @Nullable
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

  @NonNull
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

  private void handle(String... types) {
    if (types == null) {
      return;
    }
    for (String typeString : types) {
      if (typeString == null) {
        continue;
      }
      Type type = Type.getObjectType(typeString);
      String packageName = findPackage(type);
      if (packageName != null) {
        dependency.put(this.packageName, packageName);
      }
    }
  }

  Map<String, AtomicInteger> getClassCount() {
    return Collections.unmodifiableMap(classCount);
  }

  Map<String, AtomicInteger> getInterfaceCount() {
    return Collections.unmodifiableMap(interfaceCount);
  }
  Multimap<String, String> getDependency() {
	  return Multimaps.unmodifiableMultimap(dependency);
  }
}
