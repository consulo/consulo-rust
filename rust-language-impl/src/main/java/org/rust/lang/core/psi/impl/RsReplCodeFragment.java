/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsNamedElement;

import java.util.*;
import org.rust.lang.core.psi.*;

/**
 * Code fragment used in the Rust REPL console.
 */
public class RsReplCodeFragment extends RsFile {

    @Nullable
    private PsiElement context;

    public RsReplCodeFragment(consulo.language.file.FileViewProvider viewProvider) {
        super(viewProvider);
    }

    @Nonnull
    public List<RsStmt> getStmtList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, RsStmt.class);
    }

    @Nonnull
    public Map<String, RsNamedElement> getNamedElementsUnique() {
        Map<String, RsNamedElement> result = new LinkedHashMap<>();
        for (PsiElement child : getChildren()) {
            if (child instanceof RsNamedElement) {
                String name = ((RsNamedElement) child).getName();
                if (name != null) {
                    result.put(name, (RsNamedElement) child);
                }
            }
        }
        return result;
    }

    public void setContext(@Nullable PsiElement context) {
        this.context = context;
    }

    @Override
    @Nullable
    public PsiElement getContext() {
        return context;
    }
}
