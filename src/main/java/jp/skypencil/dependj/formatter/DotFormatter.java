package jp.skypencil.dependj.formatter;

import java.io.OutputStream;
import java.io.PrintStream;
import javax.annotation.WillClose;
import jp.skypencil.dependj.AnalysisResult;
import org.springframework.stereotype.Service;

@Service
public class DotFormatter {
  public void output(AnalysisResult result, @WillClose OutputStream stream) {
    try (PrintStream output = new PrintStream(stream)) {
      output.println("digraph dependency {");
      result
          .dependencies()
          .forEach(
              (from, to) -> {
                output.printf("  \"%s\" -> \"%s\";%n", from, to);
              });
      output.println("}");
    }
  }
}
