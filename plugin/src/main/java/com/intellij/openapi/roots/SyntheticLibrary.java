package com.intellij.openapi.roots;
import consulo.virtualFileSystem.VirtualFile;
import java.util.Collection;
import java.util.Collections;
/** IntelliJ-compat stub. Consulo handles synthetic libraries via module extensions. */
public abstract class SyntheticLibrary {
    public abstract Collection<VirtualFile> getSourceRoots();
    public Collection<VirtualFile> getBinaryRoots() { return Collections.emptyList(); }
    public Collection<VirtualFile> getExcludedRoots() { return Collections.emptyList(); }
}
