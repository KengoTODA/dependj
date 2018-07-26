package jp.skypencil.dependj.io;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class AsyncPathToData implements PathToData {
  private static final int BUFFER_SIZE = 8 * 1024;
  private DataBufferFactory factory = new DefaultDataBufferFactory();

  @Override
  public Mono<DataBuffer> read(Path path) {
    // not supported even by JDK10
    // https://github.com/dmlloyd/openjdk/blob/ba116d5/src/jdk.zipfs/share/classes/jdk/nio/zipfs/ZipFileSystemProvider.java#L244
    Flux<DataBuffer> channel =
        DataBufferUtils.readAsynchronousFileChannel(
            () -> AsynchronousFileChannel.open(path, StandardOpenOption.READ),
            factory,
            BUFFER_SIZE);
    return DataBufferUtils.join(channel);
  }
}
