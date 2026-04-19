/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsNamedFieldDecl;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsPatField;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.resolve.NameResolution;

import java.util.List;

public class RsPatBindingReferenceImpl extends RsReferenceCached<RsPatBinding> {

    public RsPatBindingReferenceImpl(@NotNull RsPatBinding element) {
        super(element);
    }

    @NotNull
    @Override
    protected List<RsElement> resolveInner() {
        return NameResolution.collectResolveVariants(getElement().getReferenceName(), processor ->
            NameResolution.processPatBindingResolveVariants(getElement(), false, processor)
        );
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        if (!(element instanceof RsElement)) return false;
        if (!RsPsiJavaUtil.isConstantLike(element) && !(element instanceof RsNamedFieldDecl)) return false;
        return super.isReferenceTo(element);
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newName) {
        if (!(getElement().getParent() instanceof RsPatField)) {
            return super.handleElementRename(newName);
        }
        RsPsiFactory psiFactory = new RsPsiFactory(getElement().getProject(), true, false);
        PsiElement newPatField = psiFactory.createPatFieldFull(newName, getElement().getText());
        return getElement().replace(newPatField);
    }
}
