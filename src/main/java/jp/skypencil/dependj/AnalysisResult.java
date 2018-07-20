package jp.skypencil.dependj;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.immutables.value.Value;
import org.springframework.lang.NonNull;

@Value.Immutable
interface AnalysisResult {
  @NonNull
  Map<String, Integer> classCount();

  @NonNull
  Map<String, Integer> abstractClassCount();

  @NonNull
  Map<String, Integer> interfaceCount();

  @NonNull
  Multimap<String, String> dependencies();

  @NonNull
  default AnalysisResult merge(@NonNull AnalysisResult another) {
    Preconditions.checkNotNull(another);

    Multimap<String, String> mergedDependencies = HashMultimap.create();
    mergedDependencies.putAll(this.dependencies());
    mergedDependencies.putAll(another.dependencies());
    return ImmutableAnalysisResult.builder()
        .abstractClassCount(merge(this.abstractClassCount(), another.abstractClassCount()))
        .classCount(merge(this.classCount(), another.classCount()))
        .interfaceCount(merge(this.interfaceCount(), another.interfaceCount()))
        .dependencies(mergedDependencies)
        .build();
  }

  @NonNull
  static Map<String, Integer> merge(
      @NonNull Map<String, Integer> a, @NonNull Map<String, Integer> b) {
    return Stream.of(a.entrySet(), b.entrySet())
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum));
  }
}
