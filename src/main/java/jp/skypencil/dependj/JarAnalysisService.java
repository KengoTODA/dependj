package jp.skypencil.dependj;

import java.util.jar.JarFile;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

interface JarAnalysisService {
  @NonNull
  Mono<AnalysisResult> analyse(JarFile file);
}
