package jp.skypencil.dependj.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
class SyncPathToData implements PathToData {
  private DataBufferFactory factory = new DefaultDataBufferFactory();

  @Override
  public Mono<DataBuffer> read(Path path) {
    try {
      DataBuffer buffer = factory.wrap(Files.readAllBytes(path));
      return Mono.just(buffer);
    } catch (IOException e) {
      return Mono.error(e);
    }
  }
}
