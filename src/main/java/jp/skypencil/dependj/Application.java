package jp.skypencil.dependj;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;
import java.util.jar.JarFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
public class Application implements CommandLineRunner {
  public static void main(String... args) {
    SpringApplication.run(Application.class, args);
  }

  @Autowired private JarAnalysisService jarAnalysis;

  @Override
  public void run(String... args) throws Exception {
    Flux.fromArray(args)
        .filter(name -> name.endsWith(".jar"))
        .map(File::new)
        .filter(File::isFile)
        .doOnNext(file -> System.err.printf("analysing jar file: %s%n", file))
        .flatMap(open())
        .flatMap(jarAnalysis::analyse)
        .reduce(AnalysisResult::merge)
        .subscribeOn(Schedulers.parallel())
        .subscribe(System.out::println, Throwable::printStackTrace);
  }

  private Function<File, Mono<JarFile>> open() {
    return file -> {
      try {
        return Mono.just(new JarFile(file));
      } catch (IOException e) {
        return Mono.error(e);
      }
    };
  }
}
