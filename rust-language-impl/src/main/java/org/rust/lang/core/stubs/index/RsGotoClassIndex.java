/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.index;


import consulo.language.psi.stub.StringStubIndexExtension;
import consulo.language.psi.stub.StubIndexKey;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.stubs.RsFileStub;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsGotoClassIndex extends StringStubIndexExtension<RsNamedElement> {
    @Nonnull
    public static final StubIndexKey<String, RsNamedElement> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.stubs.index.RustGotoClassIndex");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @Nonnull
    @Override
    public StubIndexKey<String, RsNamedElement> getKey() {
        return KEY;
    }
}
