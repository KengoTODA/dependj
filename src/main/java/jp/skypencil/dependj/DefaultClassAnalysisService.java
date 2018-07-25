package jp.skypencil.dependj;

import com.google.common.base.Preconditions;
import java.io.File;
import java.nio.ByteBuffer;
import jp.skypencil.dependj.io.PathToData;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
class DefaultClassAnalysisService implements ClassAnalysisService {
  @NonNull private final PathToData pathToData;

  @Autowired
  DefaultClassAnalysisService(@NonNull PathToData pathToData) {
    this.pathToData = Preconditions.checkNotNull(pathToData);
  }

  @Override
  public Mono<AnalysisResult> analyse(File... files) {
    Preconditions.checkNotNull(files);
    DependencyAnalysisVisitor visitor = new DependencyAnalysisVisitor(Opcodes.ASM6);
    return Flux.fromArray(files)
        .publishOn(Schedulers.parallel())
        .filter(File::isFile)
        .filter(file -> file.getName().endsWith(".class"))
        .map(File::toPath)
        .flatMap(pathToData::read)
        .map(DataBuffer::asByteBuffer)
        .map(ByteBuffer::array)
        .map(ClassReader::new)
        .map(
            reader -> {
              reader.accept(visitor, 0);
              return Void.TYPE;
            })
        .reduce((a, b) -> a)
        .map(a -> visitor.getAnalysisResult());
  }
}
