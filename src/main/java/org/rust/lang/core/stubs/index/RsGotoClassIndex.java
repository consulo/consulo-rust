/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.index;

import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.stubs.RsFileStub;

public class RsGotoClassIndex extends StringStubIndexExtension<RsNamedElement> {
    @NotNull
    public static final StubIndexKey<String, RsNamedElement> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.stubs.index.RustGotoClassIndex");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @NotNull
    @Override
    public StubIndexKey<String, RsNamedElement> getKey() {
        return KEY;
    }
}
