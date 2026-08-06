package com.intellij.psi.stubs;

import consulo.language.psi.stub.Stub;
import consulo.virtualFileSystem.VirtualFile;

/** Stub — Consulo has no prebuilt-stubs mechanism. */
public interface PrebuiltStubsProvider {
    Stub findStub(VirtualFile file);
}
