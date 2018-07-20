package jp.skypencil.dependj;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.function.Function;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
class DefaultClassAnalysisService implements ClassAnalysisService {

  @Override
  public Mono<AnalysisResult> analyse(File... files) {
    Preconditions.checkNotNull(files);
    DependencyAnalysisVisitor visitor = new DependencyAnalysisVisitor(Opcodes.ASM6);
    return Flux.fromArray(files)
        .publishOn(Schedulers.parallel())
        .filter(File::isFile)
        .filter(file -> file.getName().endsWith(".class"))
        .flatMap(open())
        .flatMap(read())
        .map(
            reader -> {
              reader.accept(visitor, 0);
              return Void.TYPE;
            })
        .reduce((a, b) -> a)
        .map(a -> visitor.getAnalysisResult());
  }

  private Function<File, Mono<InputStream>> open() {
    return file -> {
      try {
        // TODO use non-blocking I/O?
        return Mono.just(Files.newInputStream(file.toPath(), StandardOpenOption.READ));
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
