package com.intellij.psi.stubs;

import consulo.language.psi.stub.Stub;
import consulo.virtualFileSystem.VirtualFile;

/** Supplies a pre-built stub tree for a file. */
public interface PrebuiltStubsProvider {
    Stub findStub(VirtualFile file);
}
