package jp.skypencil.dependj;

import java.io.File;
import java.util.Collection;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

interface MainAnalysisService {
  @NonNull
  Mono<AnalysisResult> analyse(@NonNull Collection<File> directories);
}
