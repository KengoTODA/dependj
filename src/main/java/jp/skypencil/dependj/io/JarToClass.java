package jp.skypencil.dependj.io;

import java.nio.file.Path;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

@FunctionalInterface
public interface JarToClass {
  @NonNull
  Flux<Path> parse(@NonNull Path jar);
}
