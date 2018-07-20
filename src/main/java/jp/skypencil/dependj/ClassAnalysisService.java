package jp.skypencil.dependj;

import java.io.File;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

interface ClassAnalysisService {
  @NonNull
  Mono<AnalysisResult> analyse(File... files);
}
