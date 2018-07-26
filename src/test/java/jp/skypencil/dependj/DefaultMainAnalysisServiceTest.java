package jp.skypencil.dependj;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class DefaultMainAnalysisServiceTest {
  @Test
  void smokeTest(@Autowired DefaultMainAnalysisService service) throws Exception {
    Path lib =
        Paths.get(
            System.getProperty("user.home"), ".m2", "repository", "com", "github", "spotbugs");
    Path target = Paths.get("build", "classes", "java");
    service.analyse(Arrays.asList(lib.toFile(), target.toFile())).block();
  }
}
