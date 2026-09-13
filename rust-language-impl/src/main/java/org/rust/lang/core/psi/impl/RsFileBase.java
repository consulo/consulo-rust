/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import consulo.language.impl.psi.PsiFileBase;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.file.FileViewProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsFileType;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.ext.RsInnerAttributeOwner;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.psi.*;

/**
 * Base class for Rust PSI files. Fixes getOriginalFile() behavior for code fragments.
 */
public abstract class RsFileBase extends PsiFileBase implements RsInnerAttributeOwner {

    protected RsFileBase(@Nonnull FileViewProvider fileViewProvider) {
        super(fileViewProvider, RsLanguage.INSTANCE);
    }

    @Nullable
    public RsReference getReference() {
        return null;
    }

    @Nonnull
    @Override
    public RsFileBase getOriginalFile() {
        return (RsFileBase) super.getOriginalFile();
    }

    @Nonnull
    @Override
    public FileType getFileType() {
        return RsFileType.INSTANCE;
    }

    @Nullable
    public RsFileStub getStub() {
        return (RsFileStub) super.getStub();
    }

    @Nullable
    @Override
    public org.rust.lang.core.psi.ext.RsMod getCrateRoot() {
        return org.rust.lang.core.psi.ext.impl.RsElementUtil.getCrateRoot(this);
    }

    @Nonnull
    @Override
    public org.rust.lang.core.crate.Crate getContainingCrate() {
        return org.rust.lang.core.psi.ext.impl.RsElementUtil.getContainingCrate(this);
    }
}
