package jp.skypencil.dependj;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DefaultMainAnalysisServiceTest {
  @Test
  void smokeTest() throws Exception {
    DefaultMainAnalysisService service =
        new DefaultMainAnalysisService(
            new DefaultJarAnalysisService(), new DefaultClassAnalysisService());
    Path lib =
        Paths.get(
            System.getProperty("user.home"), ".m2", "repository", "com", "github", "spotbugs");
    Path target = Paths.get("build", "classes", "java");
    service.analyse(Arrays.asList(lib.toFile(), target.toFile())).subscribe();
  }
}
