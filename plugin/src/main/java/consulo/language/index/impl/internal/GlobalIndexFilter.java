package consulo.language.index.impl.internal;

import consulo.index.io.IndexId;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/** Local stub. Consulo's internal GlobalIndexFilter is not exported; redeclared here for compile-time only. */
public interface GlobalIndexFilter {
    boolean isExcludedFromIndex(@Nonnull VirtualFile virtualFile, @Nonnull IndexId<?, ?> indexId);
    boolean affectsIndex(@Nonnull IndexId<?, ?> indexId);
    int getVersion();
}
