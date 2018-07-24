package jp.skypencil.dependj;

import com.google.common.base.Preconditions;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;
import java.util.function.Function;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
class DefaultClassAnalysisService implements ClassAnalysisService {
  private final DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();

  @Override
  public Mono<AnalysisResult> analyse(File... files) {
    Preconditions.checkNotNull(files);
    DependencyAnalysisVisitor visitor = new DependencyAnalysisVisitor(Opcodes.ASM6);
    return Flux.fromArray(files)
        .publishOn(Schedulers.parallel())
        .filter(File::isFile)
        .filter(file -> file.getName().endsWith(".class"))
        .flatMap(open())
        .map(DataBuffer::asByteBuffer)
        .map(ByteBuffer::array)
        .map(ClassReader::new)
        .map(
            reader -> {
              reader.accept(visitor, 0);
              return Void.TYPE;
            })
        .reduce((a, b) -> a)
        .map(a -> visitor.getAnalysisResult());
  }

  private Function<File, Mono<DataBuffer>> open() {
    return file -> {
      Flux<DataBuffer> channel =
          DataBufferUtils.readAsynchronousFileChannel(
              () -> AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ),
              dataBufferFactory,
              4 * 1024);
      return DataBufferUtils.join(channel);
    };
  }
}
