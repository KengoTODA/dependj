package jp.skypencil.dependj;

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import jp.skypencil.dependj.io.JarToClass;
import jp.skypencil.dependj.io.PathToData;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
class DefaultJarAnalysisService implements JarAnalysisService {
  @NonNull private final JarToClass jarToClass;
  @NonNull private final PathToData pathToData;

  @Autowired
  DefaultJarAnalysisService(@NonNull PathToData pathToData, @NonNull JarToClass jarToClass) {
    this.pathToData = Preconditions.checkNotNull(pathToData);
    this.jarToClass = Preconditions.checkNotNull(jarToClass);
  }

  @Override
  public Mono<AnalysisResult> analyse(Path jar) {
    Preconditions.checkNotNull(jar);

    DependencyAnalysisVisitor visitor = new DependencyAnalysisVisitor(Opcodes.ASM6);
    return jarToClass
        .parse(jar)
        .publishOn(Schedulers.parallel())
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
