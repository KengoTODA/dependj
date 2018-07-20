package jp.skypencil.dependj;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ApplicationTest {
  @Test
  public void test(ApplicationContext context) {
    assertNotNull(context);
  }

  @Test
  void smokeTest(ApplicationContext context) throws Exception {
    Application application = context.getBean(Application.class);
    application.run(searchJarFiles().toArray(new String[0]));
  }

  private Collection<String> searchJarFiles() throws IOException {
    Set<String> jarFiles = new HashSet<>();
    Path lib =
        Paths.get(
            System.getProperty("user.home"), ".m2", "repository", "com", "github", "spotbugs");
    Files.walkFileTree(
        lib,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            String name = file.getFileName().toString();
            if (name.endsWith(".jar")
                && !name.endsWith("-sources.jar")
                && !name.endsWith("-javadoc.jar")) {
              jarFiles.add(file.toString());
            }
            return FileVisitResult.CONTINUE;
          }
        });
    return jarFiles;
  }
}
