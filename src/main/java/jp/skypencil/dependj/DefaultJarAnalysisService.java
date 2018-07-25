package jp.skypencil.dependj;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import jp.skypencil.dependj.io.PathToData;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
class DefaultJarAnalysisService implements JarAnalysisService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @NonNull private final PathToData pathToData;

  @Autowired
  DefaultJarAnalysisService(@NonNull PathToData pathToData) {
    this.pathToData = Preconditions.checkNotNull(pathToData);
  }

  @Override
  public Mono<AnalysisResult> analyse(Path jar) {
    Preconditions.checkNotNull(jar);
    FileSystem fs;
    try {
      // create ZipFileSystem
      // https://docs.oracle.com/javase/7/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
      fs = FileSystems.newFileSystem(jar, null);
    } catch (IOException e) {
      return Mono.error(e);
    }

    DependencyAnalysisVisitor visitor = new DependencyAnalysisVisitor(Opcodes.ASM6);
    return Flux.fromIterable(fs.getRootDirectories())
        .publishOn(Schedulers.parallel())
        .flatMap(this::walk)
        .flatMap(pathToData::read)
        .map(DataBuffer::asByteBuffer)
        .map(ByteBuffer::array)
        .map(ClassReader::new)
        .map(
            reader -> {
              reader.accept(visitor, 0);
              return Void.TYPE;
            })
        .reduce((a, b) -> a)
        .map(a -> visitor.getAnalysisResult())
        .doAfterTerminate(close(fs));
  }

  private Runnable close(FileSystem fs) {
    return () -> {
      try {
        fs.close();
      } catch (IOException e) {
        logger.warn("failed to close FileSystem", e);
      }
    };
  }

  private Flux<Path> walk(@NonNull Path root) {
    return Flux.create(
        emitter -> {
          try {
            Files.walkFileTree(
                root,
                new SimpleFileVisitor<Path>() {
                  @Override
                  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && file.getFileName().endsWith(".class")) {
                      emitter.next(file);
                    }
                    return FileVisitResult.CONTINUE;
                  }

                  @Override
                  public FileVisitResult visitFileFailed(Path file, IOException e) {
                    emitter.error(e);
                    return FileVisitResult.CONTINUE;
                  }
                });
          } catch (IOException e) {
            emitter.error(e);
          }
          emitter.complete();
        });
  }
}
