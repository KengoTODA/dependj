package jp.skypencil.dependj;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
class DefaultMainAnalysisService implements MainAnalysisService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private ClassAnalysisService classAnalysis;
  private JarAnalysisService jarAnalysis;

  @Autowired
  DefaultMainAnalysisService(
      @NonNull JarAnalysisService jarAnalysis, @NonNull ClassAnalysisService classAnalysis) {
    this.jarAnalysis = Preconditions.checkNotNull(jarAnalysis);
    this.classAnalysis = Preconditions.checkNotNull(classAnalysis);
  }

  @Override
  public Mono<AnalysisResult> analyse(Collection<File> directories) {
    Flux<AnalysisResult> jarFiles =
        Flux.fromIterable(directories)
            .publishOn(Schedulers.parallel())
            .flatMap(this::searchJarFiles)
            .doOnNext(file -> logger.trace("analysing jar file: {}", file))
            .map(File::toPath)
            .flatMap(jarAnalysis::analyse);
    Mono<AnalysisResult> classFiles =
        Flux.fromIterable(directories)
            .publishOn(Schedulers.parallel())
            .flatMap(this::searchClassFiles)
            .doOnNext(file -> logger.trace("analysing class file: {}", file))
            .reduce(
                new ArrayList<File>(),
                (list, file) -> {
                  list.add(file);
                  return list;
                })
            .map(list -> list.toArray(new File[0]))
            .flatMap(classAnalysis::analyse);
    return Flux.merge(jarFiles, classFiles).reduce(AnalysisResult::merge);
  }

  @NonNull
  private Flux<File> searchClassFiles(@NonNull File directory) {
    Preconditions.checkNotNull(directory);
    List<File> classFiles = new ArrayList<>();

    try {
      Files.walkFileTree(
          directory.toPath(),
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                throws IOException {
              File file = path.toFile();
              if (file.getName().endsWith(".class") && file.isFile()) {
                classFiles.add(file);
              }
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      return Flux.error(e);
    }
    return Flux.fromIterable(classFiles);
  }

  @NonNull
  private Flux<File> searchJarFiles(@NonNull File directory) {
    Preconditions.checkNotNull(directory);
    List<File> jarFiles = new ArrayList<>();

    try {
      Files.walkFileTree(
          directory.toPath(),
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                throws IOException {
              File file = path.toFile();
              String name = file.getName();
              if (name.endsWith(".jar")
                  && !name.endsWith("-sources.jar")
                  && !name.endsWith("-javadoc.jar")
                  && file.isFile()) {
                jarFiles.add(file);
              }
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      return Flux.error(e);
    }
    return Flux.fromIterable(jarFiles);
  }
}
