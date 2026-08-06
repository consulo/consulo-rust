/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import consulo.language.psi.PsiReference;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public class RsPathUsageInfo extends RsMoveUsageInfo {

    @Nonnull
    private final RsPath element;
    @Nonnull
    private final PsiReference rsReference;
    @Nonnull
    private final RsQualifiedNamedElement target;
    @Nonnull
    private RsMoveReferenceInfo referenceInfo;

    public RsPathUsageInfo(@Nonnull RsPath element, @Nonnull PsiReference rsReference, @Nonnull RsQualifiedNamedElement target) {
        super(element);
        this.element = element;
        this.rsReference = rsReference;
        this.target = target;
    }

    @Nonnull
    public RsPath getElement() {
        return element;
    }

    @Nonnull
    public RsQualifiedNamedElement getTarget() {
        return target;
    }

    @Nonnull
    public RsMoveReferenceInfo getReferenceInfo() {
        return referenceInfo;
    }

    public void setReferenceInfo(@Nonnull RsMoveReferenceInfo referenceInfo) {
        this.referenceInfo = referenceInfo;
    }

    @Override
    public PsiReference getReference() {
        return rsReference;
    }
}
