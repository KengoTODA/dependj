package jp.skypencil.dependj.io;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
class LazyJarToClass implements JarToClass {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public Flux<Path> parse(Path jar) {
    FileSystem fs;
    try {
      // create ZipFileSystem
      // https://docs.oracle.com/javase/7/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
      fs = FileSystems.newFileSystem(jar, null);
    } catch (IOException e) {
      return Flux.error(e);
    }

    return Flux.fromIterable(fs.getRootDirectories())
        .doAfterTerminate(close(fs))
        .flatMap(this::rootToChildren);
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

  private Flux<Path> rootToChildren(Path root) {
    return Flux.create(
        emitter -> {
          try (Stream<Path> children =
              Files.find(
                  root,
                  Integer.MAX_VALUE,
                  (path, attr) -> {
                    return attr.isRegularFile() && path.toString().endsWith(".class");
                  })) {
            children.forEach(emitter::next);
          } catch (IOException e) {
            emitter.error(e);
          }
          emitter.complete();
        });
  }
}
