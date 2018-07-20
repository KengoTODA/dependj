package jp.skypencil.dependj;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
class DefaultJarAnalysisService implements JarAnalysisService {
  @Override
  public Mono<AnalysisResult> analyse(JarFile file) {
    Preconditions.checkNotNull(file);
    DependencyAnalysisVisitor visitor = new DependencyAnalysisVisitor(Opcodes.ASM6);
    return Flux.fromStream(file.stream())
        .filter(entry -> entry.getName().endsWith(".class"))
        .flatMap(open(file))
        .flatMap(read())
        .map(
            reader -> {
              reader.accept(visitor, 0);
              return Void.TYPE;
            })
        .reduce((a, b) -> a)
        .map(a -> visitor.getAnalysisResult());
  }

  private Function<ZipEntry, Mono<InputStream>> open(ZipFile file) {
    return entry -> {
      try {
        // TODO use non-blocking I/O?
        return Mono.just(file.getInputStream(entry));
      } catch (IOException e) {
        return Mono.error(e);
      }
    };
  }

  private Function<InputStream, Mono<ClassReader>> read() {
    return input -> {
      try {
        return Mono.just(new ClassReader(input));
      } catch (IOException e) {
        return Mono.error(e);
      }
    };
  }
}
