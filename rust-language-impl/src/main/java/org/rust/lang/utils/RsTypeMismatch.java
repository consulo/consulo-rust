/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.infer.TypeFoldable;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.ty.Ty;

/**
 * An element whose inferred type does not match the type expected at its position.
 * <p>
 * Both types may still contain inference variables when the mismatch is reported, so the diagnostic
 * is foldable: type inference resolves the types once the containing function is fully inferred.
 */
public final class RsTypeMismatch implements RsInferenceDiagnostic, TypeFoldable<RsTypeMismatch> {
    @Nonnull
    private final PsiElement myElement;
    @Nonnull
    private final Ty myExpectedTy;
    @Nonnull
    private final Ty myActualTy;

    public RsTypeMismatch(@Nonnull PsiElement element, @Nonnull Ty expectedTy, @Nonnull Ty actualTy) {
        myElement = element;
        myExpectedTy = expectedTy;
        myActualTy = actualTy;
    }

    @Nonnull
    public PsiElement getElement() {
        return myElement;
    }

    @Nonnull
    public Ty getExpectedTy() {
        return myExpectedTy;
    }

    @Nonnull
    public Ty getActualTy() {
        return myActualTy;
    }

    @Nonnull
    @Override
    public RsTypeMismatch superFoldWith(@Nonnull TypeFolder folder) {
        return new RsTypeMismatch(myElement, myExpectedTy.foldWith(folder), myActualTy.foldWith(folder));
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        return myExpectedTy.visitWith(visitor) || myActualTy.visitWith(visitor);
    }
}
