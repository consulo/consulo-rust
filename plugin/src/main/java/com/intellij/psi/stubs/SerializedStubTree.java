package com.intellij.psi.stubs;

import consulo.language.psi.stub.Stub;

/** Minimal stub of IJ's SerializedStubTree. */
public final class SerializedStubTree {
    private final byte[] bytes;
    private final Stub stub;

    public SerializedStubTree(byte[] bytes, Stub stub) {
        this.bytes = bytes;
        this.stub = stub;
    }

    public byte[] getTreeBytes() { return bytes; }
    public Stub getStub() { return stub; }
    public Stub getStub(boolean ignored) { return stub; }
}
