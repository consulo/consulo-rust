/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsFileType;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.ext.RsInnerAttributeOwner;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.stubs.RsFileStub;

/**
 * Base class for Rust PSI files. Fixes getOriginalFile() behavior for code fragments.
 */
public abstract class RsFileBase extends PsiFileBase implements RsInnerAttributeOwner {

    protected RsFileBase(@NotNull FileViewProvider fileViewProvider) {
        super(fileViewProvider, RsLanguage.INSTANCE);
    }

    @Nullable
    public RsReference getReference() {
        return null;
    }

    @NotNull
    @Override
    public RsFileBase getOriginalFile() {
        return (RsFileBase) super.getOriginalFile();
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return RsFileType.INSTANCE;
    }

    @Nullable
    public RsFileStub getStub() {
        return (RsFileStub) super.getStub();
    }
}
