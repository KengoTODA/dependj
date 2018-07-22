package jp.skypencil.dependj.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Multimaps;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import jp.skypencil.dependj.ImmutableAnalysisResult;
import org.junit.jupiter.api.Test;

class DotFormatterTest {
  @Test
  void testDotFormat() throws IOException {
    try (ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(byteArray)) {
      ImmutableAnalysisResult result =
          ImmutableAnalysisResult.builder()
              .dependencies(
                  Multimaps.forMap(Collections.singletonMap("com.example", "jp.skypencil")))
              .build();
      new DotFormatter().output(result, stream);
      String expected =
          String.format(
              "digraph dependency {%n" + "  \"com.example\" -> \"jp.skypencil\";%n" + "}%n");
      assertEquals(expected, byteArray.toString());
    }
  }
}
