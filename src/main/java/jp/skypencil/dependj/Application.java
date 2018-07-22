package jp.skypencil.dependj;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import jp.skypencil.dependj.formatter.DotFormatter;
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
  @Autowired private DotFormatter dotFormatter;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Collection<File> directories = toDirectory(args.getNonOptionArgs());
    AnalysisResult result = analysisService.analyse(directories).block();
    output(args, System.out).accept(result);
  }

  private Collection<File> toDirectory(List<String> args) {
    return args.stream().map(File::new).filter(File::isDirectory).collect(Collectors.toList());
  }

  private Consumer<AnalysisResult> output(ApplicationArguments args, PrintStream stdout)
      throws IOException {
    if (args.containsOption("dot")) {
      return result -> {
        try (OutputStream stream = createOutputStream(args, stdout)) {
          dotFormatter.output(result, stream);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      };
    }
    return result -> {
      // do nothing
    };
  }

  private OutputStream createOutputStream(ApplicationArguments args, PrintStream stdout)
      throws IOException {
    final OutputStream stream;
    List<String> options = args.getOptionValues("dot");
    if (!options.isEmpty()) {
      Path path = Paths.get(options.get(0));
      stream = Files.newOutputStream(path, StandardOpenOption.CREATE);
    } else {
      stream = new UnclosableOutputStream(stdout);
      for (int i = 1; i < options.size(); ++i) {
        System.err.printf(
            "dot option got multiple values but only first one is used, so this value is ignored: %s%n",
            options.get(i));
      }
    }
    return stream;
  }

  private static class UnclosableOutputStream extends FilterOutputStream {
    UnclosableOutputStream(OutputStream out) {
      super(out);
    }

    @Override
    public void close() {
      // do nothing
    }
  }
}
