package jp.skypencil.dependj;

import java.nio.file.Path;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

interface JarAnalysisService {
  @NonNull
  Mono<AnalysisResult> analyse(Path jar);
}
