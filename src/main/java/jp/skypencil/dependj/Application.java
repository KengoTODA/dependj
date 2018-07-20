package jp.skypencil.dependj;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application implements ApplicationRunner {
  public static void main(String... args) {
    SpringApplication.run(Application.class, args);
  }

  @Autowired private MainAnalysisService analysisService;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Collection<File> directories = toDirectory(args.getNonOptionArgs());
    analysisService.analyse(directories).subscribe(System.out::println, Throwable::printStackTrace);
  }

  private Collection<File> toDirectory(List<String> args) {
    return args.stream().map(File::new).filter(File::isDirectory).collect(Collectors.toList());
  }
}
