/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsNamedElement;

import java.util.*;

/**
 * Code fragment used in the Rust REPL console.
 */
public class RsReplCodeFragment extends RsFile {

    @Nullable
    private PsiElement context;

    public RsReplCodeFragment(com.intellij.psi.FileViewProvider viewProvider) {
        super(viewProvider);
    }

    @NotNull
    public List<RsStmt> getStmtList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, RsStmt.class);
    }

    @NotNull
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
