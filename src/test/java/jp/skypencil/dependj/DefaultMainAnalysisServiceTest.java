package jp.skypencil.dependj;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import jp.skypencil.dependj.io.PathToData;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Mono;

class DefaultMainAnalysisServiceTest {
  @Test
  void smokeTest() throws Exception {
    PathToData pathToData = this::read;
    DefaultMainAnalysisService service =
        new DefaultMainAnalysisService(
            new DefaultJarAnalysisService(pathToData), new DefaultClassAnalysisService(pathToData));
    Path lib =
        Paths.get(
            System.getProperty("user.home"), ".m2", "repository", "com", "github", "spotbugs");
    Path target = Paths.get("build", "classes", "java");
    service.analyse(Arrays.asList(lib.toFile(), target.toFile())).subscribe();
  }

  private DataBufferFactory factory = new DefaultDataBufferFactory();

  Mono<DataBuffer> read(Path path) {
    try {
      DataBuffer buffer = factory.wrap(Files.readAllBytes(path));
      return Mono.just(buffer);
    } catch (IOException e) {
      return Mono.error(e);
    }
  }
}
