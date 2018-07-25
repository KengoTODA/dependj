package jp.skypencil.dependj.io;

import java.nio.file.Path;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface PathToData {
  /**
   * Read whole data from .class file.
   *
   * @param path non-null {@link Path} of target .class file
   * @return Data inside of specified .class file
   */
  @NonNull
  Mono<DataBuffer> read(@NonNull Path path);
}
