/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public class RsPathUsageInfo extends RsMoveUsageInfo {

    @NotNull
    private final RsPath element;
    @NotNull
    private final PsiReference rsReference;
    @NotNull
    private final RsQualifiedNamedElement target;
    @NotNull
    private RsMoveReferenceInfo referenceInfo;

    public RsPathUsageInfo(@NotNull RsPath element, @NotNull PsiReference rsReference, @NotNull RsQualifiedNamedElement target) {
        super(element);
        this.element = element;
        this.rsReference = rsReference;
        this.target = target;
    }

    @NotNull
    public RsPath getElement() {
        return element;
    }

    @NotNull
    public RsQualifiedNamedElement getTarget() {
        return target;
    }

    @NotNull
    public RsMoveReferenceInfo getReferenceInfo() {
        return referenceInfo;
    }

    public void setReferenceInfo(@NotNull RsMoveReferenceInfo referenceInfo) {
        this.referenceInfo = referenceInfo;
    }

    @Override
    public PsiReference getReference() {
        return rsReference;
    }
}
